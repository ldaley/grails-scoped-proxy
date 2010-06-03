package grails.plugin.scopedproxy

import org.springframework.beans.factory.FactoryBean
import org.springframework.transaction.interceptor.TransactionProxyFactoryBean

class TypeSpecifyableTransactionProxyFactoryBean extends TransactionProxyFactoryBean implements FactoryBean {

	protected type
	
	TypeSpecifyableTransactionProxyFactoryBean(Class type) {
		this.type = type
	}
	
	public Class getObjectType() {
		type
	}

}