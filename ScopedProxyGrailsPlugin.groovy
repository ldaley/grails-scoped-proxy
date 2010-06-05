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
import org.springframework.aop.scope.ScopedProxyFactoryBean
import org.codehaus.groovy.grails.orm.support.GroovyAwareNamedTransactionAttributeSource
import org.codehaus.groovy.grails.commons.ClassPropertyFetcher
import org.codehaus.groovy.grails.commons.GrailsClassUtils
import grails.util.Environment
import grails.util.Metadata

class ScopedProxyGrailsPlugin {

	def version = "0.2-SNAPSHOT"
	def grailsVersion = "1.2.0 > *"
	def dependsOn = [:]
	def observe = ["services"]
	def loadAfter = ['services']
	def pluginExcludes = [
		"grails-app/**/*"
	]

	def author = "Luke Daley"
	def authorEmail = "ld@ldaley.com"
	def title = "Scoped Proxy Plugin"
	def description = 'Creates proxies for scoped services by convention'
	def documentation = "http://github.com/alkemist/grails-scoped-proxy"

	static PROXY_BEAN_SUFFIX = 'Proxy'

	def doWithSpring = {
		for (serviceClass in application.serviceClasses) {
			buildServiceProxyIfNecessary(delegate, application.classLoader, serviceClass.clazz)
		}

		if (isEnvironmentClassReloadable()) {
			"reloadedScopedBeanSessionPurger"(ReloadedScopedBeanSessionPurger)
		}
	}

	def doWithWebDescriptor = { webXml ->
		if (isEnvironmentClassReloadable()) {
			if (log.debugEnabled) {
				log.debug("Registering session listener in web descriptor for ReloadedScopedBeanSessionPurger")
			}

			def listeners = webXml.'listener'
			def lastListener = listeners[listeners.size()-1]
			listeners + {
				'listener' {
					'listener-class'("grails.plugin.scopedproxy.reload.session.SessionLifecycleListener")
				}
			}
		}
	}

	def onChange = { event ->
		if (application.isServiceClass(event.source)) {
			def classLoader = application.classLoader
			def serviceClass = application.getServiceClass(event.source.name)
			def newClass = classLoader.loadClass(event.source.name, false)
			def proxyName = getProxyBeanName(GrailsClassUtils.getPropertyName(serviceClass.clazz))

			if (log.debugEnabled) {
				log.debug("handling change of service class '$newClass.name'")
			}

			def didBuildProxy = false
			def beanDefinitions = beans {
				didBuildProxy = buildServiceProxyIfNecessary(delegate, classLoader, newClass)
			}

			if (didBuildProxy) {
				beanDefinitions.registerBeans(event.ctx)
			}

			def serviceBeanName = GrailsClassUtils.getPropertyName(newClass)
			informListenersOfReload(application, serviceBeanName, getScope(newClass), proxyName)
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
			if (log.infoEnabled) {
				log.info("No scoped bean reload listeners found")
			}
		}
	}

	static buildProxy(beanBuilder, classLoader, targetBeanName, targetClass, proxyBeanName) {
		beanBuilder.with {
			"$proxyBeanName"(ClassLoaderConfigurableScopedProxyFactoryBean, targetClass) {
				delegate.targetBeanName = targetBeanName
				delegate.classLoader = wrapInSmartClassLoader(classLoader)
				proxyTargetClass = true
			}
		}
	}

	static wantsProxy(Class clazz) {
		wantsProxy(createPropertyFetcher(clazz))
	}

	static wantsProxy(ClassPropertyFetcher propertyFetcher) {
		propertyFetcher.getPropertyValue("proxy") == true
	}

	static getScope(Class clazz) {
		getScope(createPropertyFetcher(clazz))
	}

	static getScope(ClassPropertyFetcher propertyFetcher) {
		propertyFetcher.getPropertyValue("scope")
	}

	static isScoped(Class clazz) {
		isScoped(createPropertyFetcher(clazz))
	}

	static isScoped(ClassPropertyFetcher propertyFetcher) {
		getScope(propertyFetcher) != null
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
		env.reloadEnabled || (Metadata.current.getApplicationName() == "scopedproxy" && env == Environment.TEST)
	}

	static private buildServiceProxyIfNecessary(beanBuilder, classLoader, serviceClass) {
		def propertyFetcher = createPropertyFetcher(serviceClass)
		def wantsProxy = wantsProxy(propertyFetcher)
		def scope = getScope(propertyFetcher)

		if (wantsProxy) {
			if (log.debugEnabled) {
				log.debug("service class '$serviceClass.name' DOES want a proxy")
			}
			if (!scope) {
				if (log.debugEnabled) {
					log.debug("service class '$serviceClass.name' does NOT define a scope, defaulting to 'singleton'")
				}
				scope = "singleton"
			}

			if (isTransactional(propertyFetcher)) {
				buildTransactionalServiceProxy(beanBuilder, classLoader, serviceClass, scope)
			} else {
				buildServiceProxy(beanBuilder, classLoader, serviceClass)
			}
		} else {
			if (log.debugEnabled) {
				log.debug("service class '$serviceClass.name' DOES NOT want a proxy")
			}
		}

		wantsProxy
	}

	static private buildServiceProxy(beanBuilder, classLoader, serviceClass) {
		def targetBeanName = GrailsClassUtils.getPropertyName(serviceClass)
		buildProxy(beanBuilder, classLoader, targetBeanName, serviceClass, getProxyBeanName(targetBeanName))
	}

	static private buildTransactionalServiceProxy(beanBuilder, classLoader, serviceClass, scope) {
		def targetBeanName = GrailsClassUtils.getPropertyName(serviceClass)
		if (log.debugEnabled) {
			log.debug("redefining transactional proxy for '$targetBeanName'")
		}

		beanBuilder.with {
			def props = new Properties()
			props."*" = "PROPAGATION_REQUIRED"
			"${targetBeanName}"(TypeSpecifyableTransactionProxyFactoryBean, serviceClass) { bean ->
				bean.scope = scope
				bean.lazyInit = true
				target = { innerBean ->
					innerBean.lazyInit = true
					innerBean.factoryBean = "${serviceClass.name}ServiceClass"
					innerBean.factoryMethod = "newInstance"
					innerBean.autowire = "byName"
					innerBean.scope = scope
				}
				proxyTargetClass = true
				transactionAttributeSource = new GroovyAwareNamedTransactionAttributeSource(transactionalAttributes: props)
				transactionManager = ref("transactionManager")
			}
		}

		buildServiceProxy(beanBuilder, classLoader, serviceClass)
	}

	private static final log = LoggerFactory.getLogger("grails.plugin.scopedproxy.ScopedProxyGrailsPlugin")
}
