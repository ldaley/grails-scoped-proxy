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
import grails.plugin.scopedproxy.ScopedProxyUtils as SPU
import grails.plugin.scopedproxy.TypeSpecifyableTransactionProxyFactoryBean
import grails.plugin.scopedproxy.reload.session.ReloadedScopedBeanSessionPurger
import grails.plugin.scopedproxy.reload.filters.ReloadedScopedBeanFiltersReloader

import org.slf4j.LoggerFactory
import org.codehaus.groovy.grails.orm.support.GroovyAwareNamedTransactionAttributeSource
import org.codehaus.groovy.grails.commons.GrailsClassUtils

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
	def description = 'Adds support for scoped bean proxies (including hot reloading)'
	def documentation = "http://github.com/alkemist/grails-scoped-proxy"

	def doWithSpring = {
		for (serviceClass in application.serviceClasses) {
			buildServiceProxyIfNecessary(delegate, application.classLoader, serviceClass.clazz)
		}

		if (SPU.isEnvironmentClassReloadable()) {
			if (log.infoEnabled) {
				log.info("Registering session purger in application context")
			}
			reloadedScopedBeanSessionPurger(ReloadedScopedBeanSessionPurger) {
				it.autowire = true
			}

			if (log.infoEnabled) {
				log.info("Registering filters reloader in application context")
			}
			reloadedScopedBeanFiltersReloader(ReloadedScopedBeanFiltersReloader) {
				it.autowire = true
			}
		} else {
			if (log.debugEnabled) {
				log.debug("NOT registering session purger in application context (env not reloadable)")
			}
		}
	}

	def doWithWebDescriptor = { webXml ->
		if (SPU.isEnvironmentClassReloadable()) {
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
			def serviceBeanName = GrailsClassUtils.getPropertyName(newClass)
			def proxyName = SPU.getProxyBeanName(serviceBeanName)

			if (log.infoEnabled) {
				log.info("handling change of service class '$newClass.name'")
			}
			
			SPU.fireWillReloadIfNecessary(application, serviceBeanName, proxyName)
			
			def didBuildProxy = false
			def beanDefinitions = beans {
				didBuildProxy = buildServiceProxyIfNecessary(delegate, classLoader, newClass)
			}

			if (didBuildProxy) {
				beanDefinitions.registerBeans(event.ctx)
			}
			
			SPU.fireWasReloadedIfNecessary(application, serviceBeanName, proxyName)
		}
	}

	static private buildServiceProxyIfNecessary(beanBuilder, classLoader, serviceClass) {
		def propertyFetcher = SPU.createPropertyFetcher(serviceClass)
		def wantsProxy = SPU.wantsProxy(propertyFetcher)
		def scope = SPU.getScope(propertyFetcher)

		if (wantsProxy) {
			if (log.infoEnabled) {
				log.info("service class '$serviceClass.name' DOES want a proxy")
			}
			if (SPU.isTransactional(propertyFetcher)) {
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
		SPU.buildProxy(beanBuilder, classLoader, targetBeanName, serviceClass, SPU.getProxyBeanName(targetBeanName))
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

	private static final log = LoggerFactory.getLogger('grails.plugin.scopedproxy.ScopedProxyGrailsPlugin')
}
