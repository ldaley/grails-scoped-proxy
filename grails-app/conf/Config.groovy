log4j = {
    error ''
    debug 'grails.plugin.scopedproxy'
}

dataSource {
    pooled = true
    driverClassName = "org.hsqldb.jdbcDriver"
    username = "sa"
    password = ""
}
hibernate {
    cache.use_second_level_cache = true
    cache.use_query_cache = true
    cache.provider_class = 'com.opensymphony.oscache.hibernate.OSCacheProvider'
}

environments {
    development {
        dataSource {
            dbCreate = "create-drop"
            url = "jdbc:hsqldb:mem:devDB"
        }
    }
    test {
        dataSource {
            dbCreate = "create-drop"
            url = "jdbc:hsqldb:mem:testDb"
        }
    }
}

grails {
    doc {
        authors = "Luke Daley"
        license = "Apache License 2.0"
    }
}