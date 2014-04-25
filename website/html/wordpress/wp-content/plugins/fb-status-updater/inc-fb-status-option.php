<?php

function fbStatusOptionPage(){

	if (!function_exists("curl_init")) {
		echo("This plugin requires <a href=\"http://www.php.net/curl\">Curl library</a> in order to run properly. Install curl or disable this plugin");
	} elseif (!function_exists("json_decode")) {
		echo("This plugin requires <a href=\"http://php.net/manual/en/book.json.php\">Json library</a> in order to run properly. Install Json or disable this plugin");
	} else {

		global $fbStatusCookieFile, $fbStatusUpdaterVersion, $wp_version;

		if (isSet($_GET["clear-private-data"]) && $_GET["clear-private-data"] == "true") {
			delete_option(STATUS_UPDATER_OPTIONS);

			$allposts = get_posts('numberposts=0&post_type=post&post_status=');
			foreach($allposts as $postinfo) {
				deletePostMeta($postinfo->ID);
				delete_post_meta($postinfo->ID, STATUS_UPDATER_POST_META);
			}
			unset($allposts);

			$message = "Private data cleaned, now you can safely remove this plugin.";
		}
		
		$link_shortener = array(
			"none" => array(null, false, null, null),
			"bit.ly" => array (
				"http://bit.ly", // link
				true, // requires json
				array("bitly-api-login", "text", "Bit.ly API Login"), // first field
				array("bitly-api-key", "text", "Bit.ly API Key") //second field
			),
			"j.mp" => array(
				"http://j.mp/",
				true,
				array("jmp-api-login", "text", "J.mp API Login"),
				array("jmp-api-key", "text", "J.mp API Key")
			),
			"is.gd" => array("http://is.gd/", false, null, null),

			"su.pr" => array(
				"http://www.stumbleupon.com/help/su-pr-api/",
				false,
				array("supr-api-login", "text", "Su.pr API login"),
				array("supr-api-key", "text", "Su.pr API key")
			),
			"su.pr anonymous" => array("http://su.pr/", true, null, null),
			"tr.im" => array(
				"http://tr.im/",
				false,
				array("trim-username", "text", "Tr.im username"),
				array("trim-password", "Password", "Tr.im password")
			),
			"tr.im anonymous" => array("http://tr.im/", false, null, null)
		);

		$statusUpdate = get_option(STATUS_UPDATER_OPTIONS);
		
		if (isSet($_GET["unlinkFb"]) && $_GET["unlinkFb"] == "true") {
			$statusUpdate["fb-app-id"] = null;
			$statusUpdate["fb-app-secret"] = null;
			$statusUpdate["fb-access-token"] = null;
			$statusUpdate["fb-settings"] = null;
			update_option(STATUS_UPDATER_OPTIONS, $statusUpdate);
		}
		
		if (isSet($_GET["unlinkTw"]) && $_GET["unlinkTw"] == "true") {
			$statusUpdate["tw-consumer-key"] = null;
			$statusUpdate["tw-consumer-secret"] = null;
			$statusUpdate["tw-access-token"] = null;
			update_option(STATUS_UPDATER_OPTIONS, $statusUpdate);
		}
		
		$fbAppId = null;
		$fbAppSecret = null;
		$fbAccessToken = null;
		$fbSettings = null;
		
		$fbDebug = false;
		$fbLogEmail = null;
		$fbPostIds = null;
		
		$twConsumerKey = null;
		$twConsumerSecret = null;
		$twAccessToken = null;
		$twLat = null;
		$twLong = null;
		
		$fbLongUrl = true;
		$lastCron = null;
		//$cronTime = 30;
		$fbAdvancedStatus = false;
		$linkShortenerLogin = null;
		$linkShortenerPassword = null;
		$linkShortenerService = "is.gd";
		$defaultStatusTemplate = "%POST-TITLE% %POST-URL%";
		$grabComments = false;
		$facebookCommentsEmail = "facebook@".$_SERVER["HTTP_HOST"];
		$twitterCommentsEmail = "twitter@".$_SERVER["HTTP_HOST"];
		
		$version = null;

		$message = false;
		$error = false;

		if ($statusUpdate !== false) {
			if (isSet($statusUpdate["version"])) {
				$version = $statusUpdate["version"];
			}
			if ($version == null || $version != $fbStatusUpdaterVersion) {
				// run activation
				$statusUpdate = fbStatusAct();
			}

			if (isSet($statusUpdate["fb-app-id"])) {
				$fbAppId = $statusUpdate["fb-app-id"];
			}
			if (isSet($statusUpdate["fb-app-secret"])) {
				$fbAppSecret = $statusUpdate["fb-app-secret"];
			}
			if (isSet($statusUpdate["fb-access-token"])) {
				$fbAccessToken = $statusUpdate["fb-access-token"];
			}
			if (isSet($statusUpdate["fb-settings"])) {
				$fbSettings = $statusUpdate["fb-settings"];
			}
	
			// if the account is connected to facebook, retrieve profile and accounts
			if ($fbAccessToken != null) {
				$fbProfile = getPage("https://graph.facebook.com/me?access_token=".$fbAccessToken);
				$fbProfile = json_decode($fbProfile);
				$fbAccounts = getPage("https://graph.facebook.com/me/accounts?access_token=".$fbAccessToken);
				$fbAccounts = json_decode($fbAccounts);
				// https://graph.facebook.com/me?access_token=".$fbAccessToken
			}

			if ($fbSettings == null) {
				$fbSettings[$fbProfile->id] = "none";
				if (isSet($fbAccounts->data)) {
					foreach ($fbAccounts->data as $account) {
						$fbSettings[$account->id] = "none";
					}
				}
			} else {
				if ((isSet($fbSettings[$fbProfile->id]) && ($fbSettings[$fbProfile->id] == "" || $fbSettings[$fbProfile->id] == null)) || !isSet($fbSettings[$fbProfile->id])) {
					$fbSettings[$fbProfile->id] = "none";
				}
				if (isSet($fbAccounts->data)) {
					foreach ($fbAccounts->data as $account) {
						if ($fbSettings[$account->id] == "" || $fbSettings[$account->id] == false || $fbSettings[$account->id] == null) {
							$fbSettings[$account->id] = "none";
						}
					}
				}
			}

			if (isSet($statusUpdate["fb-debug"])) {
				$fbDebug = $statusUpdate["fb-debug"];
			}
			if (isSet($statusUpdate["fb-log-email"])) {
				$fbLogEmail = $statusUpdate["fb-log-email"];
			}
			if (isSet($statusUpdate["fb-post-ids"])) {
				$fbPostIds = $statusUpdate["fb-post-ids"];
			}

			if (isSet($statusUpdate["tw-consumer-key"])) {
				$twConsumerKey = $statusUpdate["tw-consumer-key"];
			}
			if (isSet($statusUpdate["tw-consumer-secret"])) {
				$twConsumerSecret = $statusUpdate["tw-consumer-secret"];
			}
			if (isSet($statusUpdate["tw-access-token"])) {
				$twAccessToken = $statusUpdate["tw-access-token"];
			}
			if (isSet($statusUpdate["tw-lat"])) {
				$twLat = $statusUpdate["tw-lat"];
			}
			if (isSet($statusUpdate["tw-long"])) {
				$twLong = $statusUpdate["tw-long"];
			}


			if (isSet($statusUpdate["fb-long-url"])) {
				$fbLongUrl = $statusUpdate["fb-long-url"];
			}

			if (isSet($statusUpdate["fb-advanced-status"])) {
				$fbAdvancedStatus = $statusUpdate["fb-advanced-status"];
			}
			if (isSet($statusUpdate["link-shortener-login"])) {
				$linkShortenerLogin = $statusUpdate["link-shortener-login"];
			}
			if (isSet($statusUpdate["link-shortener-password"])) {
				$linkShortenerPassword = $statusUpdate["link-shortener-password"];
			}
			if (isSet($statusUpdate["link-shortener-service"])) {
				$linkShortenerService = $statusUpdate["link-shortener-service"];
			}
			if (isSet($statusUpdate["default-status-template"])) {
				$defaultStatusTemplate = $statusUpdate["default-status-template"];
			}
			if (isSet($statusUpdate["last-cron"])) {
				$lastCron = $statusUpdate["last-cron"];
			}
			if (isSet($statusUpdate["grab-comments"])) {
				$grabComments = $statusUpdate["grab-comments"];
			}
			if (isSet($statusUpdate["fb-comments-email"])) {
				$facebookCommentsEmail = $statusUpdate["fb-comments-email"];
			}
			if (isSet($statusUpdate["tw-comments-email"])) {
				$twitterCommentsEmail = $statusUpdate["tw-comments-email"];
			}
		}

		if (isSet($_POST["action"]) && $_POST["action"] == "fb-status-update") {

			// Facebook
			if (isSet($_POST["fb-app-id"]) && trim($_POST["fb-app-id"]) != "") {

				$fbAppId = stripslashes($_POST["fb-app-id"]);
			} else {
				// do not set $fbAppId to null because while editing settings the param is not sent
			}

			if (isSet($_POST["fb-app-secret"]) && trim($_POST["fb-app-secret"]) != "") {
				$fbAppSecret = stripslashes($_POST["fb-app-secret"]);
			} else {
				// do not set $fbAppSecret to null because while editing settings the param is not sent
			}

			if ($fbAppId != null && $fbAppSecret != null && isSet($_POST["fb-access-token"]) && trim($_POST["fb-access-token"]) != "") {
				$fbAccessToken = stripslashes($_POST["fb-access-token"]);
			} else {
				// do not set $fbAccessToken to null because while editing settings the param is not sent
			}

			if ($fbAccessToken != null) {
				if (isSet($_POST[$fbProfile->id]) && trim($_POST[$fbProfile->id]) != "") {
					$fbSettings[$fbProfile->id] = $_POST[$fbProfile->id];
				}
				if (isSet($fbAccounts->data)) {
					foreach ($fbAccounts->data as $account) {
						if (isSet($_POST[$account->id]) && trim($_POST[$account->id]) != "") {
							$fbSettings[$account->id] = $_POST[$account->id];
						}
					}
				}
			}

			if (($fbAppId == null && $fbAppSecret != null) || ($fbAppId != null && $fbAppSecret == null)) {
				$error = "If you want to update your Facebook account, please fill both your Facebook App Api Key and Secret. Otherwise leave the whole section blank.";
			}

			if ($fbAppId != null && $fbAppSecret == null && $fbAccessToken == null) {
				$error = "Please complete the Facebook Connect process or leave \"Facebook App Id\" and \"Facebook App Secret\" fields blank.";
			}

			if (isSet($_POST["fb-long-url"]) && $_POST["fb-long-url"] == "true") {
				$fbLongUrl = true;
			} else {
				$fbLongUrl = false;
			}

			// Twitter
			if (isSet($_POST["tw-consumer-key"]) && trim($_POST["tw-consumer-key"]) != "") {
				$twConsumerKey = stripslashes(trim($_POST["tw-consumer-key"]));
			} else {
				// do not set $twConsumerKey to null because while editing settings the param is not sent
			}

			if (isSet($_POST["tw-consumer-secret"]) && trim($_POST["tw-consumer-secret"]) != "") {
				$twConsumerSecret = stripslashes(trim($_POST["tw-consumer-secret"]));
			} else {
				// do not set $twConsumerSecret to null because while editing settings the param is not sent
			}
			
			if ($twConsumerKey != null && $twConsumerSecret != null && isSet($_POST["tw-access-token-oauth"]) && trim($_POST["tw-access-token-oauth"]) != "") {
				$twAccessToken["oauth_token"] = stripslashes($_POST["tw-access-token-oauth"]);
			} else {
				// do not set $twAccessToken to null because while editing settings the param is not sent
			}
			
			if ($twConsumerKey != null && $twConsumerSecret != null && isSet($_POST["tw-access-token-osecret"]) && trim($_POST["tw-access-token-osecret"]) != "") {
				$twAccessToken["oauth_token_secret"] = stripslashes($_POST["tw-access-token-osecret"]);
			} else {
				// do not set $twAccessToken to null because while editing settings the param is not sent
			}
			
			if (isSet($_POST["tw-lat"]) && trim($_POST["tw-lat"]) != "") {
				$twLat = stripslashes(trim($_POST["tw-lat"]));
			} else {
				$twLat = null;
			}
			
			if (isSet($_POST["tw-long"]) && trim($_POST["tw-long"]) != "") {
				$twLong = stripslashes(trim($_POST["tw-long"]));
			} else {
				$twLong = null;
			}
			
			if (($twLat == null && $twLong != null) || ($twLat != null && $twLong == null)) {
				$error = "Please fill both latitude and longitude for Twitter or leave both fields blank.";
			}

			if (($twConsumerKey == null && $twConsumerSecret != null) || ($twConsumerKey != null && $twConsumerSecret == null)) {
				$error = "If you want to update your twitter account, please fill both CONSUMER KEY and CONSUMER SECRET. Otherwise leave both blank.";
			}
			
			if ($twConsumerKey != null && $twConsumerSecret != null && $twAccessToken == null) {
				$error = "Please complete the Twitter connect process, otherwise leave both CONSUMER KEY and CONSUMER SECRET fields blank.";
			}

			// link shortener
			if (isSet($_POST["link-shortener-service"]) && trim($_POST["link-shortener-service"]) != "") {
				$linkShortenerService = trim(stripslashes($_POST["link-shortener-service"]));
				if (!isSet($link_shortener[$linkShortenerService])) {
					$error = "Link shortener service not valid";
				}
			} else {
				$linkShortenerService = "is.gd";
			}
			if ($link_shortener[$linkShortenerService][2] != null) {
				if (isSet($_POST[makeJsId($linkShortenerService)."-link-shortener-login"]) && trim($_POST[makeJsId($linkShortenerService)."-link-shortener-login"]) != "") {
					$linkShortenerLogin = trim(stripslashes($_POST[makeJsId($linkShortenerService)."-link-shortener-login"]));
				} else {
					$error = $link_shortener[$linkShortenerService][2][2]." is mandatory";
				}
			}
			if ($link_shortener[$linkShortenerService][3] != null) {
				if (isSet($_POST[makeJsId($linkShortenerService)."-link-shortener-password"]) && trim($_POST[makeJsId($linkShortenerService)."-link-shortener-password"]) != "") {
					$linkShortenerPassword = trim(stripslashes($_POST[makeJsId($linkShortenerService)."-link-shortener-password"]));
				} else {
					$error = $link_shortener[$linkShortenerService][3][2]." is mandatory";
				}
			}

			// Misc options
			if (isSet($_POST["default-status-template"]) && trim($_POST["default-status-template"]) != "") {
				$defaultStatusTemplate = strip_tags(trim(stripslashes($_POST["default-status-template"])));

				if (strpos($defaultStatusTemplate, "%POST-TITLE%") === false || strpos($defaultStatusTemplate, "%POST-URL%") === false) {
					$error = "One of the mandatory status template tokens is missing. Please enter both tokens or set the default template: %POST-TITLE% %POST-URL%";
				}
			} else {
				$error = "The default status template is mandatory";
			}
			
			if (isSet($_POST["grab-comments"]) && $_POST["grab-comments"] == "true") {
				$grabComments = true;
			}
			if (isSet($_POST["fb-comments-email"]) && trim($_POST["fb-comments-email"]) != "") {
				$facebookCommentsEmail = $_POST["fb-comments-email"];
			}
			if (isSet($statusUpdate["tw-comments-email"]) && trim($_POST["tw-comments-email"]) != "") {
				$twitterCommentsEmail = $_POST["tw-comments-email"];
			}
			
			if (isSet($_POST["fb-debug"]) && $_POST["fb-debug"] == "true") {
				$fbDebug = true;
			} else {
				$fbDebug = false;
			}
			
			if (isSet($_POST["fb-log-email"]) && trim($_POST["fb-log-email"]) != "") {

				$fbLogEmail = stripslashes($_POST["fb-log-email"]);

				if (!eregi("^[_a-z0-9-]+(\.[_a-z0-9-]+)*@[a-z0-9-]+(\.[a-z0-9-]+)*(\.[a-z]{2,4})$", $_POST["fb-log-email"])) {
					$error = "Log Email not valid.";
				}
			} else {
				$error = "Log email is required";
			}
/*
			if (isSet($_POST["fb-cron-time"]) && trim($_POST["fb-cron-time"]) != "" && is_numeric($_POST["fb-cron-time"])) {
				$cronTime = stripslashes($_POST["fb-cron-time"]);
			} else {
				$error = "Cron Job interval should be a number and is mandatory.";
			}
*/
			if (isSet($_POST["fb-advanced-status"]) && trim($_POST["fb-advanced-status"]) == "true") {
				$fbAdvancedStatus = true;
			} else {
				$fbAdvancedStatus = false;
			}

			//if ($fbAppId == null && $fbAppSecret == null && $twConsumerKey == null && $twConsumerSecret == null && $myspaceEmail == null && $myspacePassword == null) {
			if ($fbAppId == null && $fbAppSecret == null && $twConsumerKey == null && $twConsumerSecret == null) {
				$error = "The plugin would be grateful if you filled at least one account data.";
			}

			if ($error === false) {
				// no validation errors here

				$statusUpdate = array();
				$statusUpdate["fb-app-id"] = $fbAppId;
				$statusUpdate["fb-app-secret"] = $fbAppSecret;
				$statusUpdate["fb-access-token"] = $fbAccessToken;

				$statusUpdate["fb-settings"]  = $fbSettings;

				$statusUpdate["fb-debug"] = $fbDebug;

				$statusUpdate["tw-consumer-key"] = $twConsumerKey;
				$statusUpdate["tw-consumer-secret"] = $twConsumerSecret;
				$statusUpdate["tw-access-token"] = $twAccessToken;
				$statusUpdate["tw-lat"] = $twLat;
				$statusUpdate["tw-long"] = $twLong;

				$statusUpdate["fb-log-email"] = $fbLogEmail;
				$statusUpdate["fb-post-ids"] = $fbPostIds;
				$statusUpdate["fb-long-url"] = $fbLongUrl;
				$statusUpdate["fb-advanced-status"] = $fbAdvancedStatus;
				$statusUpdate["link-shortener-service"] = $linkShortenerService;
				$statusUpdate["link-shortener-login"] = $linkShortenerLogin;
				$statusUpdate["link-shortener-password"] = $linkShortenerPassword;
				$statusUpdate["default-status-template"] = $defaultStatusTemplate;
				$statusUpdate["version"] = $version;
				$statusUpdate["grab-comments"] = $grabComments;
				$statusUpdate["fb-comments-email"] = $facebookCommentsEmail;
				$statusUpdate["tw-comments-email"] = $twitterCommentsEmail;

				update_option(STATUS_UPDATER_OPTIONS, $statusUpdate);
				$message = "Plugin settings have been updated.";
			}
		}
		$fbStatusBaseUrl = trailingslashit(get_bloginfo('wpurl')).PLUGINDIR.'/'.dirname(plugin_basename(__FILE__));
?>
<script type="text/javascript">

function shareOptions(checked) {
	if (checked) {
		jQuery("#shareOptions").show("slow");
	} else {

		jQuery("#shareOptions").hide("slow");
	}
}

function setBg(id) {
	<?php foreach ($link_shortener as $key => $value) { ?>
	jQuery("#table-<?php echo(makeJsId($key)); ?>").css({'background-color' : '#FFFFFF'});
	<?php } ?>
	jQuery("#table-"+id).css({'background-color' : '#EDEDED'});
}
</script>

<div class="wrap">
	<h2>Status Updater</h2>
	
	<?php if ($_SERVER["REMOTE_ADDR"] == "217.133.27.98") {
		//fbStatusComments();
	} ?>
	
	<form method="post" autocomplete="off">
		<div id="poststuff" class="metabox-holder">
			<div class="postbox">
				<h3 class="hndle"><span>Info</span></h3>
				<div class="inside">
					<div style="float:right;border:1px solid #DDDDDD;padding:10px;width:200px;margin:-7px -7px 5px 5px;">
						<p>I'm happy &amp; proud you're using this plugin, <a href="http://www.francesco-castaldo.com/plugins-and-widgets/fb-status-updater/" target="_blank">giving feedback and asking features</a>.</p>
						<p>If you find this tool useful for your business, consider doing a donation to support all the hours I spent working on it. Thanks.</p>
						<p style="text-align:center"><a href="https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=8255191" target="_blank"><img src="https://www.paypal.com/en_US/i/btn/btn_donateCC_LG.gif" alt="Donate!" /></a></p>
						<p><a href="http://www.francesco-castaldo.com/about/">My blog</a></p>
					</div>

					<p>The "Status Updater" plugin shares this blog posts on Facebook and Twitter. On Facebook posts can be shared as "Status" on walls or as "Link". The difference is that statuses might be more direct, links can be re-shared by other users. On Facebook posts can be shared on the personal linked profile or on pages whose the linked profile is admin of.</p>

					<p>The plugin uses the <a href="http://codex.wordpress.org/Category:WP-Cron_Functions" target="_blank">Wordpress cron job emulation</a>; it is run every hour and publishes a scheduled-in-the-future post. For example, if you write your posts at night and schedule them to be online at 8:00am, the first time the wordpress hourly cron job is executed after 8:00am will share your post on social networks. <strong>Important</strong>: the cron job runs only if someone is browsing the blog.</p>
					<?php if ($lastCron != null) { ?>
						<p><strong>The plugin cron job last run on <?php  echo(date('Y-m-d H:i:s', $lastCron)); ?>; next run: <?php  echo(date('Y-m-d H:i:s', wp_next_scheduled(STATUS_UPDATER_CRON))); ?></strong></p>
					<?php } ?>
					<!--
					<p>
						In case you don't trust the Wordpress Cron Job emulation, you may set up the cron job on your server (or use an external, cheap and excelent service like <a href="http://www.setcronjob.com/" target="_blank">SetCronJob</a>) with the folowing url:<br />
						<input type="text" id="cron-job-url" name="cron-job-url" value="<?php echo($fbStatusBaseUrl); ?>/fb-status-updater.php?cron=true" onclick="this.select()" style="width:300px;" />					
					</p>
					-->
					
					<p>The authentications on Facebook and Twitter are implemented with OAuth and do not require username/email and password anymore.</p>
					
					<p>The "Import comments" option publish comments on Facebook and replies on Twitter as comments on the blog. The comment publication follows the same rule as for any comment added to the blog (it might require approbation).</a>

					<div style="clear:both"></div>
				</div>
			</div>

			<div class="postbox">
				<h3 class="hndle"><span>Facebook Updater</span></h3>
				<div class="inside">
					<?php if (!is_writable($fbStatusCookieFile)) { ?>
						<p class="error">Php/Wordpress cannot write into this file: <strong><?php echo($fbStatusCookieFile) ?></strong>. Please ensure PHP has the correct permissions set to write and update that file. If you don't know what I'm talking about, please contact your server admin / webmaster. If you don't want to see this message every time you publish a new post while you try solving the problem, just <a href="/wp-admin/plugins.php">disable this plugin</a>. <a href="http://codex.wordpress.org/Changing_File_Permissions">More about file permissions on Wordpress</a></p>
					<?php } ?>

					<?php if ($message != false) { ?>
						<div id="message" class="updated fade"><p><?php echo($message); ?></p></div>
					<?php } ?>
					<?php if ($error != false) { ?>
						<div id="message" class="error"><p><?php echo($error); ?></p></div>
					<?php } ?>

					<table class="form-table">
						<?php if ($fbAccessToken == null) { ?>
							<tr valign="top">
								<td colspan="2">
									<p>In order to connect your blog to your facebook page and/or profile, you have to register a new Facebook application. Follow the instructions below:</p>
									<ol>
										<li>Open a new window or tab, login into facebook and go to the <a href="http://developers.facebook.com/setup/" target="_blank">Register new application</a> page</li>
										<li>Site name: <input type="text" style="width:300px;" id="fb-site-name" name="fb-site-name" value="<?php echo(str_replace("\"", "'", get_bloginfo("name")));?>" onclick="this.select()" /></li>
										<li>Site url: <input type="text" style="width:300px;" id="fb-site-name" name="fb-site-name" value="<?php echo(str_replace("\"", "'", get_bloginfo("url"))."/");?>" onclick="this.select()" /></li>
										<li>Locale: <em>your target one</em></li>
										<li>Fill captcha fields for security check</li>
										<li>Copy App ID and App Secret values in the fields below</li>
									</ol>
								</td>
							</tr>
							<tr valign="top">
								<td style="width:150px;"><label for="fb-app-id"><strong>Facebook App ID</strong></label></td>
								<td><input style="width: 250px;" id="fb-app-id" name="fb-app-id" type="text" value="<?php echo($fbAppId); ?>" /></td>
							</tr>
							<tr valign="top">
								<td><label for="fb-app-secret"><strong>Facebook App Secret</strong></label></td>
								<td><input style="width: 250px;" id="fb-app-secret" name="fb-app-secret" type="text" value="<?php echo(str_replace("\"", "&quot;", $fbAppSecret)); ?>" /></td>
							</tr>
							<tr valign="top">
								<td colspan="2">
									<ol start="7">
										<li>Go to the <a href="http://www.facebook.com/developers/" target="_blank">developer dashboard</a> pick your newly created app (you should see it in the top-right box) and edit its settings</li>
										<li>Add your application icon (not mandatory)</li>
										<?php
											$wildDomain = $_SERVER["HTTP_HOST"];
											if (substr_count($wildDomain, ".") > 1) {
												$wildDomain = trim(substr($wildDomain, strpos($wildDomain, ".") + 1, strlen($wildDomain)));
											}
										?>
										<li>Go to the "Web Site" tab and add <input type="text" style="width:300px;" id="fb-domain" name="fb-domain" value="<?php echo($wildDomain);?>" onclick="this.select()" /> as Site Domain. You might use this app id to control your <a href="http://developers.facebook.com/docs/opengraph" target="_blank">Graph Object</a></li>
										<li>Save :)</li>
									</ol>
								</td>
							</tr>
							<tr valign="top">
								<td colspan="2">Don't try submitting the newly created app to the FB directory, it would be useless for other users.</td>
							</tr>
							<tr valign="top">
								<td colspan="2">
									<a href="javascript:connectWithFacebook();"><img src="<?php echo($fbStatusBaseUrl); ?>/facebook/fb-connect.jpg" alt="Sign in with Facebook"/></a></td>
									<script type="text/javascript">
										function connectWithFacebook() {
											
											if (document.getElementById('fb-app-id').value == '' || document.getElementById('fb-app-secret').value == '') {
												window.alert('Please get Application keys from Facebook before signing in');
												return;
											}
											
											window.open (
												"https://graph.facebook.com/oauth/authorize?client_id="+document.getElementById('fb-app-id').value+
												"&client_secret"+document.getElementById('fb-app-secret').value+
												"&redirect_uri="+escape('<?php echo($fbStatusBaseUrl);?>/facebook/callback.php')+
												"&type=user_agent&display=popup&scope=publish_stream,manage_pages,offline_access","facebookconnect","status=0,toolbar=0,location=0,menubar=0,directories=0,resizable=1,scrollbars=0,height=400,width=700"); 
											
										}
									</script>
									<input type="hidden" id="fb-access-token" name="fb-access-token" type="text" value="<?php echo($fbAccessToken); ?>" />
								</td>
							</tr>
						<?php } else { ?>
							<tr valign="top">
								<td>
									<p>Connected to <?php echo($fbProfile->name); ?> - <a href="<?php echo($_SERVER["REQUEST_URI"]); ?>&unlinkFb=true">Unlink</a> </p>
									
									<table width="100%">
										<tr>
											<th><strong>Account name</strong></th>
											<th><strong>Description</strong></th>
											<th><strong>Push as status</strong></th>
											<th><strong>Push as link</strong></th>
											<th><strong>Do not push to this account</strong></th>
										</tr>
										<tr valign="top">
											<td><?php echo($fbProfile->name); ?></td>
											<td>You</td>
											<td><input type="radio" id="<?php echo($fbProfile->id); ?>-status" name="<?php echo($fbProfile->id); ?>" value="status" <?php if (isSet($fbSettings[$fbProfile->id]) && $fbSettings[$fbProfile->id] == "status") { echo("checked=\"checked\" "); } ?>/></td>
											<td><input type="radio" id="<?php echo($fbProfile->id); ?>-link" name="<?php echo($fbProfile->id); ?>" value="link" <?php if (isSet($fbSettings[$fbProfile->id]) && $fbSettings[$fbProfile->id] == "link") { echo("checked=\"checked\" "); } ?>/></td>
											<td><input type="radio" id="<?php echo($fbProfile->id); ?>-none" name="<?php echo($fbProfile->id); ?>" value="none" <?php if (isSet($fbSettings[$fbProfile->id]) && $fbSettings[$fbProfile->id] == "none") { echo("checked=\"checked\" "); } ?>/></td>
										</tr>
										<?php if (isSet($fbAccounts->data)) { ?>
											<?php foreach ($fbAccounts->data as $account) { ?>
												<tr valign="top">
													<td><?php echo($account->name); ?></td>
													<td><?php
														$pageInfo = getPage("https://graph.facebook.com/me?access_token=".$account->access_token);
														$pageInfo = json_decode($pageInfo);
														if (strlen($pageInfo->company_overview) > 255) {
															echo(substr($pageInfo->company_overview, 0,255)."...");
														} else {
															echo($pageInfo->company_overview);
														}
													?></td>
													<td><input type="radio" id="<?php echo($account->id); ?>-status" name="<?php echo($account->id); ?>" value="status" <?php if (isSet($fbSettings[$account->id]) && $fbSettings[$account->id] == "status") { echo("checked=\"checked\" "); } ?>/></td>
													<td><input type="radio" id="<?php echo($account->id); ?>-link" name="<?php echo($account->id); ?>" value="link" <?php if (isSet($fbSettings[$account->id]) && $fbSettings[$account->id] == "link") { echo("checked=\"checked\" "); } ?>/></td>
													<td><input type="radio" id="<?php echo($account->id); ?>-none" name="<?php echo($account->id); ?>" value="none" <?php if (isSet($fbSettings[$account->id]) && $fbSettings[$account->id] == "none") { echo("checked=\"checked\" "); } ?>/></td>
												</tr>
											<?php } ?>
										<?php } ?>
									</table>
									
									<p><strong>Don't forget to save settings at the end of the page!</strong></p>
								</td>
							</tr>
							<!--
							<tr valign="top">
								<td>
									<p>Debug</p>
									<p>
										App ID: <?php echo($fbAppId); ?><br />
										App Secret: <?php echo($fbAppSecret); ?><br />
										Access token: <?php echo($fbAccessToken); ?>
									</p>
								</td>
							</tr>
							-->
						<?php } ?>						
					</table>
				</div>
			</div>

			<div class="postbox">
				<h3 class="hndle"><span>Twitter Status Updater</span></h3>
				<div class="inside">
										
					<?php if ($twAccessToken == null) { ?>
					
						<p>Want to update your Twitter profile? <a href="http://dev.twitter.com/apps" target="_blank">Register your blog as a Twitter application</a> by following the instructions below:</p>
						<ol>
							<li>Login if you aren't already and follow the "Register a new application »" link</li>
							<li>Application name: <input type="text" style="width:300px;" id="tw-app-name" name="tw-app-name" value="<?php echo(str_replace("\"", "'", get_bloginfo()));?> status updater" onclick="this.select()" /> </li>
							<li>Description: <input type="text" style="width:300px;" id="tw-app-description" name="tw-app-description" value="Posts tweets from <?php echo(str_replace("\"", "'", get_bloginfo("url")));?>" onclick="this.select()" /> </li>
							<li>Application Website: <input type="text" style="width:300px;" id="tw-app-website" name="tw-app-website" value="http://www.francesco-castaldo.com/plugins-and-widgets/fb-status-updater/" onclick="this.select()" /> </li>
							<li>Organization: <em>your company</em></li>
							<li>Website: <em>your company website</em></li>
							<li>Application Type: <em>browser</em></li>
							<li>Callback url: <input type="text" style="width:300px;" id="tw-app-callback-url" name="tw-app-callback-url" value="<?php echo($fbStatusBaseUrl);?>/twitter/callback.php" onclick="this.select()" /> </li>
							<li>Default Access type: <em>Read & Write</em></li>
							<li>Use Twitter for login: <em>No</em></li>
							<li>Fill captcha</li>
							<li>Save</li>
							<li>Copy "Consumer key" and "Consumer secret" fields in the following fields and then click the "Sign in with Twitter" button. The button opens a popup window so make sure you allow your blog to open popups ;)</li>
						</ol>
					
						<table class="form-table">
							<tr valign="top">
								<td style="width:150px;"><label for="tw-consumer-key"><strong>Twitter consumer key</strong></label></td>
								<td><input style="width: 250px;" id="tw-consumer-key" name="tw-consumer-key" type="text" value="<?php echo($twConsumerKey); ?>" /></td>
								<td></td>
							</tr>
							<tr valign="top">
								<td><label for="tw-consumer-secret"><strong>Twitter consumer secret</strong></label></td>
								<td><input style="width: 250px;" id="tw-consumer-secret" name="tw-consumer-secret" type="text" value="<?php echo($twConsumerSecret); ?>" /></td>
								<td></td>
							</tr>
							<input id="tw-access-token-oauth" name="tw-access-token-oauth" type="hidden" value="<?php echo($twAccessToken["oauth_token"]); ?>" />
							<input id="tw-access-token-osecret" name="tw-access-token-osecret" type="hidden" value="<?php echo($twAccessToken["oauth_token_secret"]); ?>" />
							<tr valign="top">
								<td colspan="2"><a href="javascript:connectWithTwitter();"><img src="<?php echo($fbStatusBaseUrl); ?>/twitter/lighter.png" alt="Sign in with Twitter"/></a></td>
								<script type="text/javascript">
									function connectWithTwitter() {
										
										if (document.getElementById('tw-consumer-key').value == '' || document.getElementById('tw-consumer-secret').value == '') {
											window.alert('Please get authentication keys from Twitter before signing in');
											return;
										}
										
										var twCallbackUrl = escape(document.getElementById('tw-app-callback-url').value);
										
										window.open ("<?php echo($fbStatusBaseUrl);?>/twitter/connect.php?consumerKey="+document.getElementById('tw-consumer-key').value+"&consumerSecret="+document.getElementById('tw-consumer-secret').value+"&callback="+twCallbackUrl,"twitterconnect","status=0,toolbar=0,location=0,menubar=0,directories=0,resizable=1,scrollbars=0,height=400,width=700"); 
										
									}
								</script>
								<td></td>
							</tr>
							<tr valign="top">
								<td><label for="tw-lat"><strong>Tweet latitude</strong></label></td>
								<td><input style="width: 250px;" id="tw-lat" name="tw-lat" type="text" value="<?php echo($twLat); ?>" /></td>
								<td rowspan="2">Tweets can be Geo Located. If you want your tweets to be localized, enable the "Tweet Location" option on <a href="" target="_blank">your Twitter account</a>. To find latitude and longitude of the location you want your blog tweets to be, you might use a free service like <a href="http://itouchmap.com/latlong.html" target="_blank">itouchmap.com</a>. Paste values in the 2 fields here.</td>
							</tr>
							<tr valign="top">
								<td><label for="tw-long"><strong>Tweet longitude</strong></label></td>
								<td><input style="width: 250px;" id="tw-long" name="tw-long" type="text" value="<?php echo($twLong); ?>" /></td>
							</tr>
						</table>
						
						<p><strong>Don't forget to save settings at the end of the page!</strong></p>
					<?php } else { ?>
						<?php
							require_once('twitter/twitteroauth/twitteroauth.php');
							
							$connection = new TwitterOAuth($twConsumerKey, $twConsumerSecret, $twAccessToken['oauth_token'], $twAccessToken['oauth_token_secret']);
							
							$twTest = $connection->get('account/verify_credentials');
							
						?>
						<p>Currently linked with <a href="http://twitter.com/<?php echo($twTest->screen_name);?>" target="_blank"><?php echo($twTest->name);?></a> Twitter account. <a href="<?php echo($_SERVER["REQUEST_URI"]); ?>&unlinkTw=true">Unlink</a>
						<table class="form-table">
							<tr valign="top">
								<td width="200"><label for="tw-lat"><strong>Tweet latitude</strong></label></td>
								<td><input style="width: 250px;" id="tw-lat" name="tw-lat" type="text" value="<?php echo($twLat); ?>" /></td>
								<td rowspan="2">Tweets can be Geo Located. If you want your tweets to be localized, enable the "Tweet Location" option on <a href="" target="_blank">your Twitter account</a>. To find latitude and longitude of the location you want your blog tweets to be, you might use a free service like <a href="http://itouchmap.com/latlong.html" target="_blank">itouchmap.com</a>. Paste values in the 2 fields here.</td>
							</tr>
							<tr valign="top">
								<td><label for="tw-long"><strong>Tweet longitude</strong></label></td>
								<td><input style="width: 250px;" id="tw-long" name="tw-long" type="text" value="<?php echo($twLong); ?>" /></td>
							</tr>
						</table>
						<!--
						<p>Debug</p>
						<p>
							Consumer key: <?php echo($twConsumerKey); ?><br />
							Consumer secret: <?php echo($twConsumerSecret); ?><br />
							OAuth access token: <?php echo($twAccessToken['oauth_token']); ?><br />
							OAuth access secret: <?php echo($twAccessToken['oauth_token_secret']); ?><br />
						</p>
						-->
					<?php } ?>
					
				</div>
			</div>

			<div class="postbox">
				<h3 class="hndle"><span>Myspace Status Updater</span></h3>
				<div class="inside">
					<p>Currently not supported. Link your Myspace account to Twitter at <a href="http://www.myspace.com/guide/sync" target="_blank">this page</a>.</p>
					<!--
					<p>Want to update your Myspace status? Here we are (leave blank if not interested)</p>
					<table class="form-table">
						<tr valign="top">
							<td style="width:150px;"><label for="myspace-email"><strong>Myspace email</strong></label></td>
							<td><input style="width: 250px;" id="myspace-email" name="myspace-email" type="text" value="<?php echo($myspaceEmail); ?>" /></td>
							<td></td>
						</tr>
						<tr valign="top">
							<td><label for="myspace-password"><strong>Myspace password</strong></label></td>
							<td><input style="width: 250px;" id="myspace-password" name="myspace-password" type="password" value="<?php echo($myspacePassword); ?>" /></td>
							<td></td>
						</tr>
						<tr valign="top">
							<td><label for="myspace-default-mood"><strong>Myspace default mood</strong></label></td>
							<td><input style="width: 250px;" id="myspace-default-mood" name="myspace-default-mood" type="text" value="<?php echo($myspaceDefaultMood); ?>" /></td>
							<td>Set your default mood to be sent with your statuses (can be overridden if you use the Advanced Status Composition). Example: happy, sleepy, idle, sad etc.</td>
						</tr>
					</table>
					-->
				</div>
			</div>

			<div class="postbox">
				<h3 class="hndle"><span>Status and link templates</span></h3>
				<div class="inside">
					<p>Available fields:<p>
					<ul>
						<li><strong>%POST-TITLE%</strong> - the post title</li>
						<li><strong>%POST-URL%</strong> - the post url</li>
						<!-- <li><strong>%POST-CATEGORY%</strong> - the post category (the first one, if the post has more)</li> -->
					</ul>
					<p>You can use mix them as you like, adding static words, emoticons etc. Remember that a Tweet max length is 140 chars. If the text part exceed the max lenght, it will be cut so that the link is shown in its entirety.</p>
					<p><strong>%POST-TITLE%</strong> and <strong>%POST-URL%</strong> are mandatory.</p>
					<p>
						A few examples of how these tokens might be used:<br />
						<strong>%POST-TITLE%</strong> <strong>%POST-URL%</strong><br />
						<strong>%POST-TITLE%</strong> <!-- | <strong>%POST-CATEGORY%</strong> -->- <strong>%POST-URL%</strong><br />
						Hey dude, did you know <strong>%POST-TITLE%</strong> -> <strong>%POST-URL%</strong>!!<br />
						Update: <strong>%POST-TITLE%</strong> <strong>%POST-URL%</strong><br />
					</p>
					<p>No html please, Facebook, Twitter and Myspace don't like it in statuses, in case you're wondering.</p>
					<table class="form-table">
						<tr valign="top">
							<td style="width:150px;"><label for="default-status-template"><strong>Default status/tweet</strong></label></td>
							<td><input style="width: 450px;" id="default-status-template" name="default-status-template" type="text" value="<?php echo($defaultStatusTemplate); ?>" /></td>
						</tr>
					</table>
					<!--
					<table class="form-table" border="1" style="width:450px;">
						<tr>
							<td colspan="2"><strong>Link</strong></td>
						</tr>
						<tr valign="top">
							<td colspan="2"><label for="fb-link-message"><strong>Link Message</strong> <input style="width: 250px;" id="fb-link-message" name="fb-link-message" type="text" value="<?php echo($fbLinkMessage); ?>" /></label></td>
						</tr>
						<tr valign="top">
							<td width="50"><div style="background-color:#666666;width:50px;height:50px;"></div></td>
							<td><label for="fb-link-message"><strong>Link Title</strong> <input style="width: 250px;" id="fb-link-message" name="fb-link-title" type="text" value="<?php echo($fbLinkTitle); ?>" /></label></td>
						</tr>
						<tr valign="top">
							<td colspan="2">[Post excerpt]</td>
						</tr>
					</table>
					-->
				</div>
			</div>

			<div class="postbox">
				<h3 class="hndle"><span>Link shortener</span></h3>
				<div class="inside">
					<p>Twitter statues have only 140 chars available. Long links might be cut or leave less space for your title to be meaningful. Choose the link shortener service you prefer to leave more room for titles.</p>
					<p>Some services might require registration, some others don't. Some offer statistic data about clicks.</p>
					<p>It has happened twice that Facebook temporary banned any or some link shorteners. If it happens again, the error you'll find in the debug log is <em>OAuthException - (#100) link URL is not properly formatted</em>. In that case just set Link Shortener to <strong>none</strong> until Facebook resolve its issues with spammers.</p>
					<?php if (!function_exists("simplexml_load_string") && !function_exists("json_decode"))  { ?>
						<p class="error">Sorry, on this server none of the 2 required libraries from link shortener service are installed: json or simple_xml. The default and basic gs.id service will be used</p>
					<?php } else { ?>
						<?php foreach ($link_shortener as $key => $value) { ?>
							<?php if ($value != null) { ?>
								<?php if (($value[1] && function_exists("json_decode")) || !$value[1]) { ?>
									<?php if ($value[2] != null) { ?>
										<table class="form-table" style="border-bottom:1px solid #999999;background-color:#FFFFFF;margin-top:0;padding-top:10px" id="table-<?php echo(makeJsId($key)); ?>">
											<tr valign="top">
												<?php if ($value[0] != null) { ?>
													<td rowspan="2" valign="middle"><input type="radio" id="<?php echo(makeJsId($key)); ?>" name="link-shortener-service" value="<?php echo($key); ?>" onclick="setBg('<?php echo(makeJsId($key)); ?>')"<?php if ($linkShortenerService == $key) { echo(" checked=\"checked\""); } ?> /> <a href="<?php echo($value[0]); ?>" target="_blank" rel="nofollow"><?php echo($key); ?></a></td>
												<?php } else { ?>
													<td rowspan="2" valign="middle"><input type="radio" id="<?php echo(makeJsId($key)); ?>" name="link-shortener-service" value="<?php echo($key); ?>" onclick="setBg('<?php echo(makeJsId($key)); ?>')"<?php if ($linkShortenerService == $key) { echo(" checked=\"checked\""); } ?> /> <?php echo($key); ?></td>
												<?php } ?>
												<td style="width:150px;"><label for="<?php echo($value[2][0]) ?>"><strong><?php echo($value[2][2]) ?></strong></label></td>
												<td><input style="width: 350px;" id="<?php echo($value[2][0]) ?>" name="<?php echo(makeJsId($key)); ?>-link-shortener-login" type="<?php echo($value[2][1]) ?>" value="<?php if ($linkShortenerService == $key) { echo($linkShortenerLogin); } ?>" /></td>
											</tr>
											<tr valign="top">
												<td style="width:150px;"><label for="<?php echo($value[3][0]) ?>"><strong><?php echo($value[3][2]) ?></strong></label></td>
												<td><input style="width: 350px;" id="<?php echo($value[3][0]) ?>" name="<?php echo(makeJsId($key)); ?>-link-shortener-password" type="<?php echo($value[3][1]) ?>" value="<?php if ($linkShortenerService == $key) { echo($linkShortenerPassword); } ?>" /></td>
											</tr>
										</table>
									<?php } else { ?>
										<table class="form-table" style="border-bottom:1px solid #999999" id="table-<?php echo(makeJsId($key)); ?>">
											<tr valign="top">
												<?php if ($value[0] != null) { ?>
													<td><input type="radio" id="<?php echo(makeJsId($key)); ?>" name="link-shortener-service" value="<?php echo($key); ?>" onclick="setBg('<?php echo(makeJsId($key)); ?>')"<?php if ($linkShortenerService == $key) { echo(" checked=\"checked\""); } ?> /> <a href="<?php echo($value[0]); ?>" target="_blank" rel="nofollow"><?php echo($key); ?></a></td>
												<?php } else { ?>
													<td><input type="radio" id="<?php echo(makeJsId($key)); ?>" name="link-shortener-service" value="<?php echo($key); ?>" onclick="setBg('<?php echo(makeJsId($key)); ?>')"<?php if ($linkShortenerService == $key) { echo(" checked=\"checked\""); } ?> /> <?php echo($key); ?></td>
												<?php } ?>
											</tr>
										</table>
									<?php } ?>
								<?php } else { ?>
									<table class="form-table" style="border-bottom:1px solid #999999;background-color:#FFFFFF;">
										<tr valign="top">
											<td><a href="<?php echo($value[0]); ?>" target="_blank" rel="nofollow"><?php echo($key); ?></a> requires json library, which is available from php 5.2. Your php version is <?php echo(phpversion()); ?></td>
										</tr>
									</table>
								<?php } ?>
							<?php } ?>
						<?php } ?>
						<script type="text/javascript">
						jQuery("#table-<?php echo(makeJsId($linkShortenerService)); ?>").css({'background-color' : '#EDEDED'});
						</script>
					<?php } ?>
				</div>
			</div>

			<div class="postbox">
				<h3 class="hndle"><span>Miscellaneous Options</span></h3>
				<div class="inside">
					<table class="form-table" >
						<tr valign="top">
							<td style="width:150px;"><label for="fb-debug"><strong>Import comments from <acronym title="Social Networks">SN</acronym></strong></label></td>
							<td><input disabled="disabled" id="grab-comments" name="grab-comments" type="checkbox" value="true" <?php if ($grabComments) { ?>checked="checked"<?php } ?> onclick="jQuery('#importEmails').toggle('slow')"  /></td>
							<td>Scans shared items and imports comments & replies as comments on the blog.</td>
						</tr>
					</table>
					<table class="form-table" <?php if (!$grabComments) { echo("style=\"display:none\""); }?> id="importEmails">
						<tr valign="top">
							<td style="width:150px;"><label for="fb-comments-email"><strong>Facebook comments email</strong></label></td>
							<td><input style="width: 250px;" id="fb-comments-email" name="fb-comments-email" type="text" value="<?php echo($facebookCommentsEmail); ?>" /></td>
							<td>Inserts comments from Facebook with this email address</td>
						</tr>
						<tr valign="top">
							<td style="width:150px;"><label for="tw-comments-email"><strong>Twitter comments email</strong></label></td>
							<td><input style="width: 250px;" id="tw-comments-email" name="tw-comments-email" type="text" value="<?php echo($twitterCommentsEmail); ?>" /></td>
							<td>Inserts comments from Twitter with this email address</td>
						</tr>
						<tr>
							<td colspan="3">Please note that if you use real email addresses, you can add Facebook & Twitter icons on Gravatar so that imported comments are shown in a proper way.</td>
						</tr>
					</table>
					<table class="form-table" >
					<!--
						<tr valign="top">
							<td><label for="fb-long-url"><strong>Use post slug</strong></label></td>
							<td><input id="fb-long-url" name="fb-long-url" type="checkbox" value="true" <?php if ($fbLongUrl) { ?>checked="checked"<?php } ?> /></td>
							<td>Check this if you want the url in the status to be the post slug (<strong>http://www.youtsite.com/date/category/post/</strong>), uncheck if you prefer the shorter version (<strong>http://www.youtsite.com/?p=123</strong>). Note: if your blog uses the post slug, you can choose the shorter version anyway. If you use the link shortener service in the previous section, this will be the shortened link.</td>
						</tr>
					-->
						<?php if (function_exists("add_meta_box")) { ?>
							<tr valign="top">
								<td><label for="fb-advanced-status"><strong>Advanced status composition</strong></label></td>
								<td><input id="fb-advanced-status" name="fb-advanced-status" type="checkbox" value="true" <?php if ($fbAdvancedStatus) { ?>checked="checked"<?php } ?> /></td>
								<td>
									By checking this, a custom area will be added to the "Edit post" page that include these features:<br />
									- choose whether or not post the single article to Facebook/Twitter<br />
									- specify a custom status/tweet, different from the article title, associated with the single post<br />
								</td>
							</tr>
						<?php } else { ?>
							<tr valign="top">
								<td colspan="3">Advanced status composition is available only for Wordpress 2.5 or higher.</td>
							</tr>
						<?php } ?>
						<tr valign="top">
							<td style="width:150px;"><label for="fb-log-email"><strong>Log email *</strong></label></td>
							<td><input style="width: 250px;" id="fb-log-email" name="fb-log-email" type="text" value="<?php echo($fbLogEmail); ?>" /></td>
							<td>The status pushing during the publishing process happens silently. In case something goes wrong, we'll send a report to this email address.</td>
						</tr>
						<tr valign="top">
							<td style="width:150px;"><label for="fb-debug"><strong>Debug facebook update</strong></label></td>
							<td><input id="fb-debug" name="fb-debug" type="checkbox" value="true" <?php if ($fbDebug) { ?>checked="checked"<?php } ?> /></td>
							<td>Always send emails about what the plugin is doing.</td>
						</tr>
						<!--
						<tr valign="top">
							<td><label for="fb-cron-time"><strong>Cron job interval</strong></label></td>
							<td><input style="width: 100px;" id="fb-cron-time" name="fb-cron-time" type="text" value="<?php //echo($cronTime); ?>" /></td>
							<td>The these are the minutes the plugin waits before checking if a scheduled-in-the-future publication time is over and the post can be pushed to FB/Twitter/Myspace. Defaults: 30 minutes.</td>
						</tr>
						-->
					</table>
				</div>
			</div>

			<div class="postbox">
				<h3 class="hndle"><span>Disclamer</span></h3>
				<div class="inside">
					<p>This is a free tool and is provided WITHOUT ANY WARRANTY.</p>
					<p>If for, for any reason, the social network you are sharing links through this plugin bans the provided account, by using this plugin you accept that no responsability might be addressed to the plugin itself or its developer.</p>
				</div>
			</div>

			<input type="hidden" name="action" value="fb-status-update" />
			<p class="submit"><input type="submit" name="Submit" value="Save" /></p>

			<?php if ($fbPostIds != null) { ?>

				<div class="postbox">
					<h3 class="hndle"><span>Pushed posts log</span></h3>
					<div class="inside">
						<p>Here are post ids pushed to Facebook/Twitter. Clicking on an id will open the original post in a new window. If the post is not found, it means you deleted it after it was pushed.</p>
						<p>
						<?php
							$fbPostIds = explode("#", $fbPostIds);
							foreach($fbPostIds as $fbPostId) {
								if (trim($fbPostId) != "") {
						?>
									<a href="/?p=<?php echo($fbPostId); ?>" target="_blank"><?php echo($fbPostId); ?></a>
						<?php
								}
							}
						?>
						</p>
					</div>
				</div>
			<?php } ?>

			<div class="postbox">
				<h3 class="hndle"><span>Clear private data</h3>
				<div class="inside">
					<p><strong>This is very important, please read carefully</strong>. If you intend not to use this plugin anymore, before deleting/disabling it <a href="javascript:if (window.confirm('Sure?') == true) { window.location.href='<?php echo($_SERVER["PHP_SELF"]); ?>?<?php echo($_SERVER["QUERY_STRING"]); ?>&clear-private-data=true';}">click here</a>. This will delete all data the plugin stored on your database.</p>
				</div>
			</div>
			
			<?php if ($fbDebug && 1 == 2) { ?>
				<div class="postbox">
					<h3 class="hndle"><span>Debug</h3>
					<div class="inside">
						<textarea id="" name="" cols="10" rows="10" style="width:1000px; height:800px"><?php var_dump($statusUpdate); ?></textarea>
					</div>
				</div>
			<?php } ?>
		</div>
	</form>
</div>
<?php
	}
}

?>