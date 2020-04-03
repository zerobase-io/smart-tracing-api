package io.zerobase.smarttracing.notifications

import com.google.common.net.MediaType
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.zerobase.smarttracing.models.ContactInfo
import io.zerobase.smarttracing.models.Organization
import io.zerobase.smarttracing.models.ScannableId
import io.zerobase.smarttracing.pdf.DocumentFactory
import io.zerobase.smarttracing.qr.QRCodeGenerator
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import java.util.*

data class NotificationRequest(val notification: Notification, val contactInfo: ContactInfo)

enum class NotificationMedium {
    EMAIL
}

class NotificationFactory(
    private val templateEngine: TemplateEngine,
    private val documentFactory: DocumentFactory,
    private val qrCodeGenerator: QRCodeGenerator
) {
    fun simpleBusinessOnboarding(organization: Organization, defaultQrCode: ScannableId)
        = SimpleBusinessOnboarding(templateEngine, documentFactory, qrCodeGenerator, organization, defaultQrCode)

}

@SuppressFBWarnings("EI_EXPOSE_REP", "EI_EXPOSE_REP2")
class Attachment(val data: ByteArray, val name: String, val contentType: MediaType)

sealed class Notification {
    abstract val subject: String
    open val attachments: List<Attachment> = listOf()
    abstract fun render(medium: NotificationMedium = NotificationMedium.EMAIL): String
}

class SimpleBusinessOnboarding(
    private val templateEngine: TemplateEngine,
    private val documentFactory: DocumentFactory,
    private val qrCodeGenerator: QRCodeGenerator,
    val organization: Organization,
    val qrCodeId: ScannableId
) : Notification() {
    override val subject = "Welcome to Zerobase!"
    override val attachments: List<Attachment> by lazy {
        listOf(
            Attachment(
                name = "QR Code Poster.pdf",
                data = qrCodeGenerator.generate(qrCodeId.value).let(documentFactory::siteOnboarding).render(),
                contentType = MediaType.PDF
            ),
            // TODO: Load the onboarding PDF from a remote source like s3
            Attachment(name = "FAQ & Instructions.pdf", data = ByteArray(0), contentType = MediaType.PDF)
        )
    }

    override fun render(medium: NotificationMedium): String {
        when (medium) {
            NotificationMedium.EMAIL -> {
                return templateEngine.process("simple-business-onboarding/main.html", Context(Locale.US, mapOf(
                    "name" to organization.name,
                    "city" to organization.address.city,
                    "state" to organization.address.administrative
                )))
            }
        }
    }
}
