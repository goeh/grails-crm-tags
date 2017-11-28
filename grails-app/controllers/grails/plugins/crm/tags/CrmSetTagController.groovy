package grails.plugins.crm.tags

import grails.util.GrailsNameUtils

class CrmSetTagController {

    static allowedMethods = [save: 'POST']

    def crmTagService
    def selectionService

    def index() {
        def entityName = params.entityName
        def uri = params.getSelectionURI()

        def tags = crmTagService.getTagOptions(entityName)
        def result = selectionService.select(uri, [max: 10])
        return [entityName: entityName, selection: uri, tags: tags, result: result, totalCount: result.totalCount]
    }

    def save() {
        def entityName = params.entityName
        def propertyName = GrailsNameUtils.getPropertyName(entityName)
        def uri = params.getSelectionURI()
        def result = selectionService.select(uri, params)
        def tags = params.tags
        if (tags) {
            tags = tags.split(',').findAll { it.trim() } // Convert to list with non-empty elements
            log.debug "Tagging ${result.totalCount} ${entityName} with $tags"
            for (m in result) {
                for (t in tags) {
                    crmTagService.setTagValue(m, t)
                }
            }
            flash.success = message(code: propertyName + '.selection.tag.success', args: [result.totalCount, message(code: propertyName + '.label'), tags.join(', ')])
        }
        def redirectParams = params.subMap([grailsApplication.config.selection.uri.parameter ?: 'q'])
        redirect controller: entityName, action: 'list', params: redirectParams
    }
}
