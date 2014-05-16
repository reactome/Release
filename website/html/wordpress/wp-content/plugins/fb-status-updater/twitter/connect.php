<?php

$consumerKey = null;
$consumerSecret = null;
$callback = null;

if (isSet($_GET["consumerKey"]) && trim($_GET["consumerKey"]) != "") {
	$consumerKey = trim($_GET["consumerKey"]);
}

if (isSet($_GET["consumerSecret"]) && trim($_GET["consumerSecret"]) != "") {
	$consumerSecret = trim($_GET["consumerSecret"]);
}

if (isSet($_GET["callback"]) && trim($_GET["callback"]) != "") {
	$callback = trim($_GET["callback"]);
}

if ($consumerKey == null || $consumerSecret == null) {
  echo 'You need a consumer key and secret to connect to Twitter. Close this popup and carefully follow the instructions on the plugin settings page.';
  exit;
}

if ($callback == null) {
		echo("This page has been loaded with a missing parameter, someone messed up the code... no other possible reasons I believe. Cheers");
}

/*
echo($consumerKey."<br />");
echo($consumerSecret."<br />");
echo($callback."<br />");
*/

session_start();
require_once('twitteroauth/twitteroauth.php');

$connection = new TwitterOAuth($consumerKey, $consumerSecret);
 
/* Get temporary credentials. */
$request_token = $connection->getRequestToken($callback);


/* Save temporary credentials to session. */
$_SESSION['oauth_token'] = $token = $request_token['oauth_token'];
$_SESSION['oauth_token_secret'] = $request_token['oauth_token_secret'];
$_SESSION['consumer_key'] = $consumerKey;
$_SESSION['consumer_secret'] = $consumerSecret;
 
/* If last connection failed don't display authorization link. */
switch ($connection->http_code) {
  case 200:
    /* Build authorize URL and redirect user to Twitter. */
	$url = $connection->getAuthorizeURL($token);
	header('Location: ' . $url);
	break;
	default:
	/* Show notification if something went wrong. */
    echo 'Could not connect to Twitter. Refresh the page or try again later.';
}
?>