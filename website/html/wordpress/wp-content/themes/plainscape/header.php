<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" <?php language_attributes(); ?>>

<head profile="http://gmpg.org/xfn/11">
<meta http-equiv="Content-Type" content="<?php bloginfo('html_type'); ?>; charset=<?php bloginfo('charset'); ?>" />

<title><?php bloginfo('name'); ?> <?php if ( is_single() ) { ?> &raquo; Blog Archive <?php } ?> <?php wp_title(); ?></title>

<link rel="stylesheet" href="<?php bloginfo('stylesheet_url'); ?>" type="text/css" media="screen" />
<link rel="alternate" type="application/rss+xml" title="<?php bloginfo('name'); ?> RSS Feed" href="<?php bloginfo('rss2_url'); ?>" />
<link rel="pingback" href="<?php bloginfo('pingback_url'); ?>" />

<!-- Reactome-specific stylesheets -->
<!-- <link rel="stylesheet" type="text/css" href="/stylesheets/reactome_navbar_stylesheet.css" /> -->
<link rel="stylesheet" type="text/css" href="/stylesheet.css" />
<link rel="stylesheet" type="text/css" href="/stylesheets/reactome_wordpress_tweaks.css" />

<?php if ( is_singular() ) wp_enqueue_script( 'comment-reply' ); ?>
<?php wp_head(); ?>
</head>
<body>
<div id="page">


	<div id="header">

<!-- Regular Reactome banner and menu -->
<div id="wp_navbar">
<?php
virtual("/cgi-bin/navigation_bar");
?>
</div>

<!-- Marc's banner -->
<!--
    <a href="<?php bloginfo('url'); ?>">
  <img src="http://brie8.cshl.edu/news/wp-content/themes/plainscape/bannerFinal_100.png" alt="<?php bloginfo('name'); ?>" />
-->
	</div>
<div id="hmenu">
<!-- Marc's menu -->
<!--
<ul>
	<li><a href="http://www.reactome.org">Reactome Home</a></li>
	<li><a href="<?php echo get_settings('home'); ?>"><?php _e('News') ?></a></li>
	<?php wp_list_pages('title_li=&depth=1') ?>
	<li id="hmenu_rss">	<a href="<?php bloginfo('rss2_url'); ?>"  title="<?php bloginfo('name'); ?> RSS Feed">Subscribe to Feed</a></li>


</ul>
-->
</div>
<hr />

<div id="wrapper">
