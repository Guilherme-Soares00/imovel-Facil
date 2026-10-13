package br.com.fiap.imovelFacil.dto;

import br.com.fiap.imovelFacil.model.PropertyType;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record PropertyExampleFilter(
        @Parameter(description = "Tipo exato do imóvel") PropertyType type,
        @Parameter(description = "Cidade exata, ignorando maiúsculas e minúsculas")
        @Size(max = 100) String city,
        @Parameter(description = "Bairro exato, ignorando maiúsculas e minúsculas")
        @Size(max = 100) String neighborhood,
        @Parameter(description = "Quantidade exata de quartos") @Min(0) Integer bedrooms
) {
}
