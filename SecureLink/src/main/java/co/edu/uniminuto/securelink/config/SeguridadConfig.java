package co.edu.uniminuto.securelink.config;

import co.edu.uniminuto.securelink.repositorio.UsuarioRepositorio;
import co.edu.uniminuto.securelink.seguridad.RespuestasSeguridad;
import co.edu.uniminuto.securelink.seguridad.RevalidarUsuarioFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(PropiedadesSemilla.class)
public class SeguridadConfig {

    /** bcrypt: cada hash lleva su propia sal y el costo es configurable. */
    @Bean
    public PasswordEncoder passwordEncoder(@Value("${securelink.seguridad.bcrypt-costo:10}") int costo) {
        return new BCryptPasswordEncoder(costo);
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService detalles, PasswordEncoder encoder) {
        DaoAuthenticationProvider proveedor = new DaoAuthenticationProvider(detalles);
        proveedor.setPasswordEncoder(encoder);
        return new ProviderManager(proveedor);
    }

    @Bean
    public SecurityFilterChain filtros(HttpSecurity http, RespuestasSeguridad respuestas,
                                       UsuarioRepositorio usuarios) throws Exception {
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
                .addFilterAfter(new RevalidarUsuarioFilter(usuarios), SecurityContextHolderFilter.class)
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/registro").permitAll()
                        .requestMatchers("/api/auth/logout", "/error").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/usuario/**").hasRole("USUARIO")
                        .requestMatchers("/api/invitado/**").hasRole("INVITADO")
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(respuestas)
                        .accessDeniedHandler(respuestas))
                .logout(l -> l
                        .logoutUrl("/api/auth/logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler(respuestas))
                .formLogin(f -> f.disable())
                .httpBasic(b -> b.disable());
        return http.build();
    }
}
