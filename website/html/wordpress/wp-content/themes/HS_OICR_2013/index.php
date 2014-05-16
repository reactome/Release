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
	<div id="post">
		<?php if ( have_posts() ) : ?>
        	<?php while ( have_posts() ) : the_post(); ?>
            	<article <?php post_class() ?> id="post-<?php the_ID(); ?>">
                	<h2><a href="<?php the_permalink() ?>"><?php the_title(); ?></a></h2>
                    <div class="meta">
                        Posted on <?php the_time('F jS, Y') ?>, by <?php the_author() ?>, under <?php the_category(', ') ?>
                    </div>

					<div class="entry">
                    	<?php the_content(); ?>
                    </div><!--end entry-->
                    <div class="postmetadata">
                        <?php comments_popup_link('No Comments &#187;', '1 Comment &#187;', '% Comments &#187;'); ?>
                    </div>
                </article>
			<?php endwhile; ?>
        	<?php else : ?>
            	<h2>Not found</h2>
        <?php endif; ?>
       
    </div><!--end post-->
</div><!--end postWrap-->

    </div><!--close grid15-->
    <div class="grid_7"> 
        <?php get_sidebar(); ?>
    </div><!--close grid9-->
     
    <div class="clear"></div><!--clear grid15 and 9-->
    
</div><!--close wrapper-->
<!--close content-->

<?php get_footer(); ?>