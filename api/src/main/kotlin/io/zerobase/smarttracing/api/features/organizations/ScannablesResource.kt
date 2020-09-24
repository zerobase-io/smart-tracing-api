package io.zerobase.smarttracing.api.features.organizations

import javax.ws.rs.Consumes
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
class ScannablesResource(val orgId: String, val siteId: String, private val id: String, private val dao: OrganizationsDao) {

    @Path("/name")
    @PUT
    fun updateName(name: String) {
        dao.updateEntityName(id, "Scannable", name)
    }
}
