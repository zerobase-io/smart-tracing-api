package io.zerobase.smarttracing.pdf

import com.google.common.io.Resources
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.zerobase.smarttracing.utils.LoggerDelegate
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import org.w3c.tidy.Tidy
import org.xhtmlrenderer.pdf.ITextRenderer
import java.io.*
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
        // template engine parses; don't need template location because that's already set in main with prefix
        val html = templateEngine.process("/main", context)
        val xhtml = convertToXhtml(html)
        log.trace("rendered html. converting to pdf...")
        val renderer = ITextRenderer().apply {
            setDocumentFromString(xhtml, templateLocation.toString())
        }
        renderer.layout()
        val byteOutputStream = ByteArrayOutputStream()
        byteOutputStream.use(renderer::createPDF)
        log.debug("pdf created successfully")


        // And finally, we create the PDF:
        val outputStream: OutputStream = FileOutputStream("pdfs/zerobase-qr.pdf")

        renderer.createPDF(outputStream)
        outputStream.close()

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
class SiteOnboarding internal constructor(private val qrCode: ByteArray, templateEngine: TemplateEngine, xhtmlConverter: Tidy)
    : Document(templateEngine, xhtmlConverter) {
    override val templateLocation: URL = Resources.getResource("pdfs/qr-code/")
    override val context: Context by lazy {
        log.debug("creating qr code file")
        val tempPath = Files.createTempFile("zb-", ".png")
        Files.write(tempPath, qrCode)
        log.debug("temp path: $tempPath")
        log.debug("qrCode byte array: $qrCode")

        // if we are referencing path from local drive, we'll need to adjust the path stored with the file:/// delimiter
        // example: C:\Users\15108\AppData\Local\Temp\zb-3026035076189344872.png -> file:///C:/Users/15108/AppData/Local/Temp/zb-...png
        // converting the tempPath to URI
        val tempPathCrossPlatform = tempPath.toUri()
        log.debug("tempPath uri$tempPathCrossPlatform")
        Context(Locale.US, mapOf("qrCode" to tempPathCrossPlatform.toString()))
    }
}
