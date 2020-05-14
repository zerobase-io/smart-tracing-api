package io.zerobase.smarttracing.notifications

import java.io.InputStream

interface StaticResourceLoader {
    fun load(path: String): InputStream
}
