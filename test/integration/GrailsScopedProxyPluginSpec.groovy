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
		expect:
		getProxyForService('nonProxyableScopedService') == null
		
		when:
		NonProxyableScopedService.proxy = true
		PluginManagerHolder.pluginManager.informOfClassChange(NonProxyableScopedService)
		
		then:
		getProxyForService('nonProxyableScopedService') != null

		when:
		PluginManagerHolder.pluginManager.informOfClassChange(NonProxyableScopedService)
		
		then:
		getProxyForService('nonProxyableScopedService') != null

		when:
		NonProxyableScopedService.proxy = false
		PluginManagerHolder.pluginManager.informOfClassChange(NonProxyableScopedService)
		
		then:
		getProxyForService('nonProxyableScopedService') == null
	}

	protected getProxyForService(serviceBeanName) {
		grailsApplication.mainContext[getProxyNameForService(serviceBeanName)]
	}
	
	protected getProxyNameForService(serviceBeanName) {
		serviceBeanName + "Proxy"
	}
}