<?php
/*
Plugin Name: iWPhone
Plugin URI: http://iwphone.contentrobot.com
Description: A plugin and theme for WordPress that automatically reformats your blog's content for optimized viewing on Apple's <a href="http://www.apple.com/iphone/">iPhone</a> and <a href="http://www.apple.com/ipodtouch/">iPod touch</a>. Also works for <a href="http://www.android.com/devices/?f=phone">Android phones</a> too.
Author: ContentRobot
Version: 0.1.4
Author URI: http://www.contentrobot.com

# Special thanks to Imthiaz Rafiq and the wp-pda Plugin (http://imthi.com/wp-pda/) which this plugin is derived from.

# This plugin is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 2.1 of the License, or (at your option) any later version.
#
# This plugin is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with this plugin; if not, write to the Free Software
# Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

*/

class iWPhonePlugin{
	var $iphone;
	
	function iWPhonePlugin(){
		$this->iphone = false;
		add_action('plugins_loaded',array(&$this,'detectiPhone'));
		add_filter('stylesheet',array(&$this,'get_stylesheet'));
		add_filter('template',array(&$this,'get_template'));
	}

	function detectiPhone($query){
		$container = $_SERVER['HTTP_USER_AGENT'];
		//print_r($container); //this prints out the user agent array. uncomment to see it shown on page.
		$useragents = array (
			"iPhone","iPod","Mobile Safari");
		$this->iphone = false;
		foreach ( $useragents as $useragent ) {
			//if (eregi($useragent,$container)){
			if (preg_match('/'.$useragent.'/i',$container)){
				$this->iphone = true;
			}
		}
		/*if($this->iphone){
			echo ("You are on an iPhone or iPod touch - Lucky you!<br>");
		}else{
			echo ("You are not on an iPhone or iPod touch - Too bad :(<br>");
		}*/
	}
	
	function get_stylesheet($stylesheet) {
		if($this->iphone){
			return 'iwphone-by-contentrobot';
		}else{
			return $stylesheet;
		}
	}
	
	function get_template($template) {
		if($this->iphone){
			return 'iwphone-by-contentrobot';
		}else{
			return $template;
		}
	}
}
$wp_iwphone = new iWPhonePlugin();
?>