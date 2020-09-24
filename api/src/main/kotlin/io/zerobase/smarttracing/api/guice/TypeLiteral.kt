package io.zerobase.smarttracing.api.guice

import com.google.inject.TypeLiteral

inline fun <reified T> typeLiteral() = object : TypeLiteral<T>() {}
