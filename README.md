# Scoped Proxy Plugin

Version: 0.1

The _scoped-proxy_ plugin allows you to easily create proxies for scoped services.

This allows you to use your scoped services from objects in a larger scope.

## Example

To create a proxy for a scoped service, simply create a property named `proxy` with a value of `true`.

    class CartService {
        
        static scope = 'session'
        static proxy = true
        
        def items = []
        
        def getItemCount() {
            items.size()
        }
        
    }

There is now a unique instance of `CartService` for each session in your application.

Controllers are _request_ scoped in Grails and requests operate inside the session scope, meaning your `cartService` can be used without a proxy.

    class CartController {
        
        def cartService // unique per session
        
        def addItem = {
            cartService.items << new CartItem(product: Product.get(params.id))
        }
        
    }

TagLibs on the other hand are of singleton scope and therefore exist outside of session scope. To use your `cartService` you need to access it via the proxy.

    class CartTagLib {
        
        def cartServiceProxy
        
        def itemCount = {
            out << cartServiceProxy.itemCount
        }
        
    }
    
At execution time, calls to `cartServiceProxy` are delegated to the _actual session bound_ `cartService` instance for the request. 

## Installation

    grails install-plugin scoped-proxy

## Logging

All logging occurs under the `grails.plugin.scopedproxy` namespace.

## Transactions

Transactional services are fully supported. That is, proxies of transactional scoped services share the same transactional semantics as usual.

## Testing

Scoped proxies are only relevant to integration testing.

Currently, integration tests are autowired out of a request context. This means that you _must_ use scoped proxies of scoped services in integration tests if you want them to be autowired. It's also important to realise that each test method runs in a different request (and session) context.

    class CartServiceTests extends GroovyTestCase {
        def cartServiceProxy
        
        void testAdd1() {
            assert cartServiceProxy.itemCount == 0
            cartServiceProxy.items << new CartItem(product: Product.get(1))
            assert cartServiceProxy.itemCount == 1
        }

        void testAdd2() {
            assert cartServiceProxy.itemCount == 0
            cartServiceProxy.items << new CartItem(product: Product.get(1))
            assert cartServiceProxy.itemCount == 1
        }
    }
    
The above test **will pass** because the actual underlying `cartService` that the `cartServiceProxy` delegates to in `testAdd1()` and `testAdd2()` are different.

## Hot Reloading

This plugin adds explicit support for hot reloading of scoped services during development. This does mean however that when a _session_ scoped bean class is reloaded, all instances of that service class are removed from all active sessions. Depending on your application, the consequences of this will be different.

This is necessary to avoid `ClassCastException`s where a new proxy based on the new class encounters an old bean based on the old class.

### Supporting Reloading With Custom Scopes

If you are using a custom scope in your application, you may need to do some extra work to support reloading.

#### Session Based Scopes

For scopes that are inherently session based (i.e. live inside a session lifecycle), you _can_ (though it may not be best to) plugin into the existing session purging mechanism by registering your scope with the `reloadedScopedBeanSessionPurger` bean in the application context.

    import org.springframework.web.context.request.SessionScope
    import org.springframework.beans.factory.InitializingBean
    
    class CustomSessionBasedScope extends SessionScope, InitializingBean {
        static String SCOPE_NAME = 'custom'
        def reloadedScopedBeanSessionPurger // autowired
        
        void afterPropertiesSet() {
            // reloadedScopedBeanSessionPurger is only present in environments
            // that support class reloading, hence the null check.
            reloadedScopedBeanSessionPurger?.registerPurgableScope(SCOPE_NAME)
        }
    }

The above example illustrates how to register a custom scope with the `reloadedScopedBeanSessionPurger` bean via the `registerPurgableScope(String scopeName)` method. Now, whenever a bean is reloaded of our custom scope it will be removed from the session.

#### Non Session Based Scopes

If your custom scope has a completely different storage mechanism, you may need to provide a `ScopedBeanReloadListener` implementation that can purge beans based on old classes. It's likely that a convenient implementer of this will be your actual scope implementation (but does not need to be).

    import org.springframework.beans.factory.ObjectFactory
    import org.springframework.beans.factory.config.Scope
    import grails.plugin.scopedproxy.reload.ScopedBeanReloadListener
        
    class CustomScope implements Scope, ScopedBeanReloadListener {
        
        static SCOPE_NAME = "custom"
        protected storage = [:].asSynchronized()
        
        // ScopedBeanReloadListener methods
        
        void scopedBeanWasReloaded(String beanName, String scope, String proxyBeanName) {
            if (scope == SCOPE_NAME) {
                remove(beanName)
            }
        }
        
        // Scope Methods
        
        def get(String name, ObjectFactory objectFactory) {
            if (!storage.containsKey(name)) {
                storage[name] = objectFactory.object
            } 
            storage[name]
        }
        
        String getConversationId() {
            null
        }

        void registerDestructionCallback(String name, Runnable callback) {
            // not implemented, but should be
        }
        
        def remove(String name) {
            storage.remove(name)
        }
    }

All instances of `ScopedBeanReloadListener` will be informed whenever any scoped bean has had it's class reloaded.

## Proxying Custom Beans

This plugin can be used to support proxying your own beans. You do this via `static` methods on the `grails.plugin.scopedproxy.ScopedProxyGrailsPlugin` class.

    // resources.groovy
    import grails.plugin.scopedproxy.ScopedProxyGrailsPlugin as SPGP
    
    beans {
        myBean(MyBean) {
            it.scope = 'session'
        }
        
        def beanBuilder = delegate
        def classLoader = application.classLoader // Use Grails class loader
        def beanName = 'myBean'
        def proxyName = SPGP.getProxyBeanName(myBean) // returns 'myBeanProxy'
        def beanClass = MyBean
        
        SPGP.buildProxy(beanBuilder, classLoader, beanName, beanClass, proxyName)
    }

Note: while this example shows how to use the `buildProxy()` method, it would certainly be much better to use a service in this case so that you get hot reloading.

## Proxying Custom Artefacts

Plugin developers may wish to provide scoping of their artefacts and supporting proxying in the same manner as services.

For this example, we will use a new artefact type of `Thing` which is basically the same as a Grails service.

    import grails.plugin.scopedproxy.ScopedProxyGrailsPlugin as SPGP
    
    class ThingGrailsPlugin {
        
        // Usual plugin stuff
        …
        
        def artefacts = [ThingArtefactHandler]
        def watchedResources = [
            "file:./grails-app/things/**/*Thing.groovy",
            "file:../../plugins/*/things/**/*Thing.groovy"
        ]
        
        def doWithSpring = {
            for (thingGrailsClass in application.thingClasses) {
                def clazz = thingGrailsClass.clazz
                def beanName = thingGrailsClass.propertyName

                // getScope() looks for a 'scope' property, and returns 'singleton' if none found
                def scope = SPGP.getScope(clazz)
                
                "$beanName"(clazz) { beanDefinition ->
                    beanDefinition.scope = scope
                    // other definition
                }
                
                if (SPGP.wantsProxy(clazz)) { // class has a 'proxy' property set to true
                    SPGP.buildProxy(delegate, application.classLoader, beanName, clazz, SPGP.getProxyBeanName(beanName))
                }
            }
        }
        
        // Reload support
        def onChange = {
            if (application.isThingClass(event.source)) {
                def classLoader = application.classLoader
                def newClass = classLoader.loadClass(event.source.name, false) // make sure we get the new class
                def grailsClass = application.getThingClass(event.source.name)
                def beanName = grailsClass.propertyName
                def scope = SPGP.getScope(newClass)
                
                def beans = beans {
                    // Redefine the bean
                    "$beanName"(newClass) { beanDefinition ->
                        beanDefinition.scope = scope
                        // other definition
                    }
                    
                    if (SPGP.wantsProxy(newClass)) {
                        def proxyBeanName = SPGP.getProxyBeanName(beanName)
                        
                        // Redefine the proxy
                        SPGP.buildProxy(delegate, classLoader, beanName, newClass, proxyBeanName)
                        
                        // Inform listeners that a scoped bean has changed (allows purging old beans)
                        SPGP.informListenersOfReload(application, beanName, scope, proxyBeanName)
                    }
                }
                
                beanDefinitions.registerBeans(event.ctx)
            }
        }
    }

Checkout the `ScopedProxyGrailsPlugin` for more information.