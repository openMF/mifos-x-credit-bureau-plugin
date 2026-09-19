package org.mifos.creditbureau.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class EntityNotFoundExceptionMapper implements ExceptionMapper<EntityNotFoundException> {

    private static final Logger logger = LoggerFactory.getLogger(EntityNotFoundExceptionMapper.class);

    @Override
    public Response toResponse(EntityNotFoundException exception) {
        // Log the original exception for debugging, but return stable message to client
        logger.debug("Entity not found exception occurred", exception);
        
        var entity = java.util.Map.of(
            "status", 404,
            "error", "Not Found",
            "message", "Entity not found"
        );

        return Response.status(Response.Status.NOT_FOUND).entity(entity).build();
    }
}
