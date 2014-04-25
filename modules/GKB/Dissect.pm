package GKB::Dissect;
use strict;
use GD;
use GKB::Config;
use Data::Dumper;
use Getopt::Long;
use Math::Trig;
#Don't 'use' GraphViz but 'require' it when needed. This way we don't 
#necessarily need GraphViz and friends installed as long as the GraphViz
#dependent stuff is not part of the published website.
#use GraphViz;
use Exporter();

###################

my $OBJECT_REACTOME_DB;
my $OBJECT_GRAPHVIZ;
my $DB_NAME;
my $RAREMOL=1;
my $COMPLEX=1;
my $THICKNESS;
my $EXTRANODE=1;
my $ALLOW_TEXT=1;
my $WRAPLABEL;
my $LABEL;
my $ID_PARAM;
my $OBJECT_GD_IMAGE;

# Diagram building
my $FACTOR_IMAGE_MAGNIFICATION = 100;
my $FACTOR_GENMAPP_MAGNIFICATION = 1;
my $TOP_MARGIN = 10;
my $BOTTOM_MARGIN = 10;
my $LEFT_MARGIN = 10;
my $RIGHT_MARGIN = 10;
my $FIXED_IMAGE_X;
my $FIXED_IMAGE_Y;
my $MAPP_IMAGE_X;
my $MAPP_IMAGE_Y;
my @COLOUR_PALETTE = ();
my $WHITE;
my $BLACK;
my $RED;
my $GREEN;
my $PURPLE;
my $VIOLET;
my $BLUE;
my $GREY;

### Graphviz-related
my $GRAPHVIZ_PLOT_SIZE;
my $COMPLEX_SHAPE='box';
my %COMPARTMENTS;
my %SPECIES; #{sp dbid} {name} {compartment} {shortname} {accession}
my %ENTITY_DISSECTION; # This hash contains the structure and topology of complexes and sets, as well as dimensions
my $SPECIES_COUNT;
my $EDGE_LENGTH = 50;

### Map-building-related
my @KILL_HIT_LIST;
my %KILL_LOOKUP_HASH = ();
my %IMAGE_TITLE;
my %REACTIONS; #{rct dbid} {name} {Input}{sp db_id} {Output}{sp db_id} {Modifier}{sp db_id};
my %REACTION_NODE_COORDS;
my %ENTITY_NODE_COORDS;
my @ENTITY_IMAGE_MAP_COORDS;
my %EDGE_FILTER = (); #Hash lookup to avoid re-drawing the same edge
my %TITLE_MAP; #Titles and co-ordinates used for the image

### These two variables affect the dimensions of individual entity boxes on the map
my $MINIMUM_CELL_WIDTH = 0.55;
my $MINIMUM_CELL_HEIGHT = 0.33;
my $MAX_NODE_WIDTH_RECORDED=0;
my $MAX_NODE_HEIGHT_RECORDED=0;
my $MIN_X;
my $MIN_Y;

###################

sub new {

    my ($class) = @_;
    my $objref={
	_dbid_array_ref=>$_[1],
	_dba=>$_[2],
    };
    bless $objref, $class;
    $OBJECT_REACTOME_DB=$objref->{_dba};
    $DB_NAME=$OBJECT_REACTOME_DB->db_name;
    return $objref;

}#sub new

sub run_NonTrivial_Dimension_Stats{
    my($self)=@_;
    my %nontrivial;
    my $class;
    my $entity_type;
    my $instance;
    my $db_id;
    my $tweak_height;
    my $tweak_width;

    %REACTION_NODE_COORDS = ();
    @ENTITY_IMAGE_MAP_COORDS = ();
    %SPECIES = ();
    %ENTITY_NODE_COORDS=();
    %ENTITY_DISSECTION=();


    foreach $db_id ( @{$self->{_dbid_array_ref}} ){
	$instance = $OBJECT_REACTOME_DB->fetch_instance_by_db_id($db_id)->[0];
	$class=0;
	$tweak_height=0;
	$tweak_width=0;
	if($instance->is_a("Complex")){
	    $class=3;
	    $tweak_width=1;
	}elsif($instance->is_a("EntitySet")){
	    $class=4;
	    $tweak_height=1;
	}
	$entity_type=typeset($instance);
#	print join("\t",($class,$db_id,$ENTITY_DISSECTION{$db_id}{dimensions}[0],$ENTITY_DISSECTION{$db_id}{dimensions}[1])),"\n";
	$nontrivial{$class}{$db_id}{width}=$ENTITY_DISSECTION{$db_id}{dimensions}[0]-$tweak_width;
	$nontrivial{$class}{$db_id}{height}=$ENTITY_DISSECTION{$db_id}{dimensions}[1]-$tweak_height;
    }

    return (%nontrivial);

}#sub run_Dimension_Stats{

sub plot_One_Entity {
    my($self)=@_;
    my $show_labels = $_[1] || 0 ;
    my $entity_type;
    my $instance;
    my %label_coords;
    my $label_spacer;

    my $side;
    my $counter;

    %REACTION_NODE_COORDS = ();
    @ENTITY_IMAGE_MAP_COORDS = ();
    %SPECIES = ();
    %ENTITY_NODE_COORDS=();
    %ENTITY_DISSECTION=();


    my $dbid =$self->{_dbid_array_ref}->[0];
    $instance = $OBJECT_REACTOME_DB->fetch_instance_by_db_id($dbid)->[0];
    if($instance->is_a('PhysicalEntity')){
        $SPECIES{$dbid}{name}=$instance->Name->[0];
        $entity_type=typeset($instance);

        $ENTITY_NODE_COORDS{$dbid}->{x1}=0;
        $ENTITY_NODE_COORDS{$dbid}->{y1}=0;
	$ENTITY_NODE_COORDS{$dbid}->{x2}=$ENTITY_DISSECTION{$dbid}{dimensions}[0]*$MINIMUM_CELL_WIDTH*$FACTOR_IMAGE_MAGNIFICATION;
	$ENTITY_NODE_COORDS{$dbid}->{y2}=$ENTITY_DISSECTION{$dbid}{dimensions}[1]*$MINIMUM_CELL_HEIGHT*$FACTOR_IMAGE_MAGNIFICATION;
	$FIXED_IMAGE_X=$ENTITY_NODE_COORDS{$dbid}->{x2}+2;
	$FIXED_IMAGE_Y=$ENTITY_NODE_COORDS{$dbid}->{y2}+2;
	&set_GD;


        #$OBJECT_GD_IMAGE->rectangle($ENTITY_NODE_COORDS{$dbid}->{x1},$ENTITY_NODE_COORDS{$dbid}->{y1},$ENTITY_NODE_COORDS{$dbid}->{x2},$ENTITY_NODE_COORDS{$dbid}->{y2},$BLACK);

        if($SPECIES{$dbid}{type}==3){
	    $label_coords{x1}=$ENTITY_NODE_COORDS{$dbid}->{x1};
            $label_coords{y1}=$ENTITY_NODE_COORDS{$dbid}->{y1};
	    $label_coords{y2}=$ENTITY_NODE_COORDS{$dbid}->{y2};
            $label_spacer=int(($ENTITY_NODE_COORDS{$dbid}->{x2}-$ENTITY_NODE_COORDS{$dbid}->{x1})/$ENTITY_DISSECTION{$dbid}{dimensions}[0]);
            $label_coords{x2}=$label_coords{x1}+$label_spacer;
            if($show_labels){
		$SPECIES{$dbid}{summary_coords}=\%label_coords;
		&draw_Label(\%label_coords,'v',$SPECIES{$dbid}{name});
		$ENTITY_NODE_COORDS{$dbid}->{x1}=$label_coords{x2};
	    }else{
		$ENTITY_NODE_COORDS{$dbid}->{x1}=0;
		$ENTITY_NODE_COORDS{$dbid}->{x2}=$ENTITY_NODE_COORDS{$dbid}->{x2}-$label_spacer;
	    }
            $ENTITY_DISSECTION{$dbid}{dimensions}[0]--;
            &draw_Entity(\%{$ENTITY_NODE_COORDS{$dbid}},\%{$ENTITY_DISSECTION{$dbid}{vertical}},\@{$ENTITY_DISSECTION{$dbid}{dimensions}},'v');
        }elsif($SPECIES{$dbid}{type}>3){
            $label_coords{x1}=$ENTITY_NODE_COORDS{$dbid}->{x1};
            $label_coords{y1}=$ENTITY_NODE_COORDS{$dbid}->{y1};
            $label_coords{x2}=$ENTITY_NODE_COORDS{$dbid}->{x2};
            $label_spacer=int(($ENTITY_NODE_COORDS{$dbid}->{y2}-$ENTITY_NODE_COORDS{$dbid}->{y1})/$ENTITY_DISSECTION{$dbid}{dimensions}[1]);
            $label_coords{y2}=$label_coords{y1}+$label_spacer;
	    if($show_labels){
		$SPECIES{$dbid}{summary_coords}=\%label_coords;
		&draw_Label(\%label_coords,'h',$SPECIES{$dbid}{name});
		$ENTITY_NODE_COORDS{$dbid}->{y1}=$label_coords{y2};
	    }else{
		$ENTITY_NODE_COORDS{$dbid}->{y1}=0;
		$ENTITY_NODE_COORDS{$dbid}->{y2}=$ENTITY_NODE_COORDS{$dbid}->{y2}-$label_spacer;
	    }
            $ENTITY_DISSECTION{$dbid}{dimensions}[1]--;
            &draw_Entity(\%{$ENTITY_NODE_COORDS{$dbid}},\%{$ENTITY_DISSECTION{$dbid}{horizontal}},\@{$ENTITY_DISSECTION{$dbid}{dimensions}},'h');
        }else{
            $OBJECT_GD_IMAGE->filledRectangle($ENTITY_NODE_COORDS{$dbid}->{x1},$ENTITY_NODE_COORDS{$dbid}->{y1},$ENTITY_NODE_COORDS{$dbid}->{x2},$ENTITY_NODE_COORDS{$dbid}->{y2},$COLOUR_PALETTE[$SPECIES{$dbid}{type}]);
	    if($ALLOW_TEXT){$OBJECT_GD_IMAGE->string(gdSmallFont,$ENTITY_NODE_COORDS{$dbid}->{x1}+2,$ENTITY_NODE_COORDS{$dbid}->{y1}+2,$ENTITY_DISSECTION{$dbid}{genmapp}{name},$BLACK);}
	    push(@ENTITY_IMAGE_MAP_COORDS,{x1 => $ENTITY_NODE_COORDS{$dbid}->{x1}, y1 => $ENTITY_NODE_COORDS{$dbid}->{y1}, x2 => $ENTITY_NODE_COORDS{$dbid}->{x2},  y2 => $ENTITY_NODE_COORDS{$dbid}->{y2}, name => $ENTITY_DISSECTION{$dbid}{genmapp}{name}, db_id => $dbid, accession=>$ENTITY_DISSECTION{$dbid}{genmapp}{accession}});
	}#else
	}#if entity
	
	$ALLOW_TEXT=1;
    return($OBJECT_GD_IMAGE,&pass_Image_Map);
}#sub plot_One_Entity {


sub plot_One_Entity_In_Set_Space {

    #takes first db_ID in the array and plots it within the x and y confines specified by the paramaters

    my($self)=@_;
    my $x_size = $_[1]-1;
    my $y_size = $_[2]-1;
    if(defined($_[3]) && $_[3]==0){$ALLOW_TEXT=0;}
    my $ALLOW_LABEL = 1;
    if(defined($_[4]) && $_[4]==0){$ALLOW_LABEL=0;}
    my $entity_type;
    my $instance;
    my %label_coords;
    my $label_spacer;

    my $side;
    my $counter;

    %REACTION_NODE_COORDS = ();
    @ENTITY_IMAGE_MAP_COORDS = ();
    %SPECIES = ();
    %ENTITY_NODE_COORDS=();
    %ENTITY_DISSECTION=();


    $FIXED_IMAGE_X=$x_size+1;
    $FIXED_IMAGE_Y=$y_size+1;
    &set_GD;

    my $dbid =$self->{_dbid_array_ref}->[0];
    $instance = $OBJECT_REACTOME_DB->fetch_instance_by_db_id($dbid)->[0];
    if($instance->is_a('PhysicalEntity')){
	$SPECIES{$dbid}{name}=$instance->Name->[0];
	$entity_type=typeset($instance);

        $ENTITY_NODE_COORDS{$dbid}->{x1}=0;
	$ENTITY_NODE_COORDS{$dbid}->{y1}=0;
#	print $ENTITY_DISSECTION{$dbid}{dimensions}[0].' '.$ENTITY_DISSECTION{$dbid}{dimensions}[1]." DIM\n\n";

	$ENTITY_NODE_COORDS{$dbid}->{x2}=$x_size;
	$ENTITY_NODE_COORDS{$dbid}->{y2}=$y_size;
   
	$OBJECT_GD_IMAGE->rectangle($ENTITY_NODE_COORDS{$dbid}->{x1},$ENTITY_NODE_COORDS{$dbid}->{y1},$ENTITY_NODE_COORDS{$dbid}->{x2},$ENTITY_NODE_COORDS{$dbid}->{y2},$BLACK);

        if($SPECIES{$dbid}{type}==3){
	    if($ALLOW_LABEL){
		$label_coords{x1}=$ENTITY_NODE_COORDS{$dbid}->{x1};
		$label_coords{y1}=$ENTITY_NODE_COORDS{$dbid}->{y1};
		$label_coords{y2}=$ENTITY_NODE_COORDS{$dbid}->{y2};
		$label_spacer=int(($ENTITY_NODE_COORDS{$dbid}->{x2}-$ENTITY_NODE_COORDS{$dbid}->{x1})/$ENTITY_DISSECTION{$dbid}{dimensions}[0]);
		$label_coords{x2}=$label_coords{x1}+$label_spacer;
	    }else{
		$label_coords{x1}=$ENTITY_NODE_COORDS{$dbid}->{x1};
                $label_coords{y1}=$ENTITY_NODE_COORDS{$dbid}->{y1};
		$label_coords{x2}=$ENTITY_NODE_COORDS{$dbid}->{x1};
                $label_coords{y2}=$ENTITY_NODE_COORDS{$dbid}->{y1};
	    }
            $SPECIES{$dbid}{summary_coords}=\%label_coords;
            if($ALLOW_LABEL){&draw_Label(\%label_coords,'v',$SPECIES{$dbid}{name});}
            $ENTITY_NODE_COORDS{$dbid}->{x1}=$label_coords{x2};
            $ENTITY_DISSECTION{$dbid}{dimensions}[0]--;
            &draw_Entity(\%{$ENTITY_NODE_COORDS{$dbid}},\%{$ENTITY_DISSECTION{$dbid}{vertical}},\@{$ENTITY_DISSECTION{$dbid}{dimensions}},'v');
	}elsif($SPECIES{$dbid}{type}>3){
	    if($ALLOW_LABEL){
		$label_coords{x1}=$ENTITY_NODE_COORDS{$dbid}->{x1};
		$label_coords{y1}=$ENTITY_NODE_COORDS{$dbid}->{y1};
		$label_coords{x2}=$ENTITY_NODE_COORDS{$dbid}->{x2};
		$label_spacer=int(($ENTITY_NODE_COORDS{$dbid}->{y2}-$ENTITY_NODE_COORDS{$dbid}->{y1})/$ENTITY_DISSECTION{$dbid}{dimensions}[1]);
		$label_coords{y2}=$label_coords{y1}+$label_spacer;
	    }else{
		$label_coords{x1}=$ENTITY_NODE_COORDS{$dbid}->{x1};
                $label_coords{y1}=$ENTITY_NODE_COORDS{$dbid}->{y1};
		$label_coords{x2}=$ENTITY_NODE_COORDS{$dbid}->{x1};
                $label_coords{y2}=$ENTITY_NODE_COORDS{$dbid}->{y1};
	    }
	    $SPECIES{$dbid}{summary_coords}=\%label_coords;
	    if($ALLOW_LABEL){&draw_Label(\%label_coords,'h',$SPECIES{$dbid}{name});}
	    $ENTITY_NODE_COORDS{$dbid}->{y1}=$label_coords{y2};
	    $ENTITY_DISSECTION{$dbid}{dimensions}[1]--;
	    &draw_Entity(\%{$ENTITY_NODE_COORDS{$dbid}},\%{$ENTITY_DISSECTION{$dbid}{horizontal}},\@{$ENTITY_DISSECTION{$dbid}{dimensions}},'h');
	}else{
	    $OBJECT_GD_IMAGE->filledRectangle($ENTITY_NODE_COORDS{$dbid}->{x1},$ENTITY_NODE_COORDS{$dbid}->{y1},$ENTITY_NODE_COORDS{$dbid}->{x2},$ENTITY_NODE_COORDS{$dbid}->{y2},$COLOUR_PALETTE[$SPECIES{$dbid}{type}]);
            if($ALLOW_TEXT){$OBJECT_GD_IMAGE->string(gdSmallFont,$ENTITY_NODE_COORDS{$dbid}->{x1}+2,$ENTITY_NODE_COORDS{$dbid}->{y1}+2,$ENTITY_DISSECTION{$dbid}{genmapp}{name},$BLACK);}
            push(@ENTITY_IMAGE_MAP_COORDS,{x1 => $ENTITY_NODE_COORDS{$dbid}->{x1}, y1 => $ENTITY_NODE_COORDS{$dbid}->{y1}, x2 => $ENTITY_NODE_COORDS{$dbid}->{x2},  y2 => $ENTITY_NODE_COORDS{$dbid}->{y2}, name => $ENTITY_DISSECTION{$dbid}{genmapp}{name}, db_id => $dbid, accession=>$ENTITY_DISSECTION{$dbid}{genmapp}{accession}});   
	}#else
    }#if entity

    $ALLOW_TEXT=1;
    return($OBJECT_GD_IMAGE,&pass_Image_Map);
}#sub plot_One_Entity_In_Set_Space {

sub plot {
    require GraphViz;
    my($self)=@_;
    if(defined($_[1]) && $_[1]==0){$ALLOW_TEXT=0;}
    my $instance;
    my $ref_reactions; # Reference to array returned by follow_class_attributes function

    if($RAREMOL){
	@KILL_HIT_LIST = qw(ATP ADP CO2 CoA orthophosphate NAD+ NADH NADP+ NADPH FAD FADH2 H2O GTP GDP UTP Acetyl-CoA Orthophosphate);
	foreach my $molecule (@KILL_HIT_LIST){
	    $KILL_LOOKUP_HASH{$molecule}=1;
	}#foreach my $molecule (@KILL_HIT_LIST){
    }#if($RAREMOL){

    #This loop expects a list of integer event IDs passed on as a carriage-return-delimited string via the CGI 'ID' parameter
    foreach my $db_id ( @{$self->{_dbid_array_ref}} ){
	#print $db_id,"TESTING \n\n\n";
        $db_id=~s/\W//g;
        $instance = $OBJECT_REACTOME_DB->fetch_instance_by_db_id($db_id)->[0];
	$TITLE_MAP{$db_id}{'name'}='['.substr($instance->displayName,0,100).']';
	$TOP_MARGIN+=10;

	if($instance->is_a('PhysicalEntity')){
	    &add_Entity('',$instance,'PE','','');
	}elsif($instance->is_a('Event')){

	    #Convert any pathway into its component reactions
	    $ref_reactions = $instance->follow_class_attributes
		(-INSTRUCTIONS =>
		 {'Pathway' => {'attributes' => [qw(hasComponent)]},
		  'ConceptualEvent' => {'attributes' => [qw(hasSpecialisedForm)]},
		  'EquivalentEventSet' => {'attributes' => [qw(hasMember)]}},
		 -OUT_CLASSES => ['Reaction']
		 );
	    #For each reaction, extract the molecular participants
	    map {if(!((@{$_->input}==0 || @{$_->output}==0))){
		&dissect_Reaction($_)
		}} @{$ref_reactions};
	}#elsif
	    
     }#foreach

     $OBJECT_GRAPHVIZ = GraphViz->new(rankdir  => 'TB', layout => 'dot', orientation=>'landscape', edge => {len => $EDGE_LENGTH}, nodesep =>'0.3', ranksep => '1.0');

    $FACTOR_IMAGE_MAGNIFICATION=(&plot_Network)/5;
    $FACTOR_IMAGE_MAGNIFICATION=100;
    &build_Diagram_From_Graphviz;
    $ALLOW_TEXT=1;
    return($OBJECT_GD_IMAGE,&pass_Image_Map);

}#sub plot

sub pass_Image_Map{

    my $outstring = '';
    # Printing HTML image map
    foreach my $db_id (keys %REACTION_NODE_COORDS){
        $outstring.="<area shape=\"circle\" COORDS=\"$REACTION_NODE_COORDS{$db_id}{'x'},$REACTION_NODE_COORDS{$db_id}{'y'},$REACTION_NODE_COORDS{$db_id}{'c'}\" href=\"/cgi-bin/eventbrowser?DB=$DB_NAME&ID=$db_id\" target=\"_blank\" ONMOUSEOVER='ddrivetip(\"$REACTIONS{$db_id}{'name'}\",\"#DCDCDC\",250);' ONMOUSEOUT='hideddrivetip(); return true;'>\n";
    }#foreach my $db_id (keys %REACTION_NODE_COORDS){
    
    foreach my $hash (@ENTITY_IMAGE_MAP_COORDS){
        $outstring.="<area shape=\"RECT\" COORDS=\"$hash->{'x1'},$hash->{'y1'},$hash->{'x2'},$hash->{'y2'}\" href=\"/cgi-bin/eventbrowser?DB=$DB_NAME&ID=".$hash->{'db_id'}."\" target=\"_blank\" ONMOUSEOVER='ddrivetip(\"$hash->{accession}\",\"#DCDCDC\",250);' ONMOUSEOUT='hideddrivetip(); return true;'>\n";
    }#for (my $i=0; $i < $file_no; $i++){
    
    foreach my $pe_id (keys %SPECIES){
        if($SPECIES{$pe_id} && $SPECIES{$pe_id}{type}>=3){
            $outstring.="<area shape=\"RECT\" COORDS=\"$SPECIES{$pe_id}{summary_coords}{x1},$SPECIES{$pe_id}{summary_coords}{y1},$SPECIES{$pe_id}{summary_coords}{x2},$SPECIES{$pe_id}{summary_coords}{y2}\" href=\"/cgi-bin/eventbrowser?DB=$DB_NAME&ID=".$pe_id."\" target=\"_blank\" ONMOUSEOVER='ddrivetip(\"$SPECIES{$pe_id}{name}\",\"#DCDCDC\",250);' ONMOUSEOUT='hideddrivetip(); return true;'>\n";
        }#if($SPECIES{$pe_id}{type}>=3){
    }#foreach my $PE (keys %SPECIES){

    return $outstring;

}#sub pass_Image_Map

sub build_Diagram_From_Graphviz {

    my $work_title_level = 0;
    my $string_graphviz_layout_result;
    my @graphviz_output_rows;
    my @graphviz_boundary_measurements;
    my $objkey=0;
    my $max_x;
    my $max_y;
    my $min_x=9999999;
    my $min_y=9999999;
    my @graphviz_row_parse;
    my $temp;

    $string_graphviz_layout_result=$OBJECT_GRAPHVIZ->as_plain;
    @graphviz_output_rows=split(/\n/,$string_graphviz_layout_result);

    shift(@graphviz_output_rows);

    map{
	if($_=~/^node/){
	    @graphviz_row_parse = split(/\s+/,$_);
	    $temp=$graphviz_row_parse[2]-($graphviz_row_parse[4]/2);
	    if($temp<$min_x){$min_x=$temp;}
	    $temp=$graphviz_row_parse[2]+($graphviz_row_parse[4]/2);
	    if($temp>$max_x){$max_x=$temp;}
	    $temp=$graphviz_row_parse[3]-($graphviz_row_parse[5]/2);
	    if($temp<$min_y){$min_y=$temp;}
	    $temp=$graphviz_row_parse[3]+($graphviz_row_parse[5]/2);
            if($temp>$max_y){$max_y=$temp;}
 	}# if($_=~/^node/){
    }@graphviz_output_rows;

    $MIN_X=$min_x;
    $MIN_Y=$min_y;

    $FIXED_IMAGE_X = int((($max_x-$min_x+1)*$FACTOR_IMAGE_MAGNIFICATION)+$LEFT_MARGIN+$RIGHT_MARGIN);
    $FIXED_IMAGE_Y = int((($max_y-$min_y+1)*$FACTOR_IMAGE_MAGNIFICATION)+$TOP_MARGIN+$BOTTOM_MARGIN);

    $MAPP_IMAGE_X = $FIXED_IMAGE_X*$FACTOR_GENMAPP_MAGNIFICATION;
    $MAPP_IMAGE_Y = $FIXED_IMAGE_Y*$FACTOR_GENMAPP_MAGNIFICATION;

    &set_GD;

    foreach my $db_id(keys(%TITLE_MAP)){
	if($ALLOW_TEXT){$OBJECT_GD_IMAGE->string(gdMediumBoldFont,1,$work_title_level,$TITLE_MAP{$db_id}{name},$BLACK);}
	$TITLE_MAP{$db_id}{'x1'}=1;
	$TITLE_MAP{$db_id}{'y1'}=$work_title_level-1;
	$TITLE_MAP{$db_id}{'x2'}=length($TITLE_MAP{$db_id}{name})*8;
	$TITLE_MAP{$db_id}{'y2'}=$work_title_level+8;
	$work_title_level+=12;
    }# foreach my $db_id(keys(%{%TITLE_MAP})){

    map {
	if($_=~/^node/){
	    $objkey++;
	    &node($_,$objkey);
	}# if($_=~/^node/){
	if($_=~/^edge/){
	    $objkey++;
	    &edge($_,$objkey);
	}# if($_=~/^edge/){	
     } @graphviz_output_rows;

}#sub build_Diagram_From_Graphviz {

sub set_GD {
    $OBJECT_GD_IMAGE = new GD::Image($FIXED_IMAGE_X,$FIXED_IMAGE_Y);

    $WHITE = $OBJECT_GD_IMAGE->colorAllocate(255,255,255);
    $BLACK = $OBJECT_GD_IMAGE->colorAllocate(0,0,0);
    $RED = $OBJECT_GD_IMAGE->colorAllocate(255,0,0);
    $GREEN = $OBJECT_GD_IMAGE->colorAllocate(0,255,0);
    $PURPLE = $OBJECT_GD_IMAGE->colorAllocate(155,0,155);
    $VIOLET = $OBJECT_GD_IMAGE->colorAllocate(255,150,255);
    $BLUE = $OBJECT_GD_IMAGE->colorAllocate(0,0,255);
    $GREY = $OBJECT_GD_IMAGE->colorAllocate(190,190,190);
    $COLOUR_PALETTE[0] = $OBJECT_GD_IMAGE->colorAllocate(255,255,0); # A non-reference physical entity (i.e. not a Complex, Small Molecule or Sequence)
    $COLOUR_PALETTE[1] = $OBJECT_GD_IMAGE->colorAllocate(176,196,222); # "Protein"
    $COLOUR_PALETTE[2] = $OBJECT_GD_IMAGE->colorAllocate(255,255,255); # Small Molecule
    $COLOUR_PALETTE[3] = $OBJECT_GD_IMAGE->colorAllocate(222,176,196); # sequences apart from proteins

    $OBJECT_GD_IMAGE->transparent($WHITE);
    $OBJECT_GD_IMAGE->interlaced('true');

}#sub set_GD {

sub edge {

    # Plots edges
    my $row = $_[0];
    my @graphviz_row_parse = split(/\s+/,$row);
    my $objkey = $_[1];
    my $colour = '';
    my $size_ar = @graphviz_row_parse;
    my $x1;
    my $y1;
    my $x2;
    my $y2;
    my @stoichiometry;
    my $stoich_col;

    $x1=$LEFT_MARGIN-$MIN_X+($graphviz_row_parse[4]*$FACTOR_IMAGE_MAGNIFICATION);
    $y1=$FIXED_IMAGE_Y-$MIN_Y-$TOP_MARGIN-($graphviz_row_parse[5]*$FACTOR_IMAGE_MAGNIFICATION);
    $x2=$LEFT_MARGIN-$MIN_X+($graphviz_row_parse[$size_ar-7]*$FACTOR_IMAGE_MAGNIFICATION);
    $y2=$FIXED_IMAGE_Y-$MIN_Y-$TOP_MARGIN-($graphviz_row_parse[$size_ar-6]*$FACTOR_IMAGE_MAGNIFICATION);
    
    if($row=~/ Mod /){
	$colour = $GREEN;
    }elsif($row=~/ PReg /){
	$colour = $RED;
    }elsif($row=~/ NReg /){
	$colour = $BLUE;
    }else{
	$colour = $BLACK;
    }
	
    $OBJECT_GD_IMAGE->line($x1,$y1,$x2,$y2,$colour);
	
    &arrowhead($x1,$y1,$x2,$y2,$colour);
    if($graphviz_row_parse[18] =~/\_/){$stoich_col=$graphviz_row_parse[18];}else{$stoich_col=$graphviz_row_parse[12];}
    @stoichiometry=split(/\_/,$stoich_col);

    if(defined($stoichiometry[1]) && (!($stoichiometry[1]=~/\D/)) && $stoichiometry[1]>1){
	&edge_weight($x1,$y1,$x2,$y2,$stoichiometry[1]);
    }


    $x1=int($FACTOR_GENMAPP_MAGNIFICATION*$x1);
    $y1=int($FACTOR_GENMAPP_MAGNIFICATION*$y1);
    $x2=int($FACTOR_GENMAPP_MAGNIFICATION*$x2);
    $y2=int($FACTOR_GENMAPP_MAGNIFICATION*$y2);
    
}#sub line

sub edge_weight{

    my $x1=$_[0];
    my $y1=$_[1];
    my $x2=$_[2];
    my $y2=$_[3];
    my $weight = $_[4];
    my @x = ($x1,$x2);
    my @y = ($y1,$y2);
    my $x_pos = 0;
    my $y_pos = 0;

    @x = sort{$a<=>$b}@x;
    @y = sort{$a<=>$b}@y;

    $x_pos = $x[0] + (abs(int(($x1-$x2)/2))) - 5;
    $y_pos = $y[0] + (abs(int(($y1-$y2)/2))) - 15;

    if($ALLOW_TEXT){$OBJECT_GD_IMAGE->string(gdMediumBoldFont,$x_pos,$y_pos,$weight,$BLACK);}

}#sub edge_weight{

sub arrowhead {
	
  # Calculates co-ordinates for edge arrowheads
    my $x1=$_[0];
    my $y1=$_[1];
    my $x2=$_[2];
    my $y2=$_[3];
    my $colour = $_[4];
    my $ar_len = 7;
    my $ar_wid = 3;
    my $ar_hyp = sqrt(($ar_len**2)+($ar_wid**2));
    

    my $x3;
    my $y3;
    my $x4;
    my $y4;
    my $x5;
    my $y5;
    my $cntr_angle=0;
    my $theta;
    my $neta;
    my $beta;
    my $poly4 = new GD::Polygon;
    my $poly5 = new GD::Polygon;
    
    if($x1==$x2){$x2-=0.01;}
    if($y1==$y2){$y2-=0.01;}
    
    if($y2>$y1){
	
	if($x2>$x1){
	    $cntr_angle+=(pi/2)+atan(abs($y2-$y1)/abs($x2-$x1));
	}else{
	    $cntr_angle+= (pi)+atan(abs($x2-$x1)/abs($y2-$y1));
	}
	
    }else{
	
	if($x2>$x1){
	    $cntr_angle+=atan(abs($x2-$x1)/abs($y2-$y1));
	}else{
	    $cntr_angle+=((3*pi)/2)+atan(abs($y2-$y1)/abs($x2-$x1));
	}
	
    }
    
    $x4=$x2-($ar_hyp*(sin($cntr_angle-(pi/4))));
    $x5=$x2+($ar_hyp*(sin($cntr_angle-((3*pi)/4))));
    $y4=$y2-($ar_hyp*(sin($cntr_angle-((3*pi)/4))));
    $y5=$y2-($ar_hyp*(sin($cntr_angle-(pi/4))));
    
    $poly4->addPt($x2,$y2);
    $poly4->addPt($x4,$y4);
    $poly4->addPt($x5,$y5);
    
    $OBJECT_GD_IMAGE->filledPolygon($poly4,$colour);
    
}#sub arrowhead {
    
sub node {

    my $row = $_[0];
    my @graphviz_row_parse = split(/\s+/,$row);
    my $objkey = $_[1];
    my @buff;
    my $line;
    my $mapp_x1 = int(($LEFT_MARGIN+($graphviz_row_parse[2]*$FACTOR_IMAGE_MAGNIFICATION))*$FACTOR_GENMAPP_MAGNIFICATION);
    my $mapp_y1 = int(($FIXED_IMAGE_Y-$TOP_MARGIN-($graphviz_row_parse[3]*$FACTOR_IMAGE_MAGNIFICATION))*$FACTOR_GENMAPP_MAGNIFICATION);
    my $mapp_width = int(($graphviz_row_parse[4]*$FACTOR_IMAGE_MAGNIFICATION)*$FACTOR_GENMAPP_MAGNIFICATION);
    my $mapp_height = int(($graphviz_row_parse[5]*$FACTOR_IMAGE_MAGNIFICATION)*$FACTOR_GENMAPP_MAGNIFICATION);
    my $counter=0;
    my $internal_extra_node;
    my %label_coords;
    my $label_spacer;

    if($row=~/$COMPLEX_SHAPE/){
	$internal_extra_node=$EXTRANODE;
	$counter = 0;
	
	$ENTITY_NODE_COORDS{$graphviz_row_parse[6]}->{x1}=$LEFT_MARGIN-$MIN_X+(($graphviz_row_parse[2]-($internal_extra_node*$graphviz_row_parse[4]/2))*$FACTOR_IMAGE_MAGNIFICATION);
	$ENTITY_NODE_COORDS{$graphviz_row_parse[6]}->{y1}=$FIXED_IMAGE_Y-$MIN_Y-$TOP_MARGIN-(($graphviz_row_parse[3]+($internal_extra_node*$graphviz_row_parse[5]/2))*$FACTOR_IMAGE_MAGNIFICATION);
	$ENTITY_NODE_COORDS{$graphviz_row_parse[6]}->{x2}=$LEFT_MARGIN-$MIN_X+(($graphviz_row_parse[2]+($internal_extra_node*$graphviz_row_parse[4]/2))*$FACTOR_IMAGE_MAGNIFICATION);
	$ENTITY_NODE_COORDS{$graphviz_row_parse[6]}->{y2}=$FIXED_IMAGE_Y-$MIN_Y-$TOP_MARGIN-(($graphviz_row_parse[3]-($internal_extra_node*$graphviz_row_parse[5]/2))*$FACTOR_IMAGE_MAGNIFICATION);
	$OBJECT_GD_IMAGE->rectangle($ENTITY_NODE_COORDS{$graphviz_row_parse[6]}->{x1},$ENTITY_NODE_COORDS{$graphviz_row_parse[6]}->{y1},$ENTITY_NODE_COORDS{$graphviz_row_parse[6]}->{x2},$ENTITY_NODE_COORDS{$graphviz_row_parse[6]}->{y2},$BLACK);

	if($SPECIES{$graphviz_row_parse[6]}{type}==3){
	    $label_coords{x1}=$ENTITY_NODE_COORDS{$graphviz_row_parse[6]}->{x1};
	    $label_coords{y1}=$ENTITY_NODE_COORDS{$graphviz_row_parse[6]}->{y1};
	    $label_coords{y2}=$ENTITY_NODE_COORDS{$graphviz_row_parse[6]}->{y2};
	    $label_spacer=int(($ENTITY_NODE_COORDS{$graphviz_row_parse[6]}->{x2}-$ENTITY_NODE_COORDS{$graphviz_row_parse[6]}->{x1})/$ENTITY_DISSECTION{$graphviz_row_parse[6]}{dimensions}[0]);
	    $label_coords{x2}=$label_coords{x1}+$label_spacer;
	    $SPECIES{$graphviz_row_parse[6]}{summary_coords}=\%label_coords;
	    &draw_Label(\%label_coords,'v',$SPECIES{$graphviz_row_parse[6]}{name});
	    $ENTITY_NODE_COORDS{$graphviz_row_parse[6]}->{x1}=$label_coords{x2};
	    $ENTITY_DISSECTION{$graphviz_row_parse[6]}{dimensions}[0]--;
	    &draw_Entity(\%{$ENTITY_NODE_COORDS{$graphviz_row_parse[6]}},\%{$ENTITY_DISSECTION{$graphviz_row_parse[6]}{vertical}},\@{$ENTITY_DISSECTION{$graphviz_row_parse[6]}{dimensions}},'v');
	}elsif($SPECIES{$graphviz_row_parse[6]}{type}>3){
	    $label_coords{x1}=$ENTITY_NODE_COORDS{$graphviz_row_parse[6]}->{x1};
            $label_coords{y1}=$ENTITY_NODE_COORDS{$graphviz_row_parse[6]}->{y1};
            $label_coords{x2}=$ENTITY_NODE_COORDS{$graphviz_row_parse[6]}->{x2};
            $label_spacer=int(($ENTITY_NODE_COORDS{$graphviz_row_parse[6]}->{y2}-$ENTITY_NODE_COORDS{$graphviz_row_parse[6]}->{y1})/$ENTITY_DISSECTION{$graphviz_row_parse[6]}{dimensions}[1]);
            $label_coords{y2}=$label_coords{y1}+$label_spacer;
	    $SPECIES{$graphviz_row_parse[6]}{summary_coords}=\%label_coords;
	    &draw_Label(\%label_coords,'h',$SPECIES{$graphviz_row_parse[6]}{name});
            $ENTITY_NODE_COORDS{$graphviz_row_parse[6]}->{y1}=$label_coords{y2};
            $ENTITY_DISSECTION{$graphviz_row_parse[6]}{dimensions}[1]--;
	    &draw_Entity(\%{$ENTITY_NODE_COORDS{$graphviz_row_parse[6]}},\%{$ENTITY_DISSECTION{$graphviz_row_parse[6]}{horizontal}},\@{$ENTITY_DISSECTION{$graphviz_row_parse[6]}{dimensions}},'h');
	}else{
	    $OBJECT_GD_IMAGE->filledRectangle($ENTITY_NODE_COORDS{$graphviz_row_parse[6]}->{x1},$ENTITY_NODE_COORDS{$graphviz_row_parse[6]}->{y1},$ENTITY_NODE_COORDS{$graphviz_row_parse[6]}->{x2},$ENTITY_NODE_COORDS{$graphviz_row_parse[6]}->{y2},$COLOUR_PALETTE[$SPECIES{$graphviz_row_parse[6]}{type}]);
	    if($ALLOW_TEXT){$OBJECT_GD_IMAGE->string(gdSmallFont,$ENTITY_NODE_COORDS{$graphviz_row_parse[6]}->{x1}+2,$ENTITY_NODE_COORDS{$graphviz_row_parse[6]}->{y1}+2,$ENTITY_DISSECTION{$graphviz_row_parse[6]}{genmapp}{name},$BLACK);}
	    push(@ENTITY_IMAGE_MAP_COORDS,{x1 => $ENTITY_NODE_COORDS{$graphviz_row_parse[6]}->{x1}, y1 => $ENTITY_NODE_COORDS{$graphviz_row_parse[6]}->{y1}, x2 => $ENTITY_NODE_COORDS{$graphviz_row_parse[6]}->{x2},  y2 => $ENTITY_NODE_COORDS{$graphviz_row_parse[6]}->{y2}, name => $ENTITY_DISSECTION{$graphviz_row_parse[6]}{genmapp}{name}, db_id => $graphviz_row_parse[6], accession=>$ENTITY_DISSECTION{$graphviz_row_parse[6]}{genmapp}{accession}});
#	    print IMGENMAPP &print_GenMAPP('Rectangle',$ENTITY_NODE_COORDS{$graphviz_row_parse[6]}->{x1},$ENTITY_NODE_COORDS{$graphviz_row_parse[6]}->{y1},$ENTITY_NODE_COORDS{$graphviz_row_parse[6]}->{x2},$ENTITY_NODE_COORDS{$graphviz_row_parse[6]}->{y2},$ENTITY_DISSECTION{$graphviz_row_parse[6]}{genmapp}{name},$ENTITY_DISSECTION{$graphviz_row_parse[6]}{genmapp}{accession},$ENTITY_DISSECTION{$graphviz_row_parse[6]}{genmapp}{type});
	}

    }elsif($row=~/ e\d+ /){
	$graphviz_row_parse[6]=~s/^e//;
	$REACTION_NODE_COORDS{$graphviz_row_parse[6]}{'x'}=$LEFT_MARGIN-$MIN_X+($graphviz_row_parse[2]*$FACTOR_IMAGE_MAGNIFICATION);
	$REACTION_NODE_COORDS{$graphviz_row_parse[6]}{'y'}=$FIXED_IMAGE_Y-$MIN_Y-$TOP_MARGIN-($graphviz_row_parse[3]*$FACTOR_IMAGE_MAGNIFICATION);
	$REACTION_NODE_COORDS{$graphviz_row_parse[6]}{'c'}=($graphviz_row_parse[4])*$FACTOR_IMAGE_MAGNIFICATION;
	$OBJECT_GD_IMAGE->filledArc($REACTION_NODE_COORDS{$graphviz_row_parse[6]}{'x'},$REACTION_NODE_COORDS{$graphviz_row_parse[6]}{'y'},$REACTION_NODE_COORDS{$graphviz_row_parse[6]}{'c'},$REACTION_NODE_COORDS{$graphviz_row_parse[6]}{'c'},0,360,$BLUE);
	if($ALLOW_TEXT){$OBJECT_GD_IMAGE->string(gdMediumBoldFont,$REACTION_NODE_COORDS{$graphviz_row_parse[6]}{'x'}-8,$REACTION_NODE_COORDS{$graphviz_row_parse[6]}{'y'}-8,'R'.$REACTIONS{$graphviz_row_parse[6]}{localcount},$WHITE);}
	#print IMGENMAPP "OVAL\t$REACTION_NODE_COORDS{$graphviz_row_parse[6]}{'x'}\t$REACTION_NODE_COORDS{$graphviz_row_parse[6]}{'y'}\t$REACTION_NODE_COORDS{$graphviz_row_parse[6]}{'c'}\t$REACTION_NODE_COORDS{$graphviz_row_parse[6]}{'c'}\n";
	#print IMGENMAPP "TEXT\t".($REACTION_NODE_COORDS{$graphviz_row_parse[6]}{'x'}-8)."\t".($REACTION_NODE_COORDS{$graphviz_row_parse[6]}{'y'}-8)."\tR$REACTIONS{$graphviz_row_parse[6]}{localcount}\tREACTOME:$graphviz_row_parse[6]\n"; 
    }
    
}#sub node
    
sub draw_label {

    my $GD = $_[0];
    my $x = $_[1];
    my $y = $_[2];
    my $text = $_[3];
    my $min_span=30;
    my $span_count=0;
    my $y_move = 9;
    my $subphrase='';
    my $next_chunk = '';
    my $mapp_x = 0;
    my $mapp_y = 0;
    
    if($WRAPLABEL==0){
	if($ALLOW_TEXT){$$GD->string(gdSmallFont,$x,$y,$text,$BLACK);}
	$mapp_x = int($FACTOR_GENMAPP_MAGNIFICATION*$x);
	$mapp_y = int($FACTOR_GENMAPP_MAGNIFICATION*$y);
	#print IMGENMAPP "TEXT\t$mapp_x\t$mapp_y\t$text\n";
    }elsif($ALLOW_TEXT){
	$text =~ /(^.*)(\[.*\]$)/;
			my @chunks = split(/\s+|\-/,$1);
			push(@chunks,$2);
			
			foreach $next_chunk(@chunks){
			    
			    $subphrase .= $next_chunk.' ';
			    if(length($subphrase)>=$min_span){
				$$GD->string(gdSmallFont,$x,$y,$subphrase,$BLACK);
				$span_count=0;
				$subphrase='';
				$y +=$y_move;
				$mapp_x = int($FACTOR_GENMAPP_MAGNIFICATION*$x);
				$mapp_y = int($FACTOR_GENMAPP_MAGNIFICATION*$y);
				#print IMGENMAPP "TEXT\t$mapp_x\t$mapp_y\t$subphrase\n";
			    }#if(length($subphrase)>=$min_span){
			    
			}#foreach $next_chunk(@chunks){
			$$GD->string(gdSmallFont,$x,$y,$subphrase,$BLACK);
			$mapp_x = int($FACTOR_GENMAPP_MAGNIFICATION*$x);
			$mapp_y = int($FACTOR_GENMAPP_MAGNIFICATION*$y);
			#print IMGENMAPP "TEXT\t$mapp_x\t$mapp_y\t$subphrase\n";
		    }# if($WRAPLABEL==0){
	
}#sub sub draw_label {
    
sub dissect_Reaction{

  # Extracts Inputs, Output, Catalyst Entities and Regulators for display.

  my $event = $_[0];
  my $reaction_id = $event->db_id;
  my $entity;
  my $instance;
  my $regulator;
  my $non_reg_type=0;
  my $reg_type;
  my $activity;

  #The reaction name is stored for later mouseover display via the HTML image map.
  $REACTIONS{$reaction_id}{"name"}=$event->attribute_value('name')->[0];
  $REACTIONS{$reaction_id}{"name"}=~s/\"|\'/`/g;
  $REACTIONS{$reaction_id}{"name"}=~s/\"|\'/`/g;

  # Inputs
  foreach $entity (@{$event->input}){
      &add_Entity($event,$entity,'In',$non_reg_type,$reaction_id);
  }#foreach

  # Outputs
  foreach $entity (@{$event->output}){
      &add_Entity($event,$entity,'Out',$non_reg_type,$reaction_id);
  }#foreach

  # Catalysts
  foreach $activity (@{$event->attribute_value('catalystActivity')}){
      if(defined($activity->attribute_value('physicalEntity')->[0])){
	  $entity=$activity->attribute_value('physicalEntity')->[0];
	  &add_Entity($event,$entity,'Cat',$non_reg_type,$reaction_id);
      }#if

   }#foreach
  
#Regulators
foreach $regulator (@{$event->reverse_attribute_value('regulatedEntity')}){
    $entity = $regulator->Regulator->[0];
    if(($entity->is_a('PhysicalEntity'))){
      if($regulator->is_a('NegativeRegulation')){
	  $reg_type='NReg';
      }else{
	  $reg_type='PReg';
      }#if
      &add_Entity($event,$entity,'Reg',$reg_type,$reaction_id);
    }# if(!(exists($kill_hash{$entity->Name->[0]}))){
  }#foreach

}#sub dissect_pathway

sub add_Entity {
    #Adds entity nodes to graphviz, after having calculated the size of the node - this is particularly useful in case of complexes and sets.
    my $instance = $_[0];
    my $entity = $_[1];
    my $entity_name = $entity->Name->[0];
    my $entity_db_id = $entity->db_id;
    my $rltn = $_[2];
    my $reg_type = $_[3];
    my $reaction_id = $_[4];
    my $entity_type;

    if(!(exists($KILL_LOOKUP_HASH{$entity_name}))){

	$SPECIES{$entity_db_id}{name}=$entity_name;

	#Having copartment information is useful if one plans to cluster nodes by compartment.
	if(defined($entity->attribute_value('compartment')->[0])){
	    $COMPARTMENTS{$entity->attribute_value('compartment')->[0]->attribute_value('name')->[0]}=$entity->attribute_value('compartment')->[0]->db_id;
	    $SPECIES{$entity_db_id}{"compartment"}=$entity->attribute_value('compartment')->[0]->db_id;
	}else{
	    $SPECIES{$entity_db_id}{"compartment"}='';
	}#if(defined($entity->attribute_value('compartment')->[0])){

	#The 'typeset' function sets off the entity dissection and size assessment
	if(!(defined($SPECIES{$entity_db_id}{type}))){
	    $entity_type=typeset($entity);
	}else{
	    $entity_type=$SPECIES{$entity_db_id}{type};
	}

	#Node width and height are adjusted according to the size. These measurements are obtained via the dissect_Entity function that is called by the typeset method mentioned above.
	if($MAX_NODE_WIDTH_RECORDED<$ENTITY_DISSECTION{$entity_db_id}{dimensions}[0]){
	    $MAX_NODE_WIDTH_RECORDED=$ENTITY_DISSECTION{$entity_db_id}{dimensions}[0];
        }#if($MAX_NODE_WIDTH_RECORDED<$ENTITY_DISSECTION{$entity_db_id}{dimensions}[0]){

	if($MAX_NODE_HEIGHT_RECORDED<$ENTITY_DISSECTION{$entity_db_id}{dimensions}[1]){
            $MAX_NODE_HEIGHT_RECORDED=$ENTITY_DISSECTION{$entity_db_id}{dimensions}[1];
        }#if($MAX_NODE_HEIGHT_RECORDED<$ENTITY_DISSECTION{$entity_db_id}{dimensions}[1]){


	#The Graphviz map is built by connecting entity nodes to their reaction node.

	if($rltn ne 'PE'){
	    if(defined($REACTIONS{$reaction_id}{relation}{$rltn}{$entity_db_id})){
		$REACTIONS{$reaction_id}{relation}{$rltn}{$entity_db_id}++;
	    }else{
		$REACTIONS{$reaction_id}{relation}{$rltn}{$entity_db_id}=1;
	    }
	    
	    if($rltn eq 'Reg'){
		$REACTIONS{$reaction_id}{relation}{$rltn}{$entity_db_id}=$reg_type;
	    }
	}#if
    }# if(!(exists($KILL_LOOKUP_HASH{$entity_name}))){

}#sub add_Entity {
    
sub plot_Network{
    my $max_width = $MAX_NODE_WIDTH_RECORDED*$MINIMUM_CELL_WIDTH;
    my $max_height = $MAX_NODE_HEIGHT_RECORDED*$MINIMUM_CELL_HEIGHT;
    my $node_width;
    my $node_height;
    my $divisor = 1;
    my $entity;
    my $reaction;
    my $rltn;
    my $shape = $COMPLEX_SHAPE;
    my $adjuster = 20;
    my $reaction_count=0;

    foreach $entity (keys %SPECIES){
	$node_width=$MINIMUM_CELL_WIDTH*$ENTITY_DISSECTION{$entity}{dimensions}[0]/$divisor;
	$node_height=$MINIMUM_CELL_HEIGHT*$ENTITY_DISSECTION{$entity}{dimensions}[1]/$divisor;
	$OBJECT_GRAPHVIZ->add_node(name => $entity, shape => $shape, label => $entity, height => $node_height,width => $node_width, fontsize =>'2');#, rank =>$SPECIES{$entity}{"compartment"});   
    }#foreach $entity (keys %SPECIES){

    foreach $reaction (keys %REACTIONS){
	$reaction_count++;
	$REACTIONS{$reaction}{localcount}=$reaction_count;
	$OBJECT_GRAPHVIZ->add_node(name =>$reaction, shape => 'circle', fixedsize => 'true', label =>"e$reaction", height => $MINIMUM_CELL_HEIGHT, width => $MINIMUM_CELL_HEIGHT);
	foreach $rltn (keys %{$REACTIONS{$reaction}{relation}}){
	    foreach $entity (keys %{$REACTIONS{$reaction}{relation}{$rltn}}){
		if($rltn eq 'In'){
		    $OBJECT_GRAPHVIZ->add_edge($entity=>$reaction, label => "In_$REACTIONS{$reaction}{relation}{$rltn}{$entity}", arrowsize => '0.0', fontsize =>'2');
		}elsif($rltn eq 'Out'){
		    $OBJECT_GRAPHVIZ->add_edge($reaction=>$entity, label => "Out_$REACTIONS{$reaction}{relation}{$rltn}{$entity}", arrowsize => '0.0', fontsize =>'2');
		}elsif($rltn eq 'Cat'){
		    $OBJECT_GRAPHVIZ->add_edge($entity=>$reaction, color => "red", label => 'Mod', fontsize =>'2', arrowsize => '0.0');
		}elsif($rltn eq 'Reg'){
		    $OBJECT_GRAPHVIZ->add_edge($entity=>$reaction, label => $REACTIONS{$reaction}{relation}{$rltn}{$entity}, arrowsize => '0.0', fontsize =>'2');
		}
	    }
	}#foreach $rltn (keys %{$EDGE_FILTER{$entity}{$reaction}}){
    }#foreach $reaction (keys %{$EDGE_FILTER{$entity}}){
    return $divisor;
}#sub plot_Network{

sub typeset {
  # Specifies whether an entity is a complex (3), a set (4 or 5), small molecule (2), sequence (1) or none of the latter (0).
  my $entity = $_[0];
  my $entity_db_id = $entity->db_id;
  my @labels;

   if(!(defined($SPECIES{$entity_db_id}{"type"}))){

      $SPECIES{$entity_db_id}{"entity"}=$entity;
      $SPECIES{$entity_db_id}{"db_id"}=$entity_db_id;
      $SPECIES{$entity_db_id}{"name"}=$entity->displayName;
      $SPECIES{$entity_db_id}{"name"}=~s/\"|\'/`/g;
      $SPECIES{$entity_db_id}{"name"}=~s/\"|\'/`/g;
    
      #Non-trivial entities (i.e. complexes or sets) are tested for content before their type is assigned, and are sent on for further dissection
      if($entity->is_a('Complex') && (defined($entity->attribute_value('hasComponent'))) && (defined($entity->attribute_value('hasComponent')->[0]))){
	  $SPECIES{$entity_db_id}{"type"}=3;
	  #The method dissect_Entity returns the width and height of a non-trivial entity in terms of the number of entities found in stack or across it. This function also writes the topology of the non-trivial entity in the hash %ENTITY_DISSECTION.
	  if($COMPLEX){
	      @{$ENTITY_DISSECTION{$entity_db_id}{dimensions}}=dissect_Entity(\%{$ENTITY_DISSECTION{$entity_db_id}{vertical}},$entity,$SPECIES{$entity_db_id}{"type"});
	      $ENTITY_DISSECTION{$entity_db_id}{dimensions}[0]++; #Space for label
	  }
      }elsif($entity->is_a('CandidateSet') && ((defined($entity->attribute_value('hasMember')) && defined($entity->attribute_value('hasMember')->[0])) || (defined($entity->attribute_value('hasCandidate')) && defined($entity->attribute_value('hasCandidate')->[0])))){
	  $SPECIES{$entity_db_id}{"type"}=4;
	  if($COMPLEX){
	      @{$ENTITY_DISSECTION{$entity_db_id}{dimensions}}=dissect_Entity(\%{$ENTITY_DISSECTION{$entity_db_id}{horizontal}},$entity,$SPECIES{$entity_db_id}{"type"});
	      $ENTITY_DISSECTION{$entity_db_id}{dimensions}[1]++; #Space for label
	  }
      }elsif($entity->is_a('DefinedSet') && (defined($entity->attribute_value('hasMember'))) && (defined($entity->attribute_value('hasMember')->[0]))){
	  $SPECIES{$entity_db_id}{"type"}=5;
	  if($COMPLEX){
	      @{$ENTITY_DISSECTION{$entity_db_id}{dimensions}}=dissect_Entity(\%{$ENTITY_DISSECTION{$entity_db_id}{horizontal}},$entity,$SPECIES{$entity_db_id}{"type"});
	      $ENTITY_DISSECTION{$entity_db_id}{dimensions}[1]++; #Space for label
	  }
      }elsif($entity->is_a('EntityWithAccessionedSequence')){
	  $SPECIES{$entity_db_id}{"type"}=1; #sequence
	  @labels = &provide_Label_Name($entity,$SPECIES{$entity_db_id}{"type"});
	  $ENTITY_DISSECTION{$entity_db_id}{genmapp}{name}=$labels[0];
	  $ENTITY_DISSECTION{$entity_db_id}{genmapp}{accession}=$labels[1];
	  $ENTITY_DISSECTION{$entity_db_id}{genmapp}{type}=$SPECIES{$entity_db_id}{"type"};
      }elsif(($entity->is_valid_attribute('referenceEntity')) && defined($entity->ReferenceEntity->[0])){
	  if(($entity->ReferenceEntity->[0]->is_a('ReferenceMolecule'))||($entity->ReferenceEntity->[0]->is_a('ReferenceGroup'))){
	      $SPECIES{$entity_db_id}{"type"}=2; #small molecule
	      @labels = &provide_Label_Name($entity,$SPECIES{$entity_db_id}{"type"});
	      $ENTITY_DISSECTION{$entity_db_id}{genmapp}{name}=$labels[0];
	      $ENTITY_DISSECTION{$entity_db_id}{genmapp}{accession}=$labels[1];
	      $ENTITY_DISSECTION{$entity_db_id}{genmapp}{type}=$SPECIES{$entity_db_id}{"type"};
	  }elsif($entity->ReferenceEntity->[0]->is_a('ReferenceSequence')){
	      $SPECIES{$entity_db_id}{"type"}=1; #sequence
	      @labels = &provide_Label_Name($entity,$SPECIES{$entity_db_id}{"type"});
	      $ENTITY_DISSECTION{$entity_db_id}{genmapp}{name}=$labels[0];
	      $ENTITY_DISSECTION{$entity_db_id}{genmapp}{accession}=$labels[1];
	      $ENTITY_DISSECTION{$entity_db_id}{genmapp}{type}=$SPECIES{$entity_db_id}{"type"};
	  }
      }else{
	  $SPECIES{$entity_db_id}{"type"}=0;
	  @labels = &provide_Label_Name($entity,$SPECIES{$entity_db_id}{"type"});
          $ENTITY_DISSECTION{$entity_db_id}{"genmapp"}{name}=$labels[0];
          $ENTITY_DISSECTION{$entity_db_id}{"genmapp"}{accession}=$labels[1];
	  $ENTITY_DISSECTION{$entity_db_id}{genmapp}{type}=$SPECIES{$entity_db_id}{"type"};
      }
  }# if(!(defined($SPECIES{$entity_db_id}{"type"}))){

  #If we're dealing with a trivial entity, set width and height to 1.
  if($SPECIES{$entity_db_id}{"type"}<3){
      $ENTITY_DISSECTION{$entity_db_id}{dimensions}[0]=1;#Width
      $ENTITY_DISSECTION{$entity_db_id}{dimensions}[1]=1;#Height
  }#if($SPECIES{$entity_db_id}{"type"}<3){
  return $SPECIES{$entity_db_id}{"type"};

}#sub typeset {

sub dissect_Entity {
    #Recursive function that dissects non-trivial (i.e. complexes and sets) entities into trivial ones, keeping track of the structural topology and dimensions of smaler non-trivial entties as it goes along.
    my $ref_higher_structure = $_[0];#This is the parent node in the %ENTITY_DISSECTION tree to append to. 
    my $entity = $_[1];
    my $entity_type = $_[2];#This tells the function whether node growth is horizontal or vertical.
    my $local_width = 0;
    my $local_height = 0;
    my @returned_dimensions;
    my $dissection_results;
    my %component_counter = ();
    my $component_order = 0;
    my %main_component_counter = ();

    if($entity_type==3){
	#COMPLEX
	#Extract all trivial structures from this complex and any of its component subcomplexes. 
	my  $d_results = $entity->follow_class_attributes3
         (-INSTRUCTIONS =>
          {'Complex' => {'attributes' => [qw(hasComponent)]}},
          -OUT_CLASSES => [qw(EntityWithAccessionedSequence OtherEntity Polymer SimpleEntity OpenSet GenomeEncodedEntity DefinedSet CandidateSet)]
          );

	map{
            if(!(defined($main_component_counter{$_->db_id}{count}))){
                $main_component_counter{$_->db_id}{count}=1;
                $main_component_counter{$_->db_id}{order}=$component_order;
                $component_order++;
            }
        }@{$d_results};

	$dissection_results = $entity->follow_class_attributes3
         (-INSTRUCTIONS =>
          {'Complex' => {'attributes' => [qw(hasComponent)]}},
          -OUT_CLASSES => [qw(EntityWithAccessionedSequence OtherEntity Polymer SimpleEntity OpenSet GenomeEncodedEntity)]
          );

	%component_counter = ();
	$component_order = 0;

	#Keep count of the stoichiometry of the trivial entities.
	map{
	    if(defined($component_counter{$_->db_id}{count})){
		$component_counter{$_->db_id}{count}++;
		
	    }else{
		$component_counter{$_->db_id}{count}=1;
		$component_counter{$_->db_id}{order}=$main_component_counter{$_->db_id}{order};
#                $component_order++;
	    }
	    #Function simple_Type returns 1 for sequence, 2 for small molecule, and 0 if not. 
	    $component_counter{$_->db_id}{type}=simple_Type($_);
	    $component_counter{$_->db_id}{entity}=$_;
	}@{$dissection_results};

	#Stored the info required for final labelling - an array containing [0] stoichiometry and [1] type of trivial entity (for colour coding). 
	map{
	    $local_height++; #With each new component in the stack, unit height is incremented.
	    @{$ref_higher_structure->{$_}{label}}=(&provide_Label_Name($component_counter{$_}{entity},$component_counter{$_}{type}),$component_counter{$_}{type});
	    $ref_higher_structure->{$_}{order}=$component_counter{$_}{order};
	    if($component_counter{$_}{count}>1){
		$ref_higher_structure->{$_}{label}[0]=$component_counter{$_}{count}.'x'.$ref_higher_structure->{$_}{label}[0];
	    }
	}keys %component_counter;

	#Now extract any set that is a component of this complex or any of its component subcomplexes.
	$dissection_results = $entity->follow_class_attributes3
         (-INSTRUCTIONS =>
          {'Complex' => {'attributes' => [qw(hasComponent)]}},
          -OUT_CLASSES => [qw(DefinedSet CandidateSet)]
          );

	%component_counter = ();

        #Keep count of the stoichiometry of the set entities.
        map{
            if(defined($component_counter{$_->db_id}{count})){
                $component_counter{$_->db_id}{count}++;
            }else{
		$component_counter{$_->db_id}{order}=$main_component_counter{$_->db_id}{order};
#                $component_order++;
                $component_counter{$_->db_id}{count}=1;
		$component_counter{$_->db_id}{entity}=$_;
            }
        }@{$dissection_results};
	
	map{
	    $local_height++;#Making more space for sets
	    if($component_counter{$_}{count}>1){
		$ref_higher_structure->{$_}{count}=$component_counter{$_}{count};
	    }
	    $ref_higher_structure->{$_}{order}=$component_counter{$_}{order};
	    if($component_counter{$_}{entity}->is_a('CandidateSet') && ((defined($component_counter{$_}{entity}->attribute_value('hasMember')) && defined($component_counter{$_}{entity}->attribute_value('hasMember')->[0])) || (defined($component_counter{$_}{entity}->attribute_value('hasCandidate')) && defined($component_counter{$_}{entity}->attribute_value('hasCandidate')->[0])))){
		@{$ref_higher_structure->{$_}{dimensions}}=dissect_Entity(\%{$ref_higher_structure->{$_}{horizontal}},$component_counter{$_}{entity},4);#Dissecting the Candidate set, and making note of its dimensions, which....
		@returned_dimensions=@{$ref_higher_structure->{$_}{dimensions}};
		#....may have to adjust those of the complex housing this set. Width: $returned_dimensions[0], and Height: $returned_dimensions[1].
		if($local_width<$returned_dimensions[0]){$local_width=$returned_dimensions[0];}
		$local_height=$local_height+$returned_dimensions[1]-1;
	    }elsif ($component_counter{$_}{entity}->is_a('DefinedSet') && (defined($component_counter{$_}{entity}->attribute_value('hasMember'))) && (defined($component_counter{$_}{entity}->attribute_value('hasMember')->[0]))){
		#Same treatment for DefinedSet - code is kept separate in case CandidateSets are to receive different representation settings in future.
		@{$ref_higher_structure->{$_}{dimensions}}=dissect_Entity(\%{$ref_higher_structure->{$_}{horizontal}},$component_counter{$_}{entity},5);
		@returned_dimensions=@{$ref_higher_structure->{$_}{dimensions}};
		if($local_width<$returned_dimensions[0]){$local_width=$returned_dimensions[0];}
		$local_height=$local_height+$returned_dimensions[1]-1;
	    }else{
		#In case set is empty # this is an uncertain portion of logic and may have to be revised
		$ref_higher_structure->{$_}{label}[2]=simple_Type($component_counter{$_}{entity});
		$ref_higher_structure->{$_}{label}[0]=$ENTITY_DISSECTION{$_}{genmapp}{name};
		$ref_higher_structure->{$_}{label}[1]=$ENTITY_DISSECTION{$_}{genmapp}{accession};
	    }
	}keys %component_counter;

	#Catch-all if complex is completely made of trivial components.
	if($local_width==0){$local_width=1;}
	if($local_height==0){$local_height=1;}
	#END OF 'COMPLEX'
    }elsif($entity_type==4 || $entity_type==5){
	#SET
	#Extract all trivial entities and complexes, eliminating redundancies
	$dissection_results = $entity->follow_class_attributes
         (-INSTRUCTIONS =>
          {'DefinedSet' => {'attributes' => [qw(hasMember)]},
	  'CandidateSet' => {'attributes' => [qw(hasMember hasCandidate)]}},
          -OUT_CLASSES => [qw(EntityWithAccessionedSequence OtherEntity Polymer SimpleEntity OpenSet Complex GenomeEncodedEntity)]
          );

	  %component_counter = ();

	  map{
	      $component_counter{$_->db_id}{entity}=$_;
	      $component_counter{$_->db_id}{type}=simple_Type($_)
	  }@{$dissection_results};

	  map{
	      $local_width++;#Increase width with each member/candidate
	      if($component_counter{$_}{entity}->is_a('Complex') && (defined($component_counter{$_}{entity}->attribute_value('hasComponent'))) && (defined($component_counter{$_}{entity}->attribute_value('hasComponent')->[0]))){
		  #If a complex entity is encountered, dissect it further and make not of its dimensions....
		  @{$ref_higher_structure->{$_}{dimensions}}=dissect_Entity(\%{$ref_higher_structure->{$_}{vertical}},$component_counter{$_}{entity},3);
		  #...in order to adjust accordingly.
		  @returned_dimensions=@{$ref_higher_structure->{$_}{dimensions}};
		  if($local_height<$returned_dimensions[1]){$local_height=$returned_dimensions[1];}
		  $local_width=$local_width+$returned_dimensions[0]-1;
	      }else{
		  @{$ref_higher_structure->{$_}{label}}=&provide_Label_Name($component_counter{$_}{entity},$component_counter{$_}{type});
		  $ref_higher_structure->{$_}{label}[2]=$component_counter{$_}{type};
	      }
	  }keys %component_counter;

	if($local_height==0){$local_height=1;}
	if($local_width==0){$local_width=1;}
	#END OF 'SET'
    }

    return ($local_width,$local_height);

}# sub dissect_Entity {

sub draw_Entity {

    my $ref_hash_frame_coordinates = $_[0];
    my $ref_hash_higher_structure = $_[1];
    my $ref_array_dimensions = $_[2];
    my $dir = $_[3];
    my %local_frame_coordinates = %{$ref_hash_frame_coordinates};


    my $x_step = ($ref_hash_frame_coordinates->{x2} - $ref_hash_frame_coordinates->{x1})/($ref_array_dimensions->[0]);
    my $y_step = ($ref_hash_frame_coordinates->{y2} - $ref_hash_frame_coordinates->{y1})/($ref_array_dimensions->[1]);
    my $step = 0;
    my $separator_thickness = 1;
    my $width_for_set_count_marker = 10;
    my $adjust_width=0;
  
    if ($dir eq 'v'){

	my @keys_in_order = ();
        my %order_hash = ();
        foreach my $key (keys %{$ref_hash_higher_structure}){
            $order_hash{$ref_hash_higher_structure->{$key}{order}}=$key;
        }

        foreach my $key (sort keys %order_hash){
            push(@keys_in_order,$order_hash{$key});
        }

#	foreach my $key (keys %{$ref_hash_higher_structure}){
      	foreach my $key (@keys_in_order){
	    map {
		$adjust_width=0;
		if($_ eq 'label'){
		    $OBJECT_GD_IMAGE->filledRectangle($ref_hash_frame_coordinates->{x1},$ref_hash_frame_coordinates->{y1}+($step*$y_step),$ref_hash_frame_coordinates->{x2},$ref_hash_frame_coordinates->{y1}+(($step+1)*$y_step),$COLOUR_PALETTE[$ref_hash_higher_structure->{$key}{$_}[2]]);
		    $OBJECT_GD_IMAGE->rectangle($ref_hash_frame_coordinates->{x1},$ref_hash_frame_coordinates->{y1}+($step*$y_step),$ref_hash_frame_coordinates->{x2},$ref_hash_frame_coordinates->{y1}+(($step+1)*$y_step),$BLACK);
 		    if($ALLOW_TEXT){$OBJECT_GD_IMAGE->string(gdSmallFont,$ref_hash_frame_coordinates->{x1}+2,$ref_hash_frame_coordinates->{y1}+($step*$y_step)+2,$ref_hash_higher_structure->{$key}{$_}[0],$BLACK);}
		    push(@ENTITY_IMAGE_MAP_COORDS,{x1 => $ref_hash_frame_coordinates->{x1}, y1 => $ref_hash_frame_coordinates->{y1}+($step*$y_step), x2 => $ref_hash_frame_coordinates->{x2},  y2 => $ref_hash_frame_coordinates->{y1}+(($step+1)*$y_step), name => $ref_hash_higher_structure->{$key}{$_}[0], db_id => $key, accession => $ref_hash_higher_structure->{$key}{$_}[1]});
		    #print IMGENMAPP &print_GenMAPP('Rectangle',$ref_hash_frame_coordinates->{x1},$ref_hash_frame_coordinates->{y1}+($step*$y_step),$ref_hash_frame_coordinates->{x2},$ref_hash_frame_coordinates->{y1}+(($step+1)*$y_step),$ref_hash_higher_structure->{$key}{$_}[0], $ref_hash_higher_structure->{$key}{$_}[1],$ref_hash_higher_structure->{$key}{$_}[2]);
		    $step++;
		}elsif($_ eq 'vertical'){
		    $local_frame_coordinates{y1}=$ref_hash_frame_coordinates->{y1}+($step*$y_step);
		    $step+=$ref_hash_higher_structure->{$key}{dimensions}[1];
		    $local_frame_coordinates{y2}=$ref_hash_frame_coordinates->{y1}+($step*$y_step);
		    &draw_Entity(\%local_frame_coordinates,\%{$ref_hash_higher_structure->{$key}{$_}},\@{$ref_hash_higher_structure->{$key}{dimensions}},'v');
		}elsif($_ eq 'horizontal'){
		    $local_frame_coordinates{y1}=$ref_hash_frame_coordinates->{y1}+($step*$y_step);
                    $step+=$ref_hash_higher_structure->{$key}{dimensions}[1];
                    $local_frame_coordinates{y2}=$ref_hash_frame_coordinates->{y1}+($step*$y_step);
		    if((defined($ref_hash_higher_structure->{$key}{count}))&&($ref_hash_higher_structure->{$key}{count}>1)){
                        $OBJECT_GD_IMAGE->filledRectangle($local_frame_coordinates{x1}+$separator_thickness,$local_frame_coordinates{y1}+$separator_thickness,$local_frame_coordinates{x1}+$width_for_set_count_marker-$separator_thickness,$local_frame_coordinates{y2}-$separator_thickness,$BLUE);
			if($ALLOW_TEXT){$OBJECT_GD_IMAGE->string(gdMediumBoldFont,$local_frame_coordinates{x1}+$separator_thickness+2,$local_frame_coordinates{y1}+(($local_frame_coordinates{y2}-$local_frame_coordinates{y1})/5),$ref_hash_higher_structure->{$key}{count},$WHITE);}
			#print IMGENMAPP &print_GenMAPP('Rectangle',$local_frame_coordinates{x1}+$separator_thickness,$local_frame_coordinates{y1}+$separator_thickness,$local_frame_coordinates{x1}+$width_for_set_count_marker-$separator_thickness,$local_frame_coordinates{y2}-$separator_thickness,$ref_hash_higher_structure->{$key}{count},'COUNTER','COUNTER');
			$adjust_width=$width_for_set_count_marker;
                    }#if((defined($ref_hash_higher_structure->{$key}{count}))&&($ref_hash_higher_structure->{$key}{count}>1)){
		    $local_frame_coordinates{x1}+=$adjust_width;
                    &draw_Entity(\%local_frame_coordinates,\%{$ref_hash_higher_structure->{$key}{$_}},\@{$ref_hash_higher_structure->{$key}{dimensions}},'h');
		    $local_frame_coordinates{x1}-=$adjust_width;
		    $OBJECT_GD_IMAGE->filledRectangle($local_frame_coordinates{x1},$local_frame_coordinates{y2}-$separator_thickness,$local_frame_coordinates{x2},$local_frame_coordinates{y2}+$separator_thickness,$BLACK);
		}
	    }keys %{$ref_hash_higher_structure->{$key}};
	}#foreach my $key (keys %{$ref_hash_higher_structure}){
    }elsif($dir eq 'h'){
	foreach my $key (keys %{$ref_hash_higher_structure}){
	    map {
                if($_ eq 'label'){
		    #print $ref_hash_frame_coordinates->{x1}+($step*$x_step)."\n";
		    $OBJECT_GD_IMAGE->filledRectangle($ref_hash_frame_coordinates->{x1}+($step*$x_step),$ref_hash_frame_coordinates->{y1},$ref_hash_frame_coordinates->{x1}+(($step+1)*$x_step),$ref_hash_frame_coordinates->{y2},$COLOUR_PALETTE[$ref_hash_higher_structure->{$key}{$_}[2]]);
		    $OBJECT_GD_IMAGE->rectangle($ref_hash_frame_coordinates->{x1}+($step*$x_step),$ref_hash_frame_coordinates->{y1},$ref_hash_frame_coordinates->{x1}+(($step+1)*$x_step),$ref_hash_frame_coordinates->{y2},$BLACK);
	 	    if($ALLOW_TEXT){$OBJECT_GD_IMAGE->string(gdSmallFont,$ref_hash_frame_coordinates->{x1}+($step*$x_step)+2,$ref_hash_frame_coordinates->{y1}+2,$ref_hash_higher_structure->{$key}{$_}[0],$BLACK);}
		    push(@ENTITY_IMAGE_MAP_COORDS,{x1 => $ref_hash_frame_coordinates->{x1}+($step*$x_step), y1 => $ref_hash_frame_coordinates->{y1}, x2 => $ref_hash_frame_coordinates->{x1}+(($step+1)*$x_step),  y2 => $ref_hash_frame_coordinates->{y2}, name => $ref_hash_higher_structure->{$key}{$_}[0], db_id => $key, accession => $ref_hash_higher_structure->{$key}{$_}[1]});
		    #print IMGENMAPP &print_GenMAPP('Rectangle',$ref_hash_frame_coordinates->{x1}+($step*$x_step),$ref_hash_frame_coordinates->{y1},$ref_hash_frame_coordinates->{x1}+(($step+1)*$x_step),$ref_hash_frame_coordinates->{y2},$ref_hash_higher_structure->{$key}{$_}[0],$ref_hash_higher_structure->{$key}{$_}[1],$ref_hash_higher_structure->{$key}{$_}[2]);
		    $step++;
                }elsif($_ eq 'vertical'){
                    $local_frame_coordinates{x1}=$ref_hash_frame_coordinates->{x1}+($step*$x_step);
                    $step+=$ref_hash_higher_structure->{$key}{dimensions}[0];
                    $local_frame_coordinates{x2}=$ref_hash_frame_coordinates->{x1}+($step*$x_step);
                    &draw_Entity(\%local_frame_coordinates,\%{$ref_hash_higher_structure->{$key}{$_}},\@{$ref_hash_higher_structure->{$key}{dimensions}},'v');
		    $OBJECT_GD_IMAGE->filledRectangle($local_frame_coordinates{x2}-$separator_thickness,$local_frame_coordinates{y1},$local_frame_coordinates{x2}+$separator_thickness,$local_frame_coordinates{y2},$BLACK);
                }elsif($_ eq 'horizontal'){
                    $local_frame_coordinates{x1}=$ref_hash_frame_coordinates->{x1}+($step*$x_step);
                    $step+=$ref_hash_higher_structure->{$key}{dimensions}[0];
                    $local_frame_coordinates{x2}=$ref_hash_frame_coordinates->{x1}+($step*$x_step);
                    &draw_Entity(\%local_frame_coordinates,\%{$ref_hash_higher_structure->{$key}{$_}},\@{$ref_hash_higher_structure->{$key}{dimensions}},'h');
                }
	    }keys %{$ref_hash_higher_structure->{$key}};
        }#foreach my $key (keys %{$ref_hash_higher_structure}){
    }

}# sub draw_Entity {

sub print_GenMAPP{

    my $shape = $_[0];
    my $x1 = $_[1];
    my $y1 = $_[2];
    my $x2 = $_[3];
    my $y2 = $_[4];
    my $label = $_[5];
    my $accession = $_[6];
    my $type = $_[7];
    my $width;
    my $height;
    my $centre_x;
    my $centre_y;
    my $print_string='';

    if($shape eq 'Rectangle'){
	$width=int($x2-$x1+1);
	$height=int($y2-$y1+1);
	$centre_x=int(($x2+$x1)/2);
	$centre_y=int(($y2+$y1)/2);
	$print_string="$shape\t$centre_x\t$centre_y\t$width\t$height\t$label\t$accession\t$type\n";
    }

return $print_string;

}#sub print_GenMAPP

sub simple_Type {

    my $entity = $_[0];
    my $type = 0;

    if($entity->is_a('EntityWithAccessionedSequence')){
	$type = 1;
    }elsif(($entity->is_valid_attribute('referenceEntity')) && defined($entity->ReferenceEntity->[0])){
	if(($entity->ReferenceEntity->[0]->is_a('ReferenceMolecule'))||($entity->ReferenceEntity->[0]->is_a('ReferenceGroup'))){
	    $type = 2;
	}elsif($entity->ReferenceEntity->[0]->is_a('ReferenceSequence')){
	    $type = 1;
	}
    }

    $ENTITY_DISSECTION{$entity->db_id}{genmapp}{type}=$type;

    if(!(defined($ENTITY_DISSECTION{$entity->db_id}{dimensions}[0]))){
	$ENTITY_DISSECTION{$entity->db_id}{dimensions}[0]=1;
	$ENTITY_DISSECTION{$entity->db_id}{dimensions}[1]=1;
	($ENTITY_DISSECTION{$entity->db_id}{genmapp}{name},$ENTITY_DISSECTION{$entity->db_id}{genmapp}{accession})=&provide_Label_Name($entity,$type);
    }
    return $type;
}# sub simple_Type {

sub provide_Label_Name {
    
    my $entity = $_[0];
    my $type = $_[1];
    my $name = '';
    my $temp = '';
    my $sub_string = 6;
    my $accession = '';

    if($type==1){
	if(defined($entity->attribute_value('referenceEntity')->[0])){
	    foreach $temp (@{$entity->attribute_value('referenceEntity')->[0]->attribute_value('geneName')}){
		if($name eq '' || length($temp)<length($name)){$name=$temp;}
	    }#foreach
	    $accession ="UNIPROT\:".$entity->attribute_value('referenceEntity')->[0]->attribute_value('identifier')->[0].' in '.$entity->displayName;
	}
    }elsif($type==2){
	if(defined($entity->attribute_value('referenceEntity')->[0])){
	    foreach $temp (@{$entity->attribute_value('referenceEntity')->[0]->attribute_value('name')}){
		if($name eq '' || length($temp)<length($name)){$name=$temp;}
	    }#foreach
		$accession ="CHEBI\:".$entity->attribute_value('referenceEntity')->[0]->attribute_value('identifier')->[0].' in '.$entity->displayName;
	}
    }

    if($name eq ''){
	$name = $entity->displayName;
	$name =~s/\[.*//g;
    }
    $name = substr($name,0,$sub_string);
    if($accession eq ''){
	$accession = $entity->displayName;
    }

    $name=~s/\"|\'/`/g;
    $accession=~s/\"|\'/`/g;

    return ($name,$accession);
}#sub provide_Label_Name {

sub draw_Label{

    my $label_coords = $_[0];
    my $dir = $_[1];
    my $name = $_[2];
    my $text_width=0;
    my $text_height=0;

    $OBJECT_GD_IMAGE->filledRectangle($label_coords->{x1},$label_coords->{y1},$label_coords->{x2},$label_coords->{y2},$GREY);
    $OBJECT_GD_IMAGE->rectangle($label_coords->{x1},$label_coords->{y1},$label_coords->{x2},$label_coords->{y2},$BLACK);

    if($dir eq 'h'){
	$text_width=($label_coords->{x2})-($label_coords->{x1});
	$text_height=($label_coords->{y2})-($label_coords->{y1});
	&fit_Label($name,$text_width,$text_height,$dir,$label_coords);
    }else{
	$text_width=($label_coords->{y2})-($label_coords->{y1});
        $text_height=($label_coords->{x2})-($label_coords->{x1});
	&fit_Label($name,$text_width,$text_height,$dir,$label_coords);
    }

}#sub draw_Label{

sub fit_Label{

    my $name = $_[0];
    my $text_width=$_[1];
    my $text_height=$_[2];
    my $dir = $_[3];
    my $label_coords = $_[4];
    my $width_pixels_per_char=7;
    my $height_pixels_per_char=10;
    my $width_current;
    my $height_current;
    my $name_length = length($name);
    my $text_chunk_length =int(($text_width-10)/$width_pixels_per_char);
    my $position_in_string=0;
    my @string;
    my $clever_out_text = '';

    if($name_length<($position_in_string+$text_chunk_length)){$text_chunk_length=$name_length-$position_in_string;}

    if($dir eq 'h' && $ALLOW_TEXT){
	$width_current=$label_coords->{x1}+5;
	$height_current=$label_coords->{y1}+2;

	while($height_current<($label_coords->{y2}-$height_pixels_per_char)){
	    $clever_out_text = '';
	    @string = split(/\s+|\-|\,|\./,substr($name,$position_in_string,$text_chunk_length+1));
	    pop(@string);
	    $clever_out_text = join(" ",@string);
	    if($clever_out_text ne ''){
		$clever_out_text=~s/^\s+//;
		$OBJECT_GD_IMAGE->string(gdMediumBoldFont,$width_current,$height_current,$clever_out_text,$BLACK);
		$position_in_string+=length($clever_out_text)+1;
	    }
	    else{
		$clever_out_text=substr($name,$position_in_string,$text_chunk_length);
		$clever_out_text=~s/^\s+//;
		$OBJECT_GD_IMAGE->string(gdMediumBoldFont,$width_current,$height_current,$clever_out_text,$BLACK);
		$position_in_string+=$text_chunk_length;
	    }
	    $height_current+=$height_pixels_per_char;
	    if($name_length<($position_in_string+$text_chunk_length)){$text_chunk_length=$name_length-$position_in_string;}
	}#while($height_current<$text_height){
    }elsif($dir eq 'v' && $ALLOW_TEXT){
	$width_current=$label_coords->{y2}-5;
        $height_current=$label_coords->{x1}+2;

        while($height_current<($label_coords->{x2}-$height_pixels_per_char)){
	    $clever_out_text = '';
            @string = split(/\s+|\-|\,|\./,substr($name,$position_in_string,$text_chunk_length+1));
            pop(@string);
            $clever_out_text = join(" ",@string);
            if($clever_out_text ne ''){
		$clever_out_text=~s/^\s+//;
                $OBJECT_GD_IMAGE->stringUp(gdMediumBoldFont,$height_current,$width_current,$clever_out_text,$BLACK);
                $position_in_string+=length($clever_out_text)+1;
	    }else{
		$clever_out_text=substr($name,$position_in_string,$text_chunk_length);
                $clever_out_text=~s/^\s+//;
		$OBJECT_GD_IMAGE->stringUp(gdMediumBoldFont,$height_current,$width_current,$clever_out_text,$BLACK);
		$position_in_string+=$text_chunk_length;
	    }
	    $height_current+=$height_pixels_per_char;
            if($name_length<($position_in_string+$text_chunk_length)){$text_chunk_length=$name_length-$position_in_string;}
        }#while($height_current<$text_height){
    }

}#sub fit_Label{

###############################
    1;
###############################
