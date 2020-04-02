package io.zerobase.smarttracing.resources

import io.zerobase.smarttracing.models.Attachment

interface EmailSender {
    fun sendEmail(subject: String,
                  toAddress: String,
                  body: String,
                  contentType: String = "text/html; charset=UTF-8",
                  attachment: Attachment? = null)
}
