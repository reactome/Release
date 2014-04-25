=== Status Updater ===
Contributors: devu    
Donate link: https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=8255191
Tags: facebook, status, wall, update, social networks, marketing, social network marketing, twitter    
Requires at least: 2.5.1    
Tested up to: 3.0.4    
Stable tag: 1.9.1    

If you have a Facebook profile or page or a Twitter account, this plugin is what you need to connect them with your blog! When you publish a new wordpress article, the Status Updater plugin will share on a Facebook profile and/or pages wall a new status or link and a new tweet on Twitter.

== Description ==

Instead of manually update your **Facebook status** / **Fan Page Wall** / **Twitter** with every new post you publish on your blog, this plugin does it for you.   
  
**The plugin requires Php4 or higher, Curl and Json libraries.**     
    
    
== Changelog ==   
= 1.9.1 =    
* minor bug fixes    
= 1.9 =    
* Oauth authentication on Facebook & Twitter, no more needs to save your passwords on the blog    
* Improved cron job action     
= 1.5.6 =    
* Facebook profile, Facebook pages and MySpace status not updating anymore bug solved    
= 1.5.5 =    
* facebook pages not updating in some cases    
= 1.5.4 =    
* some more path issues    
= 1.5.3 =    
* the plugin was not working on windows servers    
* in some cases the post image wasn't cought correctly, if not specified in the custom fields    
* facebook asking for date of birth to confirm account issue solved    
= 1.5.2 =    
* tr.im bug fixed    
* "facebook is asking for captcha a little too much" bug fixed    
= 1.5.1 =    
* custom image not stored along with the post bug fixed    
* "header cannot be sent..." bug fixed    
= 1.5 =    
* better user interface    
* share as link on page wall option available    
* choose your favourite url shortener service    
* many bugs solved   
= 1.4 =    
* added Myspace status support    
* minor bugs fixed    
= 1.3.2 =    
* removed the plugin activation function that screwed up on so much environments   
* minor bugs fixed    
* decide from the advanced section if a specific article should be sent to both social networks or just one, the one you want    
* "share as facebook link" request not yet implemented, I know you're waiting for it    
* "Facebook not updating" issue with debug    
= 1.3 =    
* url shortener service on j.mp    
* different status for facebook/twitter with advanced options    
* added facebook group wall support    
* strict control on wall id fields. More difficult for users now not understanding that a wall id is not a url    
* default status template: decide where you want the url, the title and any other word/char    
= 1.2.4 =     
* one more plugin activation bug solved (on some environments it didn't create correctly the list of previous post ids)    
* cron job interval added as field    
* added the possibility to customize the text/link sent to FB/TW    
* added the possibility to send the same post more than once    
* added the possibility to skip one post   
* clear private data function    
= 1.2.3 =    
* plugin activation bug solved      
= 1.2.2 =    
* better plugin descriptions  
* safe mode issue solved
* the twitter part is now usable even without the facebook one
= 1.2.1 =    
* some file were not added in the svn repository for version 1.2   
= 1.2 =    
* Twitter update included.    
* cron job for posting scheduled posts.    
* minor fixes.    
= 1.1 =    
* log email format issue solved.    
* added one more wall to post to.    
= 1.0 =    
* First public release.    
   
== Installation ==

1.   Unzip and upload 'fb-status-updater' directory to your '/wp-content/plugins/' directory   
2.   Activate the plugin through the 'Plugins' menu in the Admin panel  
3.   Go to the plugin setting page to enable and tune it        
4.   Ensure the **fbSessionData.txt** file, in the **/wp-content/plugins/fb-status-updater/** directory is writable from PHP  

== Screenshots ==    
1.   The "See what Facebook says" link result, the first thing to check when the plugin is not posting on Facebook    
2.   The plugin advanced options on the post page    

== Arbitrary section ==
More information & support: http://www.francesco-castaldo.com/plugins-and-widgets/fb-status-updater/