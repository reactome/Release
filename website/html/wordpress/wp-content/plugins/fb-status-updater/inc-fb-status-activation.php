<?php
function fbStatusAct() {
	global $wpdb, $fbStatusUpdaterVersion;
	
	if (!wp_next_scheduled(STATUS_UPDATER_CRON)) {
		wp_schedule_event(time(), 'hourly', STATUS_UPDATER_CRON);
	}

	$statusUpdate = get_option(STATUS_UPDATER_OPTIONS);
	//echo("before ".$statusUpdate["fb-post-ids"]."<br />");

	// here we have to add all existing posts to the list of the already sent, otherwise the chron job will post the old ones every half an hour

	$fbPostIds = "#";

	// get all the post ids and set them in the options so that previous posts won't be sent by the cron job
	$ids = $wpdb->get_col("SELECT ID FROM ".$wpdb->posts." WHERE post_status = 'publish' AND post_type = 'post';");
	if (!empty($ids)) {
		foreach ($ids as $id) {
			$fbPostIds .= $id."#";
			
			// delete all old post meta fields (for this plugin). Too many changes to translate everything to the new values and not enough time
			delete_post_meta($id, 'fb-status-updater-send');
			
			if ($statusUpdate !== false) {
				if (isSet($statusUpdate["version"]) && $statusUpdate["version"] != null) {
					if (substr($statusUpdate["version"], 0, 3) < 1.9) {
						$oldValues = get_post_meta($id, STATUS_UPDATER_POST_META, true);
						if ($oldValues === "") {
							delete_post_meta($id, STATUS_UPDATER_POST_META);
						}
					}
				}
			}
		}
	}

	if ($statusUpdate == false) {
		$statusUpdate = array();
		$statusUpdate["fb-post-ids"] = $fbPostIds;
		$statusUpdate["version"] = $fbStatusUpdaterVersion;
		add_option(STATUS_UPDATER_OPTIONS, $statusUpdate);
		//echo("add ".$statusUpdate["fb-post-ids"]."<br />");
	} else {
		$statusUpdate["version"] = $fbStatusUpdaterVersion;
		$statusUpdate["fb-post-ids"] = $fbPostIds;
		// convert old params into new
		if (isSet($statusUpdate["jmp-api-login"]) && isSet($statusUpdate["jmp-api-key"]) && $statusUpdate["jmp-api-login"] != null && $statusUpdate["jmp-api-key"] != null) {
			$statusUpdate["link-shortener-login"] = $statusUpdate["jmp-api-login"];
			$statusUpdate["link-shortener-password"] = $statusUpdate["jmp-api-key"];
			$statusUpdate["link-shortener-service"] = "j.mp";
		} else {
			if (!isSet($statusUpdate["link-shortener-login"])) {
				$statusUpdate["link-shortener-service"] = "is.gd";
			}
		}
		
		// clean the old stuff	
		unset(
			$statusUpdate["jmp-api-login"],
			$statusUpdate["jmp-api-key"],
			$statusUpdate["fb-wall-id"],
			$statusUpdate["fb-wall-id-2"],
			$statusUpdate["fb-email"],
			$statusUpdate["fb-password"],
			$statusUpdate["fb-dob-day"],
			$statusUpdate["fb-dob-month"],
			$statusUpdate["fb-dob-year"],
			$statusUpdate["fb-dob-year"],
			$statusUpdate["fb-push-as-profile-status"],
			$statusUpdate["fb-push-as-profile-link"],
			$statusUpdate["fb-page1-url"],
			$statusUpdate["fb-push-as-page1-status"],
			$statusUpdate["fb-push-as-page1-link"],
			$statusUpdate["fb-share-icon"],
			$statusUpdate["fb-share-image"],
			$statusUpdate["tw-user"],
			$statusUpdate["tw-password"],
			$statusUpdate["myspace-email"],
			$statusUpdate["myspace-password"],
			$statusUpdate["myspace-default-mood"],
			$statusUpdate["cron-time"]
		);

		update_option(STATUS_UPDATER_OPTIONS, $statusUpdate);
		//echo("update ".$statusUpdate["fb-post-ids"]."<br />");
	}
	//echo("<p>Status updater plugin activated silently ;)</p>");
	
	// try to delete old files
	@unlink("ajax-loader.gif");
	@unlink("inc-fb-status-check.php");
	@unlink("jqModal.js");

	return $statusUpdate;
}

function deletePostMeta($postId) {
	delete_post_meta($postId, 'fb-status-updater-status');
	delete_post_meta($postId, 'fb-status-updater-share-link');
	delete_post_meta($postId, 'fb-status-updater-share-image');
	delete_post_meta($postId, 'fb-status-updater-tw-status');
	delete_post_meta($postId, 'fb-status-updater-ms-status');
	delete_post_meta($postId, 'fb-status-updater-ms-mood');
	delete_post_meta($postId, 'fb-status-updater-send');
	delete_post_meta($postId, 'fb-status-updater-send-fb');
	delete_post_meta($postId, 'fb-status-updater-send-tw');
	delete_post_meta($postId, 'fb-status-updater-send-ms');
}
?>