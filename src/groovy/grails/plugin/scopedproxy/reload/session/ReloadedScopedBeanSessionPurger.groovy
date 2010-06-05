package grails.plugin.scopedproxy.reload.session

import grails.plugin.scopedproxy.reload.ScopedBeanReloadListener
import org.slf4j.LoggerFactory

/**
 * Checks all active sessions for the presence of a bean whose class
 * was reloaded, and removes it from the session if found.
 */
class ReloadedScopedBeanSessionPurger implements ScopedBeanReloadListener {

	def sessions = [].asSynchronized()

	void scopedBeanWasReloaded(String beanName, String scope, String proxyBeanName) {
		purgeFromSessions(beanName)
	}
	
	protected purgeFromSessions(String key) {
		if (log.warnEnabled) {
			log.warn("purging '$key' from all sessions")
		}
		
		for (session in sessions) {
			def object = session.getAttribute(key)
			if (object) {
				if (log.warnEnabled) {
					log.warn("purging '$key' from session '$session'")
				}
				session.removeAttribute(key)
			}
		}
	}
	
	// Called by the SessionLifecycleListener (i.e. not really public)
	void sessionCreated(session) {
		if (log.debugEnabled) {
			log.debug("Registering session '$session'")
		}
		sessions << session
	}

	// Called by the SessionLifecycleListener (i.e. not really public)
	void sessionDestroyed(session) {
		if (log.debugEnabled) {
			log.debug("Removing session '$session'")
		}
		sessions.remove(session)
	}

	private static final log = LoggerFactory.getLogger(ReloadedScopedBeanSessionPurger)
}