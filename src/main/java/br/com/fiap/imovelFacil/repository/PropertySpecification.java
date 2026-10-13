package br.com.fiap.imovelFacil.repository;

import java.math.BigDecimal;
import java.util.Locale;

import br.com.fiap.imovelFacil.dto.PropertySearchFilter;
import br.com.fiap.imovelFacil.model.Property;
import org.springframework.data.jpa.domain.Specification;

public final class PropertySpecification {

    private PropertySpecification() {
    }

    public static Specification<Property> withFilters(PropertySearchFilter filter) {
        Specification<Property> specification = Specification.unrestricted();
        if (filter.title() != null && !filter.title().isBlank()) {
            specification = specification.and(titleContains(filter.title().strip()));
        }
        if (filter.city() != null && !filter.city().isBlank()) {
            specification = specification.and(cityEquals(filter.city().strip()));
        }
        if (filter.minPrice() != null) {
            specification = specification.and(priceAtLeast(filter.minPrice()));
        }
        if (filter.maxPrice() != null) {
            specification = specification.and(priceAtMost(filter.maxPrice()));
        }
        if (filter.minBedrooms() != null) {
            specification = specification.and(bedroomsAtLeast(filter.minBedrooms()));
        }
        if (filter.minArea() != null) {
            specification = specification.and(areaAtLeast(filter.minArea()));
        }
        return specification;
    }

    public static Specification<Property> titleContains(String title) {
        String pattern = "%" + title.toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        return (root, query, builder) -> builder.like(builder.lower(root.get("title")), pattern, '\\');
    }

    public static Specification<Property> cityEquals(String city) {
        return (root, query, builder) -> builder.equal(builder.lower(root.get("city")), city.toLowerCase(Locale.ROOT));
    }

    public static Specification<Property> priceAtLeast(BigDecimal price) {
        return (root, query, builder) -> builder.greaterThanOrEqualTo(root.get("price"), price);
    }

    public static Specification<Property> priceAtMost(BigDecimal price) {
        return (root, query, builder) -> builder.lessThanOrEqualTo(root.get("price"), price);
    }

    public static Specification<Property> bedroomsAtLeast(Integer bedrooms) {
        return (root, query, builder) -> builder.greaterThanOrEqualTo(root.get("bedrooms"), bedrooms);
    }

    public static Specification<Property> areaAtLeast(Double area) {
        return (root, query, builder) -> builder.greaterThanOrEqualTo(root.get("area"), area);
    }
}
