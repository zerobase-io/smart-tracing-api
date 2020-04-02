package io.zerobase.smarttracing.notifications

import io.zerobase.smarttracing.models.ContactInfo


class NotificationManager(private val emailSender: EmailSender) {

    fun send(contact: ContactInfo, notification: Notification) {
        if (contact.email == null) {
            return
        }

        val renderedNotification = notification.render(medium = NotificationMedium.EMAIL)
        emailSender.sendEmail(notification.subject, contact.email, renderedNotification, attachment = notification.attachment)
    }
}
