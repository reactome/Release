<?php
/*
Plugin Name: Status Updater
Plugin URI: http://www.francesco-castaldo.com/plugins-and-widgets/fb-status-updater/
Description: Shares wordpress posts on your Facebook profile and/or pages and on Twitter  | <a href="https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=8255191" target="_blank">Donate</a>
Author: Francesco Castaldo
Version: 1.9.2
Author URI: http://www.francesco-castaldo.com/
*/

/*
Copyright 2009  Francesco Castaldo  (email : fcastaldo@gmail.com)

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/

// ingnore user abort and finish whatever operation the script was performing
// so that if the cron is triggerend in the footer, even if the user changes page the post is published
ignore_user_abort(true);

// what is this?
//ini_set('memory_limit', '4000M');

$fbStatusUpdaterVersion = "1.9.2";
$fbStatusPreventDouble = null; // prevents double execution of the save_post hook

$fbStatusUpdatePath = __FILE__;
$fbStatusUpdatePath = substr($fbStatusUpdatePath, 0,  strrpos($fbStatusUpdatePath, DIRECTORY_SEPARATOR));
$fbStatusCookieFile = $fbStatusUpdatePath.DIRECTORY_SEPARATOR."fbSessionData.txt";

define("STATUS_UPDATER_OPTIONS", "fb-status-update");
define("STATUS_UPDATER_CRON", "status_updater_hourly_event");
define("STATUS_UPDATER_POST_META", "fb-status-updater-meta");
define("STATUS_UPDATER_SN_META", "fb-status-updater-sn-reference");
define("STATUS_UPDATER_FB_COMMENTS_META", "fb-status-updater-sn-fb-comments");
define("STATUS_UPDATER_TW_COMMENTS_META", "fb-status-updater-sn-tw-comments");

include_once('inc-fb-status-functions.php');

include_once('inc-fb-status-cron.php');

include_once('inc-fb-status-updater.php');

include_once('inc-fb-status-option.php');

include_once('inc-fb-status-activation.php');

include_once('inc-fb-add-post-meta.php');

include_once('inc-fb-status-comments.php');

function addFbStatusUpdaterOptionPage() {
	add_options_page('Status Updater', 'Status Updater', 9, basename(__FILE__), "fbStatusOptionPage");
}

add_action('admin_menu', 'addFbStatusUpdaterOptionPage');
add_action('admin_menu', 'addFbStatusEditBox');

// grab comments
add_action('wp_footer','fbStatusComments', 0);

// cron that shares scheduled posts
add_action(STATUS_UPDATER_CRON, 'fbStatusCron');

// add the advanced box to the post form
add_action('save_post', 'fbStatusProcessMetaFields', 0);

// add the "send to social networks" action
add_action("publish_post", "fbStatusUpdater", 15);

//adds the advanced section to the edit post
function addFbStatusEditBox() {

	global $wp_version;
	
	if (function_exists("get_post_types")) {
		$post_types = get_post_types(array(), 'objects');
		foreach ($post_types as $post_type) {
			if ( $post_type->show_ui ) {
				add_meta_box( 'fb_status_updater', 'Status Updater', "fbStatusAddMetaFields", $post_type->name, 'side', 'high');
			}
		}
	} elseif (function_exists("add_meta_box")) {
		$tmpStatusUpdate = get_option(STATUS_UPDATER_OPTIONS);
		if (isSet($tmpStatusUpdate["fb-advanced-status"]) && $tmpStatusUpdate["fb-advanced-status"] === true) {
			if (version_compare($wp_version, '2.7.0', '>')) {
				add_meta_box("fb_status_updater", "Status Updater", "fbStatusAddMetaFields", 'post', 'side', 'high');
			} else {
				add_meta_box("fb_status_updater", "Status Updater", "fbStatusAddMetaFields", 'post', 'normal', 'high');
			}
		}
		unset($tmpStatusUpdate);
	}
}
?>