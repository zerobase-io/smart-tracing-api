package io.zerobase.smarttracing.notifications

import com.google.common.eventbus.AllowConcurrentEvents
import com.google.common.eventbus.Subscribe
import io.zerobase.smarttracing.models.ContactInfo
import io.zerobase.smarttracing.models.SimpleOrganizationCreated
import io.zerobase.smarttracing.utils.LoggerDelegate


class NotificationManager(private val emailSender: EmailSender,
                          private val notificationFactory: NotificationFactory
) {
    companion object {
        val log by LoggerDelegate()
    }

    fun send(contact: ContactInfo, notification: Notification) {
        if (contact.email == null) {
            log.info("No email for sending notification. Aborting. contact={} notification={}", contact, notification)
            return
        }

        val renderedNotification = notification.render(medium = NotificationMedium.EMAIL)
        log.debug("notification rendered. sending email...")
        emailSender.sendEmail(notification.subject, contact.email, renderedNotification, attachment = notification.attachments)
    }

    @Subscribe
    @AllowConcurrentEvents
    fun handleNotificationRequest(event: SimpleOrganizationCreated) {
        log.debug("got notification that new simple business was created. sending onboarding notification. organization={}",
            event.organization)
        val notification = notificationFactory.simpleBusinessOnboarding(event.organization, event.defaultQrCode)
        send(event.organization.contactInfo, notification)
    }
}
