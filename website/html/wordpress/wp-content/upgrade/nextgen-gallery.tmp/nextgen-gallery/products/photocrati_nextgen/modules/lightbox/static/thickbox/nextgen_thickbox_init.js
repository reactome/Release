jQuery(function($) {
	var selector = nextgen_lightbox_filter_selector($, $([]));
	selector.addClass('thickbox');

    if (typeof ngg_lightbox_i18n == 'undefined') {
        // for backwards compatibility, ngg_lightbox_i18n may not exist and we should just use the English defaults
        window.thickboxL10n = {
            loadingAnimation: photocrati_ajax.wp_includes_url + '/wp-includes/js/thickbox/loadingAnimation.gif',
            closeImage: photocrati_ajax.wp_includes_url + '/wp-includes/js/thickbox/tb-close.png',
            next: 'Next &gt;',
            prev: '&lt; Prev',
            image: 'Image',
            of: 'of',
            close: 'Close',
            noiframes: 'This feature requires inline frames. You have iframes disabled or your browser does not support them.'
        };
    } else {
        window.thickboxL10n = {
            loadingAnimation: photocrati_ajax.wp_includes_url + '/wp-includes/js/thickbox/loadingAnimation.gif',
            closeImage: photocrati_ajax.wp_includes_url + '/wp-includes/js/thickbox/tb-close.png',
            next: ngg_lightbox_i18n.next,
            prev: ngg_lightbox_i18n.prev,
            image: ngg_lightbox_i18n.image,
            of: ngg_lightbox_i18n.of,
            close: ngg_lightbox_i18n.close,
            noiframes: ngg_lightbox_i18n.noiframes
        };
    }
});
