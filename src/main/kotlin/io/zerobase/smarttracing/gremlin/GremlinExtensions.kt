package io.zerobase.smarttracing.gremlin

import org.apache.tinkerpop.gremlin.process.traversal.Traversal

fun <S, T> Traversal<S, T>.getIfPresent(): T? {
    return tryNext().orElse(null)
}

fun <S, T> Traversal<S, T>.execute(): T? {
    return next()
}
