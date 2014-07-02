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
package grails.plugin.scopedproxy.reload.filters

import grails.plugin.scopedproxy.reload.ScopedBeanReloadListener
import org.slf4j.LoggerFactory
import org.codehaus.groovy.grails.plugins.web.filters.FiltersConfigArtefactHandler
import grails.util.Holders
import org.codehaus.groovy.grails.plugins.web.filters.FilterConfig

class ReloadedScopedBeanFiltersReloader implements ScopedBeanReloadListener {

	def grailsApplication // autowired
	
	def dependents = [:]
	
	void scopedBeanWillReload(String beanName, String scope, String proxyBeanName) {
		synchronized(dependents) {
			dependents[proxyBeanName] = applicationContext.beanFactory.getDependentBeans(proxyBeanName)
		}
	}

	void scopedBeanWasReloaded(String beanName, String scope, String proxyBeanName) {
		synchronized(dependents) {
			def dependent = getFirstFiltersClassDependingOnBean(proxyBeanName)
			if (dependent) {
				if (log.infoEnabled) {
					log.debug("Triggering reload of filters due to reload of '$beanName' (first dependent: $dependent)")
				}
				triggerFiltersReload(dependent)
			} else {
				if (log.debugEnabled) {
					log.debug("Ignoring reload of '$beanName' as no filters depend on it")
				}
			}
			dependents[proxyBeanName] = null
		}
	}
	
	protected getFirstFiltersClassDependingOnBean(String proxyBeanName) {
		def dependents = dependents[proxyBeanName]
		if (dependents) {
			if (log.debugEnabled) {
				log.debug("Found dependants of '$proxyBeanName': ${dependents.join(', ')}")
			}
			def firstDependentFilter = dependents.find { isFilters(it) }
			firstDependentFilter ? applicationContext.getType(firstDependentFilter) : null
		} else {
			if (log.debugEnabled) {
				log.debug("Found no dependants of '$proxyBeanName'")
			}
			null
		}
	}

	protected isFilters(String beanName) {
		def is = beanName.endsWith(FiltersConfigArtefactHandler.TYPE)
		if (is && log.debugEnabled) {
			log.debug("$beanName is a filters clasee")
		}
		is
	}

	protected getApplicationContext() {
		grailsApplication.mainContext
	}

	protected getFiltersPlugin() {
		Holders.pluginManager.getGrailsPlugin('filters')
	}
	
	protected triggerFiltersReload(Class triggeringFiltersClass) {
		GroovySystem.metaClassRegistry.removeMetaClass(FilterConfig)
		filtersPlugin.doWithDynamicMethods(grailsApplication.mainContext)
		Holders.pluginManager.informOfClassChange(triggeringFiltersClass)
	}
	
	private static final log = LoggerFactory.getLogger(ReloadedScopedBeanFiltersReloader)
}