<?php
function fbStatusCron() { //$post_ID

	$statusUpdate = get_option(STATUS_UPDATER_OPTIONS);
	if ($statusUpdate == "" || $statusUpdate == false || $statusUpdate == null) {
		return;
	}
	
    $fbLogEmail = $statusUpdate["fb-log-email"];
	
	$fbDebug = false;
	if (isSet($statusUpdate["fb-debug"])) {
		$fbDebug = $statusUpdate["fb-debug"];
	}

	fbDebug("inc-fb-status-cron.php", "fbStatusCron()", "Starting the cron job");
	if ($fbDebug) {
		sendLogEmail($fbLogEmail);
	}

/*
	if (isSet($statusUpdate["last-cron"])) {
		// please do not run more often than 2 minutes
		if ((time() - $statusUpdate["last-cron"]) < (60*2)) {
			return;
		}
	}
*/
	$querystring = "numberposts=1&order=DESC&orderby=date&post_status=publish&post_type=post";

	if (isSet($statusUpdate["fb-post-ids"])) {
		$fbPostIds = str_replace("#", ",", $statusUpdate["fb-post-ids"]);
		$fbPostIds = trim(substr($fbPostIds, 1, strlen($fbPostIds) - 2));
		$querystring .= "&exclude=".$fbPostIds;
	}

	$myposts = get_posts($querystring);

	foreach($myposts as $post) {

		if (!isSet($post->ID) || $post->ID == null || $post->ID == false || $post->ID == "") {
			setup_postdata($post); // shouldn't be necessary but docs say yes
		}
		fbDebug("inc-fb-status-cron.php", "fbStatusCron()", "Executing fbStatusUpdater(".'$post->ID = '.$post->ID.", true)");
		fbStatusUpdater($post->ID, true);
	}

	// get the updated version because the fbStatusUpdater function added a new id to the list of already published
	// the variable retrieved at the beginning of this function does not contain it and by storing the new
	// cron time on that, the new id would be lost
	$statusUpdate = get_option(STATUS_UPDATER_OPTIONS);
	$statusUpdate["last-cron"] = time();
	fbDebug("inc-fb-status-cron.php", "fbStatusCron()", "Updating last-cron: ".date('Y-m-d H:i:s', time()));
	update_option(STATUS_UPDATER_OPTIONS, $statusUpdate);

	if ($fbDebug) {
		sendLogEmail($fbLogEmail);
	}

	fbDebug("inc-fb-status-cron.php", "fbStatusCron()", "Checking comments on Social Networks");
	fbStatusComments();
}
?>