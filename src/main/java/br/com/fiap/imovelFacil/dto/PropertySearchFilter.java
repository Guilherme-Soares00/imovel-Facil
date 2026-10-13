package br.com.fiap.imovelFacil.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record PropertySearchFilter(
        @Parameter(description = "Trecho literal do título, ignorando maiúsculas e minúsculas")
        @Size(max = 100) String title,
        @Parameter(description = "Cidade exata, ignorando maiúsculas e minúsculas")
        @Size(max = 100) String city,
        @Parameter(description = "Aluguel mínimo em reais, inclusivo")
        @PositiveOrZero @Digits(integer = 12, fraction = 2) BigDecimal minPrice,
        @Parameter(description = "Aluguel máximo em reais, inclusivo")
        @PositiveOrZero @Digits(integer = 12, fraction = 2) BigDecimal maxPrice,
        @Parameter(description = "Quantidade mínima de quartos, inclusiva") @Min(0) Integer minBedrooms,
        @Parameter(description = "Área mínima em metros quadrados, inclusiva")
        @PositiveOrZero @DecimalMax("1000000000") Double minArea
) {
}
