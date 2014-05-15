package GKB::Ontology;

# Copyright 2002 Imre Vastrik <vastrik@ebi.ac.uk>

use strict;
use vars qw(@ISA $DEFAULT_ROOT_CLASS_NAME $DB_ID_NAME @EXPORT);
use Bio::Root::Root;
use Exporter;
use GKB::Config qw($NO_SCHEMA_VALIDITY_CHECK);
@ISA = qw(Bio::Root::Root Exporter);

$DEFAULT_ROOT_CLASS_NAME = 'DatabaseObject';
$DB_ID_NAME = 'DB_ID';
@EXPORT = qw($DB_ID_NAME);

sub new {
  my($pkg, $classes_hash_r) = @_;
  my $self = bless {}, $pkg;
  if ($classes_hash_r) {
      $self->_classes($classes_hash_r);
      if ((caller(0))[0] =~ /ClipsAdaptor/) {
	  $self->initiate;
      }
  } else {
      $self->_classes({});
  }
  return $self;
}

sub debug {
    my $self = shift;
    if (@_) {
	$self->{'debug'} = shift;
    }
    return $self->{'debug'};
}

sub _classes {
    my $self = shift;
#    $self->debug && print "", (caller(0))[3], "\n";
    if (@_) {
	$self->{'classes'} = shift;
    }
    return $self->{'classes'};
}

sub list_classes {
    my $self = shift;
#    $self->debug && print "", (caller(0))[3], "\n";
    return keys %{$self->{'classes'}};
}

sub get_class_hash {
    my $self = shift;
    my $class = shift || $self->throw("Need class name.");
    $self->{'classes'}->{$class} ||= {};
    return $self->{'classes'}->{$class};
}

sub class {
    my ($self,$arg) = @_;
#    $self->debug && print "", (caller(0))[3], "\n";
    if ($arg) {
	if (ref($arg)) {
	    # For adding new class to ontology. $arg has to be hash reference.
	    if (my $class = $arg->{'class'}) {
		$self->{'classes'}->{$class} = $arg;
	    } else {
		$self->throw("$arg without {'class'}.");
	    }
	} else {
	    # For checking the validity of classname withing given ontology
	    # and extracting it's "sub-hash".
	    if (my $tmp = $self->_classes->{$arg}) {
		return $tmp;
	    } elsif ($tmp = $self->{'protege_classes'}->{$arg}) {
		return $tmp;
	    } else {
		if ($NO_SCHEMA_VALIDITY_CHECK) {
		    return {};
		}
		$self->throw("Unknown class '$arg' for this ontology.");
	    }
	}
    }
}

sub class_attributes {
    my ($self,$cls) = @_;
#    $self->debug && print "", (caller(0))[3], "\n";
    $cls || $self->throw("Need class.");
    return $self->class($cls)->{'attribute'};
}

sub list_class_attributes {
    my ($self,$cls) = @_;
#    $self->debug && print "", (caller(0))[3], "\n";
    $cls || $self->throw("Need class.");
    $self->class_attributes($cls) || $self->throw("No attributes for class $cls!?");
    return keys %{$self->class_attributes($cls)};
}

sub class_referers {
    my ($self,$cl1) = @_;
#    $self->debug && print "", (caller(0))[3], "\n";
    $cl1 || $self->throw("Need class.");
    $self->{'reverse_attribute_found'} || $self->_find_referers;
    my @out;
    foreach my $at (keys %{$self->class($cl1)->{'reverse_attribute'}}) {
	foreach my $cl2 (keys %{$self->class($cl1)->{'reverse_attribute'}->{$at}}) {
	    if ($self->class($cl1)->{'reverse_attribute'}->{$at}->{$cl2}->{'multiple'}) {
		push @out, [$cl2, $at, 1];
	    } else {
		push @out, [$cl2, $at, undef];
	    }
	}
    }
    return \@out;
}

sub class_referers_by_attribute_origin {
    my ($self,$cl1) = @_;
#    $self->debug && print "", (caller(0))[3], "\n";
    $cl1 || $self->throw("Need class.");
    $self->{'reverse_attribute_found'} || $self->_find_referers;
    my @out;
    my %seen;
    foreach my $at (keys %{$self->class($cl1)->{'reverse_attribute'}}) {
	foreach my $cl2 (keys %{$self->class($cl1)->{'reverse_attribute'}->{$at}}) {
	    my $origin = $self->class_attribute_origin($cl2,$at);
	    next if ($seen{$origin}->{$at}++);
	    if ($self->class($origin)->{'attribute'}->{$at}->{'multiple'}) {
		push @out, [$origin, $at, 1];
	    } else {
		push @out, [$origin, $at, undef];
	    }
	}
    }
    return \@out;
}

# find instances which can be fetched by a given instance
sub _find_referers {
    my ($self) = @_;
#    $self->debug && print "", (caller(0))[3], "\n";
    foreach my $cl1 ($self->list_classes){
#	foreach my $at (keys %{$self->class($cl1)->{'own_attribute'}}) {
	# Can't just consider own attributes since subclasses may have
	# overrided the valid values and cardinality.
	foreach my $at (keys %{$self->class($cl1)->{'attribute'}}) {
	    foreach my $cl2 (keys %{$self->class_attributes($cl1)->{$at}->{'allowed'}}) {
		if ($self->class($cl1)->{'attribute'}->{$at}->{'multiple'}) {
		    $self->{'classes'}->{$cl2}->{'reverse_attribute'}->{$at}->{$cl1}->{'multiple'} = 1;
		    foreach my $cl3 ($self->descendants($cl2)) {
			$self->{'classes'}->{$cl3}->{'reverse_attribute'}->{$at}->{$cl1}->{'multiple'} = 1;
		    }
		} else{
		    $self->{'classes'}->{$cl2}->{'reverse_attribute'}->{$at}->{$cl1}->{'multiple'} = undef;
		    foreach my $cl3 ($self->descendants($cl2)) {
			$self->{'classes'}->{$cl3}->{'reverse_attribute'}->{$at}->{$cl1}->{'multiple'} = undef;
		    }
		}
	    }
	}
    }
    $self->{'reverse_attribute_found'} = 1;
}

sub list_own_attributes {
    my ($self,$class) = @_;
#    $self->debug && print "", (caller(0))[3], "\n";
    return keys %{$self->class($class)->{'own_attribute'}};
}

sub list_multivalue_own_attributes {
    my ($self,$class) = @_;
#    $self->debug && print "", (caller(0))[3], "\n";
    return grep {$self->is_multivalue_class_attribute($class,$_)} keys %{$self->class($class)->{'own_attribute'}};
}

sub list_singlevalue_own_attributes {
    my ($self,$class) = @_;
#    $self->debug && print "", (caller(0))[3], "\n";
    return grep {! $self->is_multivalue_class_attribute($class,$_)} keys %{$self->class($class)->{'own_attribute'}};
}

sub is_own_attribute {
    my $self = shift;
    my $class = shift || $self->throw("Need class.");
    my $att = shift || $self->throw("Need attribute.");
#    $self->debug && print "", (caller(0))[3], "\n";
    if ($self->class($class)->{'own_attribute'}) {
	return $self->class($class)->{'own_attribute'}->{$att};
    } else {
	# this bit is for being able to do the check before the 'own_attribute'
	# hash is initialised.
	my $origin = $self->class_attribute_origin($class,$att) || return;
	return $origin eq $class;
    }
}

sub list_allowed_classes_for_class_attribute {
    my $self = shift;
    return $self->list_allowed_classes(@_);
#    my ($self,$class,$attribute) = @_;
#    $self->debug && print "", (caller(0))[3], "\n";
#    return keys %{$self->class_attributes($class)->{$attribute}->{'allowed'}};
}

sub class_attribute_allowed_classes {
    my $self = shift;
    my $class = shift || $self->throw("Need class.");
    my $attribute = shift || $self->throw("Need attribute.");
    foreach (@_) {
	$self->class_attributes($class)->{$attribute}->{'allowed'}->{$_} = 1;
    }
    return keys %{$self->class_attributes($class)->{$attribute}->{'allowed'}};
}

sub is_class_attribute_allowed_class {
    my $self = shift;
    my $class = shift || $self->throw("Need class.");
    my $attribute = shift || $self->throw("Need attribute.");
    my $class2 = shift || $self->throw("Need class.");
    foreach ($self->list_allowed_classes($class,$attribute)) {
	$self->is_a($class2,$_) && return 1;
    }
    return;
}

sub list_allowed_classes_for_class_reverse_attribute {
    my ($self,$class,$attribute) = @_;
#    $self->debug && print "", (caller(0))[3], "\n";
    $self->{'reverse_attribute_found'} || $self->_find_referers;
    return keys %{$self->class($class)->{'reverse_attribute'}->{$attribute}};
}

sub list_allowed_classes_origin_for_class_reverse_attribute {
    my ($self,$class,$attribute) = @_;
#    $self->debug && print "", (caller(0))[3], "\n";
    $self->{'reverse_attribute_found'} || $self->_find_referers;
    my %h;
    foreach my $cls (keys %{$self->class($class)->{'reverse_attribute'}->{$attribute}}) {
	$h{$self->class_attribute_origin($cls,$attribute)}++;
    }
    return keys %h;
}

sub list_class_attributes_allowing_class {
    my ($self,$class1,$class2) = @_;
#    $self->debug && print "", (caller(0))[3], "\n";
    return (grep {$self->class_attributes($class1)->{$_}->{'allowed'}->{$class2}} $self->list_class_attributes($class1));
}

sub initiate {
    my $o = shift;
    $o->debug && print "", (caller(0))[3], "\n";
    $o->_handle_protege_classes;
    $o->_handle_root_classes;
    my @clss = $o->list_classes;
    my %seen;
    while (scalar(keys %seen) < scalar(@clss)) {
      OUTER: foreach my $cls (@clss){
	  $seen{$cls} && next;
#	  print "$cls\t", join(" ", $o->parents($cls)), "\n" unless ($seen{$cls});
	  unless ($o->parents($cls)) {
#	      print "$cls is root class.\n";
	      $seen{$cls}++;
	      $o->class($cls)->{'ancestor_count'} = 0;
	      $o->class($cls)->{'ancestor'} = {};
	      $o->class($cls)->{'ancestor_order'} = [];
	      foreach my $att (keys %{$o->class($cls)->{'attribute'}}) {
		  $o->class($cls)->{'own_attribute'}->{$att} = 1;
#		  print $cls, "\t", $att, "\t", $o->class($cls)->{'attribute'}->{$att}->{'origin'}, "\n";
	      }
	      next;
	  }
	  foreach my $parent (keys %{$o->class($cls)->{'parent'}}) {
	      unless ($seen{$parent}) {
		  next OUTER;
	      }
	  }
	  # if we reach here we know we've seen all parents of the current class
#	  print "$cls is " . join(" ", keys %{$o->class($cls)->{'parent'}}) . "\n";
	  $seen{$cls}++;
	  foreach my $parent (keys %{$o->class($cls)->{'parent'}}) {
	      foreach my $att (keys %{$o->class($parent)->{'attribute'}}) {
		  # copy attribute stuff from parent only if it's not defined for current class
		  $o->class($cls)->{'attribute'}->{$att} ||= $o->class($parent)->{'attribute'}->{$att};
		  # copy attribute origin from parent. This way the origin will allways point to the 1st, i.e. the "oldest"
		  # ancestor which has this attribute.
		  if ($o->class($parent)->{'attribute'}->{$att}->{'origin'}) {
		      $o->class($cls)->{'attribute'}->{$att}->{'origin'} = $o->class($parent)->{'attribute'}->{$att}->{'origin'};
		  }
	      }	      
	      # get ALL ancestors
	      $o->class($cls)->{'ancestor'}->{$parent}++;
	      @{$o->class($cls)->{'ancestor'}}{keys %{$o->class($parent)->{'ancestor'}}} =
		  values(%{$o->class($parent)->{'ancestor'}});
	  }
	  foreach my $att (keys %{$o->class($cls)->{'attribute'}}) {
	      $o->class($cls)->{'own_attribute'}->{$att} = 1 if ($o->class($cls)->{'attribute'}->{$att}->{'origin'} eq $cls);
#	      print $cls, "\t", $att, "\t", $o->class($cls)->{'attribute'}->{$att}->{'origin'}, "\n";
	  }
	  $o->class($cls)->{'ancestor_count'} = scalar(keys %{$o->class($cls)->{'ancestor'}});
	  @{$o->class($cls)->{'ancestor_order'}} =
	      sort { $o->class($a)->{'ancestor_count'} <=> $o->class($b)->{'ancestor_count'} }
	  keys %{$o->class($cls)->{'ancestor'}};
      }
    }
    $o->_handle_defining_attributes;
}

sub _handle_root_classes {
    my $self = shift;
    $self->debug && print "-->", (caller(0))[3], "\n";
    my @roots = grep {! $self->parents($_)} $self->list_classes;
    @roots || $self->throw("No root class! Something very dodgy happening.");
    if (@roots == 1) {
	$self->create_root_class_attributes($roots[0]);
	$self->{'root_class'} = $roots[0];
    } else {
	if ($self->is_valid_class($DEFAULT_ROOT_CLASS_NAME)) {
	    $self->throw("Your class name $DEFAULT_ROOT_CLASS_NAME is identical to internal root class name.");
	}
	$self->get_class_hash($DEFAULT_ROOT_CLASS_NAME);
	$self->create_root_class_attributes($DEFAULT_ROOT_CLASS_NAME);
	foreach my $cls (@roots) {
	    $self->parents($cls,$DEFAULT_ROOT_CLASS_NAME);
	}
	$self->{'root_class'} = $DEFAULT_ROOT_CLASS_NAME;
    }
    $self->debug && print "<--", (caller(0))[3], "\n";
}

sub create_root_class_attributes {
    my $self = shift;
    my $class = shift || $self->throw("Need class");
#    print Data::Dumper->Dumpxs([$self],["$self"]);exit;
    $self->_create_singlevalue_attribute($class,'_class','other_type','VARCHAR(64)');
#    $self->_create_singlevalue_attribute($class,'_Protege_id','other_type','VARCHAR(255)');
    $self->_create_singlevalue_attribute($class,'_displayName','other_type','TEXT');
    $self->_create_singlevalue_attribute($class,$DB_ID_NAME,'db_internal_id_type');
    # Just to be sure that the type is set to 'db_internal_id_type' in case the $DB_ID_NAME
    # is defined in Protege.
    $self->class_attribute_type($class,$DB_ID_NAME,'db_internal_id_type');
#    $self->_create_singlevalue_attribute($class,'_internal1','other_type','VARCHAR(255)');
#    $self->_create_singlevalue_attribute($class,'_internal2','other_type','VARCHAR(255)');
#    $self->_create_singlevalue_attribute($class,'_internal3','other_type','VARCHAR(255)');
#    $self->_create_singlevalue_attribute($class,'_partial','other_type','TINYINT DEFAULT 0 NOT NULL');
#    $self->_create_singlevalue_attribute($class,'_html','other_type','LONGBLOB');
    $self->_create_singlevalue_attribute($class,'_timestamp','other_type','TIMESTAMP');
}

sub _create_singlevalue_attribute {
    my ($self,$class,$attribute,$type,$db_col_type) = @_;
    $class || $self->throw("Need class.");
    $attribute || $self->throw("Need attribute.");
    $type || $self->throw("Need type.");
    unless ($self->is_own_attribute($class,$attribute)) {
	$self->exists_attribute($attribute) && $self->throw("Attribute name '$attribute' is identical to internal attribute name.");
	$self->class_attribute_origin($class,$attribute,$class);
	$self->class_attribute_type($class,$attribute,$type);
	$db_col_type && $self->class_attribute_db_col_type($class,$attribute,$db_col_type);
    }    
}

sub _create_multivalue_attribute {
    my ($self,$class,$attribute,$type,$db_col_type) = @_;
    $class || $self->throw("Need class.");
    $attribute || $self->throw("Need attribute.");
    $type || $self->throw("Need type.");
    unless ($self->is_own_attribute($class,$attribute)) {
	$self->exists_attribute($attribute) && $self->throw("Attribute name '$attribute' is identical to internal attribute name.");
	$self->class_attribute_origin($class,$attribute,$class);
	$self->class_attribute_type($class,$attribute,$type);
	$db_col_type && $self->class_attribute_db_col_type($class,$attribute,$db_col_type);
	$self->is_multivalue_class_attribute($class,$attribute,1);
    }    
}

sub is_root_class {
    my $self= shift;
    my $class = shift || $self->throw("Need class");
    return $class eq $self->root_class;
}

sub root_class {
    my $self= shift;
    return $self->{'root_class'} || $self->throw("Root class not known! Something strange happening.");
}

sub exists_attribute {
    my $self = shift;
    my $att = shift || $self->throw("Need attribute.");
    return $self->class_attributes(':CLIPS_TOP_LEVEL_SLOT_CLASS')->{$att};
}

sub inverse_attribute {
    my $self = shift;
    my $class = shift || $self->throw("Need class.");
    my $attribute = shift || $self->throw("Need attribute.");
    if (@_) {
#	$self->{'protege_classes'}->{':CLIPS_TOP_LEVEL_SLOT_CLASS'}->{'attribute'}->{$attribute}->{'inverse-attribute'} = shift;
	$self->class_attributes($class)->{$attribute}->{'inverse-attribute'} = shift;
    }
#    return $self->{'protege_classes'}->{':CLIPS_TOP_LEVEL_SLOT_CLASS'}->{'attribute'}->{$attribute}->{'inverse-attribute'};
    return $self->class_attributes($class)->{$attribute}->{'inverse-attribute'};
}

sub are_inverse_attributes {
    my $self = shift;
    my $att1 = shift || $self->throw("Need an attribute.");
    my $att2 = shift || $self->throw("Need another attribute.");
    return $att2 eq $self->{'protege_classes'}->{':CLIPS_TOP_LEVEL_SLOT_CLASS'}->{'attribute'}->{$att1}->{'inverse-attribute'};
}

sub _handle_protege_classes {
    my ($self,$hr)= @_;
    $hr ||= $self->_classes;
    foreach my $cls (grep {/^:/ || ($_ eq 'USER')} keys %{$hr}) {
	$self->{'protege_classes'}->{$cls} = $hr->{$cls};
	delete $hr->{$cls};
    }
}

sub _handle_defining_attributes {
    my ($self) = @_;
#    $self->debug && print "", (caller(0))[3], "\n";
    foreach my $class ($self->list_classes) {
      ATTRIBUTE:
	foreach my $attribute ($self->list_class_attributes($class)) {
	    next if (exists $self->class($class)->{'check'}->{'all'}->{$attribute});
	    next if (exists $self->class($class)->{'check'}->{'any'}->{$attribute});
	    foreach my $ancestor (reverse @{$self->class($class)->{'ancestor_order'}}) {
		if (exists $self->class($ancestor)->{'check'}->{'all'}->{$attribute}) {
		    $self->class($class)->{'check'}->{'all'}->{$attribute} = $self->class($ancestor)->{'check'}->{'all'}->{$attribute};
		    next ATTRIBUTE;
		}
		if (exists $self->class($ancestor)->{'check'}->{'any'}->{$attribute}) {
		    $self->class($class)->{'check'}->{'any'}->{$attribute} = $self->class($ancestor)->{'check'}->{'any'}->{$attribute};
		    next ATTRIBUTE;
		}
	    }
	}
    }
    foreach my $class ($self->list_classes) {
	foreach my $attribute ($self->list_class_attributes($class)) {
	    if (exists $self->class($class)->{'check'}->{'all'}->{$attribute} &&
		! $self->class($class)->{'check'}->{'all'}->{$attribute}) {
		delete $self->class($class)->{'check'}->{'all'}->{$attribute};
	    }
	    if (exists $self->class($class)->{'check'}->{'any'}->{$attribute} &&
		! $self->class($class)->{'check'}->{'any'}->{$attribute}) {
		delete $self->class($class)->{'check'}->{'any'}->{$attribute};
	    }
	}
    }
}

sub children {
    my ($self,$class) = @_;
#    $self->debug && print "", (caller(0))[3], "\n";
    return grep {$self->class($_)->{'parent'}->{$class}} $self->descendants($class);
}

sub descendants {
    my ($self,$class) = @_;
#    $self->debug && print "", (caller(0))[3], "\n";
    my @descendants;
    foreach my $cls ($self->list_classes) {
	push @descendants, $cls if (defined $self->class($cls)->{'ancestor'}->{$class});
    }
    return @descendants;
}

sub descendants_with_own_attribute {
    my ($self,$class,$att) = @_;
    return grep {$self->is_own_attribute($_,$att)} $self->descendants($class);
}

sub parents {
    my $self = shift;
    my $class = shift || $self->throw("Need class");
    foreach (@_) {
	$self->get_class_hash($class)->{'parent'}->{$_} = 1;
    }
    if ($self->class($class)->{'parent'}) {
	return keys %{$self->class($class)->{'parent'}};
    }
    return;
}

sub class_attribute_type {
    my $self = shift;
#    $self->debug && print "", (caller(0))[3], "\n";
    my $class = shift || $self->throw("Need class");
    my $attribute = shift || $self->throw("Need attribute");
    $self->check_class_attribute($class,$attribute);
    if (@_) {
	$self->get_class_hash($class)->{'attribute'}->{$attribute}->{'type'} = shift;
    }
    $self->class($class)->{'attribute'}->{$attribute} && return $self->class($class)->{'attribute'}->{$attribute}->{'type'};
}

sub list_allowed_classes {
    my ($self,$class,$attribute) = @_;
#    $self->debug && print "", (caller(0))[3], "\n";
    $class || $self->throw("Need class");
    $attribute || $self->throw("Need attribute");
    return keys %{$self->class_attributes($class)->{$attribute}->{'allowed'}};
}

# Also used for checking the validity of the class name. Hence the trickery with $_[1].
sub check_class_attribute {
    my $self = shift;
#    $self->debug && print "", (caller(0))[3], "\n";
#    @_ || return;
    $self->class($_[0]);
    defined $_[1] || return;
    $self->class($_[0])->{'attribute'}->{$_[1]} ||
	$NO_SCHEMA_VALIDITY_CHECK || 
	$self->throw("Attribute '$_[1]' is not a valid attribute for class '$_[0]'.") ;
}

sub is_valid_class {
    my ($self,$class) = @_;
    $class || $self->throw("Need class.");
    return $self->{'classes'}->{$class};
}

sub is_valid_class_attribute {
    my $self = shift;
#    $self->debug && print "", (caller(0))[3], "\n";
    return $self->class($_[0])->{'attribute'}->{$_[1]};
}

sub is_valid_attribute_of_class_or_descendants {
    my ($self,$cls,$att) = @_;
#    $self->debug && print "", (caller(0))[3], "\n";
    foreach my $desc ($cls, $self->descendants($cls)) {
	$self->class($desc)->{'attribute'}->{$att} && return 1;
    }
    return;
}

sub list_ancestors {
    my ($self,$class) = @_;
#    $self->debug && print "", (caller(0))[3], "\n";
    return @{$self->class($class)->{'ancestor_order'}};
}

sub list_recursive_attributes {
    my ($self,$class) = @_;
#    $self->debug && print "", (caller(0))[3], "\n";
    my @out;
    ATT: foreach my $att ($self->list_class_attributes($class)) {
	foreach my $cls ($self->list_allowed_classes($class,$att)) {
	    if ($self->is_a($cls,$class) || $self->is_a($class,$cls)) {
		push @out, $att;
		next ATT;
	    }
	}
    }
    return @out;
}

sub is_recursive_class_attribute {
    my ($self,$class,$attribute) = @_;
    $self->check_class_attribute($class,$attribute);
    map {($self->is_a($_,$class) || $self->is_a($class,$_)) && return 1} $self->list_allowed_classes($class,$attribute);
    return;
}

sub is_abstract_class {
    my $self = shift;
#    $self->debug && print "", (caller(0))[3], "\n";
    my $class = shift || $self->throw("Need class.");
    if (@_) {
	$self->get_class_hash($class)->{'role'}->{'abstract'} = 1;
    }
    return $self->class($class)->{'role'}->{'abstract'};
}

sub is_multivalue_class_attribute {
    my $self = shift;
#    $self->debug && print "", (caller(0))[3], "\n";
    my $class = shift || $self->throw("Need class.");
    my $attribute = shift || $self->throw("Need attribute.");
    if (@_) {
	$self->get_class_hash($class)->{'attribute'}->{$attribute}->{'multiple'} = shift;
    }
    return $self->class($class)->{'attribute'}->{$attribute}->{'multiple'};
}

# Args:
# 1. Supposed sub-class.
# 2. Supposed super-class.
sub is_a {
    my ($self,$c1,$c2) = @_;
    return 1 if ($c1 eq $c2);
    return $self->class($c1)->{'ancestor'}->{$c2};
}

sub list_joining_attributes {
    my ($self,$class,$value_class) = @_;
#    $self->debug && print "", (caller(0))[3], "\n";
#    print "$class,$value_class\n";
    return (grep{$self->class_attributes($class)->{$_}->{'allowed'}->{$value_class}} $self->list_class_attributes($class));
}

sub class_attribute_origin {
    my ($self,$class,$attribute,$origin) = @_;
#    $self->debug && print "", (caller(0))[3], "\n";
    if ($origin) {
	$self->get_class_hash($class)->{'attribute'}->{$attribute}->{'origin'} = $origin;
    }
    return $self->class($class)->{'attribute'}->{$attribute}->{'origin'};
}

# Finds a set of paths that allow you to go from a start
# class (class1) to a destination class (class2).  Tries
# each attribute in class 1 as a starting point for paths.
# Returns a reference to a list of paths.  Each path is a
# list of alternating attribute names and instance class
# names.  The attribute names give you the actual path,
# the instance class names give you extra (redundant) information
# about the type of the attribute.
sub path_from_class_to_class_all_attributes {
    my ($self,$class1,$class2) = @_;
    $self->check_class_attribute($class1);
    $self->check_class_attribute($class2);
    my (%seen,@paths,%seen_paths);
    $self->_path_from_class_to_class_all_attributes($class1,$class2,\%seen,\%seen_paths,\@paths);
    return \@paths;
}

sub _path_from_class_to_class_all_attributes {
    my ($self,$current,$class2,$seen,$seen_paths,$paths,@path) = @_;
    
    if ($current eq $class2) {
		push @{$paths}, \@path;
		for (my $i = 1; $i < @path; $i += 2) {
		    push @{$seen_paths->{$path[$i]}}, [@path[$i + 1 .. $#path]];
		}
		return;
    } elsif ($seen->{$current}++) {
		foreach (@{$seen_paths->{$current}}) {
		    push @{$paths}, [@path, @{$_}];
		}
		return;
    }
    
    my @attributes = $self->list_class_attributes($current);
    foreach my $attribute (grep {$self->class_attribute_type($current,$_) eq 'db_instance_type'} @attributes) {
		foreach my $class ($self->list_allowed_classes_for_class_attribute($current,$attribute)) {
# 	    	foreach ($class) {
		    foreach ($class,$self->descendants($class)) {
				$self->_path_from_class_to_class_all_attributes($_,$class2,$seen,$seen_paths,$paths,@path,$attribute,$_);
		    }
		}
    }
}

sub path_from_class_to_class {
    my ($self,$class1,$class2) = @_;
#    $self->debug && print "", (caller(0))[3], "\n";
    $self->check_class_attribute($class1);
    $self->check_class_attribute($class2);
    my (%seen,@paths,%seen_paths);
    $self->_path_from_class_to_class($class1,$class2,\%seen,\%seen_paths,\@paths);
    return \@paths;
}

sub _path_from_class_to_class {
    my ($self,$current,$class2,$seen,$seen_paths,$paths,@path) = @_;
#    $self->debug && print "", (caller(0))[3], "\n";
    if ($current eq $class2) {
		push @{$paths}, \@path;
		for (my $i = 1; $i < @path; $i += 2) {
		    push @{$seen_paths->{$path[$i]}}, [@path[$i + 1 .. $#path]];
		}
		return;
    } elsif ($seen->{$current}++) {
		foreach (@{$seen_paths->{$current}}) {
		    push @{$paths}, [@path, @{$_}];
		}
		return;
    }
    foreach my $attribute (grep {$self->class_attribute_type($current,$_) eq 'db_instance_type'}
                           $self->list_own_attributes($current)) {
		foreach my $class ($self->list_allowed_classes_for_class_attribute($current,$attribute)) {
# 	    	foreach ($class) {
		    foreach ($class,$self->descendants($class)) {
				$self->_path_from_class_to_class($_,$class2,$seen,$seen_paths,$paths,@path,$attribute,$_);
		    }
		}
    }
}

sub class_attribute_db_col_type {
    my $self = shift;
#    $self->debug && print "", (caller(0))[3], "\n";
    my $class = shift || $self->throw("Need class.");
    my $attribute = shift || $self->throw("Need attribute.");
    if (@_) {
	my $type = shift;
	$self->get_class_hash($class)->{'attribute'}->{$attribute}->{'db_col_type'} = $type;
	# Hack to make Protege string types which are not stored as TEXT of *CHAR look different.
	if ($self->is_string_type_class_attribute($class,$attribute) &&
	    $type !~ /(TEXT|CHAR)/i) {
	    $self->class_attribute_type($class,$attribute,'other_type');
	}
    }
    return $self->class($class)->{'attribute'}->{$attribute}->{'db_col_type'};
}

sub is_primitive_class_attribute {
    my ($self,$class,$attribute) = @_;
    return if ($self->class_attribute_type($class,$attribute) eq 'db_instance_type');
    return if ($self->class_attribute_type($class,$attribute) eq 'db_internal_id_type');
    return 1;
}

sub is_instance_type_class_attribute {
    my ($self,$class,$attribute) = @_;
    
    my $class_attribute_type = $self->class_attribute_type($class,$attribute);
    
    # If we are dealing with outdated data models, where
    # attribute names have been changed...
    if (!(defined $class_attribute_type)) {
    	return 0;
    }
    
    # Normal case.
    return $class_attribute_type eq 'db_instance_type';
}

sub is_string_type_class_attribute {
    my ($self,$class,$attribute) = @_;
    return $self->class_attribute_type($class,$attribute) eq 'db_string_type';
}

sub list_string_type_class_attributes {
    my ($self,$class) = @_;
    return grep {$self->class_attribute_type($class,$_) eq 'db_string_type'} $self->list_class_attributes($class);
}

sub list_instance_type_class_attributes {
    my ($self,$class) = @_;
    return grep {$self->class_attribute_type($class,$_) eq 'db_instance_type'} $self->list_class_attributes($class);
}

sub class_attribute_check {
    my $self = shift;
    my $class = shift || $self->throw("Need class.");
    my $attribute = shift || $self->throw("Need attribute.");
#	delete $self->get_class_hash($class)->{'check'}->{'any'}->{$attribute};
#	delete $self->get_class_hash($class)->{'check'}->{'all'}->{$attribute};
    $self->get_class_hash($class)->{'check'}->{'any'}->{$attribute} = undef;
    $self->get_class_hash($class)->{'check'}->{'all'}->{$attribute} = undef;
    if (@_) {
	if (lc $_[0] eq 'any') {
	    $self->get_class_hash($class)->{'check'}->{'any'}->{$attribute} = 1;
	} elsif (lc $_[0] eq 'all') {
	    $self->get_class_hash($class)->{'check'}->{'all'}->{$attribute} = 1;
	} else {
	    $self->throw("Unknown check type '$_[0]'. Valid values are 'any' and 'all'.");
	}
    } 
}

sub get_class_attribute_defining_type {
    my $self = shift;
    my $class = shift || $self->throw("Need class.");
    my $attribute = shift || $self->throw("Need attribute.");
    if ($self->get_class_hash($class)->{'check'}->{'any'}->{$attribute}) {
	return 'any';
    } elsif ($self->get_class_hash($class)->{'check'}->{'all'}->{$attribute}) {
	return 'all';
    }
}

sub attribute_category_check {
    my $self = shift;
    my $class = shift || $self->throw("Need class.");
    my $attribute = shift || $self->throw("Need attribute.");
    delete $self->get_class_hash($class)->{'category'}->{'MANDATORY'}->{$attribute};
    delete $self->get_class_hash($class)->{'category'}->{'REQUIRED'}->{$attribute};
    delete $self->get_class_hash($class)->{'category'}->{'OPTIONAL'}->{$attribute};
    delete $self->get_class_hash($class)->{'category'}->{'NOMANUALEDIT'}->{$attribute};
    if (@_) {
	if ($_[0] eq 'MANDATORY') {
	    $self->get_class_hash($class)->{'category'}->{'MANDATORY'}->{$attribute} = 1;
	} elsif ($_[0] eq 'REQUIRED') {
	    $self->get_class_hash($class)->{'category'}->{'REQUIRED'}->{$attribute} = 1;
	} elsif ($_[0] eq 'OPTIONAL') {
	    $self->get_class_hash($class)->{'category'}->{'OPTIONAL'}->{$attribute} = 1;
	} elsif ($_[0] eq 'NOMANUALEDIT') {
	    $self->get_class_hash($class)->{'category'}->{'NOMANUALEDIT'}->{$attribute} = 1;
	} else {
	    $self->throw("Unknown category type '$_[0]'. Valid values are 'MANDATORY', 'REQUIRED', 'OPTIONAL' and 'NOMANUALEDIT'.");
	}
    } 
}

sub get_class_attribute_category_type {
    my $self = shift;
    my $class = shift || $self->throw("Need class.");
    my $attribute = shift || $self->throw("Need attribute.");
    if ($self->get_class_hash($class)->{'category'}->{'MANDATORY'}->{$attribute}) {
	return 'MANDATORY';
    } elsif ($self->get_class_hash($class)->{'category'}->{'REQUIRED'}->{$attribute}) {
	return 'REQUIRED';
    } elsif ($self->get_class_hash($class)->{'category'}->{'OPTIONAL'}->{$attribute}) {
	return 'OPTIONAL';
    } elsif ($self->get_class_hash($class)->{'category'}->{'NOMANUALEDIT'}->{$attribute}) {
	return 'NOMANUALEDIT';
    }
}

sub list_class_attributes_with_defining_type {
    my $self = shift;
    my $class = shift || $self->throw("Need class.");
    my $type = shift || $self->throw("Need defining type ('any' or 'all').");
    $type = lc($type);
    $type eq 'any' or $type eq 'all' or $self->throw("Unknown defining type: $type.");
    my $hr = $self->get_class_hash($class)->{'check'}->{$type};
    return grep {$hr->{$_}} keys %{$hr};
}

sub list_class_defining_attributes {
    my $self = shift;
    my $class = shift || $self->throw("Need class.");
#    return keys %{$self->get_class_hash($class)->{'check'}->{'all'}}, keys %{$self->get_class_hash($class)->{'check'}->{'any'}};
    return
	$self->list_class_attributes_with_defining_type($class,'all'),
	$self->list_class_attributes_with_defining_type($class,'any');
}

sub class_attribute_min_cardinality {
    my $self = shift;
    my $class = shift || $self->throw("Need class.");
    my $attribute = shift || $self->throw("Need attribute.");
    if (@_) {
	$self->get_class_hash($class)->{'attribute'}->{$attribute}->{'min_cardinality'} = shift;
    }
    return $self->get_class_hash($class)->{'attribute'}->{$attribute}->{'min_cardinality'};
}

sub class_attribute_max_cardinality {
    my $self = shift;
    my $class = shift || $self->throw("Need class.");
    my $attribute = shift || $self->throw("Need attribute.");
    if (@_) {
	$self->get_class_hash($class)->{'attribute'}->{$attribute}->{'max_cardinality'} = shift;
    }
    return $self->get_class_hash($class)->{'attribute'}->{$attribute}->{'max_cardinality'};
}

sub list_required_class_attributes {
    my $self = shift;
    my $class = shift || $self->throw("Need class.");
#    return grep {$self->class_attribute_min_cardinality($class,$_)} $self->list_class_attributes($class);
    my $hr = $self->get_class_hash($class)->{'category'}->{'REQUIRED'} || return;
    return grep {$hr->{$_}} $self->list_class_attributes($class);
}

sub list_mandatory_class_attributes {
    my $self = shift;
    my $class = shift || $self->throw("Need class.");
#    return grep {$self->class_attribute_min_cardinality($class,$_)} $self->list_class_attributes($class);
    my $hr = $self->get_class_hash($class)->{'category'}->{'MANDATORY'} || return;
    return grep {$hr->{$_}} $self->list_class_attributes($class);
}

sub pins_file_stub {
    my $self= shift;
    if (@_) {
	$self->{'pins_file_stub'} =shift;
    }
    return $self->{'pins_file_stub'};
}

sub pont_file_content {
    my $self= shift;
    if (@_) {
	$self->{'pont_file_content'} =shift;
    }
    return $self->{'pont_file_content'};
}

sub pprj_file_content {
    my $self= shift;
    if (@_) {
	$self->{'pprj_file_content'} =shift;
    }
    return $self->{'pprj_file_content'};
}

sub is_valid_class_reverse_attribute {
    my $self = shift;
    my $class = shift || $self->throw("Need class.");
    my $attribute = shift || $self->throw("Need attribute.");
    $self->{'reverse_attribute_found'} || $self->_find_referers;
    return $self->class($class)->{'reverse_attribute'}->{$attribute};
}

sub list_classes_with_attribute {
    my ($self,$att,$cls) = @_;
    $cls ||= $self->root_class;
    my @classes = ($cls);
    my @out;
    while (my $currcls = shift @classes) {
	if ($self->is_valid_class_attribute($currcls,$att)) {
	    push @out, $currcls;
	} else {
	    push @classes, $self->children($currcls);
	}
    }
#    print qq(<PRE>$cls\t$att\t@out</PRE>\n);
    return @out;
}

sub list_classes_with_reverse_attribute {
    my ($self,$att,$cls) = @_;
    $cls ||= $self->root_class;
    my @classes = ($cls);
    my @out;
    while (my $currcls = shift @classes) {
	if ($self->is_valid_class_reverse_attribute($currcls,$att)) {
	    push @out, $currcls;
	} else {
	    push @classes, $self->children($currcls);
	}
    }
#    print qq(<PRE>$cls\t$att\t@out</PRE>\n);
    return @out;
}

### Function: timestamp
# Parses out the timestamp string from pprj file header.
###
sub timestamp {
    my $self = shift;
    my ($timestamp) = $self->pprj_file_content =~ /^; (.+?)\n/;
    return $timestamp;
}


=head
sub find_closest_common_ancestor {
    my ($self, $ar) = @_;
    my (%h,%count);
    map {$h{$_} = 1} @{$ar};
    foreach my $cls (keys %h) {
	map {%count{$_}++}  ($self->list_ancestors($cls),$cls);
    }
}
=cut

1;
