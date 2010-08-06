/*
 * Copyright 2002-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package grails.plugin.scopedproxy

import java.lang.reflect.Modifier

import org.springframework.aop.scope.*
import org.springframework.aop.framework.AopInfrastructureBean
import org.springframework.aop.framework.ProxyConfig
import org.springframework.aop.framework.ProxyFactory
import org.springframework.aop.support.DelegatingIntroductionInterceptor
import org.springframework.aop.target.SimpleBeanTargetSource
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.BeanFactoryAware
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.FactoryBeanNotInitializedException
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.util.ClassUtils
import org.springframework.beans.factory.InitializingBean
import org.aopalliance.aop.Advice

/**
 * A lift of Spring's ScopedProxyFactory bean with a crucial difference, it allows
 * configuration of the classloader used to load the proxy.
 */
class ClassLoaderConfigurableScopedProxyFactoryBean extends ProxyConfig implements FactoryBean, BeanFactoryAware, InitializingBean {

	ClassLoader classLoader
	Class type
	
	/** The TargetSource that manages scoping */
	private final SimpleBeanTargetSource scopedTargetSource = new SimpleBeanTargetSource()

	/** The name of the target bean */
	private String targetBeanName

	/** The cached singleton proxy */
	private Object proxy

	List<Advice> advices

	/**
	 * Create a new ScopedProxyFactoryBean instance.
	 */
	ClassLoaderConfigurableScopedProxyFactoryBean(Class type) {
		this.type = type
		setProxyTargetClass(true)
	}

	void afterPropertiesSet() {
		if (!classLoader) {
			throw new IllegalStateException("The property 'classLoader' is required")
		}
	}

	/**
	 * Set the name of the bean that is to be scoped.
	 */
	public void setTargetBeanName(String targetBeanName) {
		this.targetBeanName = targetBeanName
		this.scopedTargetSource.setTargetBeanName(targetBeanName)
	}

	public void setBeanFactory(BeanFactory beanFactory) {
		if (!(beanFactory instanceof ConfigurableBeanFactory)) {
			throw new IllegalStateException("Not running in a ConfigurableBeanFactory: " + beanFactory)
		}
		ConfigurableBeanFactory cbf = (ConfigurableBeanFactory) beanFactory

		this.scopedTargetSource.setBeanFactory(beanFactory)

		ProxyFactory pf = new ProxyFactory()
		pf.copyFrom(this)
		pf.setTargetSource(this.scopedTargetSource)

		if (!isProxyTargetClass() || type.isInterface() || Modifier.isPrivate(type.getModifiers())) {
			pf.setInterfaces(ClassUtils.getAllInterfacesForClass(type, this.classLoader))
		}

		// Add an introduction that implements only the methods on ScopedObject.
		ScopedObject scopedObject = new DefaultScopedObject(cbf, this.scopedTargetSource.getTargetBeanName())
		pf.addAdvice(new DelegatingIntroductionInterceptor(scopedObject))
		for (advice in advices) {
			pf.addAdvice advice
		}

		// Add the AopInfrastructureBean marker to indicate that the scoped proxy
		// itself is not subject to auto-proxying! Only its target bean is.
		pf.addInterface(AopInfrastructureBean.class)

		this.proxy = pf.getProxy(this.classLoader)
	}


	public Object getObject() {
		if (this.proxy == null) {
			throw new FactoryBeanNotInitializedException()
		}
		return this.proxy
	}

	public Class<?> getObjectType() {
		this.type
	}

	public boolean isSingleton() {
		return true
	}

}
