package org.mifos.creditbureau.exception;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class IllegalArgumentExceptionMapper implements ExceptionMapper<IllegalArgumentException> {

    @Override
    public Response toResponse(IllegalArgumentException exception) {
        var entity = java.util.Map.of(
                "status", 400,
                "error", "Bad Request",
                "message", exception.getMessage()
        );

        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(entity)
                .build();
    }
}
