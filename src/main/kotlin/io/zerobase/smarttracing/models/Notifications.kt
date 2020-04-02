package io.zerobase.smarttracing.models

import com.github.mustachejava.MustacheFactory
import com.github.mustachejava.Mustache
import java.io.StringWriter

enum class NotificationMedium {
    EMAIL
}

sealed class Notification {
    abstract fun render(medium: NotificationMedium = NotificationMedium.EMAIL): String
}

class OrganizationOnboardingNotification(val organizationName: String, val mustacheFactory: MustacheFactory): Notification() {
    private val emailTemplate = "notifications/organization-onboarding/email.mustache"

    override fun render(medium: NotificationMedium): String {
        when (medium) {
            NotificationMedium.EMAIL -> {
                val mustache = mustacheFactory.compile(emailTemplate)
                val stringWriter = StringWriter()
                mustache.execute(stringWriter, listOf(organizationName))
                return stringWriter.toString()
            } else ->
                throw UnsupportedNotificationException("Notification not supported")
        }
    }
}

class OrganizationQRCodeNotification(val organizationName: String, val mustacheFactory: MustacheFactory): Notification() {
    private val emailTemplate = "notifications/organization-onboarding/email.mustache"

    override fun render(medium: NotificationMedium): String {
        when (medium) {
            NotificationMedium.EMAIL -> {
                val mustache = mustacheFactory.compile(emailTemplate)
                val stringWriter = StringWriter()
                mustache.execute(stringWriter, listOf(organizationName))
                return stringWriter.toString()
            } else ->
                throw UnsupportedNotificationException("Notification not supported")
        }
    }
}

class UserJoinNotification(val userName: String, val mustacheFactory: MustacheFactory): Notification() {
    private val emailTemplate = "notifications/user-join/email.mustache"

    override fun render(medium: NotificationMedium): String {
        when (medium) {
            NotificationMedium.EMAIL -> {
                val mustache = mustacheFactory.compile(emailTemplate)
                val stringWriter = StringWriter()
                mustache.execute(stringWriter, listOf(userName))
                return stringWriter.toString()
            } else ->
                throw UnsupportedNotificationException("Notification not supported")
        }
    }
}

class UserDeleteNotification(val userName: String, val mustacheFactory: MustacheFactory): Notification() {
    private val emailTemplate = "notifications/user-delete/email.mustache"

    override fun render(medium: NotificationMedium): String {
        when (medium) {
            NotificationMedium.EMAIL -> {
                val mustache = mustacheFactory.compile(emailTemplate)
                val stringWriter = StringWriter()
                mustache.execute(stringWriter, listOf(userName))
                return stringWriter.toString()
            } else ->
                throw UnsupportedNotificationException("Notification not supported")
        }
    }
}
