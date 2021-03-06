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

import grails.plugins.crm.core.TenantUtils
import grails.transaction.Transactional
import groovy.transform.CompileStatic

import javax.servlet.http.HttpServletResponse

/**
 * Tag admin.
 */
class CrmTagAdminController {

    static allowedMethods = [save: 'POST']

    def crmTagService


    def index() {
        def allTags = crmTagService.list(params).sort { it.propertyName }
        [result: allTags]
    }

    @Transactional
    def save() {
        def tenant = TenantUtils.tenant
        def tagName = params.name
        def crmTag = CrmTag.findByNameAndTenantId(tagName, tenant)
        if (!crmTag) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND)
            return
        }

        bindData(crmTag, params, [include: ['description', 'mustMatch', 'multiple']])
        bindOptions(crmTag, params.options)

        crmTag.save(flush: true)

        // Display "Tag foo updated" message.
        final String propertyName = crmTag.propertyName
        def typeLabel = message(code: 'crmTag.label', default: 'Tag')
        def tagLabel = message(code: propertyName + '.label', default: propertyName)
        flash.success = message('crmTag.updated.message', args: [typeLabel, tagLabel], default: '{0} {1} updated')

        redirect action: 'index', id: crmTag.propertyName
    }

    private void bindOptions(CrmTag crmTag, String configString) {
        def map = parseOptionConfig(configString)
        def remove = []
        for (opt in crmTag.options) {
            if (!map.containsKey(opt.configuration.value)) {
                remove << opt
            }
        }
        for (opt in remove) {
            crmTag.removeFromOptions(opt)
            opt.delete()
        }
        map.each { opt, config ->
            def tagOption = crmTag.getOption(opt)
            if (tagOption) {
                tagOption.icon = config.remove('icon')
                tagOption.description = config.remove('text')
                tagOption.optionsString = opt
            } else {
                crmTag.addToOptions(opt)
            }
        }
    }

    @CompileStatic
    private String getOptionConfig(CrmTag crmTag) {
        final StringBuilder s = new StringBuilder()

        for (opt in crmTag.options) {
            s << opt.optionsString
            if (opt.icon && opt.description) {
                s << ',icon='
                s << opt.icon
                s << ',text='
                s << opt.description
            } else if (opt.icon) {
                s << ',icon='
                s << opt.icon
            } else if (opt.description) {
                s << ','
                s << opt.description
            }
            s << '\n'
        }
        s.toString()
    }

    private Map<String, Map<String, Object>> parseOptionConfig(String arg) {
        final Map<String, Map<String, Object>> result = [:]
        if (arg) {
            for (String s in arg.split('\n')) {
                s = s.trim()
                if (s) {
                    def config = CrmTagOptions.parseOption(s)
                    result[config.value] = config
                }
            }
        }
        result
    }

}
