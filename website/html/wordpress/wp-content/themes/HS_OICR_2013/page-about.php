<?php get_header(); ?>
	<div class="grid_15">
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


    </div><!--close grid15-->
    <div class="grid_9"> 
    </div><!--close grid9-->
     
    <div class="clear"></div><!--clear grid15 and 9-->
    
</div><!--close wrapper-->
<!--close content-->

<?php get_footer(); ?>