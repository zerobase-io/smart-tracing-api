package io.zerobase.smarttracing.notifications

interface EmailSender {
    fun sendEmail(subject: String,
                  toAddress: String,
                  body: String,
                  contentType: String = "text/html; charset=UTF-8",
                  attachment: List<Attachment>
    )
}
