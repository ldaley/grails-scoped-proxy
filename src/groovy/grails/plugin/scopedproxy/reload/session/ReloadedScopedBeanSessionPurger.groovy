package grails.plugin.scopedproxy.reload.session

import grails.plugin.scopedproxy.reload.ScopedBeanReloadListener
import org.slf4j.LoggerFactory

/**
 * Checks all active sessions for the presence of a bean whose class
 * was reloaded, and removes it from the session if found.
 */
class ReloadedScopedBeanSessionPurger implements ScopedBeanReloadListener {

	private sessions = [].asSynchronized()
	private scopes = [].asSynchronized()
	
	ReloadedScopedBeanSessionPurger() {
		registerPurgableScope("session")
	}
	
	def registerPurgableScope(String scope) {
		if (!isPurgableScope(scope)) {
			if (log.infoEnabled) {
				log.info("Scope '$scope' registered as a purgable scope")
			}
			scopes << scope
		} else {
			if (log.debugEnabled) {
				log.debug("Scope '$scope' is already registered as a purgable scope")
			}
		}
	}
	
	def isPurgableScope(String scope) {
		scope in scopes
	}
	
	void scopedBeanWasReloaded(String beanName, String scope, String proxyBeanName) {
		if (isPurgableScope(scope)) {
			purgeFromSessions(beanName)
		} else {
			if (log.debugEnabled) {
				log.debug("Ignoring reload of '$beanName' as scope '$scope' is not a purgable scope")
			}
		}
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