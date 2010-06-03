import grails.plugin.spock.*
import spock.lang.*

class GrailsScopeProxyPluginSpec extends IntegrationSpec {

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

	protected getProxyForService(serviceBeanName) {
		grailsApplication.mainContext[getProxyNameForService(serviceBeanName)]
	}
	
	protected getProxyNameForService(serviceBeanName) {
		serviceBeanName + "Proxy"
	}
}