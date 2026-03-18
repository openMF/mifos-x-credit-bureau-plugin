package org.mifos.creditbureau.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class EntityNotFoundExceptionMapper implements ExceptionMapper<EntityNotFoundException> {

    @Override
    public Response toResponse(EntityNotFoundException exception) {
        var entity = java.util.Map.of(
                "status", 404,
                "error", "Not Found",
                "message", exception.getMessage()
        );

        return Response.status(Response.Status.NOT_FOUND)
                .type(MediaType.APPLICATION_JSON)
                .entity(entity)
                .build();
    }
}
