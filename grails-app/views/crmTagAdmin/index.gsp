<%@ page contentType="text/html;charset=UTF-8" %><!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="main">
    <title><g:message code="crmTagAdmin.index.title" default="Tag Administration"/></title>
</head>

<body>

<header class="page-header">
    <h1><g:message code="crmTagAdmin.index.title" default="Tag Administration"/></h1>
</header>

<div class="accordion" id="accordion1">
    <g:each in="${result}" var="tag">
        <div class="accordion-group">
            <div class="accordion-heading">
                <a class="accordion-toggle" data-toggle="collapse" data-parent="#accordion1"
                   href="#${tag.propertyName}">
                    ${message(code: tag.propertyName + '.label', default: tag.propertyName)} (${tag.usage})
                </a>
            </div>

            <div id="${tag.propertyName}" class="accordion-body collapse ${tag.propertyName == params.id ? 'in' : ''}">
                <div class="accordion-inner">
                    <g:form action="save">
                        <input type="hidden" name="name" value="${tag.name}"/>

                        <div class="row-fluid">
                            <div class="span6">

                                <div class="control-group">
                                    <label class="control-label">
                                        <g:message code="crmTag.description.label" default="Description"/>
                                    </label>

                                    <div class="controls">
                                        <g:textArea name="description" value="${tag.description}" cols="70" rows="5"
                                                    class="span12" placeholder="${message(code: 'crmTag.description.help')}"/>
                                    </div>
                                </div>
                            </div>
                            <div class="span6">
                                <div class="control-group">
                                    <label class="control-label">
                                        <g:message code="crmTag.options.label" default="Options"/>
                                    </label>

                                    <div class="controls">
                                        <%
                                            def config = tag.options?.inject(new StringBuilder()) {s, opt->
                                                s << opt.toString()
                                                if(opt.icon && opt.description) {
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
                                        %>
                                        <g:textArea name="options" value="${config}" cols="70" rows="5"
                                                    class="span12" placeholder="${message(code: 'crmTag.options.help')}"/>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div class="form-actions">
                            <button type="submit" class="btn btn-success">
                                <i class="icon-ok icon-white"></i>
                                <g:message code="crmTag.button.save.label" default="Save"/>
                            </button>
                        </div>
                    </g:form>
                </div>
            </div>
        </div>
    </g:each>
</div>

</body>
</html>