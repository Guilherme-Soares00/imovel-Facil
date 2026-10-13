package br.com.fiap.imovelFacil.config;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import br.com.fiap.imovelFacil.model.Property;
import br.com.fiap.imovelFacil.model.PropertyType;
import br.com.fiap.imovelFacil.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private record Location(String city, String neighborhood, int pricePerSquareMeter) {}

    private static final List<Location> LOCATIONS = List.of(
            new Location("São Paulo", "Pinheiros", 62),
            new Location("São Paulo", "Vila Mariana", 55),
            new Location("São Paulo", "Moema", 60),
            new Location("São Paulo", "Centro", 40),
            new Location("São Paulo", "Tatuapé", 45),
            new Location("Rio de Janeiro", "Copacabana", 58),
            new Location("Rio de Janeiro", "Botafogo", 56),
            new Location("Rio de Janeiro", "Tijuca", 42),
            new Location("Belo Horizonte", "Savassi", 45),
            new Location("Belo Horizonte", "Pampulha", 35),
            new Location("Curitiba", "Batel", 48),
            new Location("Curitiba", "Água Verde", 40)
    );

    private static final PropertyType[] TYPES = {
            PropertyType.APARTMENT, PropertyType.APARTMENT, PropertyType.HOUSE,
            PropertyType.STUDIO, PropertyType.APARTMENT, PropertyType.COMMERCIAL
    };

    private static final String[] FEATURES = {
            "com varanda", "perto do metrô", "mobiliado", "com piscina", "reformado",
            "com vista", "com quintal", "silencioso", "iluminado", "com vaga de garagem"
    };

    private final PropertyRepository repository;

    @Override
    public void run(String... args) {
        if (repository.count() > 0) return;

        List<Property> properties = new ArrayList<>(60);
        for (int i = 0; i < 60; i++) {
            Location location = LOCATIONS.get(i % LOCATIONS.size());
            PropertyType type = TYPES[(i + i / LOCATIONS.size()) % TYPES.length];
            String feature = FEATURES[(i * 3) % FEATURES.length];

            int bedrooms = switch (type) {
                case STUDIO -> 1;
                case COMMERCIAL -> 0;
                case APARTMENT -> 1 + i % 3;
                case HOUSE -> 2 + i % 3;
            };

            double area = switch (type) {
                case STUDIO -> 28 + i % 10;
                case COMMERCIAL -> 40 + (i % 8) * 15;
                case APARTMENT -> 45 + bedrooms * 18 + (i % 7) * 3;
                case HOUSE -> 90 + bedrooms * 25 + (i % 5) * 8;
            };
            BigDecimal price = BigDecimal.valueOf(area * location.pricePerSquareMeter() / 50)
                    .setScale(0, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(50))
                    .setScale(2, RoundingMode.UNNECESSARY);

            String label = label(type);
            String rooms = type == PropertyType.COMMERCIAL ? ""
                    : " de " + bedrooms + (bedrooms == 1 ? " quarto" : " quartos");

            properties.add(Property.builder()
                    .title(label + " " + feature)
                    .description(label + rooms + " em " + location.neighborhood() + ", " + feature + ".")
                    .type(type)
                    .city(location.city())
                    .neighborhood(location.neighborhood())
                    .price(price)
                    .bedrooms(bedrooms)
                    .area(area)
                    .build());
        }
        repository.saveAll(properties);
    }

    private String label(PropertyType type) {
        return switch (type) {
            case APARTMENT -> "Apartamento";
            case HOUSE -> "Casa";
            case STUDIO -> "Studio";
            case COMMERCIAL -> "Sala comercial";
        };
    }
}
