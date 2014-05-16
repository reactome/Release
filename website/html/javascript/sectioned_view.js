var plus_img_src = '/icons/plus-box.gif';
var minus_img_src = '/icons/minus-box.gif';

function setSectionVisibility (section_id,off_by_default) {
        var img_id = 'toggle_' + section_id;
        var t = section_id.indexOf('_');
        var cookieName = section_id.substring(0, t);
        var cv = getCookie(cookieName);
	if (off_by_default && !cv) {
	       cv = 'off';
	}
        var div = document.getElementById(section_id);
        var img = document.getElementById(img_id);
        if ((cv == '') || (cv == 'on')) {
               div.style.display = 'block';
	       if (img != null) {
		   img.src = minus_img_src;
	       }
        } else {
               div.style.display = 'none';
	       if (img != null) {
		   img.src = plus_img_src;
	       }
        }
}

function changeSectionVisibility (section_id) {
        var img_id = 'toggle_' + section_id;
        var div = document.getElementById(section_id);
        var img = document.getElementById(img_id);
        var t = section_id.indexOf('_');
        var cookieName = section_id.substring(0, t);
        if (div.style.display == 'none') {
               div.style.display = 'block';
	       if (img != null) {
		   img.src = minus_img_src;
	       }
               setCookie(cookieName,'on');
        } else {
               div.style.display = 'none';
	       if (img != null) {
		   img.src = plus_img_src;
	       }
               setCookie(cookieName,'off');
        }
}

function getCookie (cookieName) {
        var cookieValue = '';
	var posName = document.cookie.indexOf(escape(cookieName) + '=');
	if (posName != -1) {
		var posValue = posName + (escape(cookieName) + '=').length;
		var endPos = document.cookie.indexOf(';', posValue);
		if (endPos != -1) cookieValue = unescape(document.cookie.substring(posValue, endPos));
		else cookieValue = unescape(document.cookie.substring(posValue));
	}
	return (cookieValue);
}

function setCookie (cookieName, cookieValue, expires, path, domain, secure) {
	document.cookie =
		escape(cookieName) + '=' + escape(cookieValue)
		+ (expires ? '; expires=' + expires.toGMTString() : '')
		+ (path ? '; path=' + path : '')
		+ (domain ? '; domain=' + domain : '')
		+ (secure ? '; secure' : '');
}
