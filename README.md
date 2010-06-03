# Scoped Proxy Plugin

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