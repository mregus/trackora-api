package com.fleetwise.api.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {

            @Bean
            public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();

                configuration.setAllowedOrigins(List.of(
                        "http://localhost:4200",
                        "https://trackora.solutions",
                        "https://www.trackora.solutions"
                ));

                configuration.setAllowedMethods(List.of(
                        "GET", "POST", "PUT", "DELETE", "OPTIONS"
                ));

                configuration.setAllowedHeaders(List.of("*"));
                configuration.setExposedHeaders(List.of("Authorization"));
                configuration.setAllowCredentials(false);

                UrlBasedCorsConfigurationSource source =
                        new UrlBasedCorsConfigurationSource();

                source.registerCorsConfiguration("/**", configuration);

                return source;
            }

//            @Override
//            public void addCorsMappings(CorsRegistry registry) {
//
//                registry.addMapping("/api/**")
//                        .allowedOrigins(
//                                "http://localhost:4200",
//                                "https://trackora.solutions",
//                                "https://www.trackora.solutions",
//                                "https://trackora-api-cxjn.onrender.com"
//                        )
//                        .allowedMethods(
//                                "GET",
//                                "POST",
//                                "PUT",
//                                "DELETE",
//                                "OPTIONS"
//                        )
//                        .allowedHeaders("*")
//                        .exposedHeaders("Authorization")
//                        .allowCredentials(false);
//            }
        };
    }
}