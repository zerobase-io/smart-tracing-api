package io.zerobase.smarttracing.pdf

import com.google.common.io.Resources
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.zerobase.smarttracing.utils.LoggerDelegate
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import org.w3c.tidy.Tidy
import org.xhtmlrenderer.pdf.ITextRenderer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URL
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.*

class DocumentFactory(private val templateEngine: TemplateEngine, private val xhtmlConverter: Tidy) {
    fun siteOnboarding(qrCode: ByteArray): SiteOnboarding = SiteOnboarding(qrCode, templateEngine, xhtmlConverter)
}

sealed class Document(private val templateEngine: TemplateEngine, private val xhtmlConverter: Tidy) {
    companion object {
        val log by LoggerDelegate()

    }

    protected abstract val templateLocation: URL
    protected abstract val context: Context

    fun render(): ByteArray {
        log.debug("rendering document: {}", this::class)
        val html = templateEngine.process("${templateLocation}/main", context)
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

@SuppressFBWarnings("EI_EXPOSE_REP2")
class SiteOnboarding internal constructor(private val qrCode: ByteArray, templateEngine: TemplateEngine, xhtmlConverter: Tidy)
    : Document(templateEngine, xhtmlConverter) {
    override val templateLocation: URL = Resources.getResource("qr-code")
    override val context: Context by lazy {
        val tempPath = Files.createTempFile("zb-", ".png")
        Files.write(tempPath, qrCode)
        Context(Locale.US, mapOf("qrCode" to tempPath.toString()))
    }
}
