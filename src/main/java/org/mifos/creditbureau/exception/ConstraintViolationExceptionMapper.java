package org.mifos.creditbureau.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.stream.Collectors;

@Provider
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        var details = exception.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toList());

        var entity = java.util.Map.of(
                "status", 400,
                "error", "Bad Request",
                "message", "Validation failed",
                "details", details
        );

        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(entity)
                .build();
    }
}
