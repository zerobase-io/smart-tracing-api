package io.zerobase.smarttracing.lambdas.notification

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.SNSEvent
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.io.Resources
import io.zerobase.smarttracing.common.LoggerDelegate
import io.zerobase.smarttracing.common.models.SimpleOrganizationCreated
import io.zerobase.smarttracing.common.models.ZerobaseEvent
import io.zerobase.smarttracing.notifications.*
import io.zerobase.smarttracing.notifications.pdf.DocumentFactory
import io.zerobase.smarttracing.notifications.qr.QRCodeGenerator
import org.slf4j.Logger
import org.thymeleaf.TemplateEngine
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import org.w3c.tidy.Tidy
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.ses.SesClient
import java.lang.IllegalStateException
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.*
import javax.mail.Session

fun env(key: String): String? {
    return System.getProperty(key, System.getenv(key))
}

inline fun requiredEnv(key: String): String {
    return env(key) ?: throw IllegalStateException("$key environment variable not set")
}
/**
 * Open for testing to subclass it and override the source of various pieces
 */
open class Main: RequestHandler<SNSEvent, Unit> {
    companion object {
        private val log: Logger by LoggerDelegate()
        val objectMapper = jacksonObjectMapper()
    }

    private val manager: NotificationManager by lazy {
        NotificationManager(
            emailSender(),
            NotificationFactory(templateEngine(), documentFactory(), qrCodeGenerator(), staticResourceLoader())
        )
    }

    protected open fun emailSender(): EmailSender {
        val sesBuilder = SesClient.builder().region(Region.of(env("SES_REGION") ?: env("AWS_REGION")))
        env("SES_ENDPOINT")?.let(URI::create)?.run(sesBuilder::endpointOverride)

        return AmazonEmailSender(sesBuilder.build(), Session.getDefaultInstance(Properties()), requiredEnv("SES_FROM_ADDRESS"))
    }

    protected open fun documentFactory(): DocumentFactory {
        return DocumentFactory(templateEngine(), tidy());
    }

    protected open fun staticResourceLoader(): StaticResourceLoader {
        val s3ClientBuilder = S3Client.builder().region(Region.of(env("S3_REGION") ?: requiredEnv("AWS_REGION")))
        env("S3_ENDPOINT")?.let(URI::create)?.run(s3ClientBuilder::endpointOverride)
        return S3StaticResourceLoader(s3ClientBuilder.build(), requiredEnv("STATIC_RESOURCES_BUCKET"))
    }

    private fun templateEngine(): TemplateEngine {
        val resolver = ClassLoaderTemplateResolver().apply {
            suffix = ".html"
            characterEncoding = StandardCharsets.UTF_8.displayName()
        }
        return TemplateEngine().apply {
            templateResolvers = setOf(resolver)
        }
    }

    private fun tidy(): Tidy = Tidy().apply {
        inputEncoding = StandardCharsets.UTF_8.displayName()
        outputEncoding = StandardCharsets.UTF_8.displayName()
        xhtml = true
    }

    protected open fun qrCodeGenerator(): QRCodeGenerator = QRCodeGenerator(
        baseLink = URI.create(env("BASE_LINK") ?: throw IllegalStateException("BASE_LINK environment variable not set")),
        logo = Resources.getResource("qr/qr-code-logo.png")
    )

    override fun handleRequest(event: SNSEvent, ctx: Context) {
        event.records.forEach { record ->
            val event: ZerobaseEvent = objectMapper.readValue(record.sns.message)
            when (event) {
                is SimpleOrganizationCreated -> manager.handleNotificationRequest(event)
            }
        }
    }
}
