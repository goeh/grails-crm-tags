/*
 * Copyright (c) 2014 Goran Ehrsson.
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
 */
package grails.plugins.crm.tags

import grails.events.Listener
import grails.plugins.crm.core.CrmException
import grails.plugins.crm.core.PagedResultList
import grails.plugins.crm.core.SearchUtils
import grails.plugins.crm.core.TenantUtils
import grails.plugins.selection.Selectable
import grails.util.GrailsNameUtils
import groovy.transform.CompileStatic
import org.grails.databinding.SimpleMapDataBindingSource
import org.grails.plugin.platform.events.EventMessage
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil

class CrmTagService {

    public static final String CRM_TAG_CACHE = "crmTagCache"

    def crmCoreService
    def grailsApplication
    def grailsWebDataBinder
    CacheManager grailsCacheManager

    @Listener(namespace = "crmTenant", topic = "requestDelete")
    Map requestDeleteTenant(event) {
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
        clearCache()
        log.warn("Deleted $n tags in tenant $tenant")
    }

    @Listener(namespace = "*", topic = "deleted")
    def cleanupTags(EventMessage<Map> event) {
        Map data = event.getData()
        if (data.id) {
            def ref = "${event.namespace}@${data.id}".toString()
            deleteLinks(ref)
            if (log.isDebugEnabled()) {
                log.debug "Deleted all tags for $ref"
            }
        }
    }

    CrmTag createTag(Map params) {
        def tag = CrmTag.createCriteria().get {
            eq('name', params.name)
            eq('tenantId', params.tenantId ?: TenantUtils.getTenant())
        }
        if (!tag) {
            tag = new CrmTag()
            grailsWebDataBinder.bind(tag, params as SimpleMapDataBindingSource, null, null, ['options'], null)
            for (opt in params.options) {
                tag.addToOptions(CrmTagOptions.fromString(opt))
            }
            tag.save(failOnError: true)
        }
        return tag
    }

    void deleteTag(String name, Long tenantId = null) {
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
        clearCache()
    }

    void deleteLinks(reference) {
        CrmTagLink.findAllByRef(crmCoreService.getReferenceIdentifier(reference))*.delete()
        clearCache()
    }

    protected CrmTag getTagInstance(Object nameOrInstance, Long tenant) {
        (nameOrInstance instanceof CrmTag) ? nameOrInstance : CrmTag.findByNameAndTenantId(nameOrInstance.toString(), tenant, [cache: true])
    }

    Collection<Map<String, Object>> getTagOptions(String name) {
        def tag = getTagInstance(name, TenantUtils.getTenant())
        if (!tag) {
            throw new IllegalArgumentException("Tag not found: $name")
        }
        if (tag?.options) {
            return tag.getParsedOptions()
        }
        return Collections.EMPTY_LIST
    }

    CrmTagOptions addTagOption(String name, String option, Long tenantId = null) {
        if (!tenantId) {
            tenantId = TenantUtils.getTenant()
        }
        def tag = CrmTag.createCriteria().get {
            eq('name', name)
            eq('tenantId', tenantId)
        }
        if (!tag) {
            throw new IllegalArgumentException("Tag not found: $name")
        }
        def instance = tag.getOption(option)
        if (!instance) {
            instance = CrmTagOptions.fromString(option)
            instance.crmTag = tag
            tag.addToOptions(instance)
        }
        return instance
    }

    void setTagValue(def instance, Object[] args) {
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
        def tag = getTagInstance(tagName, tenant)
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
        clearCache()

        if(isEventsEnabled()) {
            def namespace = GrailsNameUtils.getPropertyName(className)
            event(for: namespace, topic: "tagged", data: [id: instance.id, tenant: tenant, name: instance.toString(), tags: [tagValue]])
        }
    }

    private boolean isEventsEnabled() {
        return grailsApplication.config.crm.tag.events != false
    }

    @CompileStatic
    private String getCacheKey(final String reference, final String tagName) {
        "$reference[$tagName]".toString()
    }

    def getTagValue(Object instance, String tagName) {
        instance = GrailsHibernateUtil.unwrapIfProxy(instance)
        boolean multiple = tagName == null
        if (multiple) {
            tagName = instance.class.name
        }
        def ref = crmCoreService.getReferenceIdentifier(instance)
        def cacheKey = getCacheKey(ref, tagName)
        Cache cache = grailsCacheManager.getCache(CRM_TAG_CACHE)
        def result = cache.get(cacheKey)
        if (result != null) {
            return result.get()
        }
        if (instance.ident() == null) throw new CrmException("tag.reference.not.saved.error", [instance])
        def tenant = instance.hasProperty('tenantId') ? instance.tenantId : TenantUtils.getTenant()
        def tag = getTagInstance(tagName, tenant)
        if (tag) {
            multiple = tag.multiple
        } else {
            cache.put(cacheKey, null)
            return multiple ? Collections.emptyList() : null
        }
        if (multiple) {
            result = CrmTagLink.createCriteria().list {
                projections {
                    property "value"
                }
                eq('tag', tag)
                eq('ref', ref)
            }
            cache.put(cacheKey, result)
        } else {
            result = CrmTagLink.findByTagAndRef(tag, ref, [cache: true])?.value
            cache.put(cacheKey, result)
        }
        return result
    }

    @CompileStatic
    boolean isTagged(final Object instance, String tagName) {
        final String className = instance.class.name
        final Object result = getTagValue(instance, className)
        if (result instanceof Collection) {
            for (value in result) {
                if (tagName.equalsIgnoreCase(value.toString())) {
                    return true
                }
            }
            return false
        }
        return tagName.equalsIgnoreCase(result.toString())
    }

    @CompileStatic
    boolean isTagged(final Object instance, String parentTagName, String tagName) {
        final Object result = getTagValue(instance, parentTagName)
        return (result instanceof Collection) ? result.contains(tagName) : (result == tagName)
    }

    Collection deleteTag(Object instance, String tagName) {
        if (instance.id == null) throw new CrmException("tag.reference.not.saved.error", [instance])
        def tenant = instance.hasProperty('tenantId') ? instance.tenantId : TenantUtils.getTenant()
        def tag = getTagInstance(tagName, tenant)
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
        grailsCacheManager.getCache(CRM_TAG_CACHE).evict(getCacheKey(ref, tagName))

        if(isEventsEnabled()) {
            def (namespace, id) = ref.split('@').toList()
            event(for: namespace, topic: "untagged", data: [id: id, tenant: tenant, name: instance.toString(), tags: rval])
        }

        return rval
    }

    Collection deleteTagValue(instance, Object[] args) {
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
        def tag = getTagInstance(tagName, tenant)
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
        grailsCacheManager.getCache(CRM_TAG_CACHE).evict(getCacheKey(ref, tagName))
        return rval
    }

    PagedResultList findAllByTag(Class clazz, Object[] args) {
        def tagName
        def tagValue
        if (args) {
            if (args.size() > 1) {
                tagName = args[0]
                tagValue = args[1]
            } else {
                tagName = clazz.name
                tagValue = args[0]
            }
        } else {
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
        def tag = getTagInstance(tagName, tenant)
        def result
        if (tag) {
            def domainName = GrailsNameUtils.getPropertyName(clazz)
            def tagValueAddOn = tagValue ? "and lower(link.value) in (:value)" : ""
            def totalCount = clazz.executeQuery("select count(m.id) from ${clazz.name} m where m.tenantId = :tenant and exists (select link.id from CrmTagLink link where link.tag = :tag and link.ref = '${domainName}@'||m.id $tagValueAddOn)",
                    [tenant: tenant, tag: tag, value: tagValue]).head()
            result = new PagedResultList(clazz.findAll("from ${clazz.name} m where m.tenantId = :tenant and exists (select link.id from CrmTagLink link where link.tag = :tag and link.ref = '${domainName}@'||m.id $tagValueAddOn) $order",
                    [tenant: tenant, tag: tag, value: tagValue], paginateParams), totalCount.intValue())
        } else {
            result = new PagedResultList([])
        }
        return result
    }

    /**
     * Return all primary keys for a domain class that are tagged with one or more specified tags.
     * "tag1,tag2" means the domain instance must be tagged with either tag1 OR tag2.
     * "tag1&amp;tag2" means the domain instance must be tagged with both tag1 AND tag2.
     *
     * @param clazz domain class to query
     * @param args tag values
     * @return Set of matching primary keys
     */
    Set<Long> findAllIdByTag(Class clazz, Object[] args) {
        def tagName
        def tagValue
        if (args) {
            if (args.size() > 1) {
                tagName = args[0]
                tagValue = args[1]
            } else {
                tagName = clazz.name
                tagValue = args[0]
            }
        } else {
            tagName = clazz.name
        }
        final CrmTag tag = getTagInstance(tagName, TenantUtils.tenant)
        Set<Long> tagged = [] as Set
        if (tag) {
            final String domainName = GrailsNameUtils.getPropertyName(clazz)
            final int index = domainName.length() + 1
            if (tagValue) {
                final List<String> allTags = tagValue.toString().split('&').toList().findAll { it.trim() }
                for (int i = 0; i < allTags.size(); i++) {
                    tagValue = allTags[i].split(',').collect { it.trim().toLowerCase() }
                    List<Long> tmp = clazz.executeQuery("select ref from CrmTagLink where tag = :tag and ref like '${domainName}@%' and lower(value) in (:value)",
                            [tag: tag, value: tagValue]).collect { Long.valueOf(it.substring(index)) }
                    if (tmp) {
                        if (i) {
                            tagged.retainAll(tmp)
                        } else {
                            tagged.addAll(tmp)
                        }
                    } else {
                        tagged.clear()
                        break
                    }
                }
            } else {
                tagged.addAll(clazz.executeQuery("select ref from CrmTagLink where tag = :tag and ref like '${domainName}@%'",
                        [tag: tag]).collect { Long.valueOf(it.substring(index)) })
            }
        }

        return tagged
    }

    @Selectable
    def list(Map params = null) {
        CrmTag.createCriteria().list(params) {
            eq('tenantId', TenantUtils.tenant)
        }
    }

    @Selectable
    PagedResultList list(Map query, Map params) {
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

    @CompileStatic
    void clearCache() {
        grailsCacheManager.getCache(CRM_TAG_CACHE).clear()
    }
}
