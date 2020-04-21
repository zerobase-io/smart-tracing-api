package io.zerobase.smarttracing.resources

import io.dropwizard.jersey.errors.ErrorMessage
import io.dropwizard.jersey.errors.LoggingExceptionMapper
import io.zerobase.smarttracing.models.InvalidIdException
import io.zerobase.smarttracing.models.InvalidPhoneNumberException
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.ext.Provider

@Provider
class InvalidPhoneNumberExceptionMapper: LoggingExceptionMapper<InvalidPhoneNumberException>() {
    override fun toResponse(exception: InvalidPhoneNumberException?): Response {
        val id = logException(exception)
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(ErrorMessage(formatErrorMessage(id, exception)))
                .build()
    }
}

@Provider
class InvalidIdExceptionMapper: LoggingExceptionMapper<InvalidIdException>() {
    override fun toResponse(exception: InvalidIdException?): Response {
        val id = logException(exception)
        return Response.status(Response.Status.NOT_FOUND)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .entity(ErrorMessage(formatErrorMessage(id, exception)))
            .build()
    }
}
