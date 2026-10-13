package br.com.fiap.imovelFacil.service;

import java.util.Set;

import br.com.fiap.imovelFacil.dto.PropertyExampleFilter;
import br.com.fiap.imovelFacil.dto.PropertyRequest;
import br.com.fiap.imovelFacil.dto.PropertySearchFilter;
import br.com.fiap.imovelFacil.model.Property;
import br.com.fiap.imovelFacil.repository.PropertyRepository;
import br.com.fiap.imovelFacil.repository.PropertySpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PropertyService {

    private static final Set<String> SORT_FIELDS = Set.of(
            "id", "title", "type", "city", "neighborhood", "price", "bedrooms", "area");

    private final PropertyRepository repository;

    public Page<Property> findAll(Pageable pageable) {
        validatePageable(pageable);
        return repository.findAll(pageable);
    }

    public Page<Property> findByExample(PropertyExampleFilter filter, Pageable pageable) {
        validatePageable(pageable);
        Property probe = Property.builder()
                .type(filter.type())
                .city(normalize(filter.city()))
                .neighborhood(normalize(filter.neighborhood()))
                .bedrooms(filter.bedrooms())
                .build();
        ExampleMatcher matcher = ExampleMatcher.matchingAll()
                .withIgnoreNullValues()
                .withIgnoreCase()
                .withStringMatcher(ExampleMatcher.StringMatcher.EXACT);
        return repository.findAll(Example.of(probe, matcher), pageable);
    }

    public Page<Property> search(PropertySearchFilter filter, Pageable pageable) {
        validatePageable(pageable);
        if (filter.minPrice() != null && filter.maxPrice() != null
                && filter.minPrice().compareTo(filter.maxPrice()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minPrice must not exceed maxPrice");
        }
        return repository.findAll(PropertySpecification.withFilters(filter), pageable);
    }

    @Cacheable(cacheNames = "properties", key = "#id")
    public Property findById(Long id) {
        log.info("Fetching property {} from the database", id);
        return findRequired(id);
    }

    @Transactional
    public Property create(PropertyRequest request) {
        return repository.save(request.toEntity());
    }

    @Transactional
    @CachePut(cacheNames = "properties", key = "#id")
    public Property update(Long id, PropertyRequest request) {
        Property property = findRequired(id);
        request.applyTo(property);
        return repository.save(property);
    }

    @Transactional
    @CacheEvict(cacheNames = "properties", key = "#id")
    public void delete(Long id) {
        repository.delete(findRequired(id));
    }

    private Property findRequired(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found"));
    }

    private void validatePageable(Pageable pageable) {
        if (pageable.getOffset() > Integer.MAX_VALUE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page offset is too large");
        }
        for (var order : pageable.getSort()) {
            if (!SORT_FIELDS.contains(order.getProperty())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort property");
            }
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
