package grails.plugin.scopedproxy

import org.springframework.core.SmartClassLoader

/**
 * Spring AOP proxy generation looks for class loaders of this type to determine
 * whether proxies for classes can be cached or not. This implementation says that
 * every class is always reloadable so completely disables caching.
 */
class AlwaysReloadableSmartClassLoader extends ClassLoader implements SmartClassLoader {

	AlwaysReloadableSmartClassLoader(ClassLoader parent) {
		super(parent)
	}

	boolean isClassReloadable(Class clazz) {
		true
	}

}