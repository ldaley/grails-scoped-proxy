class TheTagLib {

	static namespace = 'the'
	
	def sessionScopedServiceProxy
	
	def sessionScopedServiceVar = {
		out << sessionScopedServiceProxy.var
	}

}