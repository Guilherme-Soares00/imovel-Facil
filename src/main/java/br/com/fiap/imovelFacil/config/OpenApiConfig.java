package br.com.fiap.imovelFacil.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI propertyOpenApi(@Value("${info.app.version}") String version) {
        return new OpenAPI().info(new Info()
                .title("Imóvel Fácil API")
                .version(version)
                .description("Catálogo de imóveis com paginação, Query by Example, Specification, HATEOAS e cache."));
    }
}
