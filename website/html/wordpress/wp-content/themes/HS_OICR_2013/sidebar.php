<div class="widget-area">
	<?php if ( ! dynamic_sidebar( 'Sidebar Widgets' ) ) : ?>
    	<aside id="search" class="widget">
        	<?php get_search_form(); ?>
        </aside>
    	<aside id="archives" class="widget">
        	<h3 class="widget-title">Archives</h3>
            <ul>
         	   <?php wp_get_archives('type=monthly&limit=12'); ?>
            </ul>
        </aside>
    <?php endif; // end sidebar widget area ?>
</div>