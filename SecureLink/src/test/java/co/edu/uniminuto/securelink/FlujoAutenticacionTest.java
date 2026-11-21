package co.edu.uniminuto.securelink;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integracion de la API REST: login, rutas protegidas, roles, logout, SQLite y
 * visibilidad didactica por rol. Cada ejecucion usa su propia base SQLite temporal.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite:target/test-${random.uuid}.db?foreign_keys=on",
        "securelink.seguridad.bcrypt-costo=4"
})
@AutoConfigureMockMvc
class FlujoAutenticacionTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcClient jdbc;

    private MockHttpSession login(String usuario, String clave) throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"" + usuario + "\",\"contrasena\":\"" + clave + "\"}"))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) r.getRequest().getSession(false);
    }

    private String operar(MockHttpSession s, String body) throws Exception {
        return mvc.perform(post("/api/didactica/operar").with(csrf()).session(s)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
    }

    // (a) Inicio de sesion correcto y redireccion segun el rol.
    @Test
    void loginCorrectoIndicaLaRutaSegunElRol() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"admin\",\"contrasena\":\"Admin123*\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("admin"))
                .andExpect(jsonPath("$.redirigirA").value("/admin"));
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"usuario\",\"contrasena\":\"Usuario123*\"}"))
                .andExpect(jsonPath("$.redirigirA").value("/usuario"));
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"invitado\",\"contrasena\":\"Invitado123*\"}"))
                .andExpect(jsonPath("$.redirigirA").value("/invitado"));
    }

    // (b) Inicio de sesion fallido: mismo mensaje para usuario inexistente y clave incorrecta.
    @Test
    void loginFallidoDevuelve401ConMensajeGenerico() throws Exception {
        for (String cuerpo : new String[]{
                "{\"usuario\":\"admin\",\"contrasena\":\"mala\"}",
                "{\"usuario\":\"noexiste\",\"contrasena\":\"Admin123*\"}"}) {
            mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("Usuario o contraseña incorrectos"));
        }
    }

    // (c) Sin sesion no se entra a rutas protegidas.
    @Test
    void rutaProtegidaSinSesionDevuelve401() throws Exception {
        mvc.perform(get("/api/admin/panel")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/usuario/panel")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/didactica/algoritmos")).andExpect(status().isUnauthorized());
    }

    // (d) Un usuario sin rol admin recibe acceso denegado en la ruta de administracion.
    @Test
    void usuarioSinRolAdminRecibe403() throws Exception {
        MockHttpSession usuario = login("usuario", "Usuario123*");
        mvc.perform(get("/api/admin/panel").session(usuario)).andExpect(status().isForbidden());
        MockHttpSession invitado = login("invitado", "Invitado123*");
        mvc.perform(get("/api/admin/panel").session(invitado)).andExpect(status().isForbidden());
        mvc.perform(get("/api/usuario/panel").session(invitado)).andExpect(status().isForbidden());
        MockHttpSession admin = login("admin", "Admin123*");
        mvc.perform(get("/api/admin/panel").session(admin)).andExpect(status().isOk());
    }

    // (e) Tras cerrar sesion hay que autenticarse otra vez.
    @Test
    void trasCerrarSesionSeVuelveAPedirAutenticacion() throws Exception {
        MockHttpSession sesion = login("usuario", "Usuario123*");
        mvc.perform(get("/api/usuario/panel").session(sesion)).andExpect(status().isOk());
        mvc.perform(post("/api/auth/logout").with(csrf()).session(sesion)).andExpect(status().isOk());
        mvc.perform(get("/api/usuario/panel").session(sesion)).andExpect(status().isUnauthorized());
    }

    @Test
    void sinTokenCsrfElLoginSeRechaza() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"admin\",\"contrasena\":\"Admin123*\"}"))
                .andExpect(status().isForbidden());
    }

    // SQLite: solo hay hashes bcrypt, nunca la contrasena.
    @Test
    void laBaseGuardaSoloHashBcrypt() {
        var hashes = jdbc.sql("SELECT hash_contrasena FROM usuarios").query(String.class).list();
        assertThat(hashes).hasSizeGreaterThanOrEqualTo(3);
        assertThat(hashes).allMatch(h -> h.startsWith("$2") && h.length() == 60);
        assertThat(hashes).noneMatch(h -> h.contains("Admin123") || h.contains("Usuario123"));
    }

    @Test
    void registroCreaUsuarioConRolUsuarioYRechazaDuplicados() throws Exception {
        String cuerpo = "{\"usuario\":\"nuevo_test\",\"contrasena\":\"Clave1234*\",\"confirmarContrasena\":\"Clave1234*\","
                + "\"rol\":\"admin\"}";
        mvc.perform(post("/api/auth/registro").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"nuevo_test\",\"contrasena\":\"Clave1234*\"}"))
                .andExpect(jsonPath("$.rol").value("usuario"));
        mvc.perform(post("/api/auth/registro").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/auth/registro").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"otro_test\",\"contrasena\":\"corta\",\"confirmarContrasena\":\"corta\"}"))
                .andExpect(status().isBadRequest());
    }

    // Parte didactica: el detalle depende del rol.
    @Test
    void elDetalleDelLaboratorioDependeDelRol() throws Exception {
        String texto = "\"algoritmo\":\"AES_GCM\",\"operacion\":\"CIFRAR\",\"texto\":\"hola\",\"clave\":\"secreto\"";
        // El invitado no puede ejecutar AES-GCM (nivel 2).
        MockHttpSession invitado = login("invitado", "Invitado123*");
        mvc.perform(post("/api/didactica/operar").with(csrf()).session(invitado)
                        .contentType(MediaType.APPLICATION_JSON).content("{" + texto + "}"))
                .andExpect(status().isForbidden());
        // Con SHA-256 si, pero con los pasos internos ocultos.
        mvc.perform(post("/api/didactica/operar").with(csrf()).session(invitado)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"algoritmo\":\"SHA256\",\"operacion\":\"HASH\",\"texto\":\"hola\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value(
                        "b221d9dbb083a7f33428d7c2a3c3198ae925614d70210e28716ccaa7cd4ddb79"))
                .andExpect(jsonPath("$.pasosOcultos").value(1))
                .andExpect(jsonPath("$.tiempoMicros").doesNotExist());

        // Usuario: ve parametros, valores secretos parciales.
        MockHttpSession usuario = login("usuario", "Usuario123*");
        String cifrado = mvc.perform(post("/api/didactica/operar").with(csrf()).session(usuario)
                        .contentType(MediaType.APPLICATION_JSON).content("{" + texto + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pasos[?(@.visibilidad=='PARCIAL')]").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        assertThat(cifrado).contains("\"pasosOcultos\":0");

        // Admin: todo completo, incluida la clave derivada.
        MockHttpSession admin = login("admin", "Admin123*");
        mvc.perform(post("/api/didactica/operar").with(csrf()).session(admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{" + texto + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pasos[?(@.visibilidad!='COMPLETO')]").isEmpty());
    }

    @Test
    void cifrarYDescifrarFuncionanConTodosLosCifradores() throws Exception {
        MockHttpSession admin = login("admin", "Admin123*");
        for (String alg : new String[]{"AES_GCM", "AES_CBC", "CHACHA20", "RC4", "RSA_OAEP"}) {
            String r = operar(admin, "{\"algoritmo\":\"" + alg + "\",\"operacion\":\"CIFRAR\",\"texto\":\"Hola SecureLink\",\"clave\":\"frase\"}");
            String cifrado = r.replaceAll(".*\"resultado\":\"([^\"]+)\".*", "$1");
            mvc.perform(post("/api/didactica/operar").with(csrf()).session(admin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"algoritmo\":\"" + alg + "\",\"operacion\":\"DESCIFRAR\",\"texto\":\"" + cifrado + "\",\"clave\":\"frase\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultado").value("Hola SecureLink"));
        }
        // Clave incorrecta en AES-GCM: error controlado, no 500.
        String r = operar(admin, "{\"algoritmo\":\"AES_GCM\",\"operacion\":\"CIFRAR\",\"texto\":\"x\",\"clave\":\"a\"}");
        String cifrado = r.replaceAll(".*\"resultado\":\"([^\"]+)\".*", "$1");
        mvc.perform(post("/api/didactica/operar").with(csrf()).session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"algoritmo\":\"AES_GCM\",\"operacion\":\"DESCIFRAR\",\"texto\":\"" + cifrado + "\",\"clave\":\"b\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void hashesDeContrasenasSeVerifican() throws Exception {
        MockHttpSession admin = login("admin", "Admin123*");
        for (String alg : new String[]{"BCRYPT", "ARGON2ID", "PBKDF2"}) {
            String r = operar(admin, "{\"algoritmo\":\"" + alg + "\",\"operacion\":\"HASH\",\"texto\":\"MiClave1\"}");
            String hash = r.replaceAll(".*\"resultado\":\"([^\"]+)\".*", "$1").replace("\\u0024", "$");
            String v = "{\"algoritmo\":\"" + alg + "\",\"operacion\":\"VERIFICAR\",\"texto\":\"%s\",\"referencia\":\"" + hash + "\"}";
            mvc.perform(post("/api/didactica/operar").with(csrf()).session(admin).contentType(MediaType.APPLICATION_JSON)
                            .content(v.formatted("MiClave1")))
                    .andExpect(jsonPath("$.resultado").value("COINCIDE"));
            mvc.perform(post("/api/didactica/operar").with(csrf()).session(admin).contentType(MediaType.APPLICATION_JSON)
                            .content(v.formatted("otra")))
                    .andExpect(jsonPath("$.resultado").value("NO COINCIDE"));
        }
    }

    @Test
    void elAdminVeElHashCompletoYElUsuarioSoloParcial() throws Exception {
        MockHttpSession admin = login("admin", "Admin123*");
        mvc.perform(get("/api/admin/panel").session(admin))
                .andExpect(jsonPath("$.usuarios", hasSize(org.hamcrest.Matchers.greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$.usuarios[0].hash").exists())
                .andExpect(jsonPath("$.usuarios[0].estructura.sal").exists());
        MockHttpSession usuario = login("usuario", "Usuario123*");
        mvc.perform(get("/api/usuario/panel").session(usuario))
                .andExpect(jsonPath("$.miHash.sal", containsString("ocultos")))
                .andExpect(jsonPath("$.miHash.costo").value(4));
    }
}
