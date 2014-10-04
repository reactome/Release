package GKB::Instance;

# Copyright 2002 Imre Vastrik <vastrik@ebi.ac.uk>

use strict;
use vars qw(@ISA $AUTOLOAD $PROTEGE_ID_BASE $USE_STABLE_ID_IN_DUMPING);
use Bio::Root::Root;
use GKB::Ontology;
use Data::Dumper;
use GKB::Config qw($NO_SCHEMA_VALIDITY_CHECK);
@ISA = qw(Bio::Root::Root);

$PROTEGE_ID_BASE = 'GK_';
$USE_STABLE_ID_IN_DUMPING = 1;

sub new {
  my ($pkg,@args) = @_;
  my $self = bless {}, $pkg;
  my ($ontology,$class,$id,$db_id,$dba,$debug) = $self->_rearrange
      ([qw(
	   ONTOLOGY
	   CLASS
	   ID
	   DB_ID
	   DBA
	   DEBUG
	   )], @args);
  $ontology || $self->throw("Need ontology to create instance.");
  $self->ontology($ontology);
  $self->class($class) if ($class);
  $self->id($id) if (defined $id);
  $self->db_id($db_id) if ($db_id);
  $self->dba($dba) if ($dba);
  $self->debug($debug);
  $self->_handle_attribute_values(\@args);
  return $self;
}

sub dba {
    my ($self) = shift;
    if (@_) {
	$self->{'dba'} = shift;
    }
    return $self->{'dba'};
}

sub _handle_attribute_values {
    my ($self, $args) = @_;
    for(my $i = 0; $i < @{$args}; $i += 2) {
	$args->[$i] =~ /^-(ONTOLOGY|CLASS|ID|DB_ID|DBA)$/ && next;
	$args->[$i] =~ s/^-//g;
	$self->ontology->check_class_attribute($self->class,$args->[$i]);
	if (ref($args->[$i+1]) && $args->[$i+1] =~ /^ARRAY/) {
	    # Only attempt to set the values if the array isn't empty.
	    @{$args->[$i+1]} && $self->attribute_value($args->[$i], @{$args->[$i+1]});
	} else {
	    $self->attribute_value($args->[$i], $args->[$i+1]);
	}
    }
}

sub AUTOLOAD {
    my $self = shift;
    my $attr = $AUTOLOAD;
    $attr =~ s/.*:://;
    return unless $attr =~ /[^A-Z]/;  # skip DESTROY and all-cap methods
    $attr =~ s/^(_?)([A-Z])/$ {1}.lc($2)/e unless ($attr =~ /_id$/);
    $self->is_valid_attribute($attr) || $NO_SCHEMA_VALIDITY_CHECK || $self->throw("invalid attribute method for class " . $self->class . ": ->$attr()");
    if (@_) {
	$self->attribute_value($attr,@_);
    } else {
	return $self->attribute_value($attr);
    }
}  

### Function: ontology
# Get/set for ontology
###
sub ontology {
    my $self = shift;
    if (@_){
	$self->{'ontology'} = shift;
    }
    return $self->{'ontology'};
}

### Function: id
# Get/set for id
# Normal atribute value setting by attribute_value is bypassed since this
# function requires class to be known. However, occasionally, like in
# go.xml parsing the class will be known only later (once everything is parsed and
# you can delineate the root.
# NOTE: this is not necessarily the same as db_id.
###
sub id {
    my $self = shift;
    if (@_){
	my $arg = shift;
	$self->{'id'} = $arg;
    }
    return $self->{'id'};
}

### Function: class
# Get/set for Instance's class.
# Also sets attribute '_class'.
###
sub class {
    my $self = shift;
    if (@_) {
	my $arg = shift;
	# Check that the class is valid
	$self->ontology->class($arg);
	$self->{'class'} = $arg;
#	$self->attribute_value('_class',$arg);
	$self->{'attribute'}->{'_class'}->[0] = $arg;
    }
    return $self->{'class'};
}

### Function: attribute_value
# Get/set for attribute values. Checks that the attribute is a valid attribute
# for the given instance. If the attribute is multivalue attribute stores all
# of them, if attribute is single value attribute silently stores just the 1st
# value.
# Arguments:
# 1) string attribute name. Required
# 2+) string, number or object attribute values. Optional.
# If the 2nd argument passed to the function is 'undef', the attribute is emptied.
# Returns: if invoked with attribute name only, returns reference to an array
# containing the values of this attribute. If there are no values, teh array is
# empty.
# NOTE: Throws if attribute is not a valid attribute for class of this instance
###
sub attribute_value {
    my $self = shift;
    $self->class || $self->throw("Instance has to know it's class to get/set attribute value.");
    my $attribute = shift;
    $attribute || $self->throw("Need attribute to store/retrieve value");
    if ($self->is_valid_attribute($attribute)) {
	if (@_) {
#	    print "$attribute\t@_\n";
	    if (defined $_[0]) {
		if ($self->is_multivalue_attribute($attribute)) {
		    @{$self->{'attribute'}->{$attribute}} = @_;
		} else {
		    $self->{'attribute'}->{$attribute}->[0] = shift @_;
		}
	    } else {
		$self->{'attribute'}->{$attribute} = [];
	    }
	} else {
	    if ($self->{'attribute'}->{$attribute} || $self->inflated) {
		return $self->{'attribute'}->{$attribute} || [];
	    } elsif ($self->dba) {
		#$self->inflate;
		$self->load_attribute_values($attribute);
#		$self->{'attribute'}->{$attribute} = [] unless ($self->{'attribute'}->{$attribute});
		return $self->{'attribute'}->{$attribute};
	    } else {
		$self->throw("Can't access attribute '$attribute' value since it hasn't been loaded yet ["
			     . $self->class_and_id . "].");
	    }
	}
    } else {
	$NO_SCHEMA_VALIDITY_CHECK || $self->throw("Attribute '$attribute' is not valid for class " . $self->class . ".");
	return [];
    }
}

### Function: add_attribute_value 
# In case of multivalue attributes appends to existing values, in case of single
# value attribute overwrites the previous value.
# NOTE: Throws if attribute is not a valid attribute for class of this instance.
###
sub add_attribute_value {
    my ($self,$attribute,@vals) = @_;
    $self->class || $self->throw("Instance has to know it's class to set attribute value.");
    $attribute || $self->throw("Need attribute to store value");

    if ($self->ontology->class($self->class)->{'attribute'}->{$attribute}) {
	if (@vals) {
	    if($self->is_instance_type_attribute($attribute)) {
		foreach (@vals) {
		    $self->is_attribute_allowed_class_instance($attribute,$_) ||
			$self->throw("Instance '" . $_->id_string . "' is not a valid value for attribute '$attribute' of '" . $self->id_string . "'");
		}
	    }
	    if ($self->dba && ! ($self->{'attribute'}->{$attribute} || $self->inflated)) {
		$self->load_attribute_values($attribute);
	    }
	    if ($self->ontology->class($self->class)->{'attribute'}->{$attribute}->{'multiple'}) {
		push @{$self->{'attribute'}->{$attribute}}, @vals;
	    } else {
		$self->{'attribute'}->{$attribute}->[0] = $vals[0];
	    }
	}
    } else {
	$self->throw("Attribute '$attribute' is not valid for class " . $self->class . ".");
    }
}


### Function: add_attribute_value2 
# In case of multivalue attributes appends to existing values, in case of single
# value attribute overwrites the previous value. Differs from add_attribute_value
# in that it does not check if the value instance being added is of valid class.
# Used by ClipsAdaptor where the class of the instance being added is not necessarily
# known at yet.
# NOTE: Throws if attribute is not a valid attribute for class of this instance.
###
sub add_attribute_value2 {
    my ($self,$attribute,@vals) = @_;
    $self->class || $self->throw("Instance has to know it's class to set attribute value.");
    $attribute || $self->throw("Need attribute to store value");
    #print join("\t",(caller(0))[3], $attribute,@vals), "\n";
    if ($self->ontology->class($self->class)->{'attribute'}->{$attribute}) {
	if (@vals) {
	    if ($self->dba && ! ($self->{'attribute'}->{$attribute} || $self->inflated)) {
		$self->load_attribute_values($attribute);
	    }
	    if ($self->ontology->class($self->class)->{'attribute'}->{$attribute}->{'multiple'}) {
		push @{$self->{'attribute'}->{$attribute}}, @vals;
	    } else {
		$self->{'attribute'}->{$attribute}->[0] = $vals[0];
	    }
	}
    } else {
	$self->throw("Attribute '$attribute' is not valid for class " . $self->class . ".");
    }
}

### Function: add_attribute_value_if_necessary
# In case of multivalue attributes appends to existing values if the value is not among the
# existing ones already. In case of single value attribute warns if the previous value is not
# the same as the new and then overwrites the previous values.
# RETURNS: a reference to array containing the values that were actually added.
# NOTE: Throws if attribute is not a valid attribute for class of this instance.
###
sub add_attribute_value_if_necessary {
    my ($self,$attribute,@vals) = @_;
    $self->class || $self->throw("Instance has to know it's class to set attribute value.");
    $attribute || $self->throw("Need attribute to store value");
    $self->is_valid_attribute($attribute) || $self->throw("Attribute '$attribute' is not valid for class " . $self->class . ".");
    if (@vals) {
	if ($self->is_multivalue_attribute($attribute)) {
	    no strict 'refs';
	    my %values;
	    map {$values{$_}++} @{$self->{'attribute'}->{$attribute}};
	    @vals = grep {! $values{$_}++} @vals;
	    $self->add_attribute_value($attribute,@vals);
	    use strict;
	} else {
	    if (defined $self->{'attribute'}->{$attribute}->[0]) {
		if ($self->ontology->is_string_type_class_attribute($self->class,$attribute)) {
		    unless ($self->{'attribute'}->{$attribute}->[0] eq $vals[0]) {
			$self->warn("Replacing attribute '$attribute' value '$self->{'attribute'}->{$attribute}->[0]' with '$vals[0]'.");
		    }
		} elsif ($self->is_instance_type_attribute($attribute)) {
		    unless ($self->{'attribute'}->{$attribute}->[0] == $vals[0]) {
			$self->warn("Replacing attribute '$attribute' value '" .
				    $self->{'attribute'}->{$attribute}->[0]->extended_displayName ."' with '" .
				    $vals[0]->extended_displayName . "'.");
		    }
		} else {
		    unless ($self->{'attribute'}->{$attribute}->[0] == $vals[0]) {
			$self->warn("Replacing attribute '$attribute' value '$self->{'attribute'}->{$attribute}->[0]' with '$vals[0]'.");
		    }
		}
	    }
	    $self->{'attribute'}->{$attribute}->[0] = $vals[0];
	    @vals = ($vals[0]);
	}
    }
    return \@vals;
}

# Returns the number of instances replaced
sub replace_attribute_value {
    my ($self,$att,$old,$new) = @_;
    my @tmp;
    my $replaced = 0;
    if ($self->is_instance_type_attribute($att)) {
	foreach (@{$self->attribute_value($att)}) {
	    if ($_->db_id == $old->db_id) {
		push @tmp, $new;
		$replaced++;
	    } else {
		push @tmp, $_;
	    }
	}
    } else {
	foreach (@{$self->attribute_value($att)}) {
	    if ($_ eq $old) {
		push @tmp, $new;
		$replaced++;
	    } else {
		push @tmp, $_;
	    }
	}
    }
    if ($replaced) {
	$self->attribute_value($att,@tmp);
    }
    return $replaced++;
}

### Function: attribute_value2
# Gets attribute value and appends to existing value(s) without checking
# for the validity of the attribute for this instance. Used in go.xml
# parsing where the instances are created w/o knowing their proper class.
###
sub attribute_value2 {
    my ($self,$attribute,@vals) = @_;
    $attribute || $self->throw("Need attribute to store value");
    if (@vals) {
	push @{$self->{'attribute'}->{$attribute}}, @vals;
    } else {
	return $self->{'attribute'}->{$attribute} || []; 
    }
}

sub referer_value {
    my ($self,@args) = @_;
    return $self->reverse_attribute_value(@args);
}

### Function: reverse_attribute_value
# Get/set of reverse attributes, i.e. instances which have the curnet instance as
# an attribute value. E.g. if Complex with name ORC has attribute 'hasComponent'
# with value AccessionedEntity with name Orc1, the latter has reverse attribute
# 'hasComponent' of class Complex and name ORC.
# Arguments:
# 1) string (reverse) attribute name
# 2) reference to an array containing reverse attribute values
# Returns: if invoked with attribute name only returns reference to an array
# containig values (instances). Array is empty if there are no referers.
# NOTE: Throws is reverse attributes have not been attached to this instance.
###
sub reverse_attribute_value {
    my $self = shift;
    my $attribute = shift;
    $attribute || $self->throw("Need attribute to store/retrieve value");
    if (@_) {
	if (defined $_[0] && ref($_[0]) && (ref($_[0]) eq 'ARRAY')) { 
	    $self->{'reverse_attribute'}->{$attribute} = shift;
	} else {
	    $self->throw("Need array ref, got '@_'.");
	}
    } else {
	if ($self->reverse_attributes_attached) {
	    return $self->{'reverse_attribute'}->{$attribute} || [];
	} elsif ($self->{'reverse_attribute'} && $self->{'reverse_attribute'}->{$attribute}) {
	    return $self->{'reverse_attribute'}->{$attribute} || [];
	} elsif ($self->dba) {
	    $self->dba->load_reverse_attribute_values($self,$attribute);
	    return $self->{'reverse_attribute'}->{$attribute} || [];
	} else {
	    $self->throw("Reverse attributes have not been attached to " . $self->class . " " . $self->db_id . " yet.");
	}
    }
}

sub add_reverse_attribute_value {
    my $self = shift;
    my $attribute = shift;
    $attribute || $self->throw("Need attribute to store/retrieve value");
    if (@_) {
	if (defined $_[0]) {
	    if (ref($_[0]) && (ref($_[0]) eq 'ARRAY')) { 
		push @{$self->{'reverse_attribute'}->{$attribute}}, @{$_[0]};
	    } else {
		push @{$self->{'reverse_attribute'}->{$attribute}}, @_;
	    }
	}
    }
}

sub list_refering_attributes {
    my ($self) = @_;
    return $self->list_reverse_attributes;
}

### Function: list_reverse_attributes
# Exists for backward compatibility
###
sub list_reverse_attributes {
    $_[0]->warn("Method 'list_reverse_attributes' is deprecated. Use 'list_set_reverse_attributes' instead.\n" . $_[0]->stack_trace_dump);
    return list_set_reverse_attributes(@_);
}
### Function: list_set_reverse_attributes
# Returns a list of set reverse attribute names.
###
sub list_set_reverse_attributes {
    my ($self) = @_;
    if ($self->{'reverse_attribute'}) {
	return keys %{$self->{'reverse_attribute'}};
    } else {
	return ();
    }
}

### Function: list_set_attributes
# Returns a list of set attribute names.
###
sub list_set_attributes {
    my $self = shift;
    return grep {$self->is_valid_attribute($_)} keys %{$self->{'attribute'}};
}

### Function: list_valid_attributes
# Returns a list of set valid attribute names.
###
sub list_valid_attributes {
    my $self = shift;
    return $self->ontology->list_class_attributes($self->class);
}

sub list_attributes {
    return list_valid_attributes(@_);
}

### Function: list_valid_reverse_attributes
# Returns a list of set valid reverse attribute names.
###
sub list_valid_reverse_attributes {
    my $self = shift;
    $self->ontology->{'reverse_attribute_found'} || $self->ontology->_find_referers;
    return keys %{$self->ontology->class($self->class)->{'reverse_attribute'}};    
}

### Function: is_valid_attribute
# Returns a true value if the attribute name is a valid attribute name
# for this instance's class.
# Arguments:
# 1) string attribute name
###
sub is_valid_attribute {
    my ($self,$att) = @_;
#    $self->ontology->class($self->class) && $self->ontology->class($self->class)->{'attribute'} || $self->throw("Something wrong with:\n" . $self->dumper_string);
    return $self->ontology->is_valid_class_attribute($self->class,$att);
#    return $self->ontology->class($self->class)->{'attribute'}->{$att};
}

### Function: is_valid_reverse_attribute
# Returns a true value if the attribute name is a valid reverse attribute name
# for this instance's class.
# Arguments:
# 1) string attribute name
###
sub is_valid_reverse_attribute {
    my ($self,$att) = @_;
    $self->ontology->{'reverse_attribute_found'} || $self->ontology->_find_referers;
    return $self->ontology->class($self->class)->{'reverse_attribute'}->{$att};
}

sub is_recursive_attribute {
    my ($self,$att) = @_;
    return $self->ontology->is_recursive_class_attribute($self->class,$att);
}

### Function: db_id
# Get/set for db_id a.k.a. database internal identifier or db internal id.
# Is mysql_insertid.
###
sub db_id {
    my $self = shift;
    if (@_){
	$self->{'db_id'} = shift;
    }
    return $self->{'db_id'};
}

### Function: reverse_attributes_attached
# Return true if reverse attributes have been attached to this instance
###
sub reverse_attributes_attached {
    my $self = shift;
    if (@_) {
	$self->{'reverse_attributes_attached'} = shift;
    }
    return $self->{'reverse_attributes_attached'};
}

### Function: inflated
# When called without a parameter, returns a true value if the instance is inflated,
# i.e. has attribute values attached to it. Can also be used to change the 'iflation'
# status artificially, i.e. by passing in a true value you claim that the attribute
# values have all been loaded or set. This is for example necessary when you create
# a new instance to be stored in db - before passing it to $dba->store, do
# $instance->inflated(true).
# Please note, though, that if you want to add an attribute value to a already stored
# instance and update it there is no need to call this method. Furthermore, it can
# even have ill effects - if you set inflated true but haven't loaded the attribute
# values from db yet they won't be loaded and if you then add (in effect set) value
# and update the instance you will overwrite the old values.
###
sub inflated {
    my $self = shift;
    if (@_) {
	$self->{'inflated'} = shift;
    }
    return $self->{'inflated'};
}

### Function: newly_stored
# Get/set function for indicating that the instance has been newly stored in db
# as opposed to just been given db_id of something identical stored already.
###
sub newly_stored {
    my $self = shift;
    if (@_) {
	$self->{'newly_stored'} = shift;
    }
    return $self->{'newly_stored'};
}

sub debug {
    my $self = shift;
    if (@_) {
	$self->{'debug'} = shift;
    }
    return $self->{'debug'};
}

sub clone {
    my ($self) = @_;
    my ($pkg) = $self =~ /^(\S+)=/;
    my $clone = bless {}, $pkg;
    $clone->ontology($self->ontology);
#    $clone->id($self->id);
#    $clone->db_id($self->db_id);
    $clone->class($self->class);
    $clone->dba($self->dba);
    $clone->reverse_attributes_attached($self->reverse_attributes_attached);
    foreach ($self->list_set_attributes) {
	my $ar = $self->attribute_value($_);
	if (@{$ar}) {
	    $clone->attribute_value($_, @{$ar});
	} else {
	    $clone->attribute_value($_,undef);
	}
    }
    foreach ($self->list_set_reverse_attributes) {
	$clone->reverse_attribute_value($_, $self->reverse_attribute_value($_));
    }
    $clone->inflated($self->inflated);
    return $clone;
}

sub clone_with_db_id {
    my ($self) = @_;
    my ($pkg) = $self =~ /^(\S+)=/;
    my $clone = bless {}, $pkg;
    $clone->ontology($self->ontology);
    $clone->id($self->id);
    $clone->db_id($self->db_id);
    $clone->class($self->class);
    $clone->dba($self->dba);
    $clone->reverse_attributes_attached($self->reverse_attributes_attached);
    foreach ($self->list_set_attributes) {
	my $ar = $self->attribute_value($_);
	if (@{$ar}) {
	    $clone->attribute_value($_, @{$ar});
	} else {
	    $clone->attribute_value($_,undef);
	}
    }
    foreach ($self->list_set_reverse_attributes) {
	$clone->reverse_attribute_value($_, $self->reverse_attribute_value($_));
    }
    $clone->inflated($self->inflated);
    return $clone;
}

sub namedInstance {
    my ($self) = @_;
    unless ($self->dba) {
	$self->inflated(1);
    }
    require GKB::NamedInstance;
    return GKB::NamedInstance->new(-INSTANCE => $self);
}

sub displayName {
    my ($self) = shift;
    if(@_) {
	$self->attribute_value('_displayName', @_);
	return;
    }
    # Important :don't want to rebless PrettyInstances !!!
    return $self->attribute_value('_displayName')->[0] if ($self->isa("GKB::PrettyInstance"));
    unless ($self->attribute_value('_displayName')->[0]) {
	$self->namedInstance;
    }
    return $self->attribute_value('_displayName')->[0];
}

sub extended_displayName {
    my ($self) = @_;
    return 
#	$self .
	" [".$self->class.":".($self->db_id||
			       ($self->attribute_value($GKB::Ontology::DB_ID_NAME)->[0] &&
				($self->attribute_value($GKB::Ontology::DB_ID_NAME)->[0] . "*"))||
			       $self->id||
			       'db_id_not_set'
			       )."] ".($self->displayName || '');
}

sub id_string {
    my ($self) = @_;
#    my $out = join(" : ", $self,$self->class,($self->db_id || $self->id || 'id_not_set'));
#    my $out = join(" : ", $self->class,($self->db_id || $self->id || 'id_not_set'));
    my $out = $self . " " . $self->class_and_id;
#    if ($self->is_valid_attribute('species') && $self->Species->[0]) {
#	$out .= '[' . $self->Species->[0]->displayName . ']';
#    }
    if ($self->attribute_value('_displayName')->[0]) {
	$out .= " : " . $self->attribute_value('_displayName')->[0];
    } else {
	my $att = (grep {$self->is_valid_attribute($_)} qw(name identifier surname))[0];
	$att && $self->attribute_value($att)->[0] && ($out .= " : " . $self->attribute_value($att)->[0]);
    }
    return $out;
}

sub prettyInstance {
    my $self = shift;
    require GKB::PrettyInstance;
    return GKB::PrettyInstance->new(-INSTANCE => $self, @_);
}

sub prettyfy {
    my ($self,@args) = @_;
    $self->isa("GKB::PrettyInstance") && return $self;
    return $self->prettyInstance(@args);
}

sub is_a {
    my ($self,$class) = @_;
    $class eq $self->class and return 1;
    return $self->ontology->class($self->class)->{'ancestor'}->{$class};
}

sub class_and_id {
    return $_[0]->class . ':' .
	($_[0]->db_id ||
	 ($_[0]->{'attribute'}->{$DB_ID_NAME}->[0] && ($_[0]->{'attribute'}->{$DB_ID_NAME}->[0] . '*')) ||
	 '(db_)id not set');
}

sub stableIdentifier_or_class_and_id {
    # HACK: hopefully just for the transition period
    $USE_STABLE_ID_IN_DUMPING || return class_and_id(@_);
    return ($_[0]->is_valid_attribute('stableIdentifier') && $_[0]->StableIdentifier->[0]) 
	? $_[0]->StableIdentifier->[0]->displayName
	: $_[0]->class_and_id;
}

sub attribute_origin {
    my ($self,$att) = @_;
    #return $self->ontology->class_attribute_origin($self->class,$att);
    my $origin = $self->ontology->class_attribute_origin($self->class,$att) ||
	$self->throw(sprintf "Couldn't find origin for attribute '%s' of '%s' %s\n", $att, $self->extended_displayName,join(',',keys %{$self->ontology->class($self->class)->{'attribute'}}));
    return $origin;
}

sub is_multivalue_attribute {
    my ($self,$att) = @_;
    return $self->ontology->is_multivalue_class_attribute($self->attribute_origin($att),$att);
}

sub check_attribute {
    my ($self,$att) = @_;
    return $self->ontology->check_class_attribute($self->class,$att);
}

sub attribute_type {
    my ($self,$att) = @_;
    return $self->ontology->class_attribute_type($self->class,$att);
}

sub is_instance_type_attribute {
    my ($self,$att) = @_;
    return $self->attribute_type($att) eq 'db_instance_type';
}

### Function: inflate
# Load all attribute values of an instance from db.
# In order to make code faster and leaner attribute values are lazy-loaded, i.e.
# fetched from db the 1st time they are requested for. This method allows all
# attribute values to be loaded at once. 
# The main (if not the only) scenario for calling this method is when updating
# an instance in db - this requires for all the attribute values of an instance to
# be loaded.
# NOTE: if you call this method after setting the attribute values you will overwrite
# the values you have set by those specified in the db!
###

sub inflate {
    my ($self) = @_;
    $self->dba || $self->throw("Can't inflate w/o DBA [" . $self->id_string . "].");
    return $self->dba->inflate_instance($self);
}

sub is_attribute_value_loaded {
    my ($self,$att) = @_;
    $att || $self->throw("Need attribute.");
    $self->check_attribute($att);
    return (exists $self->{'attribute'}->{$att}) ? 1 : 0;
}

sub load_attribute_values {
    my ($self,$attribute) = @_;
    $self->dba || $self->throw("Can't inflate w/o DBA [" . $self->id_string . "].");
    $self->dba->load_attribute_values($self,$attribute);
}

sub load_single_attribute_values {
    my ($self,$attribute) = @_;
    $self->dba || $self->throw("Can't inflate w/o DBA [" . $self->id_string . "].");
    $self->dba->load_single_attribute_values($self,$attribute);
}

sub list_reverse_attribute_allowed_classes {
    my $self = shift;
    return $self->ontology->list_allowed_classes_for_class_reverse_attribute($self->class,@_);
}

sub list_attribute_allowed_classes {
    my $self = shift;
    return $self->ontology->list_allowed_classes($self->class,@_);
}

sub is_attribute_allowed_class {
    my $self = shift;
    return $self->ontology->is_class_attribute_allowed_class($self->class,@_);
}

sub is_attribute_allowed_class_instance {
    my ($self,$attribute,$instance) = @_;
    $instance || $self->throw("Need instance");
    return $self->is_attribute_allowed_class($attribute,$instance->class);
}

sub create_protege_id {
    my ($self) = @_;
    my $id;
    if ($self->db_id) {
	$id = $PROTEGE_ID_BASE . $self->db_id;
#	$self->attribute_value('_Protege_id', $id);
	return $id;
    } elsif ($self->id) {
	$id = $PROTEGE_ID_BASE . $self->id;
	$id =~ s/\W+/_/g;
#	$self->attribute_value('_Protege_id', $id);
	return $id;
    } else {
	($id = $self) =~ s/\W+/_/g;
#	my $id = $self;
	$id = "$ {PROTEGE_ID_BASE}$ {id}";
#	$self->attribute_value('_Protege_id', $id);
	return $id;
    }
}

sub list_class_ancestors {
    my ($self) = @_;
    return $self->ontology->list_ancestors($self->class);
}

sub inverse_attribute{
    my $self = shift;
    return $self->ontology->inverse_attribute(@_);
}

sub are_inverse_attributes {
    my $self = shift;
    return $self->ontology->are_inverse_attributes(@_);
}

sub identical_instances_in_db {
    my $self = shift;
    if (@_) {
	$self->{'identical_instances_in_db'} = shift;
    } else {
	return $self->{'identical_instances_in_db'};
    }
}

sub check_required_attributes {
    my $self = shift;
    foreach my $att ($self->ontology->list_required_class_attributes) {
	@{$self->attribute_value($att)} || $self->warn("Attribute '$att' has to have a value.");
    }
}

sub list_mandatory_attributes_wo_value {
    my $self = shift;
    my @out;
    foreach my $att ($self->ontology->list_mandatory_class_attributes($self->class)) {
	@{$self->attribute_value($att)} || push @out, $att;
    }
    return @out;
}

# Adds to $self multi-value attribute values of $i which are not present in $self and
# single value attributes if the attribute is empty
sub merge {
    my ($self, $i) = @_;
    $self->debug && print "-->", (caller(0))[3], "\n";
    $i || $self->throw("Need Instance.");
#    foreach my $att (grep{$i->is_valid_attribute($_)} grep{$self->is_multivalue_attribute($_)} $self->list_valid_attributes) {
    foreach my $att (grep{$i->is_valid_attribute($_)} $self->list_valid_attributes) {
	next if ($att eq 'modified'or $att eq 'created');
	next unless ($self->is_multivalue_attribute($att) || ! @{$self->attribute_value($att)});
	if ($self->is_instance_type_attribute($att)) {
	    my %h;
#	    print "->$att\n";
#	    map {print $_->id_string . "\n"} @{$self->attribute_value($att)};
	    map{$h{$_} = 1} map{$self->dba->store_if_necessary($_)} @{$self->attribute_value($att)};
#	    print "-\n";
	    foreach (@{$i->attribute_value($att)}) {
		$self->dba->store_if_necessary($_);
		$self->add_attribute_value($att, $_) unless ($h{$_->db_id});
	    }
#	    print join("\t",map {$_->id_string} @{$self->attribute_value($att)}), "\n";
#	    print "<-$att\n";
	} else {
	    my %h;
	    map{$h{uc($_)} = 1} @{$self->attribute_value($att)};
	    map{$self->add_attribute_value($att, $_)} grep{! $h{uc($_)}} @{$i->attribute_value($att)};
	}
    }
    $self->namedInstance;
    $self->debug && print "<--", (caller(0))[3], "\n\n";
}

# This function just works in the context of store_if_necessary. The instances retrieved from
# the database have been retrieved on the basis of "defining attributes".
sub reasonably_identical {
    my ($self,$i) = @_;
    (ref($i) && $i->isa("GKB::Instance")) || $self->throw("Need GKB::Instance, got '$i'.");
#    foreach my $attribute (keys %{$self->ontology->class($self->class)->{'check'}->{'all'}}) {
    foreach my $attribute ($self->ontology->list_class_attributes_with_defining_type($self->class,'all')) {
	# just check if the number of attribute values is same. Are there cases where we need to
	# check order?
	return unless (scalar(@{$self->attribute_value($attribute)}) == scalar(@{$i->attribute_value($attribute)}));
	# Also have to check the composition. For example, if you've 1st stored a heterodimer AB and
	# then try to store homodimer AA, the latter would "match" the former unless you check composition.
	my (%seen1,%seen2);
	if ($self->is_instance_type_attribute($attribute)) {
	    map {$seen1{$_->db_id}++} @{$self->attribute_value($attribute)};
	    map {$seen2{$_->db_id}++} @{$i->attribute_value($attribute)};
	} else {
	    map {$seen1{$_}++} @{$self->attribute_value($attribute)};
	    map {$seen2{$_}++} @{$i->attribute_value($attribute)};
	}
	return unless (scalar(keys %seen1) == scalar(keys %seen2));
	# Then check the number of each component since AAAB is different from AABB
	map {($seen1{$_} == $seen2{$_}) || return } map {$seen2{$_} ? $_ : return } keys %seen1;
    }
    return 1;
}

sub equals {
    my ($self,$i) = @_;
    (ref($i) && $i->isa("GKB::Instance")) || $self->throw("Need GKB::Instance, got '$i'.");
#    print "",(caller(0))[3], "\t", $self->extended_displayName, "\t", $i->extended_displayName, "\n";
    if ($self->class eq $i->class) {
	foreach my $att (grep {$_ ne $GKB::Ontology::DB_ID_NAME}
			 grep {!/^_/} $self->list_valid_attributes) {
#	    print "$att\n";
	    next unless ($i->is_valid_attribute($att));
	    unless (@{$self->attribute_value($att)} == @{$i->attribute_value($att)}) {
#		print $self->id_string, "\t$att\t", scalar(@{$self->attribute_value($att)}), " != ", scalar(@{$i->attribute_value($att)}), "\n";
		return;
	    }
	    @{$self->attribute_value($att)} || next;
	    if ($self->is_instance_type_attribute($att)) {
		foreach my $j (0 .. $#{$self->attribute_value($att)}) {
		    unless (($self->attribute_value($att)->[$j]->db_id ||
			     $self->attribute_value($att)->[$j]->attribute_value($GKB::Ontology::DB_ID_NAME)->[0])
			    == 
			    ($i->attribute_value($att)->[$j]->db_id ||
			     $i->attribute_value($att)->[$j]->attribute_value($GKB::Ontology::DB_ID_NAME)->[0])) {
#			print $self->id_string, "\t$att\t",$self->attribute_value($att)->[$j]->extended_displayName, " != ", $i->attribute_value($att)->[$j]->extended_displayName, "\n";
			return;
		    }
		}
	    } else {
		foreach my $j (0 .. $#{$self->attribute_value($att)}) {
		    unless ($self->attribute_value($att)->[$j] eq $i->attribute_value($att)->[$j]) {
#			print $self->id_string, "\t$att\t",$self->attribute_value($att)->[$j], " != ", $i->attribute_value($att)->[$j], "\n";
			return;
		    }
		}
	    }
	}
#	print $self->extended_displayName, " == ", $i->extended_displayName, "\n";
	return 1;
    } else {
#	print $self->id_string, "\t", $self->class, " != ", $i->class, "\n";
	return;
    }
}

sub equals1 {
    my ($self,$i) = @_;
    (ref($i) && $i->isa("GKB::Instance")) || $self->throw("Need GKB::Instance, got '$i'.");
#    print "",(caller(0))[3], "\t", $self->extended_displayName, "\t", $i->extended_displayName, "\n";
    if ($self->class ne $i->class) {
	$self->_create_InstanceBeforeChange_if_necessary($i);
    }
    ATT: foreach my $att (grep {$i->is_valid_attribute($_)}
			  grep {$_ ne $GKB::Ontology::DB_ID_NAME}
			  grep {!/^_/} $self->list_valid_attributes) {
	unless (@{$self->attribute_value($att)} == @{$i->attribute_value($att)}) {
	    $self->_create_AttributeValueBeforeChange($i,$att);
	    next;
	}
	@{$self->attribute_value($att)} || next;
	if ($self->is_instance_type_attribute($att)) {
	    foreach my $j (0 .. $#{$self->attribute_value($att)}) {
		unless (($self->attribute_value($att)->[$j]->db_id ||
			 $self->attribute_value($att)->[$j]->attribute_value($DB_ID_NAME)->[0])
			== 
			($i->attribute_value($att)->[$j]->db_id ||
			 $i->attribute_value($att)->[$j]->attribute_value($DB_ID_NAME)->[0])) {
		    $self->_create_AttributeValueBeforeChange($i,$att);
		    next ATT;
		}
	    }
	} else {
	    foreach my $j (0 .. $#{$self->attribute_value($att)}) {
		unless ($self->attribute_value($att)->[$j] eq $i->attribute_value($att)->[$j]) {
		    $self->_create_AttributeValueBeforeChange($i,$att);
		    next ATT;
		}
	    }
	}
    }
    foreach my $att (grep {! $self->is_valid_attribute($_)} 
		     grep {$_ ne $DB_ID_NAME}
		     grep {!/^_/} $i->list_valid_attributes) {
	$self->_create_AttributeValueBeforeChange($i,$att);
    }
    if ($self->{'_InstanceBeforeChange'}) {
	return;
    } else {
	return 1;
    }
}

sub _create_AttributeValueBeforeChange {
    my ($self,$i,$att) = @_;
    my $avbc = GKB::Instance->new(-CLASS => '_AttributeValueBeforeChange', -ONTOLOGY => $i->ontology, 'changedAttribute' => $att);
    $avbc->inflated(1);
    if ($i->is_instance_type_attribute($att)) {
	$avbc->PreviousValue(map {$_->class_and_id} @{$i->attribute_value($att)});
    } else {
	$avbc->PreviousValue(@{$i->attribute_value($att)});
    }
    $self->_create_InstanceBeforeChange_if_necessary($i)->add_attribute_value('attributeValuesBeforeChange', $avbc);
}

sub _create_InstanceBeforeChange_if_necessary {
    my ($self,$i) = @_;
    unless ($self->{'_InstanceBeforeChange'}) {
	my $ibc = GKB::Instance->new(
				     -ONTOLOGY => $i->ontology,
				     -CLASS => '_InstanceBeforeChange',
				     'changedInstanceDB_ID' => $i->db_id
				     );
	$ibc->inflated(1);
	$self->{'_InstanceBeforeChange'} = $ibc;
    }
    return $self->{'_InstanceBeforeChange'};
}

sub get_InstanceBeforeChange {
    return $_[0]->{'_InstanceBeforeChange'};
}

sub is_being_stored {
    my $self = shift;
    if (@_) {
	$self->{'is_being_stored'} = shift;
    }
    return $self->{'is_being_stored'};
}

sub print_simple {
    my $self = shift;
    print $self->extended_displayName, "\n";
    foreach my $att (sort {$a cmp $b} $self->list_set_attributes) {
	if ($self->is_instance_type_attribute($att)) {
	    print join("\n", map {$att . "\t" . $_->extended_displayName} @{$self->attribute_value($att)});
	} else {
	    print join("\n", map {$att . "\t" . $_} @{$self->attribute_value($att)});
	}
	print "\n";
    }
    print "\n";
}

# Get some descriptive text out of the instance.
sub get_description {
	my($self) = @_;
	
	my $description = '';
	if ($self->is_valid_attribute("summation") && (defined $self->summation->[0]) && !($self->summation->[0]->text->[0] eq '')) {
		$description = $self->summation->[0]->text->[0];
		
		# Get rid of most of the warning text that comes with
		# ortho-inferred events
		if ($description =~ /has been computationally inferred/) {
			my @lines = split(/<p>/, $description);
			$description = $lines[0];
		}
	} elsif ($self->is_valid_attribute("name") && (defined $self->name->[0])) {
		my $name;
		foreach $name (@{$self->name}) {
			$description .= "$name; ";
		}
	} elsif ($self->is_valid_attribute("activity") && (defined $self->activity->[0])) {
		$description .= $self->activity->[0]->_displayName->[0];
	} elsif ($self->is_valid_attribute("comment") && (defined $self->comment->[0])) {
		$description .= $self->comment->[0];
	}
	
	return $description;
}

sub status {
    my $self = shift;
    if (@_) {
	$self->{'status'} = shift;
    }
    return $self->{'status'};
}

sub is_ghost {
    my $self = shift;
    return $self->attribute_value('__is_ghost')->[0];
}

sub deflate {
    my $self = shift;
    #print STDERR $self->class_and_id, "\n", (caller(0))[3], "\n", $self->stack_trace_dump, "\n";
    my $tmp = $self->{'attribute'}->{'_displayName'}->[0];
    delete $self->{'attribute'};
    $self->{'attribute'}->{'_displayName'}->[0] = $tmp;
    delete $self->{'inflated'};
    delete $self->{'reverse_attribute'};
    delete $self->{'reverse_attributes_attached'};
    delete $self->{'cached_things'};
}

sub set_cached_value {
    my ($self,$key,$value) = @_;
    $self->{'cached_things'}->{$key} = $value;
}

sub get_cached_value {
    my ($self,$key) = @_;
    return $self->{'cached_things'}->{$key};
}

sub get_cached_value_keys {
    return keys %{$_[0]->{'cached_things'}};
}

sub delete_cached_value {
    my ($self,$key) = @_;
    delete $self->{'cached_things'}->{$key};
}

sub deflate_attribute {
    my $self = shift;
    my $att = shift || $self->throw("Need attribute");
    delete $self->{'attribute'}->{$att};
    delete $self->{'inflated'};
}

sub DESTROY {
#    print $_[0]->class_and_id . " destroyed.\n";
#    print STDERR $_[0]->class_and_id, "\n", (caller(0))[3], "\n", $_[0]->stack_trace_dump, "\n";
    foreach my $k (keys %{$_[0]}) {
	delete $_[0]->{$k};
    }
#    delete $_[0]->{'attribute'};
#    delete $_[0]->{'reverse_attribute'};
#    delete $_[0]->{'dba'};
}

sub follow_class_attributes {
    my $self = shift;
    my ($instructions,$out_classes,$out_condition) =
	$self->_rearrange([qw(
			      INSTRUCTIONS
			      OUT_CLASSES
			      OUT_CONDITION
			      )], @_);
			      
    # Check that attributes are valid and of type instance so that it won't have to be
    # checked inside the loop below.
    foreach my $class (keys %{$instructions}) {
		if (my $ar = $instructions->{$class}->{'attributes'}) {
		    foreach my $att (@{$ar}) {
				# This throws in itself
				$self->ontology->check_class_attribute($class,$att);
				# This has to be handled "manually"
				$self->ontology->is_instance_type_class_attribute($class,$att) ||
				    $NO_SCHEMA_VALIDITY_CHECK || 
				    $self->throw("Attribute '$att' of class '$class' is not an instance type attribute.");
		    }
		}
		if (my $ar = $instructions->{$class}->{'reverse_attributes'}) {
		    foreach my $att (@{$ar}) {
			$self->ontology->is_valid_class_reverse_attribute($class,$att) ||
			    $NO_SCHEMA_VALIDITY_CHECK || 
			    $self->throw("Class '$class' does not have a reverse attribute '$att'.");
		    }
		}
    }
    
    my %seen = (($self->db_id || "$self") => 1);
    my @list = ($self);
    my @out;
    while (my $current = shift @list) {
		foreach my $class (keys %{$instructions}) {
		    if ($current->is_a($class)) {
				if (my $ar = $instructions->{$class}->{'attributes'}) {
				    foreach my $att (@{$ar}) {
					push @list, grep {! $seen{$_->db_id||"$_"}++} @{$current->attribute_value($att)};
				    }
				}
				if (my $ar = $instructions->{$class}->{'reverse_attributes'}) {
				    foreach my $att (@{$ar}) {
					push @list, grep {! $seen{$_->db_id||"$_"}++} @{$current->reverse_attribute_value($att)};
				    }
				}		
		    }
		}
		
		if ($out_classes) {
		    foreach my $class (@{$out_classes}) {
				if ($current->is_a($class)) {
				    push @out, $current;
				    last;
				}
		    }
		} else {
		    push @out, $current;
		}
    }
    
    # Understands conditionals of the form:
    # attribute='referenceDatabase.name', conditional='=', value='ChEBI'
    # and uses them to filter @out if defined.
    my @new_out = ();
    if ($out_condition) {
    	my @nested_attributes;
    	my $nested_attribute;
    	my $found_value;
    	foreach my $condition (@{$out_condition}) {
			my $attribute = $condition->{'attribute'};
			my $comparator = $condition->{'comparator'};
			my $value = $condition->{'value'};
				
			if ($comparator eq '=') {
				while (my $current = shift @out) {
					@nested_attributes = split(/\./, $attribute);
					$found_value = $current;
					foreach $nested_attribute (@nested_attributes) {
						if ($found_value->is_valid_attribute($nested_attribute) && scalar(@{$found_value->$nested_attribute})>0) {
							$found_value = $found_value->$nested_attribute->[0];
						} else {
							$found_value = undef;
							last;
						}
					}
						
					if (defined $found_value && $found_value eq $value) {
						push @new_out, $current;
					}
				}
			}
    	}
    } else {
    	@new_out = @out;
    }
    
    return \@new_out;
}

sub follow_class_attributes2 {
    my $self = shift;
    my ($instructions,$out_classes) =
	$self->_rearrange([qw(
			      INSTRUCTIONS
			      OUT_CLASSES
			      )], @_);
    # Check that attributes are valid and of type instance so that it won't have to be
    # checked inside the loop below.
    foreach my $class (keys %{$instructions}) {
	if (my $ar = $instructions->{$class}->{'attributes'}) {
	    foreach my $att (@{$ar}) {
		# This throws in itself
		$self->ontology->check_class_attribute($class,$att);
	    }
	}
	if (my $ar = $instructions->{$class}->{'reverse_attributes'}) {
	    foreach my $att (@{$ar}) {
		$self->ontology->is_valid_class_reverse_attribute($class,$att) ||
		    $self->throw("Class '$class' does not have a reverse attribute '$att'.");
	    }
	}
    }
    my %seen = ($self->db_id => 1);
    my @list = ($self);
    my @out;
    while (my $current = shift @list) {
	foreach my $class (keys %{$instructions}) {
	    if ($current->is_a($class)) {
		if (my $ar = $instructions->{$class}->{'attributes'}) {
		    foreach my $att (@{$ar}) {
			if ($self->ontology->is_instance_type_class_attribute($class,$att)) {
			    push @list, grep {! $seen{$_->db_id}++} @{$current->attribute_value($att)};
			} else {
			    push @out, @{$current->attribute_value($att)};
			}
		    }
		}
		if (my $ar = $instructions->{$class}->{'reverse_attributes'}) {
		    foreach my $att (@{$ar}) {
			push @list, grep {! $seen{$_->db_id}++} @{$current->reverse_attribute_value($att)};
		    }
		}		
	    }
	}
	if ($out_classes) {
	    foreach my $class (@{$out_classes}) {
		if ($current->is_a($class)) {
		    push @out, $current;
		    last;
		}
	    }
	} else {
	    push @out, $current;
	}
    }
    return \@out;
}

sub follow_class_attributes3 { #takes stochiometry into account
    my $self = shift;
    my ($instructions,$out_classes) =
	$self->_rearrange([qw(
			      INSTRUCTIONS
			      OUT_CLASSES
			      )], @_);
    # Check that attributes are valid and of type instance so that it won't have to be
    # checked inside the loop below.
    foreach my $class (keys %{$instructions}) {
	if (my $ar = $instructions->{$class}->{'attributes'}) {
	    foreach my $att (@{$ar}) {
		# This throws in itself
		$self->ontology->check_class_attribute($class,$att);
		# This has to be handled "manually"
		$self->ontology->is_instance_type_class_attribute($class,$att) ||
		    $self->throw("Attribute '$att' of class '$class' is not an instance type attribute.");
	    }
	}
	if (my $ar = $instructions->{$class}->{'reverse_attributes'}) {
	    foreach my $att (@{$ar}) {
		$self->ontology->is_valid_class_reverse_attribute($class,$att) ||
		    $self->throw("Class '$class' does not have a reverse attribute '$att'.");
	    }
	}
    }
    #my %seen = ($self->db_id => 1);
    my @list = ($self);
    my @out;
    while (my $current = shift @list) {
	foreach my $class (keys %{$instructions}) {
	    if ($current->is_a($class)) {
		if (my $ar = $instructions->{$class}->{'attributes'}) {
		    foreach my $att (@{$ar}) {
			push @list, @{$current->attribute_value($att)};
		    }
		}
		if (my $ar = $instructions->{$class}->{'reverse_attributes'}) {
		    foreach my $att (@{$ar}) {
			push @list, @{$current->reverse_attribute_value($att)};
		    }
		}		
	    }
	}
	if ($out_classes) {
	    foreach my $class (@{$out_classes}) {
		if ($current->is_a($class)) {
		    push @out, $current;
		    last;
		}
	    }
	} else {
	    push @out, $current;
	}
    }
    return \@out;
}


sub get_instance_type_attribute_values {
    my $self = shift;
    my @out;
    foreach my $att (grep {$self->is_instance_type_attribute($_)} $self->list_set_attributes) {
	push @out, @{$self->attribute_value($att)};
    }
    return \@out;
}

sub dumper_string {
    my ($self,$depth) = @_;
    $depth ||= 3;
    my $o = $self->ontology;
    $self->ontology("$o");
    my $dba = $self->dba;
    $dba && $self->dba("$dba");
    $Data::Dumper::Maxdepth = $depth;
    my $out = Dumper($self);
    $self->ontology($o);
    $dba && $self->dba($dba);
    return $out;
}

sub _taxon2species_hack {
    my ($self,$att) = @_;
    if ($att 
	&& ($att eq 'taxon') 
	&& (! $self->ontology->class($self->class)->{'attribute'}->{$att}) 
	&& $self->ontology->class($self->class)->{'attribute'}->{'species'}) {
	return 'species';
    }
    return $att;
}

sub remote_attribute_value {
    my ($self,$str) = @_;
    my @tmp = split(/\./, $str);
    my @in = ($self);
    my @out;
    foreach my $att (@tmp) {
	@out = ();
	foreach my $i (@in) {
	    push @out, @{$i->attribute_value($att)};
	}
	@in = @out;
    }
    return \@out;
}

1;
