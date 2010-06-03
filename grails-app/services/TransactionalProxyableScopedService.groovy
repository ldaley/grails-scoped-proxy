class TransactionalProxyableScopedService {
	static scope = "request"
	static proxy = true
	def var = 0
	
	def call(closure) {
		closure()
	}
}