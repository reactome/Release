<?php
function fbStatusUpdater($post_ID, $cron = false) {

	if (!function_exists("curl_init")) {
		return;
	}

	global $fbStatusCookieFile, $fbDebugLog, $fbStatusUpdaterVersion;
	
	fbDebug("inc-fb-status-updater.php", "fbStatusUpdater(".'$post_ID = '.$post_ID.', $cron = '.var_export($cron, true).")", "Starting");

	if (!is_writable($fbStatusCookieFile)) {
		if (!$cron) {
			echo("Your post has been published but there was an error while updating the status. The file ".$fbStatusCookieFile." is not writable from PHP. Please ensure PHP has the correct permissions set to write and update that file. If you don't know what I'm speaking about, please contact your server admin / webmaster. If you don't want to see this message every time you publish a new post while you try solving the problem, just <a href=\"/wp-admin/plugins.php\">disable this plugin</a>. <a href=\"http://codex.wordpress.org/Changing_File_Permissions\">More about file permissions on Wordpress</a>");
			exit();
		} else {
			/* useless... monkey users should just read instructions or read errors on the settings page
			if ($fbLogEmail != null) {
				sendLogEmail($fbLogEmail, "The Status Updater Cron tried to post an article but the file ".$fbStatusCookieFile." is not writable from PHP.\n\n Please ensure PHP has the correct permissions set to write and update that file. If you don't know what I'm speaking about, please contact your server admin / webmaster. If you don't want to see this message every time you publish a new post while you try solving the problem, just disable this plugin.");
			}
			*/
			return;
			
		}
	}

	$startTime = getMicrotime();

	$statusUpdate = get_option(STATUS_UPDATER_OPTIONS);

	if ($statusUpdate === false) {
		// no settings stored
		return;
	}

	if (!isSet($statusUpdate["version"]) || (isSet($statusUpdate["version"]) && $statusUpdate["version"] != $fbStatusUpdaterVersion) ) {
		// the plugin has not been activated yet
		fbDebug("inc-fb-status-updater.php", "fbStatusUpdater(...)", "Launching activation. Current version: ".$fbStatusUpdaterVersion." - previously installed version: ".$statusUpdate["version"]);
		$statusUpdate = fbStatusAct();
	}
	
	// in case the version is 2.0, usernames & passwords have been deleted and api keys are empty. It's like no settings are stored, so...
	if ($statusUpdate["fb-access-token"] == null && $statusUpdate["tw-access-token"] == null) {
		return;
	}

	if (isSet($statusUpdate["fb-post-ids"]) && $statusUpdate["fb-post-ids"] != "" && $statusUpdate["fb-post-ids"] != "#") {
		// check if the current post id has already been sent to facebook
		if (strpos($statusUpdate["fb-post-ids"], "#".$post_ID."#") !== false) {
			// the post was already sent to facebook
			fbDebug("inc-fb-status-updater.php", "fbStatusUpdater(...)", "The post has already been shared. Post Id: ".$post_ID." - Post Ids: ".$statusUpdate["fb-post-ids"]);
			if ($statusUpdate["fb-debug"]) {
				sendLogEmail($statusUpdate["fb-log-email"]);
			}
			return;
		}
	} else {
		// should already be set when installing the plugin from version 1.2
		// #1#2#3#100#1850#
		$statusUpdate["fb-post-ids"] = "#";
	}

	$fbAppId = null;
	$fbAppSecret = null;
	$fbAccessToken = null;
	$fbSettings = null;
	
	$twConsumerKey = null;
	$twConsumerSecret = null;
	$twAccessToken = null;
	$twLat = null;
	$twLong = null;
	
	$fbLongUrl = true;
	$fbLogEmail = null;
	$fbDebug = false;
	$fbAdvancedStatus = false;
	
	$linkShortenerLogin = null;
	$linkShortenerPassword = null;
	
	$defaultStatusTemplate = "%POST-TITLE% %POST-URL%";
	
	// checks
	$facebookIsUp = true;
	
	if (isSet($statusUpdate["fb-access-token"])) {
		$fbAppId = $statusUpdate["fb-app-id"];
		$fbAppSecret = $statusUpdate["fb-app-secret"];
		$fbAccessToken = $statusUpdate["fb-access-token"];
		$fbSettings = $statusUpdate["fb-settings"];
	}
	
	if (isSet($statusUpdate["fb-debug"])) {
		$fbDebug = $statusUpdate["fb-debug"];
	}
	if (isSet($statusUpdate["fb-log-email"])) {
		$fbLogEmail = $statusUpdate["fb-log-email"];
	}
	if (isSet($statusUpdate["fb-long-url"])) {
		$fbLongUrl = $statusUpdate["fb-long-url"];
	}
	if (isSet($statusUpdate["fb-advanced-status"])) {
		$fbAdvancedStatus = $statusUpdate["fb-advanced-status"];
	}
	
	if (isSet($statusUpdate["tw-access-token"])) {
		$twConsumerKey = $statusUpdate["tw-consumer-key"];
		$twConsumerSecret = $statusUpdate["tw-consumer-secret"];
		$twAccessToken = $statusUpdate["tw-access-token"];
		$twLat = $statusUpdate["tw-lat"];
		$twLong = $statusUpdate["tw-long"];
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
	if ($twAccessToken == null && $fbAccessToken == null) {
		// nothing stored, just leave
		return;
	}
	
	// get post data
	$post = get_post($post_ID);
	if ($fbLongUrl) {
		$postUrl = get_permalink($post_ID);
	} else {
		$postUrl = get_bloginfo("wpurl")."/?p=".$post_ID;
	}
	$postTitle = $post->post_title;
	if (function_exists(strip_shortcodes)) {
		$postTitle = strip_shortcodes($postTitle);
	}
	
	$postSummary = strip_tags($post->post_excerpt);
	if (trim($postSummary."") == "") {
		$postSummary = substr(strip_tags($post->post_content), 0, 500);
	}
	$postSummary = str_replace("\n", " ", $postSummary);
	$postSummary = trim(preg_replace('/\s+/', " ", $postSummary))."...";
	
	if (function_exists(strip_shortcodes)) {
		$postSummary = strip_shortcodes($postSummary);
	}
	
	$postContent = $post->post_content;
	$postStatus = $post->post_status;
	unset($post);

	fbDebug("inc-fb-status-updater.php", "fbStatusUpdater(...)", "Post title: ".$postTitle);
	fbDebug("inc-fb-status-updater.php", "fbStatusUpdater(...)", "Post summary: ".$postSummary);
	fbDebug("inc-fb-status-updater.php", "fbStatusUpdater(...)", "Post status: ".$postStatus);
	
	// if the post is scheduled, do nothing
	if ($postStatus == "future" || $postStatus == "draft" || $postStatus == "private") {
		if ($fbDebug) {
			sendLogEmail($fbLogEmail);
		}
		return;
	}

	$statusFacebook = $postTitle;
	$statusFacebookCustom = null;
	$statusTwitter = $postTitle;

	// get the custom settings for the current post
	if ($fbAdvancedStatus) {

		fbDebug("inc-fb-status-updater.php", "fbStatusUpdater(...)", "Advanced status composition");

		$statusUpdateMeta = get_post_meta($post_ID, STATUS_UPDATER_POST_META, true);
		
		// serialization for meta fields is not working, translating types
		if (isSet($statusUpdateMeta["push"]) && ($statusUpdateMeta["push"] == 0 || $statusUpdateMeta["push"] == "0" || $statusUpdateMeta["push"] == ""))  {
			$statusUpdateMeta["push"] = false;
		}

		// if the user chose not to send the current post, just add its id to the sent list and end
		if ($statusUpdateMeta["push"] == false) {
			// post id alread added to the "done" list at the beginning, just need to stop the function
			fbDebug("inc-fb-status-updater.php", "fbStatusUpdater(...) / Advanced status composition", "Push option: ".var_export($statusUpdateMeta["push"], true));
			if ($fbDebug) {
				sendLogEmail($fbLogEmail);
			}
			return;
		}

		// overwrites default options with meta associated with the post
		if ($fbAccessToken != null && $fbSettings != null) {

			// serialization for meta fields is not working, translating types
			if (isSet($statusUpdateMeta["custom-facebook-status"]) && $statusUpdateMeta["custom-facebook-status"] == "") {
				$statusUpdateMeta["custom-facebook-status"] = null;
			}
			if (isSet($statusUpdateMeta["fb-push"]) && ($statusUpdateMeta["fb-push"] == 0 || $statusUpdateMeta["fb-push"] == "0" || $statusUpdateMeta["fb-push"] == ""))  {
				$statusUpdateMeta["fb-push"] = false;
			}
			
			if ($statusUpdateMeta["custom-facebook-status"] != null) {
				$statusFacebookCustom = $statusUpdateMeta["custom-facebook-status"];
				fbDebug("inc-fb-status-updater.php", "fbStatusUpdater(...) / Advanced status composition", "Custom facebook status: ".$statusFacebook);
			}

			if ($statusUpdateMeta["fb-push"] == false) {
				$statusFacebook = null;
				fbDebug("inc-fb-status-updater.php", "fbStatusUpdater(...) / Advanced status composition", "Facebook push option: ".var_export($statusUpdateMeta["fb-push"], true));
			}
		}

		if ($twConsumerKey !== null && $twConsumerSecret !== null && $twAccessToken !== null) {
			
			// serialization for meta fields is not working, translating types
			if (isSet($statusUpdateMeta["custom-twitter-status"]) && $statusUpdateMeta["custom-twitter-status"] == "") {
				$statusUpdateMeta["custom-twitter-status"] = null;
			}
			if (isSet($statusUpdateMeta["tw-push"]) && ($statusUpdateMeta["tw-push"] == 0 || $statusUpdateMeta["tw-push"] == "0" || $statusUpdateMeta["tw-push"] == ""))  {
				$statusUpdateMeta["tw-push"] = false;
			}
			
			if ($statusUpdateMeta["custom-twitter-status"] != null) {
				$statusTwitter = $statusUpdateMeta["custom-twitter-status"];
				fbDebug("inc-fb-status-updater.php", "fbStatusUpdater(...) / Advanced status composition", "Custom twitter status: ".$statusTwitter);
			}

			if ($statusUpdateMeta["tw-push"] == false) {
				$statusTwitter = null;
				fbDebug("inc-fb-status-updater.php", "fbStatusUpdater(...) / Advanced status composition", "Twitter push option: ".var_export($statusUpdateMeta["tw-push"], true));
				//echo("The user decided not to send this post to Twitter<br />"); //debug
			}
		}
	}

	// store immediately as already sent, otherwise the cron job could work the post too (because the script takes so long to execute, facebook is slow :) or the database connection could time out by the end of the scritp
	$statusUpdate["fb-post-ids"] .= $post_ID."#";
	update_option(STATUS_UPDATER_OPTIONS, $statusUpdate);
	
	$shortLink = shortenLink($postUrl, $statusUpdate);
	
	fbDebug("inc-fb-status-updater.php", "fbStatusUpdater(...)", "Short link: ".$shortLink);
	
	// stores feed ids of pushed content so that each post is related to its related facebook feeds
	$snRef = array();

	// if $fbAccessToken is not null then send but if $statusFacebook is null then this post shouldn't be sent because of advanced options
	if ($fbAccessToken != null && $fbSettings != null && $statusFacebook != null) {
		
		$fbProfile = getPage("https://graph.facebook.com/me?access_token=".$fbAccessToken);
		$fbProfile = json_decode($fbProfile);
		//fbDebug("inc-fb-status-updater.php", "fbStatusUpdater(...)", "Facebook profile: ".var_export($fbProfile, true));
		
		$fbAccounts = getPage("https://graph.facebook.com/me/accounts?access_token=".$fbAccessToken);
		$fbAccounts = json_decode($fbAccounts);
		//fbDebug("inc-fb-status-updater.php", "fbStatusUpdater(...)", "Facebook accounts: ".var_export($fbAccounts, true));
		
		// if the image to share is still null, let's try to get it from the post content
		preg_match('/<img.+src="http:\/\/(.*?)"/i', $postContent, $postImage);
		if (isSet($postImage[1])) {
			$fbShareImage = "http://".$postImage[1];
		} else {
			preg_match('/<img.+src=\'http:\/\/(.*?)\'/i', $postContent, $postImage);
			if (isSet($postImage[1])) {
				$fbShareImage = "http://".$postImage[1];
			}
		}
		fbDebug("inc-fb-status-updater.php", "fbStatusUpdater(...)", "Post image: ".$fbShareImage);

		fbDebug("inc-fb-status-updater.php", "fbStatusUpdater(...)", "Share settings: ".var_export($fbSettings, true));
		
		if ($statusFacebookCustom !== null) {
			$statusFacebook = $statusFacebookCustom;
		}
		$statusFacebook = str_replace("%POST-TITLE%", $statusFacebook, $defaultStatusTemplate);
		$statusFacebook = str_replace("%POST-URL%", $shortLink, $statusFacebook);

		// if required, post to profile
		if (isSet($fbSettings[$fbProfile->id])) {
			if ($fbSettings[$fbProfile->id] == "status") {
				fbDebug("inc-fb-status-updater.php", "fbStatusUpdater(...)", "Posting status to profile id: ".$fbProfile->id." - name: ".$fbProfile->name);

				$postData = "access_token=".$fbAccessToken."&message=".$statusFacebook;
				$fbResponse = getPage("https://graph.facebook.com/me/feed", $postData);
				$isError = checkFbResponseError($fbResponse);
				if ($isError !== false) {
					fbDebug("inc-fb-status-updater.php", "fbStatusUpdater(...)", "Error while posting to profile status: ".$isError[0]." - ".$isError[1]);
					sendLogEmail($fbLogEmail);
				} else {
					$fbResponse = json_decode($fbResponse);
					fbDebug("inc-fb-status-updater.php", "fbStatusUpdater(...)", "Posted to profile status, new feed id: ".$fbResponse->id);
					array_push($snRef, array("facebook", $fbResponse->id));
				}
			}

			if ($fbSettings[$fbProfile->id] == "link") {
				fbDebug("inc-fb-status-updater.php", "fbStatusUpdater(...)", "Posting link to profile id: ".$fbProfile->id." - name: ".$fbProfile->name);
				$postData = "access_token=".$fbAccessToken;
				//$postData .= "&message=".$statusFacebook;
				$postData .= "&link=".$shortLink;
				$postData .= "&picture=".$fbShareImage;
				$postData .= "&name=".$postTitle;
				$postData .= "&caption=".get_bloginfo("description");
				$postData .= "&description=".$postSummary;
				if ($statusFacebookCustom != null) {
					$postData .= "&message=".$statusFacebookCustom;
				}
				//$postData .= "&source=http://www.francesco-castaldo.com/plugins-and-widgets/fb-status-updater/";
				$fbResponse = getPage("https://graph.facebook.com/me/feed", $postData);
				$isError = checkFbResponseError($fbResponse);
				if ($isError !== false) {
					fbDebug("inc-fb-status-updater.php", "fbStatusUpdater(...)", "Error while posting to profile link: ".$isError[0]." - ".$isError[1]);
					sendLogEmail($fbLogEmail);
				} else {
					$fbResponse = json_decode($fbResponse);
					fbDebug("inc-fb-status-updater.php", "fbStatusUpdater(...)", "Posted to profile link, new feed id: ".$fbResponse->id);
					array_push($snRef, array("facebook", $fbResponse->id));
				}
			}
		}

		if (isSet($fbAccounts->data)) {
			foreach($fbAccounts->data as $account) {

				if ($fbSettings[$account->id] == "status") {
					fbDebug("inc-fb-status-updater.php", "fbStatusUpdater(...)", "Posting status to page id: ".$account->id." - name: ".$account->name);

					$postData = "access_token=".$account->access_token."&message=".$statusFacebook;
					$fbResponse = getPage("https://graph.facebook.com/me/feed", $postData);
					$isError = checkFbResponseError($fbResponse);
					if ($isError !== false) {
						fbDebug("inc-fb-status-updater.php", "fbStatusUpdater(...)", "Error while posting to page status: ".$isError[0]." - ".$isError[1]);
						sendLogEmail($fbLogEmail);
					} else {
						$fbResponse = json_decode($fbResponse);
						fbDebug("inc-fb-status-updater.php", "fbStatusUpdater(...)", "Posted to page status, new feed id: ".$fbResponse->id);
						array_push($snRef, array("facebook", $fbResponse->id));
					}
				}

				if ($fbSettings[$account->id] == "link") {
					fbDebug("inc-fb-status-updater.php", "fbStatusUpdater(...)", "Posting link to page id: ".$account->id." - name: ".$account->name);
					$postData = "access_token=".$account->access_token;
					//$postData .= "&message=".$statusFacebook;
					$postData .= "&link=".$shortLink;
					$postData .= "&picture=".$fbShareImage;
					$postData .= "&name=".$postTitle;
					$postData .= "&caption=".get_bloginfo("description");
					$postData .= "&description=".$postSummary;
					//$postData .= "&source=http://www.francesco-castaldo.com/plugins-and-widgets/fb-status-updater/";
					$fbResponse = getPage("https://graph.facebook.com/me/feed", $postData);
					$isError = checkFbResponseError($fbResponse);
					if ($isError !== false) {
						fbDebug("inc-fb-status-updater.php", "fbStatusUpdater(...)", "Error while posting to page link: ".$isError[0]." - ".$isError[1]);
						sendLogEmail($fbLogEmail);
					} else {
						$fbResponse = json_decode($fbResponse);
						fbDebug("inc-fb-status-updater.php", "fbStatusUpdater(...)", "Posted to page link, new feed id: ".$fbResponse->id);
						array_push($snRef, array("facebook", $fbResponse->id));
					}
				}
			}
		}
	}

	//Twitter?
	if ($twConsumerKey != null && $twConsumerSecret != null && $twAccessToken != null && $statusTwitter !== null) {

		$tmpTwitterStatus =  str_replace("%POST-URL%", $shortLink, $defaultStatusTemplate);
		$availableTwitterStatusLength = 140 - strlen($tmpTwitterStatus) + strlen("%POST-TITLE%");

		if (strlen($statusTwitter) > $availableTwitterStatusLength) {
			$statusTwitter = substr($statusTwitter, 0, ($availableTwitterStatusLength - 3))."...";
		}

		$statusTwitter = str_replace("%POST-TITLE%", $statusTwitter, $tmpTwitterStatus);

		fbDebug("inc-fb-status-updater.php", "fbStatusUpdater(...)", "Twitter status: ".$statusTwitter." (".strlen($statusTwitter).")");
		
		require_once('twitter/twitteroauth/twitteroauth.php');

		$connection = new TwitterOAuth($twConsumerKey, $twConsumerSecret, $twAccessToken['oauth_token'], $twAccessToken['oauth_token_secret']);
		
		// Check what the ->post method returns
		if ($twLat != null && $twLong != null) {
			fbDebug("inc-fb-status-updater.php", "fbStatusUpdater(...)", "Twitter geo location: ".$twLat.":".$twLong);
			$twParameters = array('status' => $statusTwitter, 'lat' => $twLat, 'long' => $twLong, 'trim_user' => '1');
		} else {
			$twParameters = array('status' => $statusTwitter, 'trim_user' => '1');
		}
		$twResponse = $connection->post('statuses/update', $twParameters);

		if ($twResponse === null) {
			fbDebug("inc-fb-status-updater.php", "fbStatusUpdater(...)", "Twitter response is null. Maybe not available.");
			sendLogEmail($fbLogEmail);
		} else {
			if (isSet($twResponse->id)) {
				fbDebug("inc-fb-status-updater.php", "fbStatusUpdater(...)", "Tweet posted. New tweet id: ".$twResponse->id);
				array_push($snRef, array("twitter", $twResponse->id));	
			} else {
				fbDebug("inc-fb-status-updater.php", "fbStatusUpdater(...)", "Error while posting to twitter: ".var_export($twResponse, true));
				sendLogEmail($fbLogEmail);
			}
		}
	}

	if (count($snRef) > 0) {
		delete_post_meta($post_ID, STATUS_UPDATER_SN_META);
		add_post_meta($post_ID, STATUS_UPDATER_SN_META, $snRef, true);
	}

	$time = getMicrotime() - $startTime;

	fbDebug("inc-fb-status-updater.php", "fbStatusUpdater(...)", "The push process took: ".number_format($time, 3, ".", "")." seconds");
	fbDebug("inc-fb-status-updater.php", "fbStatusUpdater(...)", "Post ids: ".$statusUpdate["fb-post-ids"]);

	if ($fbDebug && ! $cron) {
		sendLogEmail($fbLogEmail);
	}

	deleteFbSessionData();
}
?>