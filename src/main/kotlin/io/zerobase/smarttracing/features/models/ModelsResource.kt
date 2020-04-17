package io.zerobase.smarttracing.features.models

import com.google.common.collect.Multimap
import ru.vyarus.dropwizard.guice.module.yaml.bind.Config
import com.google.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/models")
@Produces(MediaType.APPLICATION_JSON)
class ModelsResource(private val siteTypes: Multimap<String, String>, private val scannableTypes: List<String>) {

    @Path("/site-types")
    @GET
    fun getSiteTypes(): Multimap<String, String> {
        return siteTypes;
    }

    @Path("/scannable-types")
    @GET
    fun getScannableTypes(): List<String> {
        return scannableTypes
    }
}
