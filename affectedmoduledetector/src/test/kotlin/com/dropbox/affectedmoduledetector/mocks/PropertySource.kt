package com.dropbox.affectedmoduledetector.mocks

import org.gradle.api.provider.Provider

/**
 * A property source that can be used to provide a value for a property.  This makes it
 * a bit easier for [MockProperty] to manage.
 */
sealed interface PropertySource<out T> {
    fun get(): T?
}

class ProviderSource<out T>(private val provider: Provider<T>) : PropertySource<T> {
    override fun get(): T? {
        return provider.get()
    }
}

class ValueSource<out T>(private val value: T?) : PropertySource<T> {
    override fun get(): T? {
        return value
    }
}
