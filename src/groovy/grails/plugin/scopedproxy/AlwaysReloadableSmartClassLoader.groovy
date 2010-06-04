package grails.plugin.scopedproxy

import org.springframework.core.SmartClassLoader

class AlwaysReloadableSmartClassLoader extends ClassLoader implements SmartClassLoader {

	AlwaysReloadableSmartClassLoader(ClassLoader parent) {
		super(parent)
	}

	boolean isClassReloadable(Class clazz) {
		true
	}

}