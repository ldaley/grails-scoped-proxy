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
package grails.plugin.scopedproxy.reload

/**
 * When a scoped bean is reloaded, all implementing classes of this interface
 * in the application context will be informed.
 */
interface ScopedBeanReloadListener {
	
	/**
	 * Called when a scoped bean's class has been reloaded.
	 * 
	 * Implementations should remove the bean from their storage to avoid class
	 * cast exceptions with the new proxy based on the new class.
	 * 
	 * @param beanName The name of the bean whose class was reloaded
	 * @param scope The bean scope of the bean
	 * @param proxyBeanName The name of the proxy for this bean (may be null if there is no proxy)
	 */
	void scopedBeanWasReloaded(String beanName, String scope, String proxyBeanName)
	
}