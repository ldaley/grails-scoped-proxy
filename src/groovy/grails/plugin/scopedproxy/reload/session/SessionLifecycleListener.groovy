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