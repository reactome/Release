<article <?php post_class() ?> id="post-<?php the_ID(); ?>">
    <h1><?php the_title(); ?></h1>
    
	<div class="entry">
        <?php the_content(); ?>
		<?php wp_link_pages( array( 'before' => '<div class="page-link"><span>' . __( 'Pages:', 'twentyeleven' ) . '</span>', 'after' => '</div>' ) ); ?>
	</div><!-- .entry-content -->
</article><!-- #post-<?php the_ID(); ?> -->
