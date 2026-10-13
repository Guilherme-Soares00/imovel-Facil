package br.com.fiap.imovelFacil;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import br.com.fiap.imovelFacil.repository.PropertyRepository;
import br.com.fiap.imovelFacil.service.PropertyService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:api-tests")
@AutoConfigureMockMvc
class PropertyApiTests {

    private static final String PROPERTY = """
            {"title":"Apartamento VARANDA","description":"Perto do metrô","type":"APARTMENT",
             "city":"Cidade de Teste","neighborhood":"Centro","price":3200.00,"bedrooms":2,"area":68.5}
            """;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private PropertyService service;

    @MockitoSpyBean
    private PropertyRepository repository;

    private final List<Long> createdIds = new ArrayList<>();

    @BeforeEach
    void clearCache() {
        cacheManager.getCache("properties").clear();
        clearInvocations(repository);
    }

    @AfterEach
    void removeCreatedProperties() {
        for (Long id : createdIds) {
            if (repository.existsById(id)) {
                service.delete(id);
            }
        }
    }

    @Test
    void listsSixtySeededPropertiesWithDefaultPaginationAndLinks() throws Exception {
        mvc.perform(get("/properties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.properties.length()").value(10))
                .andExpect(jsonPath("$._embedded.properties[0].id").value(1))
                .andExpect(jsonPath("$._embedded.properties[9].id").value(10))
                .andExpect(jsonPath("$._embedded.properties[0]._links.self.href", endsWith("/properties/1")))
                .andExpect(jsonPath("$._embedded.properties[0]._links.properties.href").exists())
                .andExpect(jsonPath("$.page.size").value(10))
                .andExpect(jsonPath("$.page.totalElements").value(60))
                .andExpect(jsonPath("$.page.totalPages").value(6))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$._links.first.href").exists())
                .andExpect(jsonPath("$._links.next.href").exists())
                .andExpect(jsonPath("$._links.last.href").exists())
                .andExpect(jsonPath("$._links.prev").doesNotExist());
    }

    @Test
    void supportsCustomPageAndSortAndBoundsPageSize() throws Exception {
        mvc.perform(get("/properties").param("page", "2").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.properties[0].id").value(11))
                .andExpect(jsonPath("$.page.number").value(2))
                .andExpect(jsonPath("$.page.size").value(5))
                .andExpect(jsonPath("$._links.prev.href").exists());
        mvc.perform(get("/properties").param("sort", "id,desc").param("size", "1000000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.size").value(100))
                .andExpect(jsonPath("$._embedded.properties[0].id").value(60));
    }

    @Test
    void matchesExampleExactlyIgnoringCaseAndNullFilters() throws Exception {
        mvc.perform(get("/properties/example").param("type", "APARTMENT")
                        .param("city", "são paulo").param("bedrooms", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
                .andExpect(jsonPath("$._embedded.properties[*].city", everyItem(is("São Paulo"))))
                .andExpect(jsonPath("$._embedded.properties[*].bedrooms", everyItem(is(2))))
                .andExpect(jsonPath("$._embedded.properties[*].type", everyItem(is("APARTMENT"))));
        mvc.perform(get("/properties/example"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(60));
        mvc.perform(get("/properties/example").param("city", "São"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(0))
                .andExpect(jsonPath("$._embedded").doesNotExist());
        mvc.perform(get("/properties/example").param("bedrooms", "0"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
                .andExpect(jsonPath("$._embedded.properties[*].bedrooms", everyItem(is(0))));
        mvc.perform(get("/properties/example").param("neighborhood", "PINHEIROS"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
                .andExpect(jsonPath("$._embedded.properties[*].neighborhood", everyItem(is("Pinheiros"))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/properties/example", "/properties/search"})
    void preservesSearchFiltersWhenFollowingNextLink(String route) throws Exception {
        String body = mvc.perform(get(route).queryParam("city", "São Paulo").queryParam("size", "2"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String next = JsonPath.read(body, "$._links.next.href");
        assertThat(next).contains("city=", "page=1", "size=2");
        mvc.perform(get(URI.create(next)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page.number").value(1))
                .andExpect(jsonPath("$._embedded.properties[*].city", everyItem(is("São Paulo"))));
    }

    @Test
    void combinesSpecificationsWithInclusiveBounds() throws Exception {
        createProperty(PROPERTY);
        mvc.perform(get("/properties/search").param("title", "varanda").param("city", "cidade de teste")
                        .param("minPrice", "3200").param("maxPrice", "3200")
                        .param("minBedrooms", "2").param("minArea", "68.5"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$._embedded.properties[0].title").value("Apartamento VARANDA"));
        for (String[] filter : List.of(new String[]{"minArea", "68.6"}, new String[]{"minBedrooms", "3"},
                new String[]{"minPrice", "3201"}, new String[]{"maxPrice", "3199"})) {
            mvc.perform(get("/properties/search").param("city", "Cidade de Teste").param(filter[0], filter[1]))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(0));
        }
    }

    @Test
    void ignoresMissingSearchFiltersAndOmitsEmbeddedForEmptyPage() throws Exception {
        mvc.perform(get("/properties/search"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(60));
        mvc.perform(get("/properties/search").param("city", "São"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(0));
        mvc.perform(get("/properties").param("page", "100"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(60))
                .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {"%", "_", "' OR 1=1 --", "\\"})
    void treatsSearchTextAsLiteral(String title) throws Exception {
        mvc.perform(get("/properties/search").param("title", title))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    void createsReadsUpdatesAndDeletesWithCacheCoherence() throws Exception {
        long id = createProperty(PROPERTY);
        clearInvocations(repository);
        mvc.perform(get("/properties/{id}", id)).andExpect(status().isOk());
        mvc.perform(get("/properties/{id}", id)).andExpect(status().isOk());
        verify(repository, times(1)).findById(id);
        mvc.perform(put("/properties/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(PROPERTY.replace("3200.00", "4500.00")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.price").value(4500.0))
                .andExpect(jsonPath("$._links.self.href", endsWith("/properties/" + id)))
                .andExpect(jsonPath("$._links.properties.href").exists());
        clearInvocations(repository);
        mvc.perform(get("/properties/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(4500.0));
        verify(repository, never()).findById(id);
        mvc.perform(delete("/properties/{id}", id)).andExpect(status().isNoContent());
        assertThat(cacheManager.getCache("properties").get(id)).isNull();
        mvc.perform(get("/properties/{id}", id)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Property not found"));
    }

    @Test
    void returnsContractErrorsForMissingProperties() throws Exception {
        mvc.perform(get("/properties/999999")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Property not found"));
        mvc.perform(put("/properties/999999").contentType(MediaType.APPLICATION_JSON).content(PROPERTY))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("Property not found"));
        mvc.perform(delete("/properties/999999")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Property not found"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/properties/example?type=CASTLE", "/properties/example?bedrooms=-1",
            "/properties/search?minPrice=abc", "/properties/search?minPrice=-1",
            "/properties/search?minPrice=20&maxPrice=10", "/properties/search?minArea=-1",
            "/properties/search?minArea=NaN", "/properties/search?minBedrooms=-1",
            "/properties?sort=unknown", "/properties?page=2147483647&size=100", "/properties/abc"
    })
    void returnsBadRequestForInvalidFilters(String url) throws Exception {
        mvc.perform(get(URI.create(url))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").isString());
    }

    @Test
    void rejectsInvalidBodiesWithoutChangingStoredDataOrCache() throws Exception {
        long id = createProperty(PROPERTY);
        mvc.perform(get("/properties/{id}", id)).andExpect(status().isOk());
        for (String body : List.of("{}", "{", PROPERTY.replace("3200.00", "-1"),
                PROPERTY.replace("3200.00", "1.234"), PROPERTY.replace("Apartamento VARANDA", " "),
                PROPERTY.replace("Cidade de Teste", "x".repeat(101)), PROPERTY.replace("68.5", "0"))) {
            mvc.perform(post("/properties").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").isString());
            mvc.perform(put("/properties/{id}", id).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").isString());
        }
        mvc.perform(get("/properties/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(3200.0));
        assertThat(repository.count()).isEqualTo(61);
    }

    @Test
    void exposesOnlyRequiredReadOnlyActuatorEndpoints() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP")).andExpect(jsonPath("$.components").doesNotExist());
        mvc.perform(get("/actuator/info")).andExpect(status().isOk())
                .andExpect(jsonPath("$.app.name").value("Imóvel Fácil API"))
                .andExpect(jsonPath("$.app.version").value("0.0.1-SNAPSHOT"));
        mvc.perform(get("/actuator/caches")).andExpect(status().isOk())
                .andExpect(jsonPath("$.cacheManagers.*.caches.properties").isNotEmpty());
        for (String endpoint : List.of("env", "beans", "metrics", "configprops", "heapdump", "mappings")) {
            mvc.perform(get("/actuator/" + endpoint)).andExpect(status().isNotFound());
        }
        mvc.perform(delete("/actuator/caches")).andExpect(status().isMethodNotAllowed());
        mvc.perform(get("/h2-console/")).andExpect(status().isNotFound());
    }

    @Test
    void publishesSwaggerAndDocumentsFiltersAndErrorResponses() throws Exception {
        mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("swagger-ui")));
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Imóvel Fácil API"))
                .andExpect(jsonPath("$.paths['/properties/search'].get.summary").isString())
                .andExpect(jsonPath("$.paths['/properties/search'].get.parameters[?(@.name == 'minPrice')].description").isNotEmpty())
                .andExpect(jsonPath("$.paths['/properties/example'].get.parameters[?(@.name == 'neighborhood')].description").isNotEmpty())
                .andExpect(jsonPath("$.paths['/properties'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/properties/{id}'].get.responses['404']").exists());
    }

    @Test
    void permitsExpoWebRequestsAndExposesLocation() throws Exception {
        mvc.perform(options("/properties").header("Origin", "http://localhost:8081")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "*"));
        mvc.perform(get("/properties").header("Origin", "http://localhost:8081"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Expose-Headers", "Location"));
        mvc.perform(get("/actuator/health").header("Origin", "http://localhost:8081"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "*"));
    }

    private long createProperty(String body) throws Exception {
        String location = mvc.perform(post("/properties").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.properties.href").exists())
                .andReturn().getResponse().getHeader("Location");
        long id = Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
        createdIds.add(id);
        return id;
    }
}
