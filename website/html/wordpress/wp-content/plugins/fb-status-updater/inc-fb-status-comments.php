<?php

function fbStatusComments() {  //

	fbDebug("inc-fb-status-comments.php", "fbStatusComments()", "Starting");

	$statusUpdate = get_option(STATUS_UPDATER_OPTIONS);
	if (!isSet($statusUpdate) || !isSet($statusUpdate["grab-comments"])) {
		return;
	}

	if ($statusUpdate["grab-comments"] == false) {
		return;
	}

	global $wpdb;
	// all posts might be too much, let's try with last 10 days of posted items	
	$query = "SELECT post_id, meta_value FROM ".$wpdb->postmeta." WHERE meta_key = '".STATUS_UPDATER_SN_META."';";
	fbDebug("inc-fb-status-comments.php", "fbStatusComments()", "Getting all shared post ids: ".$query);

	$snReferences = $wpdb->get_results($query);
	fbDebug("inc-fb-status-comments.php", "fbStatusComments()", "Shared post ids found: ".count($snReferences));

	$addedComments = array();

	$metionResult = false;

	foreach ($snReferences as $reference) {

		$values = $reference->meta_value;
		if (!is_array($value)) {
			$values = unserialize($reference->meta_value);
		}

		/*
			fb-status-updater-sn-reference contains for each single post has an array with ids of shared items
			array (
				array("twitter", "tweet id"),
				array("facebook", "feed id"),
				array("facebook", "another feed id")
			)

			fb-status-updater-sn-tw-comments contains for each single post an array with ids in_reply_to_status_id metions
			array("related tweet id", "another related tweet id", "one more related tweet id")

			fb-status-updater-sn-fb-comments contains for each single post an array with ids of comments
			array("comment id", "another comment id")
		*/

		fbDebug("inc-fb-status-comments.php", "fbStatusComments()", "Working on post id: ".$reference->post_id);

		foreach ($values as $value) {

			fbDebug("inc-fb-status-comments.php", "fbStatusComments()", "Related ".ucwords($value[0])." reference: ".$value[1]);

			if ($value[0] == "facebook") {
				$comments = getPage("https://graph.facebook.com/".$value[1]."/comments?access_token=".$statusUpdate["fb-access-token"]);
				if ($comments != null) {

					$comments = json_decode($comments);

					$facebookAddedComments = get_post_meta($reference->post_id, STATUS_UPDATER_FB_COMMENTS_META, true);

					if ($facebookAddedComments != "" && $facebookAddedComments != null) {
						if (!is_array($facebookAddedComments)) {
							$facebookAddedComments = unserialize($facebookAddedComments);
							if (!is_array($facebookAddedComments)) {
									$facebookAddedComments = array();
							}
						}
					} else {
						$facebookAddedComments = array();
					}

					fbDebug("inc-fb-status-comments.php", "fbStatusComments()", "Already added facebook comments: ".var_export($facebookAddedComments, true));

					if (!isSet($comments->error)) {

						foreach ($comments->data as $fbComment) {

							fbDebug("inc-fb-status-comments.php", "fbStatusComments()", "Has the comment ".$fbComment->id." already been added? ".var_export(in_array($fbComment->id, $facebookAddedComments), true));
							if (!in_array($fbComment->id, $facebookAddedComments)) {

								$comment_post_ID = $reference->post_id;
								$comment_author = $fbComment->from->name;
								$comment_author_email = $statusUpdate["fb-comments-email"];
								//$comment_author_email = "";
								$comment_author_url = "http://www.facebook.com/profile.php?id=".$fbComment->from->id;
								$comment_content = trim($fbComment->message);
								$comment_type = "";
								$comment_parent = 0;
								$comment_date = date("Y-m-d H:i:s", strtotime($fbComment->created_time));

								$commentdata = compact('comment_post_ID', 'comment_author', 'comment_author_email', 'comment_author_url', 'comment_content', 'comment_type', 'comment_parent', 'comment_date');
								// what is this?
								//$commentdata = apply_filters('preprocess_comment', $commentdata);

								fbDebug("inc-fb-status-comments.php", "fbStatusComments()", "Adding facebook comment.\nPost id: ".$comment_post_ID."\nComment author:".$comment_author."\nComment author url: ".$comment_author_url."\nComment: ".$comment_content."\nComment date: ".$comment_date);

								$comment_id = wp_new_comment( $commentdata );

								if ($comment_id != null && $comment_id != "" && $comment_id !== false) {
									array_push($facebookAddedComments, $fbComment->id);
								} else {
									fbDebug("inc-fb-status-comments.php", "fbStatusComments()", "Comment not added for unknown reason");
								}
							} else {
								fbDebug("inc-fb-status-comments.php", "fbStatusComments()", "Comment ".$fbComment->id." was already added");
							}
						}
					}

					delete_post_meta($reference->post_id, STATUS_UPDATER_FB_COMMENTS_META);
					add_post_meta($reference->post_id, STATUS_UPDATER_FB_COMMENTS_META, $facebookAddedComments, true);
					fbDebug("inc-fb-status-comments.php", "fbStatusComments()", "Added facebook comments: ".var_export($facebookAddedComments, true));
				}
			}

			if ($value[0] == "twitter") {

				// https://api.twitter.com/1/related_results/show/25044314886.json
				// 'related_results/show/25044314886' // not working
				if ($metionResult == false) {

					require_once('twitter/twitteroauth/twitteroauth.php');

					$connection = new TwitterOAuth($statusUpdate["tw-consumer-key"], $statusUpdate["tw-consumer-secret"], $statusUpdate["tw-access-token"]['oauth_token'], $statusUpdate["tw-access-token"]['oauth_token_secret']);

					$params = array("count" => "200", "include_rts" => "0", "trim_user" => "0", "include_entities" => "0"); //
					$metionResult = $connection->get('statuses/mentions', $params);

					fbDebug("inc-fb-status-comments.php", "fbStatusComments()", "Getting once Twitter metions: ".count($metionResult));
				}

				if ($metionResult != null && $metionResult !== "") {

					$twitterAddedComments = get_post_meta($reference->post_id, STATUS_UPDATER_TW_COMMENTS_META, true);

					if ($twitterAddedComments != "" && $twitterAddedComments != null) {
						if (!is_array($twitterAddedComments)) {
							$twitterAddedComments = unserialize($twitterAddedComments);
							if (!is_array($twitterAddedComments)) {
								$twitterAddedComments = array();
							}
						}
					} else {
						$twitterAddedComments = array();
					}

					fbDebug("inc-fb-status-comments.php", "fbStatusComments()", "Already added twitter comments: ".var_export($twitterAddedComments, true));

					foreach ($metionResult as $item) {

						//var_dump($item);

						fbDebug("inc-fb-status-comments.php", "fbStatusComments()", "In reply to status id metion value: ".$item->in_reply_to_status_id. " - current reference value: ".$value[1]);
						
						if ($item->in_reply_to_status_id == $value[1]) {

							if (!in_array($item->id, $twitterAddedComments)) {

								$comment_post_ID = $reference->post_id;
								$comment_author = $item->user->name;
								$comment_author_url = "http://twitter.com/".$item->screen_name;
								$comment_author_email = $statusUpdate["tw-comments-email"];
								//$comment_author_email = "";
								$comment_content = trim($item->text);
								$comment_type = "";
								$comment_parent = 0;
								$comment_date = date("Y-m-d H:i:s", strtotime($item->created_at));

								$commentdata = compact('comment_post_ID', 'comment_author', 'comment_author_email', 'comment_author_url', 'comment_content', 'comment_type', 'comment_parent', 'comment_date');

								fbDebug("inc-fb-status-comments.php", "fbStatusComments()", "Adding twitter comment.\nPost id: ".$comment_post_ID."\nComment author:".$comment_author."\nComment author url: ".$comment_author_url."\nComment: ".$comment_content."\nComment date: ".$comment_date);

								$comment_id = wp_new_comment( $commentdata );

								if ($comment_id != null && $comment_id != "" && $comment_id !== false) {
									array_push($twitterAddedComments, $item->id);
								} else {
									fbDebug("inc-fb-status-comments.php", "fbStatusComments()", "Comment not added for unknown reason");
								}
							} else {
								fbDebug("inc-fb-status-comments.php", "fbStatusComments()", "Comment ".$comment->id." was already added");
							}
						}
					}

					delete_post_meta($reference->post_id, STATUS_UPDATER_TW_COMMENTS_META);
					add_post_meta($reference->post_id, STATUS_UPDATER_TW_COMMENTS_META, $twitterAddedComments, true);
					fbDebug("inc-fb-status-comments.php", "fbStatusComments()", "Added twitter comments: ".var_export($twitterAddedComments, true));
				}
			}
		}
	}

	if ($statusUpdate["fb-debug"]) {
		sendLogEmail($statusUpdate["fb-log-email"]);
	}
}
?>