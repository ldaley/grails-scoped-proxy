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
import grails.plugin.scopedproxy.*

import org.slf4j.LoggerFactory
import org.springframework.aop.scope.ScopedProxyFactoryBean
import org.codehaus.groovy.grails.orm.support.GroovyAwareNamedTransactionAttributeSource

class ScopedProxyGrailsPlugin {
	
	def version = "0.1-SNAPSHOT"
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
			if (wantsProxy(serviceClass)) {
				def proxyBeanName = getProxyBeanName(serviceClass)
				if (log.debugEnabled) {
					log.debug("creating proxy for service class '$serviceClass.clazz.name', with name '$proxyBeanName'")
				}
				buildProxyWithName(delegate, serviceClass, proxyBeanName, application.classLoader)
			}
		}
	}

	def onChange = { event ->
		if (application.isServiceClass(event.source)) {
			def serviceClass = application.getServiceClass(event.source.name)
			def proxyBeanName = getProxyBeanName(serviceClass)

			if (wantsProxy(serviceClass)) {
				if (log.debugEnabled) {
					log.debug("re-configuring proxy for service class '$serviceClass.clazz.name', with name '$proxyBeanName'")
				}
				beans { buildProxyWithName(delegate, serviceClass, proxyBeanName, application.classLoader) }.registerBeans(event.ctx)
			}
		}
	}
	
	static buildProxyWithName(beanBuilder, serviceClass, proxyBeanName, classLoader) {
		beanBuilder.with {
			def serviceBeanName = serviceClass.propertyName
			def scope = getScope(serviceClass)
			if (serviceClass.transactional) {
				def props = new Properties()
				props."*" = "PROPAGATION_REQUIRED"
				"${serviceBeanName}"(TypeSpecifyableTransactionProxyFactoryBean, serviceClass.clazz) { bean ->
					bean.scope = scope
					bean.lazyInit = true
					target = { innerBean ->
						innerBean.lazyInit = true
						innerBean.factoryBean = "${serviceClass.fullName}ServiceClass"
						innerBean.factoryMethod = "newInstance"
						innerBean.autowire = "byName"
						innerBean.scope = scope
					}
					proxyTargetClass = true
					transactionAttributeSource = new GroovyAwareNamedTransactionAttributeSource(transactionalAttributes:props)
					transactionManager = ref("transactionManager")
				}
			}
			
			"$proxyBeanName"(ClassLoaderConfigurableScopedProxyFactoryBean) {
				targetBeanName = serviceBeanName
				proxyTargetClass = true
				delegate.classLoader = classLoader
			}
		}
	}

	static wantsProxy(serviceClass) {
		isScoped(serviceClass) && serviceClass.getPropertyValue('proxy') == true
	}
	
	static getScope(serviceClass) {
		serviceClass.getPropertyValue("scope")
	}

	static isScoped(serviceClass) {
		getScope(serviceClass) != null
	}
	
	static getProxyBeanName(serviceClass) {
		serviceClass.propertyName + PROXY_BEAN_SUFFIX
	}

	private static final log = LoggerFactory.getLogger("grails.plugin.scopedproxy.ScopedProxyGrailsPlugin")
}
