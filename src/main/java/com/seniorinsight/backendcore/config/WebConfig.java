package com.seniorinsight.backendcore.config;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.seniorinsight.backendcore.security.UsuarioAutenticadoArgumentResolver;

/**
 * Registra o resolver que entrega {@link com.seniorinsight.backendcore.security.UsuarioAutenticado} aos controllers.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new UsuarioAutenticadoArgumentResolver());
    }
}
