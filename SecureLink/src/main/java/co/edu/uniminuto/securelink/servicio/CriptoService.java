package co.edu.uniminuto.securelink.servicio;

import co.edu.uniminuto.securelink.controlador.ErroresApi.SolicitudInvalida;
import co.edu.uniminuto.securelink.repositorio.CatalogoRepositorio;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

/**
 * Operaciones criptograficas del laboratorio. Cada operacion devuelve el resultado y una lista de
 * pasos; cada paso indica desde que nivel de rol se ve completo (ver {@link Visibilidad}).
 */
@Service
public class CriptoService {

    public record Paso(String titulo, String valor, int nivelCompleto) {
    }

    public record Resultado(String salida, List<Paso> pasos) {
    }

    private static final int MAX_TEXTO = 1000;
    private static final int ITERACIONES_PBKDF2 = 120_000;
    private static final HexFormat HEX = HexFormat.of();

    /** Parametros de cada cifrador simetrico. */
    private record Simetrico(String transformacion, String algoritmoClave, int bitsClave, int bytesIv, String modo) {
    }

    private final SecureRandom azar = new SecureRandom();
    private final CatalogoRepositorio catalogo;
    private final PasswordEncoder bcrypt = new BCryptPasswordEncoder(10);
    private final PasswordEncoder argon2 = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();

    public CriptoService(CatalogoRepositorio catalogo) {
        this.catalogo = catalogo;
    }

    public Resultado ejecutar(String algoritmo, String operacion, String texto, String clave, String referencia) {
        if (texto == null || texto.isEmpty()) {
            throw new SolicitudInvalida("Escribe un texto de entrada.");
        }
        if (texto.length() > MAX_TEXTO) {
            throw new SolicitudInvalida("El texto no puede superar " + MAX_TEXTO + " caracteres.");
        }
        try {
            return switch (algoritmo) {
                case "BASE64" -> base64(operacion, texto);
                case "SHA256" -> resumen("SHA-256", operacion, texto, referencia);
                case "MD5" -> resumen("MD5", operacion, texto, referencia);
                case "HMAC_SHA256" -> hmac(operacion, texto, clave, referencia);
                case "BCRYPT" -> adaptativo("bcrypt", bcrypt, operacion, texto, referencia);
                case "ARGON2ID" -> adaptativo("Argon2id", argon2, operacion, texto, referencia);
                case "PBKDF2" -> pbkdf2(operacion, texto, referencia);
                case "RC4" -> simetrico(new Simetrico("ARCFOUR", "ARCFOUR", 128, 0, "Flujo (sin IV)"), operacion, texto, clave);
                case "AES_CBC" -> simetrico(new Simetrico("AES/CBC/PKCS5Padding", "AES", 128, 16, "CBC + PKCS5"), operacion, texto, clave);
                case "AES_GCM" -> simetrico(new Simetrico("AES/GCM/NoPadding", "AES", 256, 12, "GCM (AEAD)"), operacion, texto, clave);
                case "CHACHA20" -> simetrico(new Simetrico("ChaCha20-Poly1305", "ChaCha20", 256, 12, "Poly1305 (AEAD)"), operacion, texto, clave);
                case "RSA_OAEP" -> rsa(operacion, texto);
                default -> throw new SolicitudInvalida("Algoritmo no soportado.");
            };
        } catch (GeneralSecurityException e) {
            throw new SolicitudInvalida("No se pudo completar la operación: clave incorrecta o datos alterados.");
        }
    }

    // ---------------------------------------------------------------- codificacion

    private Resultado base64(String op, String texto) {
        List<Paso> pasos = new ArrayList<>();
        if ("CODIFICAR".equals(op)) {
            byte[] bytes = texto.getBytes(StandardCharsets.UTF_8);
            String salida = Base64.getEncoder().encodeToString(bytes);
            pasos.add(new Paso("Tamaño", bytes.length + " bytes → " + salida.length() + " caracteres", 1));
            pasos.add(new Paso("Bytes UTF-8 (hex)", HEX.formatHex(bytes), 2));
            pasos.add(new Paso("Primeros 3 bytes en bits y grupos de 6", grupos6(bytes), 3));
            pasos.add(new Paso("Aviso", "Base64 NO cifra: cualquiera lo revierte sin clave.", 1));
            return new Resultado(salida, pasos);
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(texto.trim());
        } catch (IllegalArgumentException e) {
            throw new SolicitudInvalida("El texto no es Base64 válido.");
        }
        pasos.add(new Paso("Tamaño", texto.trim().length() + " caracteres → " + bytes.length + " bytes", 1));
        pasos.add(new Paso("Bytes obtenidos (hex)", HEX.formatHex(bytes), 2));
        return new Resultado(new String(bytes, StandardCharsets.UTF_8), pasos);
    }

    private String grupos6(byte[] b) {
        StringBuilder bits = new StringBuilder();
        for (int i = 0; i < Math.min(3, b.length); i++) {
            bits.append(String.format("%8s", Integer.toBinaryString(b[i] & 0xFF)).replace(' ', '0'));
        }
        StringBuilder out = new StringBuilder(bits.toString()).append(" → ");
        for (int i = 0; i < bits.length(); i += 6) {
            out.append(bits, i, Math.min(i + 6, bits.length())).append(' ');
        }
        return out.toString().trim();
    }

    // ---------------------------------------------------------------- hash y MAC

    private Resultado resumen(String alg, String op, String texto, String referencia) throws GeneralSecurityException {
        byte[] entrada = texto.getBytes(StandardCharsets.UTF_8);
        byte[] h = MessageDigest.getInstance(alg).digest(entrada);
        String hex = HEX.formatHex(h);
        if ("VERIFICAR".equals(op)) {
            return new Resultado(compararHex(hex, referencia), List.of(
                    new Paso("Hash calculado", hex, 2),
                    new Paso("Comparación", "En tiempo constante (MessageDigest.isEqual)", 2)));
        }
        byte[] h2 = MessageDigest.getInstance(alg).digest((texto + ".").getBytes(StandardCharsets.UTF_8));
        List<Paso> pasos = List.of(
                new Paso("Algoritmo", alg + " · salida de " + (h.length * 8) + " bits", 1),
                new Paso("Bytes de entrada", String.valueOf(entrada.length), 2),
                new Paso("Hash en Base64", Base64.getEncoder().encodeToString(h), 2),
                new Paso("Efecto avalancha: hash de \"texto.\"", HEX.formatHex(h2)
                        + " (difieren " + bitsDistintos(h, h2) + " de " + (h.length * 8) + " bits)", 3));
        return new Resultado(hex, pasos);
    }

    private Resultado hmac(String op, String texto, String clave, String referencia) throws GeneralSecurityException {
        exigirClave(clave);
        Mac mac = Mac.getInstance("HmacSHA256");
        byte[] k = clave.getBytes(StandardCharsets.UTF_8);
        mac.init(new SecretKeySpec(k, "HmacSHA256"));
        String hex = HEX.formatHex(mac.doFinal(texto.getBytes(StandardCharsets.UTF_8)));
        if ("VERIFICAR".equals(op)) {
            return new Resultado(compararHex(hex, referencia), List.of(
                    new Paso("MAC calculado", hex, 2),
                    new Paso("Comparación", "En tiempo constante", 2)));
        }
        return new Resultado(hex, List.of(
                new Paso("Algoritmo", "HMAC con SHA-256 · 256 bits", 1),
                new Paso("Longitud de la clave", k.length + " bytes", 2),
                new Paso("Clave usada (hex)", HEX.formatHex(k), 3),
                new Paso("Fórmula", "H((K xor opad) || H((K xor ipad) || mensaje))", 2)));
    }

    private String compararHex(String calculado, String referencia) {
        if (referencia == null || referencia.isBlank()) {
            throw new SolicitudInvalida("Escribe el valor de referencia a verificar.");
        }
        boolean ok = MessageDigest.isEqual(calculado.getBytes(StandardCharsets.UTF_8),
                referencia.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
        return ok ? "COINCIDE" : "NO COINCIDE";
    }

    private int bitsDistintos(byte[] a, byte[] b) {
        int n = 0;
        for (int i = 0; i < Math.min(a.length, b.length); i++) {
            n += Integer.bitCount((a[i] ^ b[i]) & 0xFF);
        }
        return n;
    }

    // ---------------------------------------------------------------- hashes de contrasenas

    private Resultado adaptativo(String nombre, PasswordEncoder encoder, String op, String texto, String referencia) {
        if ("VERIFICAR".equals(op)) {
            if (referencia == null || referencia.isBlank()) {
                throw new SolicitudInvalida("Escribe el hash de referencia a verificar.");
            }
            boolean ok = encoder.matches(texto, referencia.trim());
            return new Resultado(ok ? "COINCIDE" : "NO COINCIDE", List.of(
                    new Paso("Método", "matches(): extrae la sal del hash, recalcula y compara. No descifra nada.", 2)));
        }
        String hash;
        try {
            hash = encoder.encode(texto);
        } catch (IllegalArgumentException e) {
            throw new SolicitudInvalida("La contraseña supera el límite del algoritmo (72 bytes en bcrypt).");
        }
        List<Paso> pasos = new ArrayList<>();
        pasos.add(new Paso("Algoritmo", nombre + " · la sal aleatoria va dentro del hash", 1));
        pasos.add(new Paso("Longitud del hash", hash.length() + " caracteres", 1));
        pasos.add(new Paso("Prueba", "Repite la operación: obtendrás un hash distinto (sal nueva) que igual verifica.", 1));
        String[] p = hash.split("\\$");
        if (hash.startsWith("$argon2")) {
            pasos.add(new Paso("Variante y versión", p[1] + " · " + p[2], 2));
            pasos.add(new Paso("Memoria, iteraciones y paralelismo", p[3], 2));
            pasos.add(new Paso("Sal (Base64)", p[4], 3));
            pasos.add(new Paso("Digest (Base64)", p[5], 3));
        } else {
            String resto = p[3];
            pasos.add(new Paso("Versión", "$" + p[1], 2));
            pasos.add(new Paso("Costo", p[2] + " → 2^" + p[2] + " = " + (1 << Integer.parseInt(p[2])) + " iteraciones", 2));
            pasos.add(new Paso("Sal (22 caracteres)", resto.substring(0, 22), 3));
            pasos.add(new Paso("Digest (31 caracteres)", resto.substring(22), 3));
        }
        return new Resultado(hash, pasos);
    }

    private Resultado pbkdf2(String op, String texto, String referencia) throws GeneralSecurityException {
        if ("VERIFICAR".equals(op)) {
            if (referencia == null) {
                throw new SolicitudInvalida("Escribe el hash de referencia a verificar.");
            }
            String[] p = referencia.trim().split("\\$");
            if (p.length != 4 || !"pbkdf2_sha256".equals(p[0])) {
                throw new SolicitudInvalida("Formato esperado: pbkdf2_sha256$iteraciones$sal$hash");
            }
            byte[] esperado = Base64.getDecoder().decode(p[3]);
            byte[] calculado = derivar(texto.toCharArray(), Base64.getDecoder().decode(p[2]),
                    Integer.parseInt(p[1]), esperado.length * 8);
            return new Resultado(MessageDigest.isEqual(esperado, calculado) ? "COINCIDE" : "NO COINCIDE",
                    List.of(new Paso("Iteraciones leídas del hash", p[1], 2)));
        }
        byte[] sal = new byte[16];
        azar.nextBytes(sal);
        byte[] dk = derivar(texto.toCharArray(), sal, ITERACIONES_PBKDF2, 256);
        String salida = "pbkdf2_sha256$" + ITERACIONES_PBKDF2 + "$" + Base64.getEncoder().encodeToString(sal)
                + "$" + Base64.getEncoder().encodeToString(dk);
        return new Resultado(salida, List.of(
                new Paso("Algoritmo", "PBKDF2 con HMAC-SHA256", 1),
                new Paso("Iteraciones", String.valueOf(ITERACIONES_PBKDF2), 2),
                new Paso("Sal (hex)", HEX.formatHex(sal), 3),
                new Paso("Clave derivada (hex)", HEX.formatHex(dk), 3)));
    }

    private byte[] derivar(char[] clave, byte[] sal, int iteraciones, int bits) throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(clave, sal, iteraciones, bits);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }

    // ---------------------------------------------------------------- cifrado simetrico

    private Resultado simetrico(Simetrico s, String op, String texto, String clave) throws GeneralSecurityException {
        exigirClave(clave);
        boolean cifrar = "CIFRAR".equals(op);
        byte[] sal;
        byte[] iv = new byte[s.bytesIv()];
        byte[] datos;
        if (cifrar) {
            sal = new byte[16];
            azar.nextBytes(sal);
            azar.nextBytes(iv);
            datos = texto.getBytes(StandardCharsets.UTF_8);
        } else {
            byte[] todo;
            try {
                todo = Base64.getDecoder().decode(texto.trim());
            } catch (IllegalArgumentException e) {
                throw new SolicitudInvalida("El texto cifrado no es Base64 válido.");
            }
            if (todo.length <= 16 + s.bytesIv()) {
                throw new SolicitudInvalida("El texto cifrado es demasiado corto.");
            }
            sal = Arrays.copyOfRange(todo, 0, 16);
            iv = Arrays.copyOfRange(todo, 16, 16 + s.bytesIv());
            datos = Arrays.copyOfRange(todo, 16 + s.bytesIv(), todo.length);
        }
        byte[] k = derivar(clave.toCharArray(), sal, ITERACIONES_PBKDF2, s.bitsClave());
        Cipher cipher = Cipher.getInstance(s.transformacion());
        SecretKeySpec ks = new SecretKeySpec(k, s.algoritmoClave());
        int modo = cifrar ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE;
        if (s.bytesIv() == 0) {
            cipher.init(modo, ks);
        } else if (s.transformacion().contains("GCM")) {
            cipher.init(modo, ks, new GCMParameterSpec(128, iv));
        } else {
            cipher.init(modo, ks, new IvParameterSpec(iv));
        }
        byte[] resultado = cipher.doFinal(datos);

        String salida;
        byte[] cifrado = cifrar ? resultado : datos;
        if (cifrar) {
            byte[] bloque = new byte[16 + iv.length + resultado.length];
            System.arraycopy(sal, 0, bloque, 0, 16);
            System.arraycopy(iv, 0, bloque, 16, iv.length);
            System.arraycopy(resultado, 0, bloque, 16 + iv.length, resultado.length);
            salida = Base64.getEncoder().encodeToString(bloque);
        } else {
            salida = new String(resultado, StandardCharsets.UTF_8);
        }

        List<Paso> pasos = new ArrayList<>();
        pasos.add(new Paso("Operación", cifrar ? "Texto claro → texto cifrado" : "Texto cifrado → texto claro", 1));
        pasos.add(new Paso("Formato del cifrado", s.bytesIv() == 0 ? "Base64(sal ‖ texto cifrado)"
                : "Base64(sal ‖ IV ‖ texto cifrado)", 1));
        pasos.add(new Paso("Transformación JCE", s.transformacion() + " · " + s.modo(), 2));
        pasos.add(new Paso("Derivación de la clave", "PBKDF2-HMAC-SHA256 · " + ITERACIONES_PBKDF2
                + " iteraciones → " + s.bitsClave() + " bits", 2));
        if (s.bytesIv() > 0) {
            pasos.add(new Paso("Tamaño del IV/nonce", s.bytesIv() + " bytes (aleatorio por mensaje)", 2));
        }
        pasos.add(new Paso("Sal de PBKDF2 (hex)", HEX.formatHex(sal), 3));
        if (s.bytesIv() > 0) {
            pasos.add(new Paso("IV/nonce (hex)", HEX.formatHex(iv), 3));
        }
        pasos.add(new Paso("Clave derivada (hex)", HEX.formatHex(k), 3));
        pasos.add(new Paso("Texto cifrado (hex)", HEX.formatHex(cifrado), 3));
        return new Resultado(salida, pasos);
    }

    private void exigirClave(String clave) {
        if (clave == null || clave.isBlank()) {
            throw new SolicitudInvalida("Escribe una clave o frase secreta.");
        }
    }

    // ---------------------------------------------------------------- RSA

    private Resultado rsa(String op, String texto) throws GeneralSecurityException {
        String[] par = claveRsa();
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PublicKey pub = kf.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(par[0])));
        PrivateKey priv = kf.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(par[1])));
        OAEPParameterSpec oaep = new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        String salida;
        boolean cifrar = "CIFRAR".equals(op);
        if (cifrar) {
            byte[] datos = texto.getBytes(StandardCharsets.UTF_8);
            if (datos.length > 190) {
                throw new SolicitudInvalida("RSA-2048 con OAEP solo cifra hasta 190 bytes.");
            }
            cipher.init(Cipher.ENCRYPT_MODE, pub, oaep);
            salida = Base64.getEncoder().encodeToString(cipher.doFinal(datos));
        } else {
            byte[] datos;
            try {
                datos = Base64.getDecoder().decode(texto.trim());
            } catch (IllegalArgumentException e) {
                throw new SolicitudInvalida("El texto cifrado no es Base64 válido.");
            }
            cipher.init(Cipher.DECRYPT_MODE, priv, oaep);
            salida = new String(cipher.doFinal(datos), StandardCharsets.UTF_8);
        }
        RSAPublicKey rp = (RSAPublicKey) pub;
        List<Paso> pasos = List.of(
                new Paso("Operación", cifrar ? "Cifra con la clave PÚBLICA" : "Descifra con la clave PRIVADA", 1),
                new Paso("Tamaño de la clave", rp.getModulus().bitLength() + " bits", 1),
                new Paso("Relleno", "OAEP con SHA-256 y MGF1", 2),
                new Paso("Exponente público e", rp.getPublicExponent().toString(), 2),
                new Paso("Clave pública (X.509, Base64)", par[0], 2),
                new Paso("Módulo n (hex)", rp.getModulus().toString(16), 3),
                new Paso("Clave privada (PKCS#8, Base64) — solo demostración", par[1], 3));
        return new Resultado(salida, pasos);
    }

    private synchronized String[] claveRsa() throws GeneralSecurityException {
        var existente = catalogo.claveDemo("RSA_OAEP");
        if (existente.isPresent()) {
            return existente.get();
        }
        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        KeyPair kp = g.generateKeyPair();
        String pub = Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());
        String priv = Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded());
        catalogo.guardarClaveDemo("RSA_OAEP", pub, priv);
        return new String[]{pub, priv};
    }
}
