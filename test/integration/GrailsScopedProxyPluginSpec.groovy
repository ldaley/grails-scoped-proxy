import grails.plugin.spock.*
import spock.lang.*

import org.codehaus.groovy.grails.plugins.PluginManagerHolder
import org.codehaus.groovy.grails.test.support.GrailsTestRequestEnvironmentInterceptor

class GrailsScopedProxyPluginSpec extends IntegrationSpec {

	def grailsApplication
	
	@Unroll("proxy was created for '#name' = #created")
	void proxyGeneration() {
		expect:
		(getProxyForService(name) != null) == created
		
		where:
		name | created
		'proxyableNonScopedService' | false
		'nonProxyableScopedService' | false
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

		when: "the service is reloaded (without changing the proxy flag)"
		reload(NonProxyableScopedService)
		
		then: "there is still a proxy"
		getProxyForService('nonProxyableScopedService').var == 0

		when: "the proxy flag back is changed to false and the service is reloaded"
		NonProxyableScopedService.proxy = false
		reload(NonProxyableScopedService)
		
		then: "there is no longer a proxy"
		getProxyForService('nonProxyableScopedService') == null
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