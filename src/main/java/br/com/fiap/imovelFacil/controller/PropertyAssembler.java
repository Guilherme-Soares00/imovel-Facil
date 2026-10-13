package br.com.fiap.imovelFacil.controller;

import br.com.fiap.imovelFacil.model.Property;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class PropertyAssembler implements RepresentationModelAssembler<Property, EntityModel<Property>> {

    @Override
    public EntityModel<Property> toModel(Property property) {
        return EntityModel.of(property,
                linkTo(methodOn(PropertyController.class).findById(property.getId())).withSelfRel(),
                linkTo(methodOn(PropertyController.class).findAll(null)).withRel("properties"));
    }
}
