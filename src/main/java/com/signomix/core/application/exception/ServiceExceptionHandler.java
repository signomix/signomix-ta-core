package com.signomix.core.application.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Provider
public class ServiceExceptionHandler implements ExceptionMapper<ServiceException> {

    @ConfigProperty(name ="signomix.exception.device.not.found")
    String deviceNotFoundException;
    @ConfigProperty(name ="signomix.exception.device.create")
    String deviceCreateException;
    @ConfigProperty(name ="signomix.exception.device.update")
    String deviceUpdateException;
    @ConfigProperty(name ="signomix.exception.api.param.missing")
    String missingParameterException;

    @ConfigProperty(name ="signomix.exception.api.unauthorized")
    String userNotAuthorizedException;


    @Override
    public Response toResponse(ServiceException e) {

        switch(e.getMessage()){
            case "signomix.exception.device.not.found":
                return Response.status(Response.Status.NOT_FOUND).entity(new ErrorMessage(e.getMessage()))
                        .build();
            case "signomix.exception.device.create":
                return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorMessage(e.getMessage()))
                        .build();
            case "signomix.exception.device.update":
                return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorMessage(e.getMessage()))
                        .build();
            case "signomix.exception.api.param.missing":
                return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorMessage(e.getMessage()))
                        .build();
            case "signomix.exception.api.unauthorized":
                return Response.status(Response.Status.FORBIDDEN).entity(new ErrorMessage(e.getMessage()))
                        .build();
            default:
                return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorMessage(e.getMessage()))
                        .build();
        }
    }
}
