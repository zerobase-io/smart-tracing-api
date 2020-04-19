package io.zerobase.smarttracing.features.notifications

import com.google.common.io.Resources
import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Scopes
import com.google.inject.Singleton
import io.zerobase.smarttracing.config.AmazonEmailConfig
import io.zerobase.smarttracing.config.S3Config
import io.zerobase.smarttracing.notifications.*
import io.zerobase.smarttracing.qr.QRCodeGenerator
import org.thymeleaf.TemplateEngine
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import org.w3c.tidy.Tidy
import ru.vyarus.dropwizard.guice.module.yaml.bind.Config
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.ses.SesClient
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.*
import javax.mail.Session
import javax.ws.rs.core.UriBuilder

class NotificationModule : AbstractModule() {
    override fun configure() {
        bind(Session::class.java).toInstance(Session.getDefaultInstance(Properties()))
        bind(EmailSender::class.java).to(AmazonEmailSender::class.java).`in`(Scopes.SINGLETON)
        bind(StaticResourceLoader::class.java).to(S3StaticResourceLoader::class.java)
        bind(NotificationManager::class.java).`in`(Scopes.SINGLETON)
    }

    @Provides
    @Singleton
    fun sesClient(@Config config: AmazonEmailConfig): SesClient {
        val sesClientBuilder = SesClient.builder().region(config.region)
        config.endpoint?.let(sesClientBuilder::endpointOverride)
        return sesClientBuilder.build()
    }

    @Provides
    @Singleton
    fun s3Client(@Config config: S3Config): S3Client {
        val s3ClientBuilder = S3Client.builder().region(config.region)
        config.endpoint?.let(s3ClientBuilder::endpointOverride)
        return s3ClientBuilder.build()
    }

    @Provides
    @Singleton
    fun templateEngine(): TemplateEngine {
        val resolver = ClassLoaderTemplateResolver().apply {
            suffix = ".html"
            characterEncoding = StandardCharsets.UTF_8.displayName()
        }
        return TemplateEngine().apply {
            templateResolvers = setOf(resolver)
        }
    }

    @Provides
    @Singleton
    fun tidy(): Tidy = Tidy().apply {
        inputEncoding = StandardCharsets.UTF_8.displayName()
        outputEncoding = StandardCharsets.UTF_8.displayName()
        xhtml = true
    }

    @Provides
    @Singleton
    fun qrCodeGenerator(@Config("baseQrCodeLink") baseLink: URI): QRCodeGenerator = QRCodeGenerator(
        baseLink = UriBuilder.fromUri(baseLink).path("{code}"),
        logo = Resources.getResource("qr/qr-code-logo.png")
    )
}
