<?php
/*
 * Template Name: Developers Guide Pages Template
 * Description: Template for pages in Document Section
 */
  get_header();
?>
<div style="width:100%;background-color:#1F419A;color:white;font-size:larger;">
  <div style="padding: 5px 0 5px 15px">Developer's Zone</div>
</div>

<div class="breadcrumb"><!-- Use breadcrumbs to tell users where they are -->
  <div class="breadcrumbs">
    <?php
      if(function_exists('bcn_display')){
          bcn_display();
      }
    ?>
  </div>
</div><!--close breadcrumb-->

<link type="text/css" rel="stylesheet" href="/wordpress/wp-content/themes/HS_OICR_2013/dev-guide.css?v=20160701">

<div id="postWrap">
  <div id="page-post">
    <?php while ( have_posts() ) : the_post(); ?>
    <?php get_template_part( 'content', 'page' ); ?>
    <?php endwhile; // end of the loop. ?>
  </div><!-- #content -->
</div><!-- #primary -->


<div class="clear"></div><!--clear grid15 and 9-->
</div>
<?php get_footer(); ?>