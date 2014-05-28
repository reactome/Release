<script type="text/javascript">
if (window.opener && !window.opener.closed) {
	var url = window.location.href;
	var accessToken = url.substring(url.indexOf("#access_token=") + 14, url.lastIndexOf("&"));
	window.opener.document.getElementById('fb-access-token').value = unescape(accessToken);
	document.write("<p>Your blog is now linked to your Facebook account. Close this window AND save the WP settings page after filling all the fileds.</p>");
} else {
	document.write("<p>There's a problem, this window can't talk with the opener one, there's some data they need to exchange. If you didn't close the main window, try checking your browser security / privacy settings and retry</p>");
}
</script>
	