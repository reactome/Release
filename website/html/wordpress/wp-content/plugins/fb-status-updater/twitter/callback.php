<?php
/**
 * @file
 * Take the user when they return from Twitter. Get access tokens.
 * Verify credentials and redirect to based on response from Twitter.
 */


session_start();
require_once('./twitteroauth/twitteroauth.php');


/* If the oauth_token is old redirect to the connect page. */
if (isset($_REQUEST['oauth_token']) && $_SESSION['oauth_token'] !== $_REQUEST['oauth_token']) {
	unset($_SESSION['oauth_token']);
	unset($_SESSION['oauth_token_secret']);
	unset($_SESSION['consumer_key']);
	unset($_SESSION['consumer_secret']);
	echo("Something went wrong, close this window and repeat from the beginnning [errorcode: 1]");
	exit();
}

/* Create TwitteroAuth object with app key/secret and token key/secret from default phase */
$connection = new TwitterOAuth($_SESSION['consumer_key'], $_SESSION['consumer_secret'], $_SESSION['oauth_token'], $_SESSION['oauth_token_secret']);

/* Request access tokens from twitter */
$access_token = $connection->getAccessToken($_REQUEST['oauth_verifier']);

/* Save the access tokens. Normally these would be saved in a database for future use. */
$_SESSION['access_token'] = $access_token;

/* Remove no longer needed request tokens */
unset($_SESSION['oauth_token']);
unset($_SESSION['oauth_token_secret']);
unset($_SESSION['consumer_key']);
unset($_SESSION['consumer_secret']);

/* If HTTP response is 200 continue otherwise send to connect page to retry */
if (200 == $connection->http_code) {
	/* The user has been verified and the access tokens can be saved for future use */
	$_SESSION['status'] = 'verified';
	?>
	<script type="text/javascript">
	if (window.opener && !window.opener.closed) {
		window.opener.document.getElementById('tw-access-token-oauth').value = '<?php echo($access_token['oauth_token']); ?>';
		window.opener.document.getElementById('tw-access-token-osecret').value = '<?php echo($access_token['oauth_token_secret']); ?>';
		document.write("<p>Your blog is now linked to your Twitter account. Close this window AND save the WP settings page</p>");
	} else {
		document.write("<p>There's a problem, this window can't talk with the opener one, there's some data they need to exchange. If you didn't close the main window, try checking your browser security / privacy settings and retry</p>");
	}
	</script>
	
	<?php
  
} else {
	/* Save HTTP status for error dialog on connnect page.*/
	unset($_SESSION['oauth_token']);
	unset($_SESSION['oauth_token_secret']);
	unset($_SESSION['consumer_key']);
	unset($_SESSION['consumer_secret']);
	echo("Something went wrong, close this window and repeat from the beginnning [errorcode: 2]");
	exit();
}
