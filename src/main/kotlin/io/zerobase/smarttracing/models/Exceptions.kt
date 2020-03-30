package io.zerobase.smarttracing.models

class InvalidPhoneNumberException(message: String) : Exception(message)

class EntityCreationException(message: String? = null, cause: Throwable? = null): Exception(message, cause)

class InvalidIdException(val id: Id): Exception("The provided id [$id] was not found.")
