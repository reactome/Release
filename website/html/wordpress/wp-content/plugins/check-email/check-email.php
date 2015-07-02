<?php
/*
Plugin Name: Check Email
Plugin URI: http://www.stillbreathing.co.uk/wordpress/check-email/
Description: Check email allows you to test if your WordPress installation is sending emails correctly.
Version: 0.3
Author: Chris Taylor
Author URI: http://www.stillbreathing.co.uk
*/

// Plugin Register from http://wordpress.org/extend/plugins/plugin-register/
require_once( "plugin-register.class.php" );
$register = new Plugin_Register();
$register->file = __FILE__;
$register->slug = "checkemail";
$register->name = "Check Email";
$register->version = "0.2";
$register->developer = "Chris Taylor";
$register->homepage = "http://www.stillbreathing.co.uk";
$register->Plugin_Register();

// add the admin menu option
add_action( 'admin_menu', 'checkemail_add_admin' );
function checkemail_add_admin() {
	add_submenu_page( 'tools.php', __("Check Email", "checkemail"), __("Check Email", "checkemail"), 'edit_users', 'checkemail', 'checkemail' );
}

// add the JavaScript
add_action( 'admin_head', 'checkemail_add_js' );
function checkemail_add_js() {
	if ( isset( $_GET["page"] ) && $_GET["page"] == "checkemail" ) {
		echo '
		<script type="text/javascript">
		jQuery(document).ready(function(){
			jQuery(".checkemail-hide").hide();
			jQuery("#checkemail_autoheaders,#checkemail_customheaders").bind("change", function(){
				if (jQuery("#checkemail_autoheaders").is(":checked")){
					jQuery("#customheaders").hide();
					jQuery("#autoheaders").show();
				}
				if (jQuery("#checkemail_customheaders").is(":checked")){
					jQuery("#autoheaders").hide();
					jQuery("#customheaders").show();
				}
			});
		});
		</script>
		';
	}
}
// add the CSS
add_action( 'admin_head', 'checkemail_add_css' );
function checkemail_add_css() {
	if ( isset( $_GET["page"] ) && $_GET["page"] == "checkemail" ) {
		echo '
		<style type="text/css">
		#checkemail label {
			width: 16em;
			float: left;
		}
		#checkemail .text {
			width: 30em;
		}
		#checkemail p, #checkemail pre {
			clear: left;
		}
		</style>
		';
	}
}

// load the check email admin page
function checkemail() {
	global $current_user;

	echo '
	<div id="checkemail" class="wrap">
	';
	
	if (isset( $_POST["checkemail_to"]) && $_POST["checkemail_to"] != "" )
	{
		$headers = checkemail_send( $_POST["checkemail_to"], $_POST["checkemail_headers"] );
		echo '<div class="updated"><p>' . __( 'The test email has been sent by WordPress. Please note this does NOT mean it has been delivered. See <a href="http://codex.wordpress.org/Function_Reference/wp_mail">wp_mail in the Codex</a> for more information. The headers sent were:', "checkemail" ) . '</p><pre>' . str_replace( chr( 10 ), '\n' . "\n", str_replace( chr( 13 ), '\r', $headers ) ) . '</pre></div>';
	}
		
	echo '
	<h2>' . __( "Check Email" ) . '</h2>
	
	<form action="tools.php?page=checkemail" method="post">
	<p>SMTP server: ' . ini_get("SMTP") . '</p>
	<p><label for="checkemail_to">' . __( "Send test email to:", "checkemail" ) . '</label>
	<input type="text" name="checkemail_to" id="checkemail_to" class="text"';
		if ( isset( $_POST["checkemail_to"] ) ) {
			echo ' value="' . $_POST["checkemail_to"] . '"';
		}
		echo ' /></p>
	<p><label for="checkemail_autoheaders">' . __( "Use standard headers", "checkemail" ) . '</label>
	<input type="radio" id="checkemail_autoheaders" name="checkemail_headers" value="auto"';
	if ( !isset($_POST["checkemail_headers"]) || $_POST["checkemail_headers"] == "auto" ){
		echo ' checked="checked"';
	}
	echo '	/></p>
	<pre id="autoheaders"';
	if ( isset($_POST["checkemail_headers"]) && $_POST["checkemail_headers"] == "custom" ){
		echo ' class="checkemail-hide"';
	}
	echo '>MIME-Version: 1.0
From: ' . $current_user->user_email . '
Content-Type: text/plain; charset="' . get_option( 'blog_charset' ) . '"</pre>
	<p><label for="checkemail_customheaders">' . __( "Use custom headers", "checkemail" ) . '</label>
	<input type="radio" id="checkemail_customheaders" name="checkemail_headers" value="custom"';
	if ( isset($_POST["checkemail_headers"]) && $_POST["checkemail_headers"] == "custom" ){
		echo ' checked="checked"';
	}
	echo '	/></p>
	<div id="customheaders"';
	if ( !isset($_POST["checkemail_headers"]) || $_POST["checkemail_headers"] == "auto" ){
		echo ' class="checkemail-hide"';
	}
	echo '>
		<p>' . __( "Set your custom headers below", "checkemail" ) . '</p>
		<p><label for="checkemail_mime">' . __( "MIME Version", "checkemail" ) . '</label>
		<input type="text" name="checkemail_mime" id="checkemail_mime" value="';
		if ( isset( $_POST["checkemail_mime"] ) ) {
			echo $_POST["checkemail_mime"];
		} else {
			echo '1.0';
		}
		echo '" /></p>
		<p><label for="checkemail_type">' . __( "Content type", "checkemail" ) . '</label>
		<input type="text" name="checkemail_type" id="checkemail_type" value="';
		if ( isset( $_POST["checkemail_type"] ) ) {
			echo $_POST["checkemail_type"];
		} else {
			echo 'text/html; charset=iso-8859-1';
		}
		echo '" class="text"  /></p>
		<p><label for="checkemail_from">' . __( "From", "checkemail" ) . '</label>
		<input type="text" name="checkemail_from" id="checkemail_from" value="';
		if ( isset( $_POST["checkemail_from"] ) ) {
			echo $_POST["checkemail_from"];
		} else {
			echo $current_user->user_email;
		}
		echo '" class="text"  /></p>
		<p><label for="checkemail_cc">' . __( "CC", "checkemail" ) . '</label>
		<textarea name="checkemail_cc" id="checkemail_cc" cols="30" rows="4" class="text">';
		if ( isset( $_POST["checkemail_cc"] ) ) {
			echo $_POST["checkemail_cc"];
		}
		echo '</textarea></p>
		<p><label for="checkemail_break_n">' . __( "Header line break type", "checkemail" ) . '</label>
		<input type="radio" name="checkemail_break" id="checkemail_break_n" value="\n"';
		if ( !isset( $_POST["checkemail_break"] ) || $_POST["checkemail_break"] == '\n' ) {
			echo ' checked="checked"';
		}
		echo ' /> \n
		<input type="radio" name="checkemail_break" id="checkemail_break_rn" value="\r\n"';
		if ( isset( $_POST["checkemail_break"] ) && $_POST["checkemail_break"] == '\r\n' ) {
			echo ' checked="checked"';
		}
		echo ' /> \r\n</p>
	</div>
	<p><label for="checkemail_go" class="checkemail-hide">' . __( "Send", "checkemail" ) . '</label>
	<input type="submit" name="checkemail_go" id="checkemail_go" class="button-primary" value="' . __( "Send test email", "checkemail" ) . '" /></p>
	</form>
	
	</div>
	';
		
}

// send a test email
function checkemail_send($to, $headers = "auto") {
	global $current_user;
	if ( $headers == "auto" ) {
		$headers = "MIME-Version: 1.0\r\n" .
		"From: " . $current_user->user_email . "\r\n" .
		"Content-Type: text/plain; charset=\"" . get_option('blog_charset') . "\"\r\n";
	} else {
		$break = chr( 10 );
		if ( stripslashes( $_POST["checkemail_break"] ) == '\r\n' ) {
			$break = chr( 13 ) . chr( 10 );
		}
		$headers = "MIME-Version: " . trim( $_POST["checkemail_mime"] ) . $break .
		"From: " . trim( $_POST["checkemail_from"] ) . $break .
		"Cc: " . trim( $_POST["checkemail_cc"] ) . $break .
		"Content-Type: " . trim( $_POST["checkemail_type"] ) . $break;
	}
	wp_mail( $to, "Test email from ".get_bloginfo("url"), "This test email proves that your WordPress installation at ".get_bloginfo( "url" )." can send emails.\n\nSent: " . date( "r" ), $headers );
	return $headers;
}
?>