package br.com.fiap.imovelFacil;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.core.Ordered;

@SpringBootApplication
@EnableCaching(order = Ordered.HIGHEST_PRECEDENCE)
public class ImovelFacilApplication {

	public static void main(String[] args) {
		SpringApplication.run(ImovelFacilApplication.class, args);
	}

}
