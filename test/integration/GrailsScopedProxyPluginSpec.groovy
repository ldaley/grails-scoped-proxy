import grails.plugin.spock.*
import spock.lang.*

import org.codehaus.groovy.grails.plugins.PluginManagerHolder

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
		PluginManagerHolder.pluginManager.informOfClassChange(NonProxyableScopedService)
		
		then: "there is a proxy"
		getProxyForService('nonProxyableScopedService') != null

		when: "the service is reloaded (without changing the proxy flag)"
		PluginManagerHolder.pluginManager.informOfClassChange(NonProxyableScopedService)
		
		then: "there is still a proxy"
		getProxyForService('nonProxyableScopedService') != null

		when: "the proxy flag back is changed to false and the service is reloaded"
		NonProxyableScopedService.proxy = false
		PluginManagerHolder.pluginManager.informOfClassChange(NonProxyableScopedService)
		
		then: "there is no longer a proxy"
		getProxyForService('nonProxyableScopedService') == null
	}

	protected getProxyForService(serviceBeanName) {
		grailsApplication.mainContext[getProxyNameForService(serviceBeanName)]
	}
	
	protected getProxyNameForService(serviceBeanName) {
		serviceBeanName + "Proxy"
	}
}