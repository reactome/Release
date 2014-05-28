<?php get_header(); ?>

	<div id="content">

	<?php if (have_posts()) : while (have_posts()) : the_post(); ?>


		<div class="post" id="post-<?php the_ID(); ?>">
			<h2><?php the_title(); ?></h2>
			
			<div class="postmetadata">Posted on <?php the_time(get_option('date_format')) ?>, <?php the_time(get_option('time_format')) ?>, by <?php the_author() ?>, under <?php the_category(', ') ?>.</div>

			<div class="entry">
				<?php the_content('<p class="serif">Read the rest of this entry &raquo;</p>'); ?>

				<?php wp_link_pages(array('before' => '<p><strong>Pages:</strong> ', 'after' => '</p>', 'next_or_number' => 'number')); ?>
			</div>

			<div class="postmetadata">
					<?php if( function_exists('the_tags') ) 
						the_tags(__('Tags: '), ', ', '<br />'); 
					?>
					
					<?php if('open' == $post->comment_status) { ?><a href="#respond"><?php _e('Comment') ?></a> (<?php comments_rss_link('RSS'); ?>)<?php } ?>
					<?php if('open' == $post->ping_status) { ?>&nbsp;|&nbsp;&nbsp;<a href="<?php trackback_url(true); ?> " rel="trackback"><?php _e('Trackback') ?></a><?php } ?>
					<?php edit_post_link(__('Edit'), '&nbsp;|&nbsp;&nbsp;', ''); ?>
			 </div>
		</div>

	<?php comments_template(); ?>

	<?php endwhile; ?>
		<div class="navigation">
			<div style="text-align:left;"><?php previous_post_link('&laquo; %link') ?></div>
			<div style="text-align:right;"><?php next_post_link('%link &raquo;') ?></div>
		</div>
		
	

	<?php else: ?>

		<p>Sorry, no posts matched your criteria.</p>

<?php endif; ?>

	</div>
	
<?php get_sidebar(); ?>

<?php get_footer(); ?>
