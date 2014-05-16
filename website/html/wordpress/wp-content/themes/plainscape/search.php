<?php get_header(); ?>

	<div id="content">

	<?php if (have_posts()) : ?>

		<h2>Search Results for &#8216;<?php the_search_query(); ?>&#8217;</h2>


		<div class="navigation">
			<div class="alignleft"><?php next_posts_link('&laquo; Older Entries') ?></div>
			<div class="alignright"><?php previous_posts_link('Newer Entries &raquo;') ?></div>
		</div>
		

		<?php while (have_posts()) : the_post(); ?>

			<div class="post" id="post-<?php the_ID(); ?>">
				<h3 class="post-title"><a href="<?php the_permalink() ?>" rel="bookmark" title="Permanent Link to <?php the_title_attribute(); ?>"><?php the_title(); ?></a></h3>
				<div class="postmetadata">Posted on <?php the_time(get_option('date_format')) ?>, <?php the_time(get_option('time_format')) ?>, by <?php the_author() ?>, under <?php the_category(', ') ?>.</div>


				<div class="entry">
					<?php the_excerpt(); ?>
				</div>

				<div class="postmetadata">
				<?php if( function_exists('the_tags') ) 
						the_tags(__('Tags: '), ', ', '<br />'); 
				?>
				<?php edit_post_link('Edit', '', ' | '); ?>  <?php comments_popup_link('No Comments', '1 Comment', '% Comments'); ?> | <a href="<?php the_permalink() ?>" title="Permanent Link to <?php the_title_attribute(); ?>"><strong>Read the rest of this entry &raquo;</strong></a></div>
			</div>

		<?php endwhile; ?>

		<div class="navigation">
			<div class="alignleft"><?php next_posts_link('&laquo; Older Entries') ?></div>
			<div class="alignright"><?php previous_posts_link('Newer Entries &raquo;') ?></div>
		</div>
		
	<?php else : ?>

		<h2>No posts found. Try a different search?</h2>

	<?php endif; ?>

	<div class="searchbox"><?php include (TEMPLATEPATH . '/searchform.php'); ?></div>

	</div>

<?php get_sidebar(); ?>

<?php get_footer(); ?>
