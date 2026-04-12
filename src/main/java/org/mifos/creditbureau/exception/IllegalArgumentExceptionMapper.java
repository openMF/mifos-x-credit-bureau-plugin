package org.mifos.creditbureau.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class IllegalArgumentExceptionMapper implements ExceptionMapper<IllegalArgumentException> {

    private static final Logger logger = LoggerFactory.getLogger(IllegalArgumentExceptionMapper.class);
    private static final String DEFAULT_MESSAGE = "Invalid argument";

    @Override
    public Response toResponse(IllegalArgumentException exception) {
        // Log the original exception for debugging
        logger.debug("Illegal argument exception occurred", exception);
        
        String message = exception.getMessage() != null ? exception.getMessage() : DEFAULT_MESSAGE;
        
        var entity = java.util.Map.of(
            "status", 400,
            "error", "Bad Request",
            "message", message
        );

        return Response.status(Response.Status.BAD_REQUEST).entity(entity).build();
    }
}
