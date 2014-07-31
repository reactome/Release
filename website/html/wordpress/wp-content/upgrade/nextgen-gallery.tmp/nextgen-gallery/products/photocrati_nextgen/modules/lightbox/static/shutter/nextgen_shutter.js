jQuery(function($) {
    var callback = function() {
        var selector = nextgen_lightbox_filter_selector($, $([]));
        selector.addClass('shutterset');
        window.shutterSettings = {
            imageCount: true,
            msgLoading: ngg_lightbox_i18n.msgLoading,
            msgClose: ngg_lightbox_i18n.msgClose
        };
        shutterReloaded.init();
    };
    $(this).bind('refreshed', callback);

    var flag = 'shutter';

    if (typeof($(window).data(flag)) == 'undefined') {
        $(window).data(flag, true);
    } else {
        return;
    }

    callback();
});
