package dev.algocode.web;

import dev.algocode.user.UserResolver;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final UserResolver userResolver;
    private final String[] corsOrigins;

    public WebConfig(UserResolver userResolver, @Value("${algocode.cors-origins:}") String[] corsOrigins) {
        this.userResolver = userResolver;
        this.corsOrigins = corsOrigins;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(userResolver);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (corsOrigins.length > 0) {
            registry.addMapping("/api/**")
                    .allowedOrigins(corsOrigins)
                    .allowedMethods("GET", "POST", "PUT", "DELETE")
                    .allowedHeaders("*");
        }
    }
}
