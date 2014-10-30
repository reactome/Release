$(document).ready(function() {

    $('#local-searchbox').autocomplete({
        serviceUrl: '${pageContext.request.contextPath}/getTags',
        paramName: "tagName",
        delimiter: ",",
        transformResult: function(response) {
            return {
                suggestions: $.map($.parseJSON(response), function(item) {
                    return { value: item };
                })
            };
        },
        onSelect: function(value, data){$("#search_form").submit()}
    });
});
