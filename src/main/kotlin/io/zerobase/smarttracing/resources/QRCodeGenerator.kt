package io.zerobase.smarttracing.resources

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.awt.*
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.util.*
import javax.imageio.ImageIO

class QRCodeGenerator(var logoPath: String, // have to use source path
                     var qrCodeFinalPath: String) {
    @Throws(IOException::class)
    private fun getQRCodeWithOverlay(qrcode: BufferedImage) {
        val scaledOverlay = scaleOverlay(qrcode)
        val deltaHeight = qrcode.height - scaledOverlay.height
        val deltaWidth = qrcode.width - scaledOverlay.width
        val combined = BufferedImage(qrcode.width, qrcode.height, BufferedImage.TYPE_INT_ARGB)
        val g2 = combined.graphics as Graphics2D
        g2.drawImage(qrcode, 0, 0, null)
        val overlayTransparency = .9f
        g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, overlayTransparency)
        g2.drawImage(scaledOverlay, Math.round(deltaWidth / 2.toFloat()), Math.round(deltaHeight / 2.toFloat()), null)
        val filePath = qrCodeFinalPath
        val outputfile = File(filePath)
        try {
            ImageIO.write(combined, "png", outputfile)
        } catch (e: IOException) {
            println("image io write io exception: " + e.message)
        }
    }

    @Throws(IOException::class)
    private fun scaleOverlay(qrcode: BufferedImage): BufferedImage {
        val scaledWidth = Math.round(qrcode.width * 2 / 9.toFloat())
        val scaledHeight = Math.round(qrcode.height * 2 / 9.toFloat())
        val filePath = logoPath
        val overlay: Image = ImageIO.read(File(filePath))
        val imageBuff = BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB)
        val g: Graphics = imageBuff.createGraphics()
        //          0 and 0 for i and i1 change the position of the scaled instance; to set transparent must add in alpha param for color
        g.drawImage(overlay.getScaledInstance(scaledWidth, scaledHeight, BufferedImage.SCALE_SMOOTH), 0, 0, Color(0, 0, 0, 0), null)
        g.dispose()
        //          to test if the image is coming out right
        //            File outputfile = new File("src/test/resources/zerobase_qr_logo_processed.png");
        //            ImageIO.write(imageBuff, "png", outputfile);
        return imageBuff
    }

    @Throws(WriterException::class, IOException::class)
    fun generateQRCodeImage(text: String?, width: Int, height: Int) {
        val qrCodeWriter = QRCodeWriter()
        val hints = HashMap<EncodeHintType, ErrorCorrectionLevel?>()
        hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H
        val bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height, hints)
        val bufimgFromBitMatrix = MatrixToImageWriter.toBufferedImage(bitMatrix)
        try {
            getQRCodeWithOverlay(bufimgFromBitMatrix)
        } catch (e: IOException) {
            println("generateQRCodeImage, IOException :: " + e.message)
        }
    }

}