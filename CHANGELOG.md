# 0.1

* Support for creating proxy via `static proxy = true` convention.
* Reloading support for proxy definitions

# 0.2 

* Improved externally usable API for registering proxies
* Improved reloading support by avoiding proxy class caching in CGLIB
* API and pluggable mechanism for destroying old scoped beans on class reload
* Improved reload support for session scoped beans (the old beans are now removed from the session)
* An improved proxy factory bean that specifies the target class