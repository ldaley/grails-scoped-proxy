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

import geb.spock.GebSpec
import org.codehaus.groovy.grails.plugins.PluginManagerHolder

class ScopedProxyGrailsPluginSpec extends GebSpec {

	def grailsApplication

	void testSessionPurgingOnReload() {
		when: "requesting a page using the session scoped service proxy"
		go("sessionScopedServiceVar")
		then: "the request is successful"
		title == "sessionScopedServiceVar"
		
		when: "the session scoped service is reloaded"
		//reload(SessionScopedService)
		and: "requesting a page using the session scoped service proxy in the same session"
		go("sessionScopedServiceVar")
		then: "the request is successful"
		// It the session purging didn't happen, we will get a 500 
		// here due to a ClassCastException
		title == "sessionScopedServiceVar"
	}

	void testSessionPurgingOnReloadOfRequestScopedInsideSessionScoped() {
		when: "requesting a page using the request proxy via the session scope"
		go("sessionScopedServiceVar")
		go("requestScopedInsideSessionVar")
		then: "the request is successful"
		driver.pageSource == "<html><head></head><body>requestScopedInsideSessionVar: 0</body></html>"

		when: "the session scoped service is reloaded"
		//reload(RequestScopedService)
		and: "requesting a page using the request proxy via the session scope"
		go("requestScopedInsideSessionVar")
		then: "the request is successful"
		// It the session purging didn't happen, we will get a 500 
		// here due to a ClassCastException
		driver.pageSource == "<html><head></head><body>requestScopedInsideSessionVar: 0</body></html>"
	}
	
	void testFilterReloadingOnReload() {
		when: "requesting a page using the usedInFilter scoped service proxy in a filter"
		go("usedInFilterScopedServiceInvoker")
		then: "the request is successful"
		driver.pageSource == "<html><head></head><body>If you are seeing this after reloading UsedInFilterScopedService, you win.</body></html>"
		
		when: "the usedInFilter scoped service is reloaded"
		//reload(UsedInFilterScopedService)
		and: "requesting a page using the usedInFilter scoped service proxy in a filter"
		go("usedInFilterScopedServiceInvoker")
		then: "the request is successful"
		driver.pageSource == "<html><head></head><body>If you are seeing this after reloading UsedInFilterScopedService, you win.</body></html>"
	}

	protected reload(clazz) {
		def newClazz = grailsApplication.classLoader.reloadClass(clazz.name)
		PluginManagerHolder.pluginManager.informOfClassChange(newClazz)
	}

}