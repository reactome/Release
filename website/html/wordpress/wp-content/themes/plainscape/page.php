<?php get_header(); ?>

	<div id="content">

		<?php if (have_posts()) : while (have_posts()) : the_post(); ?>
		<div class="post" id="post-<?php the_ID(); ?>">
		<h1><?php the_title(); ?></h1>
			<div class="entry">
				<?php the_content('<p class="serif">Read the rest of this page &raquo;</p>'); ?>

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
		<?php endwhile; endif; ?>
	
	</div>

<?php get_sidebar(); ?>

<?php get_footer(); ?>
