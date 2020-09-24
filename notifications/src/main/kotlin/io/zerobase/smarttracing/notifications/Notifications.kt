package io.zerobase.smarttracing.notifications

import com.google.common.io.Resources
import com.google.common.net.MediaType
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.zerobase.smarttracing.common.models.ContactInfo
import io.zerobase.smarttracing.common.models.Organization
import io.zerobase.smarttracing.notifications.pdf.DocumentFactory
import io.zerobase.smarttracing.notifications.qr.QRCodeGenerator
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import java.io.InputStream
import java.util.*

data class NotificationRequest(val notification: Notification, val contactInfo: ContactInfo)

enum class NotificationMedium {
    EMAIL
}

class NotificationFactory(
            private val templateEngine: TemplateEngine,
            private val documentFactory: DocumentFactory,
            private val qrCodeGenerator: QRCodeGenerator,
            private val staticResourceLoader: StaticResourceLoader
    ) {

    fun simpleBusinessOnboarding(organization: Organization, defaultQrCode: String)
        = SimpleBusinessOnboarding(templateEngine, documentFactory, qrCodeGenerator, staticResourceLoader, organization, defaultQrCode)
}

@SuppressFBWarnings("EI_EXPOSE_REP", "EI_EXPOSE_REP2")
class Attachment(val data: InputStream, val name: String, val contentType: MediaType)

sealed class Notification {
    abstract val subject: String
    open val attachments: List<Attachment> = listOf()
    abstract fun render(medium: NotificationMedium = NotificationMedium.EMAIL): String
}

class SimpleBusinessOnboarding(
        private val templateEngine: TemplateEngine,
        private val documentFactory: DocumentFactory,
        private val qrCodeGenerator: QRCodeGenerator,
        private val staticResourceLoader: StaticResourceLoader,
        private val organization: Organization,
        private val qrCodeId: String
) : Notification() {
    override val subject = "Welcome to Zerobase!"
    override val attachments: List<Attachment> by lazy {
        listOf(
            Attachment(
                name = "QR Code Poster.pdf",
                data = qrCodeGenerator.generate(qrCodeId).let { documentFactory.siteOnboarding(organization, it) }.render(),
                contentType = MediaType.PDF
            ),
            Attachment(
                name = "logo.png",
                data = Resources.getResource("logo.png").openStream(),
                contentType = MediaType.PNG
            ),
            Attachment(
                name = "FAQ & Instructions.pdf",
                data = staticResourceLoader.load("notifications/business-faq.pdf"),
                contentType = MediaType.PDF)
        )
    }

    override fun render(medium: NotificationMedium): String {
        when (medium) {
            NotificationMedium.EMAIL -> {
                return templateEngine.process("notifications/simple-business-onboarding/main.html", Context(Locale.US, mapOf(
                    "organizationName" to organization.name,
                    "locality" to organization.address.locality,
                    "administrativeArea" to organization.address.administrativeArea
                )))
            }
        }
    }



}
