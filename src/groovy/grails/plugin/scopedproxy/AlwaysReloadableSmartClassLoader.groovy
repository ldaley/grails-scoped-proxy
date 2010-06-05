/*
 * Copyright 2010 Luke Daley
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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