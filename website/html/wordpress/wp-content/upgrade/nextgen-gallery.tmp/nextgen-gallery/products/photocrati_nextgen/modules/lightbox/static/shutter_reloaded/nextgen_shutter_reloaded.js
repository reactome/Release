jQuery(function($){
	var callback = function(){
		var selector = nextgen_lightbox_filter_selector($, $([]));
		selector.addClass('shutterset');
        if (typeof ngg_lightbox_i18n != 'undefined') {
            shutterReloaded.L10n = ngg_lightbox_i18n;
        }
        shutterReloaded.Init();
	};
	$(this).bind('refreshed', callback);

   var flag = 'shutterReloaded';
   if (typeof($(window).data(flag)) == 'undefined')
       $(window).data(flag, true);
   else return;

   callback();
});
