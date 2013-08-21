/*
 * Copyright (c) 2012 Goran Ehrsson.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * under the License.
 */
package grails.plugins.crm.tags

import grails.events.Listener
import grails.plugins.crm.core.SearchUtils
import grails.plugins.crm.core.TenantUtils
import grails.plugins.crm.core.CrmException
import grails.plugins.crm.core.PagedResultList
import grails.util.GrailsNameUtils

class CrmTagService {

    static transactional = true

    def crmCoreService

    @Listener(namespace = "crmTenant", topic = "requestDelete")
    def requestDeleteTenant(event) {
        def tenant = event.id
        def count = CrmTag.countByTenantId(tenant)
        return count ? [namespace: "crmTags", topic: "deleteTenant"] : null
    }

    @Listener(namespace = "crmTags", topic = "deleteTenant")
    def deleteTenant(event) {
        def tenant = event.id
        int n = 0
        for (t in CrmTag.findAllByTenantId(tenant)) {
            deleteTag(t.name, tenant)
            n++
        }
        log.warn("Deleted $n tags in tenant $tenant")
    }

    def createTag(params) {
        def tag = CrmTag.createCriteria().get {
            eq('name', params.name)
            eq('tenantId', params.tenantId ?: TenantUtils.getTenant())
            cache true
        }
        if (!tag) {
            tag = new CrmTag(params).save(failOnError: true)
        }
        return tag
    }

    def deleteTag(String name, Long tenantId = null) {
        if (!tenantId) {
            tenantId = TenantUtils.getTenant()
        }
        def tag = CrmTag.createCriteria().get {
            eq('name', name)
            eq('tenantId', tenantId)
        }
        if (tag) {
            CrmTagLink.findAllByTag(tag)*.delete()
            tag.delete(flush: true)
        } else {
            throw new IllegalArgumentException("Tag not found: $name")
        }
    }

    def deleteLinks(item) {
        CrmTagLink.findAllByRef(crmCoreService.getReferenceIdentifier(item))*.delete()
    }

    def getTagOptions(String name) {
        def tag = CrmTag.findByNameAndTenantId(name, TenantUtils.getTenant())
        if (tag) {
            return tag.options.sort()
        } else {
            throw new IllegalArgumentException("Tag not found: $name")
        }
    }

    def setTagValue(def instance, Object[] args) {
        def className = instance.class.name
        def tagName = args[0]
        def tagValue
        if (args.size() > 1) {
            tagValue = args[1]
        } else {
            tagValue = args[0]
            tagName = className
        }
        if (instance.id == null) throw new CrmException("tag.reference.not.saved.error", [instance])
        def tenant = instance.hasProperty('tenantId') ? instance.tenantId : TenantUtils.getTenant()
        def tag = (tagName instanceof CrmTag) ? tagName : CrmTag.findByNameAndTenantId(tagName.toString(), tenant)
        if (!tag) {
            if (tagName == instance.class.name) {
                tag = createTag([name: tagName, multiple: true])
            } else {
                throw new CrmException("tag.not.found.error", [tagName])
            }
        }

        if (!tag?.isValidOption(tagValue)) {
            throw new CrmException("tag.invalid.option.error", [tagValue, tag, tag?.options])
        }
        def ref = crmCoreService.getReferenceIdentifier(instance)
        tagValue = tagValue?.toString()

        def links = CrmTagLink.withCriteria {
            eq('tag', tag)
            eq('ref', ref)
        }
        def link
        if (links && !tag.multiple) {
            link = links.iterator().next()
        } else {
            link = links.find { it.value == tagValue } ?: new CrmTagLink(tag: tag, ref: ref)
        }
        link.value = tagValue
        link.save(failOnError: true)
    }

    def getTagValue(Object instance, String tagName) {
        def className = instance.class.name
        if (tagName == null) {
            tagName = className
        }
        if (instance.id == null) throw new CrmException("tag.reference.not.saved.error", [instance])
        def tenant = instance.hasProperty('tenantId') ? instance.tenantId : TenantUtils.getTenant()
        def tag = (tagName instanceof CrmTag) ? tagName : CrmTag.findByNameAndTenantId(tagName.toString(), tenant)
        if (!tag) return null
        def ref = crmCoreService.getReferenceIdentifier(instance)
        def links = CrmTagLink.createCriteria().list {
            eq('tag', tag)
            eq('ref', ref)
        }
        if (tag.multiple) {
            return links*.value
        } else if (links) {
            return links.iterator().next().value
        }
        return null
    }

    boolean isTagged(Object instance, String tagName) {
        def className = instance.class.name
        def result = getTagValue(instance, className)
        return (result instanceof Collection) ? result.contains(tagName) : (result == tagName)
    }

    def deleteTag(Object instance, String tagName) {
        if (instance.id == null) throw new CrmException("tag.reference.not.saved.error", [instance])
        def tenant = instance.hasProperty('tenantId') ? instance.tenantId : TenantUtils.getTenant()
        def tag = (tagName instanceof CrmTag) ? tagName : CrmTag.findByNameAndTenantId(tagName.toString(), tenant)
        if (!tag) throw new CrmException("tag.not.found.error", [tagName])
        def ref = crmCoreService.getReferenceIdentifier(instance)
        def rval = []
        def result = CrmTagLink.createCriteria().list {
            eq('tag', tag)
            eq('ref', ref)
        }
        for (link in result) {
            rval << link.value
            link.delete(flush: true)
        }
        return rval
    }

    def deleteTagValue(instance, Object[] args) {
        def className = instance.class.name
        def tagName = args[0]
        def tagValue
        if (args.size() > 1) {
            tagValue = args[1]
        } else {
            tagValue = args[0]
            tagName = className
        }
        if (instance.id == null) throw new CrmException("tag.reference.not.saved.error", [instance])
        def tenant = instance.hasProperty('tenantId') ? instance.tenantId : TenantUtils.getTenant()
        def tag = (tagName instanceof CrmTag) ? tagName : CrmTag.findByNameAndTenantId(tagName.toString(), tenant)
        if (!tag) throw new CrmException("tag.not.found.error", [tagName])
        def ref = crmCoreService.getReferenceIdentifier(instance)
        def rval = []
        def result = CrmTagLink.createCriteria().list {
            if (tagValue != null) {
                eq('value', tagValue.toString())
            }
            eq('tag', tag)
            eq('ref', ref)
        }
        for (link in result) {
            rval << link.value
            link.delete(flush: true)
        }
        return rval
    }

    def findAllByTag(Class clazz, Object[] args) {
        def tagName = args[0]
        def tagValue
        if (args.size() > 1) {
            tagValue = args[1]
        } else {
            tagValue = args[0]
            tagName = clazz.name
        }
        def paginateParams = [offset: 0, max: 100000]
        def order = ""
        if (args.size() > 2 && (args[2] instanceof Map)) {
            def params = args[2]
            if (params.sort) {
                order = "order by m.${params.sort} ${params.order ?: 'asc'}"
            }
            if (params.offset) paginateParams.offset = Integer.valueOf(params.offset.toString())
            if (params.max) paginateParams.max = Integer.valueOf(params.max.toString())
        }

        if (tagValue) {
            tagValue = tagValue.toString().split(',').collect { it.trim().toLowerCase() }
        }
        def tenant = TenantUtils.getTenant()
        def tag = CrmTag.findByNameAndTenantId(tagName.toString(), tenant, [cache: true])
        def result
        if (tag) {
            def domainName = GrailsNameUtils.getPropertyName(clazz)
            def tagValueAddOn = tagValue ? "and lower(link.value) in (:value)" : ""
            def totalCount = clazz.findAll("from ${clazz.name} m where m.tenantId = :tenant and exists (select link.id from CrmTagLink link where link.tag = :tag and link.ref = '${domainName}@'||m.id $tagValueAddOn)",
                    [tenant: tenant, tag: tag, value: tagValue]).size()
            result = new PagedResultList(clazz.findAll("from ${clazz.name} m where m.tenantId = :tenant and exists (select link.id from CrmTagLink link where link.tag = :tag and link.ref = '${domainName}@'||m.id $tagValueAddOn) $order",
                    [tenant: tenant, tag: tag, value: tagValue], paginateParams), totalCount)
        } else {
            result = new PagedResultList([])
        }
        return result
    }

    def list(Map query, Map params) {
        def domainClass = crmCoreService.getDomainClass(query.entity)
        findAllByTag(domainClass, query.value)
    }

    List<String> listDistinctValue(String className, String q = null, Map params = [:]) {
        CrmTagLink.createCriteria().list(params) {
            projections {
                distinct('value')
            }
            tag {
                eq('tenantId', TenantUtils.tenant)
                eq('name', className)
            }
            if (q) {
                ilike('value', SearchUtils.wildcard(q))
            }
        }.sort()
    }
}
