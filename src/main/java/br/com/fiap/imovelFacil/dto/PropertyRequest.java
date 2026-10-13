package br.com.fiap.imovelFacil.dto;

import java.math.BigDecimal;

import br.com.fiap.imovelFacil.model.Property;
import br.com.fiap.imovelFacil.model.PropertyType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PropertyRequest(
        @NotBlank @Size(max = 100) String title,
        @Size(max = 500) String description,
        @NotNull PropertyType type,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Size(max = 100) String neighborhood,
        @NotNull @DecimalMin("0.01") @Digits(integer = 12, fraction = 2) BigDecimal price,
        @NotNull @Min(0) Integer bedrooms,
        @NotNull @Positive @DecimalMax("1000000000") Double area
) {

    public Property toEntity() {
        Property property = new Property();
        applyTo(property);
        return property;
    }

    public void applyTo(Property property) {
        property.setTitle(title.strip());
        property.setDescription(description == null ? null : description.strip());
        property.setType(type);
        property.setCity(city.strip());
        property.setNeighborhood(neighborhood.strip());
        property.setPrice(price);
        property.setBedrooms(bedrooms);
        property.setArea(area);
    }
}
