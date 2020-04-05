package io.zerobase.smarttracing.pdf

import com.google.common.io.Resources
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.zerobase.smarttracing.models.Organization
import io.zerobase.smarttracing.utils.LoggerDelegate
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import org.w3c.tidy.Tidy
import org.xhtmlrenderer.pdf.ITextRenderer
import java.io.*
import java.net.URI
import java.net.URL
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.*
import kotlin.collections.HashMap

class DocumentFactory(private val templateEngine: TemplateEngine, private val xhtmlConverter: Tidy) {
    fun siteOnboarding(organization: Organization, qrCode: ByteArray): SiteOnboarding = SiteOnboarding(organization, qrCode, templateEngine, xhtmlConverter)
}

sealed class Document(private val templateEngine: TemplateEngine, private val xhtmlConverter: Tidy) {
    companion object {
        val log by LoggerDelegate()

    }

    protected abstract val name: String
    protected abstract val context: Context

    protected open val templateLocation: URL
        get() = Resources.getResource("pdfs/$name/")

    fun render(): ByteArray {
        log.debug("rendering document: {}", this::class)
        val generalContextMap = mutableMapOf<String, Any>()
        context.setVariables(generalContextMap)

        // template engine parses; don't need template location because that's already set in main with prefix
        val html = templateEngine.process("pdfs/$name/main", context)
        val xhtml = convertToXhtml(html)
        log.trace("rendered html. converting to pdf...")
        val renderer = ITextRenderer().apply {
            setDocumentFromString(xhtml, templateLocation.toString())
        }
        renderer.layout()
        val byteOutputStream = ByteArrayOutputStream()
        byteOutputStream.use(renderer::createPDF)
        log.debug("pdf created successfully")

        return byteOutputStream.toByteArray()
    }

    private fun convertToXhtml(html: String): String {
        val inputStream = ByteArrayInputStream(html.toByteArray(StandardCharsets.UTF_8))
        val outputStream = ByteArrayOutputStream()
        xhtmlConverter.parseDOM(inputStream, outputStream)
        return outputStream.toString(StandardCharsets.UTF_8)
    }
}

@SuppressFBWarnings("EI_EXPOSE_REP2") // ask david about this value annotation
class SiteOnboarding internal constructor(
    private val organization: Organization,
    private val qrCode: ByteArray,
    templateEngine: TemplateEngine,
    xhtmlConverter: Tidy
) : Document(templateEngine, xhtmlConverter) {
    override val name: String = "qr-code"
    override val context: Context by lazy {
        log.debug("creating qr code file")
        val tempPath = Files.createTempFile("zb-", ".png")
        Files.write(tempPath, qrCode)
        log.debug("qr written to temp path: $tempPath")

        Context(Locale.US, mapOf(
            "qrCode" to tempPath.toUri().toString(),
            "organizationName" to organization.name,
            "administrativeArea" to organization.address.administrativeArea)
        )
    }
}
