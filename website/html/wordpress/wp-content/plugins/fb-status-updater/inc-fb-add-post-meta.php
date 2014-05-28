<?php

function fbStatusAddMetaFields() {

		global $fbStatusUpdaterVersion;

		$statusUpdate = get_option(STATUS_UPDATER_OPTIONS);

		if (!isSet($statusUpdate["version"]) || (isSet($statusUpdate["version"]) && $statusUpdate["version"] != $fbStatusUpdaterVersion) ) {
			// the plugin has not been activated yet
			$statusUpdate = fbStatusAct();
		}

		//do not add anything if the user didn't choose the advanced option
		if (!isSet($statusUpdate["fb-advanced-status"]) || (isSet($statusUpdate["fb-advanced-status"]) && $statusUpdate["fb-advanced-status"] !== true)) {
			return;
		}

	    global $post;

	    $post_id = $post;
	    if (is_object($post_id)) {
	    	$post_id = $post_id->ID;
	    }

		$statusUpdateMeta = get_post_meta($post_id, STATUS_UPDATER_POST_META, true);
		if ($statusUpdateMeta == "") {
			
			$statusUpdateMeta["fb-push"] = true;
			$statusUpdateMeta["custom-facebook-status"] = null;
			
			$statusUpdateMeta["tw-push"] = true;
			$statusUpdateMeta["custom-twitter-status"] = null;
			
			$statusUpdateMeta["push"] = true;
		} else {
			// bah
			if ($statusUpdateMeta["fb-push"] == 1 || $statusUpdateMeta["fb-push"] == "1") {
				$statusUpdateMeta["fb-push"] = true;
			} else {
				$statusUpdateMeta["fb-push"] = false;
			}
			
			if ($statusUpdateMeta["tw-push"] == 1 || $statusUpdateMeta["tw-push"] == "1") {
				$statusUpdateMeta["tw-push"] = true;
			} else {
				$statusUpdateMeta["tw-push"] = false;
			}
			
			if ($statusUpdateMeta["push"] == 1 || $statusUpdateMeta["push"] == "1") {
				$statusUpdateMeta["push"] = true;
			} else {
				$statusUpdateMeta["push"] = false;
			}
			
			if ($statusUpdateMeta["custom-facebook-status"] == "") {
				$statusUpdateMeta["custom-facebook-status"] = null;
			}
			
			if ($statusUpdateMeta["custom-twitter-status"] == "") {
				$statusUpdateMeta["custom-twitter-status"] = null;
			}
		}
		if (strpos($statusUpdate["fb-post-ids"], "#".$post_id."#") !== false) {
			$statusUpdateMeta["push"] = false;
		}

	    ?>
			<script type="text/javascript">
				function customFbStatus() {
					var check = document.getElementById("su-push-to-facebook").checked;
					var isVisible = jQuery('#customFbStatus').is(':visible');
					if (check) {
						if (!isVisible) {
							jQuery("#customFbStatus").show("slow");
						}
					}

					if (!check) {
						jQuery("#customFbStatus").hide("slow");
					}

				}

				function twCheck() {
					var check = document.getElementById("su-push-to-twitter").checked;
					var isVisible = jQuery('#customTwStatus').is(':visible');
					if (check) {
						if (!isVisible) {
							jQuery("#customTwStatus").show("slow");
						}
					} else {
						jQuery("#customTwStatus").hide("slow");
					}
				}

				function suCheck() {
					var check = document.getElementById("su-push-yes").checked;
					var isVisible = jQuery('#suWrapper').is(':visible');
					if (check) {
						if (!isVisible) {
							jQuery("#suWrapper").show("slow");
						}
					} else {
						jQuery("#suWrapper").hide("slow");
					}
				}
			</script>

			<input id="fb-status-updater-edit" name="fb-status-updater-edit" type="hidden" value="true" />

			<div class="form-table" style="margin-bottom:20px;">
				<div id="suWrapper"<?php if (!$statusUpdateMeta["push"]) { echo(" style=\"display:none\""); } ?>>
					<?php if (isSet($statusUpdate["fb-access-token"]) && $statusUpdate["fb-access-token"] != null) { ?>
						<p>
							<label for="su-push-to-facebook">
								<input type="checkbox" id="su-push-to-facebook" name="su-push-to-facebook" value="true" <?php if ($statusUpdateMeta["fb-push"]) { echo("checked=\"checked\" "); } ?>onclick="customFbStatus()" /> Share on Facebook
							</label>
						</p>

						<p id="customFbStatus"<?php if (!$statusUpdateMeta["fb-push"]) { echo(" style=\"display:none\"");} ?>>
							<label for="su-custom-facebook-status"><strong>Custom facebook status</strong>:</label> <a href="javascript:void(null)" onclick="jQuery('#infoFb').toggle('slow');">?</a><br />
							<input style="width: 100%;" id="su-custom-facebook-status" name="su-custom-facebook-status" type="text" value="<?php echo(str_replace("\"", "&quot;", $statusUpdateMeta["custom-facebook-status"])); ?>" onkeyup="fbStatusCheckOption('fb-status-up-custom-status', 'fb-status', 'Facebook');" /><br />
							<em id="infoFb" style="display:none">By leaving this field empty, the default status (the post title) will be used if the status is pushed to the profile or a page</em>
						</p>

					<?php } ?>
					<?php if (isSet($statusUpdate["tw-access-token"]) && is_array($statusUpdate["tw-access-token"])) { ?>

						<p><label for="su-push-to-twitter"><input type="checkbox" id="su-push-to-twitter" name="su-push-to-twitter" value="true" <?php if ($statusUpdateMeta["tw-push"]) { echo("checked=\"checked\" "); } ?>onclick="twCheck()" /> Share on Twitter</label></p>

						<p id="customTwStatus"<?php if (!$statusUpdateMeta["tw-push"]) { echo(" style=\"display:none\""); }?>>
							<label for="su-custom-tw-status"><strong>Custom twitter status</strong>:</label> <a href="javascript:void(null)" onclick="jQuery('#infoTw').toggle('slow');">?</a><br />
							<input style="width: 100%;" id="su-custom-tw-status" name="su-custom-tw-status" type="text" value="<?php echo(str_replace("\"", "&quot;", $statusUpdateMeta["custom-twitter-status"])); ?>" /><br />
							<em id="infoTw" style="display:none">By leaving this field empty, the default status will be used.<br />Please note that Twitter allows 140 chars updates, your status might be truncated if longer.</em>
						</p>
					<?php } ?>
				</div>
				<!-- add the number of characters used -->
				<?php if (strpos($statusUpdate["fb-post-ids"], "#".$post_id."#") === false) { ?>
					<p>
						<strong>Push this post to Social Networks?:</strong><br />
						<label for="su-push-yes"><input id="su-push-yes" name="su-push" type="radio" value="true" <?php if ($statusUpdateMeta["push"]) { echo("checked=\"checked\""); } ?> onclick="suCheck()" /> Yes</label>
						<label for="su-push-no"><input id="su-push-no" name="su-push" type="radio" value="false" <?php if (!$statusUpdateMeta["push"]) { echo("checked=\"checked\""); } ?> onclick="suCheck()" /> No</label>
						<br />If none of the checkbox above is selected, this post won't be sent
					</p>
				<?php } else { ?>
					<p>
						<strong>This post was already shared on Social Networks. Want to push it again?</strong><br />
						<label for="su-push-again-yes"><input id="su-push-again-yes" name="su-push-again" type="radio" value="true" onclick="jQuery('#suWrapper').show('slow');" /> Yes</label>
						<label for="su-push-again-no"><input id="su-push-again-no" name="su-push-again" type="radio" value="false" onclick="jQuery('#suWrapper').hide('slow');" checked="checked" /> No</label>
						<br />If none of the checkbox above is selected, this post won't be sent
					</p>
				<?php } ?>
			</div>
	    <?php
}

function fbStatusProcessMetaFields($id) {

	fbDebug("inc-fb-status-updater.php", "fbStatusProcessMetaFields(".'$id = '.$id.")", "Starting");
	
	if (function_exists("wp_is_post_revision")) {
		$post = get_post($id);
		$test = wp_is_post_revision($post);
		
		if ($test !== false) {
			fbDebug("inc-fb-status-updater.php", "fbStatusProcessMetaFields(...)", "The post is a revision, parent id: ".$test);
			//return;
			$id = $test;			
		}
	}
	
	global $fbStatusPreventDouble;
	
	$statusUpdate = get_option(STATUS_UPDATER_OPTIONS);
	
	if ($fbStatusPreventDouble !== null) {
		fbDebug("inc-fb-status-updater.php", "fbStatusProcessMetaFields(...)", "The fbStatusProcessMetaFields() has already run for this post");
		if ($statusUpdate["fb-debug"]) {
			sendLogEmail($statusUpdate["fb-log-email"]);
		}
		return;
	} else {
		$fbStatusPreventDouble = $id;
	}
	
	if (isSet($_POST["fb-status-updater-edit"]) && trim($_POST["fb-status-updater-edit"]) == "true") {

		$statusUpdateMeta["custom-facebook-status"] = null;
		$statusUpdateMeta["custom-twitter-status"] = null;
		$statusUpdateMeta["fb-push"] = false;
		$statusUpdateMeta["tw-push"] = false;
		$statusUpdateMeta["push"] = true;

		if (isSet($_POST["su-push-to-facebook"]) && trim($_POST["su-push-to-facebook"]) == "true") {
			$statusUpdateMeta["fb-push"] = true;
			fbDebug("inc-fb-status-updater.php", "fbStatusProcessMetaFields(...)", "Push to facebook");
		} else {
			fbDebug("inc-fb-status-updater.php", "fbStatusProcessMetaFields(...)", "Don't push to facebook");
		}
		
		if (isSet($_POST["su-custom-facebook-status"]) && trim($_POST["su-custom-facebook-status"]) != "") {
			$statusUpdateMeta["custom-facebook-status"] = trim(strip_tags(stripslashes($_POST["su-custom-facebook-status"])));
			fbDebug("inc-fb-status-updater.php", "fbStatusProcessMetaFields(...)", "Facebook custom status: ".$statusUpdateMeta["custom-facebook-status"]);
		}


		if (isSet($_POST["su-push-to-twitter"]) && trim($_POST["su-push-to-twitter"]) == "true") {
			$statusUpdateMeta["tw-push"] = true;
			fbDebug("inc-fb-status-updater.php", "fbStatusProcessMetaFields(...)", "Push to Twitter");
		} else {
			fbDebug("inc-fb-status-updater.php", "fbStatusProcessMetaFields(...)", "Don't push to Twitter");
		}

		if (isSet($_POST["su-custom-tw-status"]) && trim($_POST["su-custom-tw-status"]) != "") {
			$statusUpdateMeta["custom-twitter-status"] = trim(strip_tags(stripslashes($_POST["su-custom-tw-status"])));
			fbDebug("inc-fb-status-updater.php", "fbStatusProcessMetaFields(...)", "Twitter custom status: ".$statusUpdateMeta["custom-twitter-status"]);
		}

		if (isSet($_POST["su-push"]) && trim($_POST["su-push"]) == "true") {
			$statusUpdateMeta["push"] = true;
			fbDebug("inc-fb-status-updater.php", "fbStatusProcessMetaFields(...)", "Push");
		}

		if (isSet($_POST["su-push"]) && trim($_POST["su-push"]) == "false") {
			$statusUpdateMeta["push"] = false;
			fbDebug("inc-fb-status-updater.php", "fbStatusProcessMetaFields(...)", "Don't push");
		}
		
		// if the user want to push the post to sn again, just delete its id in the id list so that the next time the "send" function is called, this post is included
		if (isSet($_POST["su-push-again"]) && trim($_POST["su-push-again"]) == "true") {
			fbDebug("inc-fb-status-updater.php", "fbStatusProcessMetaFields(".'$id = '.$id.")", "Push again");

			fbDebug("inc-fb-status-updater.php", "fbStatusProcessMetaFields(".'$id = '.$id.")", "Ids before: ".$statusUpdate["fb-post-ids"]);
			if (isSet($statusUpdate["fb-post-ids"])) {
				$statusUpdate["fb-post-ids"] = str_replace("#".$id."#", "#", $statusUpdate["fb-post-ids"]);
				update_option(STATUS_UPDATER_OPTIONS, $statusUpdate);
			}
			fbDebug("inc-fb-status-updater.php", "fbStatusProcessMetaFields(".'$id = '.$id.")", "Ids after: ".$statusUpdate["fb-post-ids"]);
		}
		
		fbDebug("inc-fb-status-updater.php", "fbStatusProcessMetaFields(".'$id = '.$id.")", "Ids final: ".$statusUpdate["fb-post-ids"]);

		delete_post_meta($id, STATUS_UPDATER_POST_META);
		add_post_meta($id, STATUS_UPDATER_POST_META, $statusUpdateMeta, true);
		
		if ($statusUpdate["fb-debug"]) {
			sendLogEmail($statusUpdate["fb-log-email"]);
		}
	}
}

?>