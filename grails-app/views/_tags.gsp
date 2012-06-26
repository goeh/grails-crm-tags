<g:content tag="head">
    <r:require module="autocomplete"/>
    <r:script>
        function refreshTags(newValue) {
            $.get("${createLink(controller: 'crmTag', action: 'list', params: [id: bean.ident(), entity: bean.class.name])}", function(data) {
                var html = "";
                for (i = 0; i < data.length; i++) {
                    var cls = (newValue != null && jQuery.inArray(data[i], newValue) != -1) ? ' pulse' : '';
                    html = html + (' <span class="label label-info' + cls + '">' + data[i] + '</span>');
                }
                var container = $("#tags .tag-list")
                container.html(html);
                $("span", container).click(
                        function(event) {
                            var value = $(this).html();
                            var input = $("#tags input[name=value]");
                            var inputValue = input.val();
                            if (inputValue != null && inputValue != "") {
                                inputValue += ",";
                            } else {
                                inputValue = "";
                            }
                            inputValue += value;
                            input.val(inputValue);
                        }).addClass("clickable");
            }, "json");
        }

        jQuery(document).ready(function() {
/*
            $("#tags input[name=value]").crmAutocomplete("${createLink(controller: 'crmTag', action: 'autocomplete', params: [entity: bean.class.name, id: bean.ident()])}", function(data) {
                var arr = [],i = data.results.length;
                while(i--) {
                    arr[i] = data.results[i]
                }
                return {labels:arr, values:arr};
            });
*/
            $("#tags input[name='value']").autocomplete("${createLink(controller: 'crmTag', action: 'autocomplete')}", {
                remoteDataType: 'json',
                preventDefaultReturn: true,
                selectFirst: true,
                extraParams: {entity: "${bean.class.name}", id: "${bean.ident()}"}
            });

            $("#tags form").submit(function(event) {
                var form = $(this);
                var input = $("input[name=value]", form)
                var value = input.val();
                event.stopPropagation();
                $.post("${createLink(controller: 'crmTag', action: 'save')}", form.serialize(), function(data) {
                    $("input[name='value']", form).val("");
                    refreshTags(data.value);
                }, "json");
                return false;
            });

            // Add a submit handler to the save button/icon.
            $("#tags .tag-save").click(function(event) {
                $("#tags form").submit();
            });
            // Add an AJAX search function to the find button/icon.
            $("#tags .tag-find").click(function(event) {
                var form = $("#tags form");
                var query = form.serialize();
                $.post("${createLink(controller: 'crmTag', action: 'find')}", query, function(data) {
                    window.location = "${select.createLink(action: 'list')}" + data.selection;
                });
            });
            // Add an AJAX delete request to the delete button/icon.
            $("#tags .tag-delete").click(function(event) {
                var form = $("#tags form");
                $.post("${createLink(controller: 'crmTag', action: 'delete')}", form.serialize(), function(data) {
                    $("#tags .tag-list span").each(function(index, span) {
                        span = $(span);
                        var value = span.html();
                        var deleted = data.value;
                        span.removeClass("pulse");
                        if (jQuery.inArray(value, deleted) != -1) {
                            span.removeClass("label-info");
                            span.addClass("label-important");
                            span.addClass("pulse");
                        }
                    });
                    $("input[name='value']", form).val("");
                    setTimeout("refreshTags(null)", 2000);
                }, "json");
            });

            refreshTags(null);
        });
    </r:script>
</g:content>

<div class="well sidebar-nav" id="tags">

    <ul class="nav nav-list">
        <li class="nav-header"><i class="icon-tags"></i> <g:message code="crmTag.list.title" default="Tags"/></li>
        <li class="tag-list"></li>
    </ul>

    <crm:hasPermission permission="crmTag:save">
        <g:form class="clearfix hide" controller="crmTag" action="save" style="margin-top: 9px;">
            <input type="hidden" name="entity" value="${bean.class.name}"/>
            <input type="hidden" name="id" value="${bean.id}"/>
            <input type="text" name="value" class="span2" style="margin-left:15px;" autocomplete="off" data-provide="typeahead"/>

            <div style="margin-left:15px;">
                <a href="#" class="btn btn-mini btn-primary tag-save"><i class="icon-ok icon-white"></i> <g:message code="crmTag.button.add.label" default="Add"/></a>
                <a href="#" class="btn btn-mini tag-find"><i class="icon-search"></i> <g:message code="crmTag.button.find.label" default="Find"/></a>
                <a href="#" class="btn btn-mini btn-danger tag-delete"><i class="icon-trash icon-white"></i> <g:message code="crmTag.button.delete.label" default="Delete"/></a>
            </div>
        </g:form>
    </crm:hasPermission>

</div>
