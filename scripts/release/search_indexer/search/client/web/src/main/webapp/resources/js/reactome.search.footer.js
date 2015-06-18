$(document).ready(function() {
    $('#local-searchbox').autocomplete({
        serviceUrl: '/content/getTags',
        minChars:2,
        deferRequestBy: 250,
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

$('ul.term-list').each(function(){

    var LiN = $(this).find('li').length;

    if( LiN > 6){
        $('li', this).eq(5).nextAll().hide().addClass('toggleable');
        $(this).append('<li class="more">More...</li>');
    }

});


$('ul.term-list').on('click','.more', function(){

    if( $(this).hasClass('less') ){
        $(this).text('More...').removeClass('less');
    }else{
        $(this).text('Less...').addClass('less');
    }

    $(this).siblings('li.toggleable').slideToggle();

});

$('#search_form').submit(function(e) {
    if (!$('#local-searchbox').val()) {
        e.preventDefault();
    } else if ($('#local-searchbox').val().match(/^\s*$/)){
        e.preventDefault();
    }
});

$(".plus").click(function () {
    $plus = $(this);
    $treeContent = $plus.next();
    $treeContent.slideToggle(500, function () {
        if ($treeContent.is(":visible") ){
            return $plus.find(".image").attr("src", "../resources/images/minus.png");
        } else {
            return $plus.find(".image").attr("src", "../resources/images/plus.png");
        }
    });
});