package grails.plugin.scopedproxy.reload

/**
 * When a scoped bean is reloaded, all implementing classes of this interface
 * in the application context will be informed.
 */
interface ScopedBeanReloadListener {
	
	void scopedBeanWasReloaded(String beanName, String scope, String proxyBeanName)
	
}