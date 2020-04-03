package io.zerobase.smarttracing.notifications

import com.google.common.eventbus.AllowConcurrentEvents
import com.google.common.eventbus.Subscribe
import io.zerobase.smarttracing.models.ContactInfo
import io.zerobase.smarttracing.models.SimpleOrganizationCreated


class NotificationManager(private val emailSender: EmailSender,
                          private val notificationFactory: NotificationFactory
) {

    fun send(contact: ContactInfo, notification: Notification) {
        if (contact.email == null) {
            return
        }

        val renderedNotification = notification.render(medium = NotificationMedium.EMAIL)
        emailSender.sendEmail(notification.subject, contact.email, renderedNotification, attachment = notification.attachments)
    }

    @Subscribe
    @AllowConcurrentEvents
    fun handleNotificationRequest(event: SimpleOrganizationCreated) {
        val notification = notificationFactory.simpleBusinessOnboarding(event.orgnization, event.defaltQrCode)
        send(event.orgnization.contactInfo, notification)
    }

}
