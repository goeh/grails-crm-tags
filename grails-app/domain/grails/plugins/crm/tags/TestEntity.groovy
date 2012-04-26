package grails.plugins.crm.tags

import grails.plugins.crm.core.TenantEntity

@TenantEntity
class TestEntity {

    String name

    static taggable = true
}
