import spock.lang.*
import grails.plugin.spock.*

import org.codehaus.groovy.grails.plugins.PluginManagerHolder

class SessionPurgingOnReloadSpec extends FunctionalSpec {

	def grailsApplication

	void testSessionPurgingOnReload() {
		when: "requesting a page using the session scoped service proxy"
		get("/")
		then: "the request is successful"
		response.statusCode == 200
		
		when: "the session scoped service is reloaded"
		reload(SessionScopedService)
		and: "requesting a page using the session scoped service proxy in the same session"
		get("/")
		then: "the request is successful"
		// It the session purging didn't happen, we will get a 500 
		// here due to a ClassCastException
		response.statusCode == 200
	}
	
	protected reload(clazz) {
		def newClazz = grailsApplication.classLoader.reloadClass(clazz.name)
		PluginManagerHolder.pluginManager.informOfClassChange(newClazz)
	}

}