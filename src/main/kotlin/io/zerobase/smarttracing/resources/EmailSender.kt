package io.zerobase.smarttracing.resources

interface EmailSender {
    fun sendEmail(arr: ByteArray)
}
