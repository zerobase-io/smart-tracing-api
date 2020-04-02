package io.zerobase.smarttracing.notifications

import com.github.mustachejava.MustacheFactory
import com.google.common.net.MediaType
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings

enum class NotificationMedium {
    EMAIL
}

class NotificationFactory(private val templateFactory: MustacheFactory) {

}

@SuppressFBWarnings("EI_EXPOSE_REP", "EI_EXPOSE_REP2")
class Attachment(val array: ByteArray, val name: String, val contentType: MediaType)

sealed class Notification {
    abstract val subject: String
    abstract val attachment: Attachment?
    abstract fun render(medium: NotificationMedium = NotificationMedium.EMAIL): String
}
