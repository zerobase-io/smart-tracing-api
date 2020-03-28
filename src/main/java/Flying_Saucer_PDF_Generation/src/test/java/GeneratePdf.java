import com.google.zxing.WriterException;
import org.junit.Test;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.w3c.tidy.Tidy;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.*;
import java.nio.file.FileSystems;

import static org.thymeleaf.templatemode.TemplateMode.HTML;

/**
 * generate a PDF using Flying Saucer
 * and Thymeleaf templates. The PDF will display a letter styled with
 * CSS. The letter has two pages and will contain text and images.
 * <p>
 * Simply run this test to generate the PDF. The file is called:
 * <p>
 * /test.pdf
 */
public class GeneratePdf {

    private static final String OUTPUT_FILE = "src/test/pdfs/zerobase-qr.pdf";
    private static final String UTF_8 = "UTF-8";
    private static final String QR_CODE_IMAGE_PATH = "src/test/resources/qr/zerobase-zerobase-qr.png";
    private static final String QR_VALUE = "https://zerobase.io/";

    public void generatePdf() throws Exception {
        try {
            GenerateQRCode.generateQRCodeImage(QR_VALUE, 350, 350, QR_CODE_IMAGE_PATH);
        } catch (WriterException e) {
            System.out.println("Could not generate QR Code, WriterException :: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Could not generate QR Code, IOException :: " + e.getMessage());
        }

        // We set-up a Thymeleaf rendering engine. All Thymeleaf templates
        // are HTML-based files located under "src/test/resources". Beside
        // of the main HTML file, we also have partials like a footer or
        // a header. We can re-use those partials in different documents.
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(HTML);
        templateResolver.setCharacterEncoding(UTF_8);

        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        Context context = new Context();

        // Flying Saucer needs XHTML - not just normal HTML. To make our life
        // easy, we use JTidy to convert the rendered Thymeleaf template to
        // XHTML. Note that this might not work for very complicated HTML. But
        // it's good enough for a simple letter.
        String renderedHtmlContent = templateEngine.process("template", context);
        String xHtml = convertToXhtml(renderedHtmlContent);

        ITextRenderer renderer = new ITextRenderer();

        // FlyingSaucer has a working directory. If you run this test, the working directory
        // will be the root folder of your project. However, all files (HTML, CSS, etc.) are
        // located under "/src/test/resources". So we want to use this folder as the working
        // directory.
        String baseUrl = FileSystems
                                .getDefault()
                                .getPath("src", "test", "resources")
                                .toUri()
                                .toURL()
                                .toString();
        renderer.setDocumentFromString(xHtml, baseUrl);
        renderer.layout();

        // And finally, we create the PDF:
        OutputStream outputStream = new FileOutputStream(OUTPUT_FILE);

        renderer.createPDF(outputStream);
        outputStream.close();
    }

    private String convertToXhtml(String html) throws UnsupportedEncodingException {
        Tidy tidy = new Tidy();
        tidy.setInputEncoding(UTF_8);
        tidy.setOutputEncoding(UTF_8);
        tidy.setXHTML(true);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(html.getBytes(UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        tidy.parseDOM(inputStream, outputStream);
        return outputStream.toString(UTF_8);
    }

    @Test
    public void main() throws Exception {
        try {
            generatePdf();
        } catch (WriterException e) {
            System.out.println("Could not generate pdf, WriterException :: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Could not generate pdf, IOException :: " + e.getMessage());
        }
    }

}
