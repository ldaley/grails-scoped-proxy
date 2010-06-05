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
package grails.plugin.scopedproxy.reload.session

import org.slf4j.LoggerFactory
import javax.servlet.http.HttpSessionListener
import javax.servlet.http.HttpSessionEvent
import org.springframework.web.context.support.WebApplicationContextUtils

/**
 * Simply delegates to the 'reloadedScopedBeanSessionPurger' bean
 * in the application context.
 * 
 * This is registered by ScopedProxyGrailsPlugin.
 */
class SessionLifecycleListener implements HttpSessionListener {

	SessionLifecycleListener() {
		if (log.infoEnabled) {
			log.info("SessionLifecycleListener initialised")
		}
	}

	void sessionCreated(HttpSessionEvent se) {
		def session = se.session
		getPurger(session).sessionCreated(session)
	}

	void sessionDestroyed(HttpSessionEvent se) {
		def session = se.session
		getPurger(session).sessionDestroyed(session)
	}

	protected getPurger(session) {
		def rootContext = WebApplicationContextUtils.getWebApplicationContext(session.servletContext)
		def grailsApplication = rootContext.getBean('grailsApplication')
		grailsApplication.mainContext.getBean('reloadedScopedBeanSessionPurger')
	}

	private static final log = LoggerFactory.getLogger(SessionLifecycleListener)
}