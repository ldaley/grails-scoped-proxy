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