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
package grails.plugin.scopedproxy.reload.request

import grails.plugin.scopedproxy.reload.ScopedBeanReloadListener
import org.slf4j.LoggerFactory
import org.codehaus.groovy.grails.plugins.web.filters.FiltersConfigArtefactHandler
import org.codehaus.groovy.grails.plugins.PluginManagerHolder
import org.codehaus.groovy.grails.plugins.web.filters.FilterConfig
import grails.plugin.scopedproxy.ScopedProxyUtils as SPU

class RequestScopedBeanReloadSessionPurger implements ScopedBeanReloadListener {

	def grailsApplication // autowired
	def reloadedScopedBeanSessionPurger
	
	def sessionScopedDependentsCache = [:]
	
	void scopedBeanWillReload(String beanName, String scope, String proxyBeanName) {
		if (scope == "request") {
			synchronized(sessionScopedDependentsCache) {
				def sessionScopedDependents = []
				def dependents = grailsApplication.mainContext.beanFactory.getDependentBeans(proxyBeanName)
				if (dependents) {
					if (log.debugEnabled) {
						log.debug("found dependents of $proxyBeanName: ${dependents.join(', ')}")
					}
					dependents.each {
						if (SPU.getBeanScope(grailsApplication.mainContext, it) == "session") {
							sessionScopedDependents << it
						}
					}
				} else {
					if (log.debugEnabled) {
						log.debug("$proxyBeanName has no dependents")
					}
				}
				sessionScopedDependentsCache[proxyBeanName] = sessionScopedDependents
			}
		} else {
			if (log.debugEnabled) {
				log.debug("ignoring bean $beanName of scope $scope")
			}
		}
	}

	void scopedBeanWasReloaded(String beanName, String scope, String proxyBeanName) {
		synchronized(sessionScopedDependentsCache) {
			def sessionScopedDependents = sessionScopedDependentsCache[proxyBeanName]
			sessionScopedDependents.each { 
				if (log.infoEnabled) {
					log.debug("Purging dependent '$it' of reloaded '$beanName' ($proxyBeanName) from the session")
				}
				reloadedScopedBeanSessionPurger.purgeFromSessions(it)
			}
			sessionScopedDependentsCache.remove(proxyBeanName)
		}
	}

	private static final log = LoggerFactory.getLogger(RequestScopedBeanReloadSessionPurger)
}
