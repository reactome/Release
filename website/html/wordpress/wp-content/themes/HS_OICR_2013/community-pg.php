<?php
/*
 * Template Name: Community Pages Template
 * Description: Template for pages in Community Section
 */
?>
<?php get_header(); ?>
	<div class="grid_17">
     <div class="breadcrumb"><!-- Use breadcrumbs to tell users where they are -->
     <div class="breadcrumbs">
		<?php if(function_exists('bcn_display'))
        {
            bcn_display();
        }?>
    </div>
     </div><!--close breadcrumb-->

		<div id="postWrap">
			<div id="page-post">

				<?php while ( have_posts() ) : the_post(); ?>

					<?php get_template_part( 'content', 'page' ); ?>

				<?php endwhile; // end of the loop. ?>

			</div><!-- #content -->
		</div><!-- #primary -->


    </div><!--close grid17-->
    <div class="grid_7"> <div class="sidebar">
        <?php wp_nav_menu( array( 'theme_location' => 'community-menu' ) ); ?>
    </div><!--close sidebar-->
    </div><!--close grid7-->
     
    <div class="clear"></div><!--clear grid15 and 9-->
    
</div><!--close wrapper-->
<!--close content-->

<?php get_footer(); ?>