package cl.andesstay.reservations.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS acotado a los orígenes permitidos (CORS_ALLOWED_ORIGINS, separados por coma).
 * Se reemplaza la configuración de EP1 (origen * + credenciales).
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${andesstay.cors.allowed-origins:http://localhost:4200,http://localhost:8080}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOriginPatterns(allowedOrigins.split(","))
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true);
    }
}
