<?php
	if (function_exists('register_sidebar')) {
    	register_sidebar(array(
    		'name' => 'Sidebar Widgets',
    		'id'   => 'sidebar-widgets',
    		'description'   => 'Widgets for the sidebar.',
    		'before_widget' => '<aside id="%1$s" class="widget %2$s">',
    		'after_widget'  => '</aside>',
    		'before_title'  => '<h3>',
    		'after_title'   => '</h3>'
    	));
    }
?>
<?php
function my_filter_head() { remove_action('wp_head', '_admin_bar_bump_cb'); }
add_action('get_header', 'my_filter_head');
?>
<?php 
	function register_my_menus() {
	  register_nav_menus(
		array(
		  'header-menu' => __( 'Header Menu' ),
		  'about-menu' => __( 'About Menu' ),
		  'content-menu' => __( 'Content Menu' ),
		  'documentation-menu' => __( 'Documentation Menu' ),
		  'tools-menu' => __( 'Tools Menu' ),
		  'community-menu' => __( 'Community Menu' ),
		  'contactus-menu' => __( 'Contact Us Menu' )
		)
	  );
	}
	add_action( 'init', 'register_my_menus' );
?>
