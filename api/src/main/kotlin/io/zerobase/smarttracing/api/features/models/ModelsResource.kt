package io.zerobase.smarttracing.api.features.models

import com.google.common.collect.Multimap
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/models")
@Produces(MediaType.APPLICATION_JSON)
class ModelsResource(private val siteTypes: Multimap<String, String>, private val scannableTypes: Set<String>) {

    @Path("/site-types")
    @GET
    fun getSiteTypes(): Multimap<String, String> {
        return siteTypes;
    }

    @Path("/scannable-types")
    @GET
    fun getScannableTypes(): Set<String> {
        return scannableTypes
    }
}
