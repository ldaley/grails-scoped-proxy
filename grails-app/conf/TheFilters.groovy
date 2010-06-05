import org.slf4j.LoggerFactory

class TheFilters {

	def usedInFilterScopedServiceProxy
	
	void setUsedInFilterScopedServiceProxy(usedInFilterScopedServiceProxy) {
		log.info("incoming usedInFilterScopedService to filters hash: ${System.identityHashCode(usedInFilterScopedServiceProxy)}")
		this.usedInFilterScopedServiceProxy = usedInFilterScopedServiceProxy
	}
	
	def filters = {
		beforeFilter(controller:'*', action:'usedInFilterScopedServiceInvoker') {
			before = {
				log.info("hash of usedInFilterScopedServiceProxy in beforeFilter: ${System.identityHashCode(usedInFilterScopedServiceProxy)}")
				// trigger access of the underlying bean
				log.info("var of usedInFilterScopedServiceProxy in beforeFilter: ${usedInFilterScopedServiceProxy.var}")
			}
		}
	}
	
	private static final log = LoggerFactory.getLogger('grails.app.filters.TheFilters')
}