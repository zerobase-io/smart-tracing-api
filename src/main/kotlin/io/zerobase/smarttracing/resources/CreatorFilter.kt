package io.zerobase.smarttracing.resources

import io.zerobase.smarttracing.models.IdWrapper
import javax.ws.rs.NameBinding
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerResponseContext
import javax.ws.rs.container.ContainerResponseFilter
import javax.ws.rs.core.HttpHeaders

@NameBinding
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class Creator

@Creator
class CreatorFilter: ContainerResponseFilter {
    override fun filter(req: ContainerRequestContext, resp: ContainerResponseContext) {
        val entity = resp.entity
        if (resp.status == 200 && entity is IdWrapper) {
            resp.status = 201

            resp.headers[HttpHeaders.LOCATION] = listOf(req.uriInfo.requestUriBuilder.path(entity.id.value).build())
        }
    }
}
