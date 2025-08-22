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

import grails.converters.JSON
import grails.plugins.crm.core.TenantUtils
import grails.plugins.crm.core.WebUtils
import grails.util.GrailsNameUtils

import javax.servlet.http.HttpServletResponse

class CrmTagController {

    def grailsApplication
    def selectionService
    def crmTagService

    def list(String entity) {
        def tenant = TenantUtils.tenant
        def ref = GrailsNameUtils.getPropertyName(params.entity) + '@' + params.id
        def tag = crmTagService.getTagInstance(params.entity, tenant)
        if (!tag) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND)
            return
        }
        def tags = CrmTagLink.createCriteria().list([sort: 'value', order: 'asc']) {
            eq('tag', tag)
            eq('ref', ref)
            cache true
        }.collect {
            [value: it.value, defined: it.tag.isDefinedOption(it.value)]
        }

        def options = crmTagService.getTagOptions(params.entity)
        def existing = tags.collect{it.value}
        for(opt in options) {
            opt.checked = existing.contains(opt.value)
        }
        def result = [name: tag.name, description: tag.description, tags: tags, options: options]
        WebUtils.shortCache(response)
        render result as JSON
    }

    def save() {
        def values = params.list('option') ?: []
        if(params.value) {
            def tmp = new ArrayList(values.size() + 1)
            tmp.addAll(values)
            tmp.addAll(params.value.split(',').collect { it.trim() })
            values = tmp
        }
        if (values) {
            def clazz = grailsApplication.classLoader.loadClass(params.entity)
            def instance = clazz.get(params.id)
            if (instance) {
                for (v in values) {
                    instance.setTagValue(params.entity, v)
                }
                def map = [entity: params.entity, id: params.id, value: values]
                render map as JSON
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "${params.entity} with id ${params.id} not found")
            }
        } else {
            response.status = javax.servlet.http.HttpServletResponse.SC_NO_CONTENT
        }
    }

    def delete() {
        def values = params.value
        if (values) {
            def clazz = grailsApplication.classLoader.loadClass(params.entity)
            def instance = clazz.get(params.id)
            if (instance) {
                values = values.split(',').collect { it.trim() }
                for (v in values) {
                    instance.deleteTagValue(params.entity, v)
                }
                def map = [entity: params.entity, id: params.id, value: values]
                render map as JSON
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "${params.entity} with id ${params.id} not found")
            }
        } else {
            response.status = javax.servlet.http.HttpServletResponse.SC_NO_CONTENT
        }
    }

    def find() {
        def baseURI = new URI('bean://crmTagService/list')
        def query = params.getSelectionQuery()
        def uri = selectionService.addQuery(baseURI, query)
        def json = [selection: selectionService.encodeSelection(uri)]
        render json as JSON
    }

    def autocomplete() {
        params.offset = params.offset ? params.int('offset') : 0
        if (params.limit && !params.max) params.max = params.limit
        params.max = Math.min(params.max ? params.int('max') : 25, 100)
        def tenant = TenantUtils.tenant
        def tag = crmTagService.getTagInstance(params.entity, tenant)
        if (!tag) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND)
            return
        }
        def result = CrmTagLink.createCriteria().list(offset: params.offset, max: params.max) {
            projections {
                distinct('value')
            }
            eq('tag', tag)
            if (params.q) {
                ilike('value', this.wildcard(params.q))
            }
        } as Set<String>

        def options = CrmTagOptions.createCriteria().list(offset: params.offset, max: params.max, sort: 'optionsString', order: 'asc') {
            eq('crmTag', tag)
            if (params.q) {
                ilike('optionsString', this.wildcard(params.q))
            }
        }

        if (options) {
            result.addAll(options.collect{it.configuration.value})
        }

        WebUtils.noCache(response)
        render result.sort() as JSON
    }

    private String wildcard(String q) {
        q = q.toLowerCase()
        if (q.contains('*')) {
            return q.replace('*', '%')
        } else if (q[0] == '=') { // Exact match.
            return q[1..-1]
        } else { // Starts with is default.
            return '%' + q + '%'
        }
    }
}
