package com.dropbox.affectedmoduledetector.mocks

import org.gradle.api.Transformer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import java.util.concurrent.atomic.AtomicReference
import java.util.function.BiFunction

/**
 * Mock implementation of the Gradle's [ObjectFactory] for testing purposes.
 */
class MockProperty<T : Any> : Property<T> {
    private val valueSourceRef = AtomicReference<PropertySource<T>?>()
    private val conventionRef = AtomicReference<PropertySource<T>?>()

    override fun get(): T {
        return getOrNull() ?: throw IllegalStateException("Property value is not set")
    }

    override fun getOrNull(): T? {
        return valueSourceRef.get()?.get()
            ?: conventionRef.get()?.get()
    }

    override fun isPresent(): Boolean {
        return getOrNull() != null
    }

    @Deprecated("super is deprecated", ReplaceWith("modern stuff"))
    override fun forUseAtConfigurationTime(): Provider<T> = this

    override fun finalizeValue() {
        // No-op
    }

    override fun finalizeValueOnRead() {
        // No-op
    }

    override fun disallowChanges() {
        // No-op
    }

    override fun disallowUnsafeRead() {
        // No-op
    }

    override fun convention(provider: Provider<out T>): Property<T> = apply {
        conventionRef.set(ProviderSource(provider))
    }

    override fun convention(value: T?): Property<T> = apply {
        conventionRef.set(ValueSource(value))
    }

    override fun value(provider: Provider<out T>): Property<T> = apply {
        set(provider)
    }

    override fun value(value: T?): Property<T> = apply {
        set(value)
    }

    override fun set(provider: Provider<out T>) {
        valueSourceRef.set(ProviderSource(provider))
    }

    override fun set(value: T?) {
        valueSourceRef.set(ValueSource(value))
    }

    override fun <B : Any?, R : Any?> zip(
        provider: Provider<B>,
        biFunction: BiFunction<T, B, R>
    ): Provider<R> {
        TODO("Not yet implemented")
    }

    override fun orElse(provider: Provider<out T>): Provider<T> = apply {
        conventionRef.set(ProviderSource(provider))
    }

    override fun orElse(value: T): Provider<T> = apply {
        conventionRef.set(ValueSource(value))
    }

    override fun <S : Any?> flatMap(transformer: Transformer<out Provider<out S>, in T>): Provider<S> {
        TODO("Not yet implemented")
    }

    override fun <S : Any?> map(transformer: Transformer<out S, in T>): Provider<S> {
        TODO("Not yet implemented")
    }

    override fun getOrElse(defaultValue: T): T {
        return getOrNull() ?: defaultValue
    }
}
