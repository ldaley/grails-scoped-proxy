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

import grails.plugin.scopedproxy.reload.session.*
import grails.plugin.scopedproxy.reload.ScopedBeanReloadListener

import org.slf4j.LoggerFactory
import org.codehaus.groovy.grails.commons.ClassPropertyFetcher
import org.codehaus.groovy.grails.commons.GrailsClassUtils
import grails.util.Environment
import grails.util.Metadata

class ScopedProxyUtils {

	static PROXY_BEAN_SUFFIX = 'Proxy'
	static DEFAULT_SCOPE = 'singleton'
	static NON_PROXYABLE_SCOPES = ['singleton', 'prototype'].asImmutable()

	static buildProxy(beanBuilder, classLoader, targetBeanName, targetClass, proxyBeanName) {
		beanBuilder.with {
			"$proxyBeanName"(ClassLoaderConfigurableScopedProxyFactoryBean, targetClass) {
				delegate.targetBeanName = targetBeanName
				delegate.classLoader = wrapInSmartClassLoader(classLoader)
				proxyTargetClass = true
			}
		}
	}

	static informListenersOfReload(application, beanName, scope, proxyBeanName) {
		def listeners = application.mainContext.getBeansOfType(ScopedBeanReloadListener)
		if (listeners) {
			listeners.each { listenerName, listener ->
				if (log.infoEnabled) {
					log.info("Informing '$listenerName' of reload of '$beanName'")
				}
				listener.scopedBeanWasReloaded(beanName, scope, proxyBeanName)
			}
		} else {
			if (log.warnEnabled) {
				log.warn("No scoped bean reload listeners found")
			}
		}
	}

	static wantsProxy(Class clazz) {
		wantsProxy(createPropertyFetcher(clazz))
	}

	static wantsProxy(ClassPropertyFetcher propertyFetcher) {
		if (propertyFetcher.getPropertyValue("proxy") == true) {
			def isProxyableScope = isProxyableScope(getScope(propertyFetcher))
			if (isProxyableScope) {
				true
			} else {
				if (log.warnEnabled) {
					// TODO ClassPropertyFetcher in Grails 1.2 has no way to get the 
					// underlying class, should test for 1.3 and display class name
					log.warn("class has 'proxy = true' but is not a proxyable scope")
				}
				false
			}
		} else {
			false
		}
	}

	static isProxyableScope(String scope) {
		!(scope in NON_PROXYABLE_SCOPES)
	}

	static getScope(Class clazz) {
		getScope(createPropertyFetcher(clazz))
	}

	static getScope(ClassPropertyFetcher propertyFetcher) {
		propertyFetcher.getPropertyValue("scope") ?: DEFAULT_SCOPE
	}

	static isTransactional(Class clazz) {
		isTransactional(createPropertyFetcher(clazz))
	}

	static isTransactional(ClassPropertyFetcher propertyFetcher) {
		def transactional = propertyFetcher.getPropertyValue('transactional')
		transactional == null || transactional != false
	}

	static createPropertyFetcher(clazz) {
		// TODO, use the better ClassPropertyFetcher.forClass() if in Grails 1.3+
		new ClassPropertyFetcher(clazz, [getReferenceInstance: { -> clazz.newInstance() }] as ClassPropertyFetcher.ReferenceInstanceCallback)
	}

	static getProxyBeanName(beanName) {
		beanName + PROXY_BEAN_SUFFIX
	}

	static wrapInSmartClassLoader(ClassLoader classLoader) {
		new AlwaysReloadableSmartClassLoader(classLoader)
	}

	static isEnvironmentClassReloadable() {
		def env = Environment.current
		env.reloadEnabled || (Metadata.current.getApplicationName() == "scoped-proxy" && env == Environment.TEST)
	}

	private static final log = LoggerFactory.getLogger(ScopedProxyUtils)
}
