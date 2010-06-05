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
import grails.plugin.spock.*
import spock.lang.*

import org.codehaus.groovy.grails.plugins.PluginManagerHolder
import org.codehaus.groovy.grails.test.support.GrailsTestRequestEnvironmentInterceptor
import org.springframework.transaction.interceptor.TransactionAspectSupport

class ScopedProxyGrailsPluginSpec extends IntegrationSpec {

	static transactional = false
	
	def grailsApplication
	def sessionFactory
	
	@Unroll("proxy was created for '#name' = #created")
	void proxyGeneration() {
		expect:
		(getProxyForService(name) != null) == created
		
		where:
		name | created
		'nonProxyableScopedService' | false
		'proxyableNonScopedService' | true
		'transactionalProxyableScopedService' | true
		'nonTransactionalProxyableScopedService' | true
	}

	void classReloading() {
		expect: "there is no proxy"
		getProxyForService('nonProxyableScopedService') == null
		
		when: "the proxy flag is set to true and the service is reloaded"
		NonProxyableScopedService.proxy = true
		reload(NonProxyableScopedService)
		
		then: "there is a proxy"
		getProxyForService('nonProxyableScopedService').var == 0

		cleanup:
		NonProxyableScopedService.proxy = false
		grailsApplication.mainContext.removeBeanDefinition(getProxyNameForService('nonProxyableScopedService'))
		assert getProxyForService('nonProxyableScopedService') == null
	}

	void proxyActuallyAccessingDifferentObjectsWhenScopeChanges() {
		given: "a request scoped proxy"
		def proxy = getProxyForService('nonTransactionalProxyableScopedService')
		
		when: "a var is set to a value"
		proxy.var = 5
		then: "it is read as the same value"
		proxy.var == 5
		
		when: "working on a different request"
		simulateNewRequest()
		then: "the value is different when accessed throught the same proxy"
		proxy.var == 0
	}
	
	void proxiesOfTransactionalServicesAreTransactional() {
		given: "a transactional proxy and a non-transactional proxy"
		def transactionalProxy = grailsApplication.mainContext['transactionalProxyableScopedService']
		def nonTransactionalProxy = getProxyForService('nonTransactionalProxyableScopedService')
		
		and: "we are not running inside a transaction"
		isInTransaction() == false
		
		when: "a method is called on a non transactional proxy"
		def wasInTransaction = true // true just to verify closure below gets invoked
		nonTransactionalProxy.call {
			wasInTransaction = isInTransaction()
		}
		
		then: "it does NOT operate inside a transaction"
		wasInTransaction == false
		
		when: "a method is called on a transactional proxy"
		wasInTransaction = false
		transactionalProxy.call {
			wasInTransaction = isInTransaction()
		}
		
		then: "it does operate inside a transaction"
		wasInTransaction == true
	}
	
	protected isInTransaction() {
		try {
			TransactionAspectSupport.currentTransactionStatus()
			true
		} catch (Exception e) {
			false
		}
	}
		
	protected getProxyForService(serviceBeanName) {
		grailsApplication.mainContext[getProxyNameForService(serviceBeanName)]
	}
	
	protected getProxyNameForService(serviceBeanName) {
		serviceBeanName + "Proxy"
	}
	
	protected reload(clazz) {
		PluginManagerHolder.pluginManager.informOfClassChange(clazz)
	}
	
	protected simulateNewRequest() {
		def i = new GrailsTestRequestEnvironmentInterceptor(grailsApplication.mainContext)
		i.destroy()
		i.init()
	}
}