package io.zerobase.smarttracing

import io.dropwizard.jersey.errors.ErrorMessage
import io.dropwizard.jersey.errors.LoggingExceptionMapper
import io.zerobase.smarttracing.models.InvalidPhoneNumberException
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

class InvalidPhoneNumberExceptionMapper: LoggingExceptionMapper<InvalidPhoneNumberException>() {
    override fun toResponse(exception: InvalidPhoneNumberException?): Response {
        val id = logException(exception)
        return Response.status(Response.Status.BAD_REQUEST.statusCode)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(ErrorMessage(formatErrorMessage(id, exception)))
                .build()
    }
}
