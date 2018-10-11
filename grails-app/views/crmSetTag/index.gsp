<%@ page import="grails.util.GrailsNameUtils" %>
<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="main">
    <g:set var="propertyName" value="${GrailsNameUtils.getPropertyName(entityName)}"/>
    <title><g:message code="${propertyName}.selection.tag.title" args="${[totalCount, message(code: propertyName + '.label')]}"/></title>
    <r:require module="select2"/>
    <r:script>
        $(document).ready(function() {
            $.ajax({
                cache: false,
                url: "${createLink(controller: 'crmTag', action: 'autocomplete', params: [entity: entityName])}",
                dataType: "json",
                success: function(tags) {
                    $("#inputTags").select2({
                        tags: tags,
                        tokenSeparators: [",", " "]
                    });
                }
            });
        });
    </r:script>
</head>

<body>

<crm:header title="${propertyName}.selection.tag.title" args="${[totalCount, message(code: propertyName + '.label')]}"/>

<g:form action="save">

    <input type="hidden" name="q" value="${select.encode(selection: selection)}"/>
    <input type="hidden" name="entityName" value="${propertyName}"/>


    <div class="control-group">
        <label for="inputTags" class="control-label"><g:message code="crmTag.list.label"
                                                                default="Tags"/></label>

        <div class="controls">
            <input type="hidden" id="inputTags" name="tags" value="" class="input-xlarge">
        </div>
    </div>

    <table class="table table-striped">
        <thead>
        <tr>
            <th><g:message code="${propertyName}.selection.tag.sample" args="${totalCount}"/></th>
        </tr>
        </thead>
        <tbody>
        <g:each in="${result}" var="m">
            <tr>
                <td><crm:referenceLink reference="${m}">${m}</crm:referenceLink></td>
            </tr>
        </g:each>
        </tbody>
    </table>


    <div class="form-actions">
        <crm:button action="save" label="Tagga" icon="icon-ok icon-white" visual="success"
                    confirm="${message(code: propertyName + '.selection.tag.confirm', args: [totalCount])}"/>
        <crm:button action="remove" label="Ta bort" icon="icon-trash icon-white" visual="danger"
                    confirm="${message(code: propertyName + '.selection.untag.confirm', args: [totalCount])}"/>
        <select:link controller="${propertyName}" action="list" selection="${selection}" class="btn">
            <i class="icon-remove"></i>
            <g:message code="${propertyName}.button.back.label" default="Back"/>
        </select:link>
    </div>
</g:form>

</body>
