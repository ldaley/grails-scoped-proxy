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
import org.slf4j.LoggerFactory

class TheTagLib {

	static namespace = 'the'
	
	def sessionScopedServiceProxy
		
	void setSessionScopedServiceProxy(sessionScopedServiceProxy) {
		log.info("incoming sessionScopedServiceProxy to taglib hash: ${System.identityHashCode(sessionScopedServiceProxy)}")
		this.sessionScopedServiceProxy = sessionScopedServiceProxy
	}
	
	def sessionScopedServiceVar = {
		log.info("hash of sessionScopedServiceProxy in tag lib: ${System.identityHashCode(this.sessionScopedServiceProxy)}")
		out << sessionScopedServiceProxy.var
	}

	private static final log = LoggerFactory.getLogger('grails.app.tagLib.TheTagLib')
}