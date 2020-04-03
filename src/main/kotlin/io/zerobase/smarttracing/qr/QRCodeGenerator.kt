package io.zerobase.smarttracing.qr

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import io.zerobase.smarttracing.utils.LoggerDelegate
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.net.URL
import javax.imageio.ImageIO
import javax.ws.rs.core.UriBuilder
import kotlin.math.roundToInt
import com.google.zxing.Writer as BarcodeWriter


class QRCodeGenerator(private val baseLink: UriBuilder,
                      private val logo: URL,
                      private val writer: BarcodeWriter = QRCodeWriter(),
                      private val options: Map<EncodeHintType, Any> = mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H)
) {
    companion object {
        private val log by LoggerDelegate()
    }

    private fun getQRCodeWithOverlay(qrcode: BufferedImage, overlay: BufferedImage): ByteArray {
        log.debug("adding logo overlay to qr code...")
        val deltaHeight = qrcode.height - overlay.height
        val deltaWidth = qrcode.width - overlay.width
        val combined = BufferedImage(qrcode.width, qrcode.height, BufferedImage.TYPE_INT_ARGB)
        val g2 = combined.graphics as Graphics2D
        g2.drawImage(qrcode, 0, 0, null)
        val overlayTransparency = .9f
        g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, overlayTransparency)
        g2.drawImage(overlay, (deltaWidth / 2f).roundToInt(), (deltaHeight / 2f).roundToInt(), null)
        val outputStream = ByteArrayOutputStream()
        outputStream.use {
            ImageIO.write(combined, "png", outputStream)
            outputStream.flush()
        }
        return outputStream.toByteArray()
    }

    private fun scale(image: BufferedImage, scaledWidth: Int, scaledHeight: Int): BufferedImage {
        log.debug("scaling image from {}x{} to {}x{}", image.width, image.height, scaledWidth, scaledHeight)
        val imageBuff = BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB)
        val g: Graphics = imageBuff.createGraphics()
        try{
            // 0 and 0 for i and i1 change the position of the scaled instance; to set transparent must add in alpha param for color
            g.drawImage(image.getScaledInstance(scaledWidth, scaledHeight, BufferedImage.SCALE_SMOOTH), 0, 0, Color(0, 0, 0, 0), null)
            return imageBuff
        } finally {
            g.dispose()
        }
    }

    fun generate(codeId: String, width: Int = 1000, height: Int = 1000): ByteArray {
        val bitMatrix = writer.encode(baseLink.build(codeId).toString(), BarcodeFormat.QR_CODE, width, height, options)
        val image = MatrixToImageWriter.toBufferedImage(bitMatrix)
        val overlay = ImageIO.read(logo)
        val scaledWidth = (image.width * 2 / 9f).roundToInt()
        val scaledHeight = (image.height * 2 / 9f).roundToInt()

        return getQRCodeWithOverlay(image, scale(overlay, scaledWidth, scaledHeight))
    }
}
