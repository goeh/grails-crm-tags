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

import org.codehaus.groovy.grails.commons.GrailsClassUtils

class CrmTagsGrailsPlugin {
    def groupId = ""
    def version = "2.0.2"
    def grailsVersion = "2.2 > *"
    def dependsOn = [:]
    def loadAfter = ['crmCore']
    def observe = ["domain"]
    def pluginExcludes = [
            "grails-app/views/error.gsp",
            "grails-app/domain/grails/plugins/crm/tags/TestEntity.groovy"
    ]
    def title = "GR8 CRM Tagging Support"
    def author = "Goran Ehrsson"
    def authorEmail = "goran@technipelago.se"
    def organization = [name: "Technipelago AB", url: "http://www.technipelago.se/"]
    def description = "Provides tagging support for GR8 CRM applications"
    def documentation = "http://gr8crm.github.io/plugins/crm-tags/"
    def license = "APACHE"
    def issueManagement = [system: "github", url: "https://github.com/goeh/grails-crm-tags/issues"]
    def scm = [url: "https://github.com/goeh/grails-crm-tags"]

    def features = {
        crmTag {
            description "Tag objects with user-defined labels"
            permissions {
                guest "crmTag:list"
                partner "crmTag:list"
                user "crmTag:*"
                admin "crmTag,crmTagAdmin:*"
            }
            required true
            hidden true
        }
    }

    def doWithDynamicMethods = { ctx ->
        def crmTagService = ctx.getBean("crmTagService")
        for (domainClass in application.domainClasses) {
            def taggableProperty = getTaggableProperty(domainClass)
            if (taggableProperty) {
                addDomainMethods(domainClass.clazz.metaClass, crmTagService)
            }
        }
    }

    def doWithApplicationContext = { applicationContext ->
        // crm.tags.crmContact.show.sidebar = true
        def crmPluginService = applicationContext.getBean('crmPluginService')
        def cfg = application.config.crm.tags
        if(cfg) {
            for(controller in cfg) {
                for(view in controller) {
                    for(location in view) {
                        crmPluginService.registerView(controller, view, location, [id: 'tags', template: '/tags', plugin: 'crm-tags', model: []])
                    }
                }
            }
        }
        //<g:render template="/tags" plugin="crm-tags" model="${[bean: crmContact]}"/>
        //<g:render template="${view.template}" model="${view.model}" plugin="${view.plugin}"/>
    }

    def onChange = { event ->
        def ctx = event.ctx
        if (event.source && ctx && event.application) {
            def service = ctx.getBean('crmTagService')
            // enhance domain classes with taggable property
            if ((event.source instanceof Class) && application.isDomainClass(event.source)) {
                def domainClass = application.getDomainClass(event.source.name)
                if (getTaggableProperty(domainClass)) {
                    addDomainMethods(domainClass.metaClass, service)
                }
            }
        }
    }

    private void addDomainMethods(MetaClass mc, def crmTagService) {
        mc.setTagValue = { Object[] args ->
            crmTagService.setTagValue(delegate, args)
            return delegate
        }
        mc.getTagValue = { String tagName ->
            crmTagService.getTagValue(delegate, tagName)
        }
        mc.getClassTags = {->
            def v = crmTagService.getTagValue(delegate, null) ?: []
            if (!(v instanceof Collection)) {
                v = [v]
            }
            return v
        }
        mc.isTagged = { String tagName, String arg = null ->
            if(arg) {
                return crmTagService.isTagged(delegate, tagName, arg)
            }
            return crmTagService.isTagged(delegate, tagName)
        }
        mc.deleteTagValue = { Object[] args ->
            crmTagService.deleteTagValue(delegate, args)
            return delegate
        }
        mc.deleteTag = { String tagName ->
            crmTagService.deleteTag(delegate, tagName)
        }
        mc.static.findAllByTag = {Object[] args ->
            crmTagService.findAllByTag(delegate, * args)
        }
    }

    public static final String TAGGABLE_PROPERTY_NAME = "taggable";

    private getTaggableProperty(domainClass) {
        GrailsClassUtils.getStaticPropertyValue(domainClass.clazz, TAGGABLE_PROPERTY_NAME)
    }

}
