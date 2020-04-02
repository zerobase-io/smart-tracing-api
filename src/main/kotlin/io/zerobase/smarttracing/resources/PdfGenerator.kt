package io.zerobase.smarttracing.resources

import com.google.zxing.WriterException
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import org.thymeleaf.templatemode.TemplateMode
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import org.w3c.tidy.Tidy
import org.xhtmlrenderer.pdf.ITextRenderer
import java.io.*
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
import java.util.*

/**
 * generate a PDF using Flying Saucer
 * and Thymeleaf templates. The PDF will display a letter styled with
 * CSS. The letter has two pages and will contain text and images.
 *
 *
 * Simply run this test to generate the PDF. The file is called:
 *
 *
 * /test.pdf
 */
class GeneratePdf {
    @Throws(Exception::class)
    fun generatePdf(businessname: String, town: String, state: String, outputfile: String) {
        val genQR = GenerateQRCode(ZB_LOGO_IMAGE_PATH, QR_CODE_IMAGE_PATH)
        try {
            genQR.generateQRCodeImage(QR_VALUE, QR_WIDTH, QR_HEIGHT)
        } catch (e: WriterException) {
            println("Could not generate QR Code, WriterException :: " + e.message)
        } catch (e: IOException) {
            println("Could not generate QR Code, IOException :: " + e.message)
        }
        // We set-up a Thymeleaf rendering engine. All Thymeleaf templates
// are HTML-based files located under "src/test/resources". Beside
// of the main HTML file, we also have partials like a footer or
// a header. We can re-use those partials in different documents.
        val templateResolver = ClassLoaderTemplateResolver()
        templateResolver.prefix = "/"
        templateResolver.suffix = ".html"
        templateResolver.templateMode = TemplateMode.HTML
        templateResolver.characterEncoding = UTF_8
        val templateEngine = TemplateEngine()
        templateEngine.setTemplateResolver(templateResolver)
        val data = organizationData(businessname, town, state)
        val context = Context()
        val dataMap = HashMap<String, Any>()
        dataMap["data"] = data
        dataMap["logoPath"] = QR_CODE_IMAGE_PATH
        context.setVariables(dataMap)
        // Flying Saucer needs XHTML - not just normal HTML. To make our life
// easy, we use JTidy to convert the rendered Thymeleaf template to
// XHTML. Note that this might not work for very complicated HTML. But
// it's good enough for a simple letter.

        val renderedHtmlContent = templateEngine.process("template", context)
        val xHtml = convertToXhtml(renderedHtmlContent)
        val renderer = ITextRenderer()
        // FlyingSaucer has a working directory. If you run this test, the working directory
// will be the root folder of your project. However, all files (HTML, CSS, etc.) are
// located under "/src/test/resources". So we want to use this folder as the working
// directory.
        val baseUrl = FileSystems
                .getDefault()
                .getPath("src", "main", "resources", "pdfconfig")
                .toUri()
                .toURL()
                .toString()
        print(baseUrl);
        renderer.setDocumentFromString(xHtml, baseUrl)
        renderer.layout()
        // And finally, we create the PDF:
        val outputStream: OutputStream = FileOutputStream(outputfile)
        renderer.createPDF(outputStream)
        outputStream.close()
    }

    private fun convertToXhtml(html: String): String {
        try{
            val tidy = Tidy()
            tidy.inputEncoding = UTF_8
            tidy.outputEncoding = UTF_8
            tidy.xhtml = true
            val inputStream = ByteArrayInputStream(html.toByteArray(StandardCharsets.UTF_8))
            val outputStream = ByteArrayOutputStream()
            tidy.parseDOM(inputStream, outputStream)
            return outputStream.toString(UTF_8)
        } catch (e: UnsupportedEncodingException){
            throw Exception(e)
        }
    }

    private fun organizationData(businessname: String, state: String, town: String): Data {
        val data = Data()
        data.businessname = businessname
        data.state = state
        data.town = town
        return data
    }

    internal class Data {
        var businessname: String? = null
        var town: String? = null
        var state: String? = null

    }

    companion object {
        private const val UTF_8 = "UTF-8"
        private const val QR_CODE_IMAGE_PATH = "src/main/resources/pdfconfig/qr/processed/qr_logo_overlay.png"
        private const val ZB_LOGO_IMAGE_PATH = "src/main/resources/pdfconfig/qr/rawfiles/zerobase_qr_logo.png"
        private const val QR_VALUE = "https://zerobase.io/"
        private const val QR_WIDTH = 1000 // also makes qr overlay better quality with increase in qr width:height ratio
        private const val QR_HEIGHT = 1000
        @JvmStatic
        fun main(args: Array<String>) {
            val businesssname = "Test101"
            val town = "Testingtown"
            val state = "Testingstate"
            val outputfile = args[0]
            try {
                GeneratePdf().generatePdf(businesssname, town, state, outputfile)
            } catch (e: WriterException) {
                println("Could not generate pdf, WriterException :: " + e.message)
            } catch (e: IOException) {
                println("Could not generate pdf, IOException :: " + e.message)
            }
        }
    }
}