package io.zerobase.smarttracing.api.features.sessions

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.scribejava.apis.GoogleApi20
import com.github.scribejava.core.builder.ServiceBuilder
import com.github.scribejava.core.oauth.OAuth20Service
import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.name.Names
import io.fusionauth.jwt.Signer
import io.zerobase.smarttracing.api.guice.typeLiteral
import ru.vyarus.dropwizard.guice.module.yaml.bind.Config
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.kms.KmsClient
import software.amazon.awssdk.services.ssm.SsmClient
import javax.inject.Named
import javax.inject.Singleton

class AdminManagementModule(private val config: SessionManagementConfig): AbstractModule() {

    override fun configure() {
//        bind(SessionsDao::class.java).to(DynamoSessionsDao::class.java)
        bind(SessionsDao::class.java).to(InMemorySessionDao::class.java)
    }

    @Provides
    @Named("acceptableDomains")
    fun acceptableDomains(): Set<String> = setOf("zerobase.io")

    @Provides
    @Singleton
    fun databaseClient(): DynamoDbClient {
        var builder = DynamoDbClient.builder().region(config.database.region)
        config.database.endpoint?.let(builder::endpointOverride)
        return builder.build()
    }

    @Provides
    @Singleton
    fun ssmClient(): SsmClient {
        var builder = SsmClient.builder().region(config.database.region)
        config.database.endpoint?.let(builder::endpointOverride)
        return builder.build()
    }


    @Provides
    fun oauthConfig(ssm: SsmClient, jsonMapper: ObjectMapper): OauthClient {
        val parameterName: String = config.oauth.client.resource().resource()
        val response = ssm.getParameter { it.name(parameterName).withDecryption(true) }
        val rawParameter: String = response.parameter().value()
        return jsonMapper.readValue(rawParameter)
    }

    @Provides
    @Singleton
    fun oauthImplementation(client: OauthClient): OAuth20Service {
        return ServiceBuilder(client.id)
            .apiSecret(client.secret)
            .defaultScope("profile email") // replace with desired scope
            .callback(config.oauth.callback.toString())
            .build(GoogleApi20.instance());
    }

    @Provides
    @Singleton
    @Named("tableName")
    fun tableName(): String = config.database.tableName

    @Provides
    @Singleton
    fun kmsClient(): KmsClient {
        var builder = KmsClient.builder().region(config.signing.key.region().map(Region::of).orElse(Region.US_EAST_1))
        config.signing.endpoint?.let(builder::endpointOverride)
        return builder.build()
    }

    @Provides
    fun jwtSigner(kms: KmsClient): Signer = KmsJwtSigner(config.signing.key, kms)
}
