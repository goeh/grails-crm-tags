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

import org.codehaus.groovy.grails.commons.GrailsClassUtils

class CrmTagsGrailsPlugin {
    // the plugin dependency group
    def groupId = "grails.crm"
    // the plugin version
    def version = "0.9.3"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "2.0 > *"
    // the other plugins this plugin depends on
    def dependsOn = [:]
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp",
            "grails-app/domain/grails/plugins/crm/tags/TestEntity.groovy"
    ]

    def title = "Grails CRM Tags"
    def author = "Goran Ehrsson"
    def authorEmail = "goran@technipelago.se"
    def organization = [name: "Technipelago AB", url: "http://www.technipelago.se/"]
    def description = "Provides tagging support for Grails CRM"
    def documentation = "http://grails.org/plugin/crm-tags"
    def license = "APACHE"

    // Location of the plugin's issue tracker.
    def issueManagement = [system: "github", url: "https://github.com/goeh/grails-crm-tags/issues"]

    // Online location of the plugin's browseable source code.
    def scm = [url: "https://github.com/goeh/grails-crm-tags"]

    def observe = ["domain"]

    def doWithDynamicMethods = { ctx ->
        def crmTagService = ctx.getBean("crmTagService")
        for (domainClass in application.domainClasses) {
            def taggableProperty = getTaggableProperty(domainClass)
            if (taggableProperty) {
                addDomainMethods(domainClass.clazz.metaClass, crmTagService)
            }
        }
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
        }
        mc.getTagValue = { String tagName ->
            crmTagService.getTagValue(delegate, tagName)
        }
        mc.getClassTags = { ->
            def v = crmTagService.getTagValue(delegate, null) ?: []
            if(! (v instanceof Collection)) {
                v = [v]
            }
            return v
        }
        mc.isTagged = { String tagName ->
            crmTagService.isTagged(delegate, tagName)
        }
        mc.deleteTagValue = { Object[] args ->
            crmTagService.deleteTagValue(delegate, args)
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
