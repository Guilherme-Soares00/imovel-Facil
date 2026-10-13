package br.com.fiap.imovelFacil.controller;

import br.com.fiap.imovelFacil.dto.ErrorResponse;
import br.com.fiap.imovelFacil.dto.PropertyExampleFilter;
import br.com.fiap.imovelFacil.dto.PropertyRequest;
import br.com.fiap.imovelFacil.dto.PropertySearchFilter;
import br.com.fiap.imovelFacil.model.Property;
import br.com.fiap.imovelFacil.service.PropertyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/properties")
@RequiredArgsConstructor
@Tag(name = "Imóveis", description = "Catálogo, buscas e gerenciamento de imóveis")
@ApiResponse(responseCode = "400", description = "Dados ou parâmetros inválidos",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
public class PropertyController {

    private final PropertyService service;
    private final PropertyAssembler assembler;
    private final PagedResourcesAssembler<Property> pagedAssembler;

    @GetMapping
    @Operation(summary = "Listar imóveis com paginação")
    public PagedModel<EntityModel<Property>> findAll(
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return pagedAssembler.toModel(service.findAll(pageable), assembler);
    }

    @GetMapping("/example")
    @Operation(summary = "Buscar imóveis por Query by Example")
    public PagedModel<EntityModel<Property>> findByExample(
            @Valid @ModelAttribute @ParameterObject PropertyExampleFilter filter,
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return pagedAssembler.toModel(service.findByExample(filter, pageable), assembler);
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar imóveis por faixas e título com Specification")
    public PagedModel<EntityModel<Property>> search(
            @Valid @ModelAttribute @ParameterObject PropertySearchFilter filter,
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return pagedAssembler.toModel(service.search(filter, pageable), assembler);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar um imóvel por ID")
    @ApiResponse(responseCode = "404", description = "Imóvel não encontrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public EntityModel<Property> findById(@PathVariable Long id) {
        return assembler.toModel(service.findById(id));
    }

    @PostMapping
    @Operation(summary = "Cadastrar um imóvel")
    @ApiResponse(responseCode = "201", description = "Imóvel criado")
    public ResponseEntity<EntityModel<Property>> create(@Valid @RequestBody PropertyRequest request) {
        EntityModel<Property> model = assembler.toModel(service.create(request));
        return ResponseEntity.created(model.getRequiredLink("self").toUri()).body(model);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar um imóvel e seu cache")
    @ApiResponse(responseCode = "404", description = "Imóvel não encontrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public EntityModel<Property> update(@PathVariable Long id, @Valid @RequestBody PropertyRequest request) {
        return assembler.toModel(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir um imóvel e remover seu cache")
    @ApiResponse(responseCode = "204", description = "Imóvel excluído")
    @ApiResponse(responseCode = "404", description = "Imóvel não encontrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
