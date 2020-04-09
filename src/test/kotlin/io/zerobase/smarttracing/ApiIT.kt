package io.zerobase.smarttracing

import io.dropwizard.testing.ConfigOverride
import io.dropwizard.testing.junit5.DropwizardAppExtension
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport
import io.zerobase.smarttracing.models.Address
import io.zerobase.smarttracing.resources.Contact
import io.zerobase.smarttracing.resources.CreateOrganizationRequest
import io.zerobase.smarttracing.resources.OrganizationsResource
import io.zerobase.smarttracing.resources.SiteResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.containers.localstack.LocalStackContainer.Service.S3
import org.testcontainers.containers.localstack.LocalStackContainer.Service.SES
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import javax.ws.rs.client.Entity
import javax.ws.rs.core.GenericType
import javax.ws.rs.core.UriBuilder

class KGenericContainer(imageName: String) : GenericContainer<KGenericContainer>(imageName)

@Testcontainers
@ExtendWith(DropwizardExtensionsSupport::class)
class ApiIt {
    companion object {
        @JvmStatic
        @Container
        val database: KGenericContainer = KGenericContainer("tinkerpop/gremlin-server:latest")
            .withExposedPorts(8182)
            .withClasspathResourceMapping(
                "tinkergraph-overrides.properties",
                "/opt/gremlin-server/conf/tinkergraph-empty.properties",
                BindMode.READ_ONLY
            )

        @JvmStatic
        @Container
        val aws = LocalStackContainer().withServices(SES, S3)

        @JvmStatic
        val app = DropwizardAppExtension(Main::class.java, "src/main/resources/config.yml",
            ConfigOverride.config("server.connector.port", "0"),
            ConfigOverride.config("allowedOrigins", "'*'"),
            ConfigOverride.config("database.endpoints.write", database::getContainerIpAddress),
            ConfigOverride.config("database.port") { "${database.getMappedPort(8182)}" },
            ConfigOverride.config("database.enableAwsSigner", "false"),
            ConfigOverride.config("database.enableSsl", "false"),
            ConfigOverride.config("baseQrCodeLink", "http://zerobase.test"),
            ConfigOverride.config("aws.ses.region") { aws.getEndpointConfiguration(SES).signingRegion },
            ConfigOverride.config("aws.ses.endpoint") { aws.getEndpointConfiguration(SES).serviceEndpoint },
            ConfigOverride.config("aws.s3.region") { aws.getEndpointConfiguration(S3).signingRegion },
            ConfigOverride.config("aws.s3.endpoint") { aws.getEndpointConfiguration(S3).serviceEndpoint }
        )
    }

    @Test
    fun shouldHaveHealthchecks() {
        val response = app.client().target("http://localhost:${app.adminPort}/admin/healthcheck").request().get()
        assertThat(response.status).isEqualTo(200)
    }

    @Test
    fun shouldCreateOrganization() {
        val request = CreateOrganizationRequest("test-org", Contact("+12225551234", "test@zerobase.io", "Testy McTesterson"),
            Address("123", "Happy St", "Fryburg", "CA", "90210", "USA"),
            hasTestingFacilities = false, hasMultipleSites = false
        )

        val createResponse: Map<String,String> = app.client().target(UriBuilder.fromUri("http://localhost:${app.getPort(0)}")
            .path(OrganizationsResource::class.java)
            .build()).request().post(Entity.json(request), object: GenericType<Map<String,String>>(){})

        assertThat(createResponse).isNotNull.containsKey("id")

        val sites = app.client().target(UriBuilder.fromUri("http://localhost:${app.getPort(0)}")
            .path(OrganizationsResource::class.java).path(OrganizationsResource::class.java, "getSites")
            .build(createResponse["id"])).request().get(object: GenericType<List<SiteResponse>>(){})

        assertThat(sites).isNotNull.isNotEmpty.hasSize(1)
    }
}
