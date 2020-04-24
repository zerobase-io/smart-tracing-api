package io.zerobase.smarttracing.features.organizations

import io.zerobase.smarttracing.models.OrganizationId
import io.zerobase.smarttracing.models.ScannableId
import io.zerobase.smarttracing.models.SiteId
import javax.ws.rs.Consumes
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
class ScannablesResource(val orgId: OrganizationId, val siteId: SiteId, private val id: ScannableId, private val dao: OrganizationsDao) {

    @Path("/name")
    @PUT
    fun updateName(name: String) {
        dao.updateEntityName(id, "Scannable", name)
    }
}
