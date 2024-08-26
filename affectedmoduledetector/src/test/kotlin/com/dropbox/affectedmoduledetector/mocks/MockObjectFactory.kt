package com.dropbox.affectedmoduledetector.mocks

import org.gradle.api.DomainObjectSet
import org.gradle.api.ExtensiblePolymorphicDomainObjectContainer
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.NamedDomainObjectList
import org.gradle.api.NamedDomainObjectSet
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

/**
 * Mock implementation of Gradle's [ObjectFactory] for testing purposes.
 */
class MockObjectFactory : ObjectFactory {
    override fun <T : Named?> named(type: Class<T>, name: String): T {
        TODO("Not yet implemented")
    }

    override fun <T : Any> newInstance(type: Class<out T>, vararg parameters: Any): T {
        TODO("Not yet implemented")
    }

    override fun sourceDirectorySet(name: String, displayName: String): SourceDirectorySet {
        TODO("Not yet implemented")
    }

    override fun fileCollection(): ConfigurableFileCollection {
        TODO("Not yet implemented")
    }

    override fun fileTree(): ConfigurableFileTree {
        TODO("Not yet implemented")
    }

    override fun <T : Any> domainObjectContainer(elementType: Class<T>): NamedDomainObjectContainer<T> {
        TODO("Not yet implemented")
    }

    override fun <T : Any> domainObjectContainer(
        elementType: Class<T>,
        factory: NamedDomainObjectFactory<T>
    ): NamedDomainObjectContainer<T> {
        TODO("Not yet implemented")
    }

    override fun <T : Any> polymorphicDomainObjectContainer(elementType: Class<T>): ExtensiblePolymorphicDomainObjectContainer<T> {
        TODO("Not yet implemented")
    }

    override fun <T : Any> domainObjectSet(elementType: Class<T>): DomainObjectSet<T> {
        TODO("Not yet implemented")
    }

    override fun <T : Any> namedDomainObjectSet(elementType: Class<T>): NamedDomainObjectSet<T> {
        TODO("Not yet implemented")
    }

    override fun <T : Any> namedDomainObjectList(elementType: Class<T>): NamedDomainObjectList<T> {
        TODO("Not yet implemented")
    }

    override fun <T : Any> property(valueType: Class<T>): Property<T> {
        return MockProperty()
    }

    override fun <T : Any> listProperty(elementType: Class<T>): ListProperty<T> {
        TODO("Not yet implemented")
    }

    override fun <T : Any> setProperty(elementType: Class<T>): SetProperty<T> {
        TODO("Not yet implemented")
    }

    override fun <K : Any, V : Any> mapProperty(
        keyType: Class<K>,
        valueType: Class<V>
    ): MapProperty<K, V> {
        TODO("Not yet implemented")
    }

    override fun directoryProperty(): DirectoryProperty {
        TODO("Not yet implemented")
    }

    override fun fileProperty(): RegularFileProperty {
        TODO("Not yet implemented")
    }
}
