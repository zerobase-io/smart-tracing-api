package io.zerobase.smarttracing.resources

import org.apache.commons.lang3.RandomStringUtils
import org.slf4j.MDC
import javax.ws.rs.container.*
import javax.ws.rs.ext.Provider

@Provider
@PreMatching
class TraceIdFilter: ContainerRequestFilter, ContainerResponseFilter {
    override fun filter(requestContext: ContainerRequestContext) {
        MDC.put("traceId", RandomStringUtils.randomAlphanumeric(32))
    }

    override fun filter(requestContext: ContainerRequestContext, responseContext: ContainerResponseContext) {
        MDC.remove("traceId")
    }
}
