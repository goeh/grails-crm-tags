import grails.plugins.crm.tags.CrmTag
import grails.plugins.crm.tags.CrmTagLink
import grails.plugins.crm.tags.CrmTagService

config = {
    cache {
        name CrmTagService.CRM_TAG_CACHE // "crmTagCache"
    }
    domain {
        name CrmTag
    }
    domain {
        name CrmTagLink
    }
}