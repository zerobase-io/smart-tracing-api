package io.zerobase.smarttracing.resources

import io.zerobase.smarttracing.MultiMap
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/models")
@Produces(MediaType.APPLICATION_JSON)
class ModelsResource(private val siteTypes: MultiMap<String, String>) {

    @Path("/site-types")
    @GET
    fun getSiteTypes(): MultiMap<String, String> {
        return siteTypes;
    }

    @Path("/scannable-types")
    fun getScannableTypes(): List<String> {
        return listOf("QR_CODE")
    }
}
