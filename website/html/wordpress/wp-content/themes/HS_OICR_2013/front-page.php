<?php get_header(); ?>
<!--     <div id="hideMe" style="background:yellow;font-size:medium;padding:5px;padding-left:20px;border:1px solid red">
     July 31, 2014: fly.reactome.org and gallus.reactome.org are offline for scheduled maintenance
     </div>
-->
     <div class="grid_15">     
     <div class="buttonswrap"><!--set left colum with buttons-->
	  <div class="grid_5 alpha">
	    <a class="button icon browse" href="/PathwayBrowser/"><span>Browse Pathways</span></a>
 	 </div>
  	<div class="grid_5">
   	 <a class="button icon data" href="/PathwayBrowser/#TOOL=AT"><span>Analyze Data</span></a>
  	</div>

    <div class="grid_5 omega">

    <a class="button icon cytoscape" href="//wiki.reactome.org/index.php/ReactomeFIViz"><span>Reactome FIViz app</span></a>
    </div>
  
 	 <div class="clear"></div> <!--clear the first row of buttons -->

    <div class="grid_5 alpha">
    <a class="button icon overview" href="//wiki.reactome.org/index.php/Usersguide"><span>User Guide</span></a>
    </div>
	  <div class="grid_5">
	    <a class="button icon download" href="/pages/download-data/"><span>Data Download</span></a>
	  </div>
          <div class="grid_5 omega">
        <a class="button icon contact" href="/pages/contact-us/"><span>Contact Us</span></a>
      </div>
      
      <div class="clear"></div> <!--clear second row of buttons -->
    
    </div><!--close buttonswrap-->
    
    <div class="contentwrap">
    <div class="contenthead">About Reactome</div><!--close contenthead-->
    <div class="contentbody">
    	Reactome is a free, open-source, curated and peer reviewed pathway database. Our goal is to provide intuitive bioinformatics tools for the visualization, interpretation and analysis of pathway knowledge to support basic research, genome analysis, modeling, systems biology and education. The current version (v62) of Reactome was released on September 27, 2017.</p> 

If you use Reactome in Asia, we suggest using our Chinese mirror site at <a href="http://reactome.ncpsb.org/"><span>reactome.ncpsb.org</a>.

    </div><!--close contentbody-->
    </div><!--close contentwrap-->
    
    <div class="credits">
    <a href="//www.oicr.on.ca" target="_new"><img src="<?php bloginfo('template_directory'); ?>/images/logos/OICR2logo.png" height="50"></a>
    <a href="//www.med.nyu.edu/" target="_new"><img src="<?php bloginfo('template_directory'); ?>/images/logos/NYUL-Health_logo.png" height="50"></a>
    <a href="//www.ebi.ac.uk/" target="_new"><img src="<?php bloginfo('template_directory'); ?>/images/logos/EMBL_EBI_Logo_black.png" height="40"></a>
    <a href="//www.ohsu.edu/" target="_new"><img src="<?php bloginfo('template_directory'); ?>/images/logos/ohsu_logo.png" height="50"></a>
    <br>
    </br>
    <p>The development of Reactome is supported by grants from the National Institutes of Health (P4115HG003751 and U54GM114833), European Bioinformatics Institute (EMBL-EBI), Open Targets (The Target Validation Platform), and Medicine by Design (University of Toronto).</p>
    </div><!--close credits-->    </div><!--close grid15-->
    <div class="grid_9"> 
            <div class="contentwrap">
            <div class="contenthead">

            Tweets  </div>
            <div class="contentbody">
            
            <div class="announcement">

            <a href="/version-62-released/"> Current Version: Reactome V62             <!--This is a placeholder for static announcements. For example, downtime due to server maintenance, or some other important announcement-->

            </div><!--close annoucement-->

            <!--twitter feed --><div class="twitter">
            <a class="twitter-timeline" width="300" height="400" href="//twitter.com/reactome" data-widget-id="344458797739294721" data-chrome="noheader noborders transparent">Tweets by @reactome</a></div>
             </div><!--close content body-->
             </div><!--close contentwrap-->

    </div><!--close grid9-->
     
    <div class="clear"></div><!--clear grid15 and 9-->
    
</div><!--close wrapper-->
<!--close content-->

<?php get_footer(); ?>