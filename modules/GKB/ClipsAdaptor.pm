package GKB::ClipsAdaptor;

# Copyright 2002 Imre Vastrik <vastrik@ebi.ac.uk>

use GKB::Ontology;
#use GKB::Schema;
use GKB::Instance;
use GKB::InstanceCache;
use GKB::Utils;
use Data::Dumper;
use File::Spec;
use vars qw(@ISA);
use strict;
use Bio::Root::IO;
@ISA = qw(Bio::Root::IO);


sub new2 {
    my ($caller, @args) = @_;
    my $self = $caller->SUPER::new(@args);
    my ($schema,$debug) = $self->_rearrange
	(
	 [qw(SCHEMA DEBUG)],@args
	 );
    $self->instance_cache(GKB::InstanceCache->new(-DEBUG => $debug));
    require GKB::Schema;
    $self->schema($schema || GKB::Schema->new());
    $self->debug($debug);
    return $self;
}

sub new {
    my ($caller, @args) = @_;
    my $self = bless {}, $caller;
    my ($ontology,$debug,$file,$schema,$exclude_hr) = $self->_rearrange
	(
	 [qw(ONTOLOGY DEBUG FILE SCHEMA EXCLUDE_CLASS_ATTRIBUTES)],@args
	 );
    $self->instance_cache(GKB::InstanceCache->new(-DEBUG => $debug));
    $self->ontology($ontology || GKB::Ontology->new());
    $self->debug($debug);
    if ($file && ($file =~ s/pprj$//) && -T "$ {file}pprj" && -T "$ {file}pont" && -T "$ {file}pins") {
	$self->_attach_pprj_file_content("$ {file}pprj");
	$self->_initialize_io(-FILE => "$ {file}pont");
	$self->fetch_ontology;
	$self->_initialize_io(-FILE => "$ {file}pins");
    } else {
	$self->_initialize_io(@args);
    }
    $exclude_hr && $self->_set_exclude_list($exclude_hr);
    return $self;
}

sub _set_exclude_list {
    my ($self,$hr) = @_;
    my %h;
    while (my ($cls,$ar) = each %{$hr}) {
#	print STDERR "$cls ",@{$ar}, "\n";
	map {$h{$cls}->{$_} = 1} @{$ar};
	foreach my $d ($self->ontology->descendants($cls)) {
#	    print STDERR "\t$d ",@{$ar}, "\n";
	    map {$h{$d}->{$_} = 1} @{$ar};
	}
    }
    $self->{'exclude'} = \%h;
}

sub _attach_pprj_file_content {
    my ($self,$pprj_file) = @_;
    $self->_initialize_io(-FILE => $pprj_file);
    local $/ = undef;
    $self->ontology->pprj_file_content($self->_readline);
}

sub attach_pins_file_stub {
    my $self = shift;
    local $/ = undef;
    $self->ontology->pins_file_stub($self->_readline);
}

sub fetch_ontology {
    my ($self) =@_;
    $self->debug && print "", (caller(0))[3], "\n";
    my $ontology = $self->ontology;
    my ($class,$hr,$attribute,$type,$pont_file_content);
    while (defined($_ = $self->_readline)){
	$pont_file_content .= $_;
	chomp;
	$_ || next;
	_dehex(\$_);
	if (/^\(defclass (\S+)/) {
	    $class = $1;
	    $class =~ s/\+/_s_/g;
	    $ontology->get_class_hash($class);
	    undef $attribute;
	    undef $type;
	    next;
	}
	$class || next;
	if (/^\s*\(is-a (.+)\)$/) {
	    # Skip the 'USER' class since the .pont file doesn't contain any information about it.
	    # As the result classes which have just 'USER' as parent will be root classes.
	    my @tmp = split(/ /, $1);
	    foreach (@tmp) {
		s/\+/_s_/g;
	    }
#	    foreach my $parent (grep {$_ ne 'USER' and $_ ne ':DIRECTED-BINARY-RELATION'} split(/ /, $1)) {
	    foreach my $parent (grep {$_ ne 'USER' and $_ ne ':DIRECTED-BINARY-RELATION'} @tmp) {
		$ontology->get_class_hash($parent);
		$ontology->parents($class,$parent);
	    }
	    next;
	}
	if (/^\s*\(role abstract/) {
	    $ontology->is_abstract_class($class);
	    next;
	}
	if (/^\s*\(multislot (\S+)/) {
	    $attribute = $1;
	    $attribute =~ s/^\:/_colon_/;
	    $attribute = 'name' if ($attribute eq 'name_');
	    $ontology->is_multivalue_class_attribute($class,$attribute,1);
	    next;
	}
	if (/^\s*\(single-slot (\S+)/) {
	    $attribute = $1;
	    $attribute =~ s/^\:/_colon_/;
	    $attribute = 'name' if ($attribute eq 'name_');
	    $ontology->is_multivalue_class_attribute($class,$attribute,0);
	    next;
	}
	if (/^\s*\(type (\S+)\)/) {
	    $attribute || $self->throw("No attribute defined.");
	    $type = $1;
	    $ontology->class_attribute_origin($class,$attribute,$class);
	    if ($type eq 'INSTANCE') {
		$ontology->class_attribute_type($class,$attribute,'db_instance_type');
	    } elsif ($type eq 'INTEGER') {
		$ontology->class_attribute_type($class,$attribute,'db_integer_type');
	    } elsif ($type eq 'STRING') {
		$ontology->class_attribute_type($class,$attribute,'db_string_type');
	    } elsif ($type eq 'FLOAT') {
		$ontology->class_attribute_type($class,$attribute,'db_float_type');
	    } else {
		$ontology->class_attribute_type($class,$attribute,'db_string_type');
	    }
	    next;
	}
	if (/^;\+\s*\(allowed-classes ?(.*)\)$/) {
	    $attribute || $self->throw("No attribute defined.");
#	    print "$class,$attribute,$_\n";
	    foreach my  $allowed (split(/ /, $1)) {
		next if ($allowed =~ /^:/);
		#print "$class,$attribute,$allowed\n";
		$ontology->class_attribute_allowed_classes($class,$attribute,$allowed);
	    }
	    next;
	}
	if (/^;\+\s*\(user-facet _identityDefinedBy (ANY|ALL|none)/) {
	    $attribute || $self->throw("No attribute defined.");
	    if ($1 eq 'ANY') {
		$ontology->class_attribute_check($class,$attribute,'any');
	    } elsif ($1 eq 'ALL') {
		$ontology->class_attribute_check($class,$attribute,'all');
	    } else {
		$ontology->class_attribute_check($class,$attribute);
	    }
#	    print "$class\t$attribute\tall\t$1\t", ($hr->{$class}->{'check'}->{'all'}->{$attribute} || 'undef'), "\n";
	    next;
	}
	if (/^;\+\s*\(user-facet _databaseColumnSpecification "(.+)"/) {
	    $attribute || $self->throw("No attribute defined.");
	    $ontology->class_attribute_db_col_type($class,$attribute,$1);
	    next;
	}
	if (/^;\+\s*\(user-facet _attributeCategory (MANDATORY|REQUIRED|OPTIONAL|NOMANUALEDIT)/) {
	    $attribute || $self->throw("No attribute defined.");
	    if ($1 eq 'MANDATORY') {
		$ontology->attribute_category_check($class,$attribute,'MANDATORY');
	    } elsif ($1 eq 'REQUIRED') {
		$ontology->attribute_category_check($class,$attribute,'REQUIRED');
	    } elsif ($1 eq 'OPTIONAL') {
		$ontology->attribute_category_check($class,$attribute,'OPTIONAL');
	    } elsif ($1 eq 'NOMANUALEDIT') {
		$ontology->attribute_category_check($class,$attribute,'NOMANUALEDIT');
	    }
	    next;
	}
	if (/^;\+\s*\(inverse-slot\s+(.+)\)/) {
	    $attribute || $self->throw("No attribute defined.");
	    $ontology->inverse_attribute($class,$attribute,$1);
	    next;
	}
	if (/^;\+\s*\(cardinality\s+(\d+)\s+(.+?)\)/) {
	    $attribute || $self->throw("No attribute defined.");
	    $ontology->class_attribute_min_cardinality($class,$attribute,$1);
	    $ontology->class_attribute_max_cardinality($class,$attribute,$2) if ($2 =~ /^\d+$/);
	    next;
	}
    }
    $ontology->pont_file_content($pont_file_content);
    $ontology->debug($self->debug);
    $ontology->initiate;
    return $ontology;
}

sub fetch_schema {
    my ($self) =@_;
    $self->debug && print "", (caller(0))[3], "\n";
    my $schema = $self->schema;
    my ($class,$hr,$attribute,$type,$pont_file_content);
    while (defined($_ = $self->_readline)){
#	print;
	$pont_file_content .= $_;
	chomp;
	$_ || next;
	_dehex(\$_);
	if (/^\(defclass (\S+)/) {
	    my $class_name = $1;
	    if ($class_name eq ':CLIPS_TOP_LEVEL_SLOT_CLASS') {
		$class = $schema;
	    } elsif ($class_name =~ /^:/) {
		$class = undef;
	    } else {
		$class = $schema->schema_item_from_cache_or_new($class_name,'SchemaClass');
		$class->set_property_value('name',$class_name);
		$schema->add_property_value('classes',$class);
	    }
	    undef $attribute;
	    undef $type;
	    next;
	}
	$class || next;
	if (/^\s*\(is-a (.+)\)$/) {
	    # Skip the 'USER' class since the .pont file doesn't contain any information about it.
	    # As the result classes which have just 'USER' as parent will be root classes.
	    foreach my $super_class (grep {$_ ne 'USER'} split(/ /, $1)) {
		$super_class = $schema->schema_item_from_cache_or_new($super_class,'SchemaClass');
		$class->add_property_value('super_classes',$super_class);
	    }
	    next;
	}
	if (/^\s*\(role abstract/ && $class->isa('GKB::SchemaClass')) {
	    $class->set_property_value('abstract','TRUE');
	    next;
	}
	if (/^\s*\(multislot (\S+)/) {
	    my $attribute_name = $1;
	    $attribute_name = 'name' if ($attribute_name eq 'name_');
	    my $key = ($schema == $class) ? $attribute_name : $class->name . ":" . $attribute_name;
	    $attribute = $schema->schema_item_from_cache_or_new($key,'SchemaClassAttribute');
	    $class->add_property_value('attributes',$attribute);
	    $attribute->set_property_value('class',$class) if ($schema != $class);
	    $attribute->set_property_value('multiple','TRUE');
	    $attribute->set_property_value('name',$attribute_name);
	    next;
	}
	if (/^\s*\(single-slot (\S+)/) {
	    my $attribute_name = $1;
	    $attribute_name = 'name' if ($attribute_name eq 'name_');
	    my $key = ($schema == $class) ? $attribute_name : $class->name . ":" . $attribute_name;
	    $attribute = $schema->schema_item_from_cache_or_new($key,'SchemaClassAttribute');
	    $class->add_property_value('attributes',$attribute);
	    $attribute->set_property_value('class',$class) if ($schema != $class);
	    $attribute->set_property_value('multiple',undef);
	    $attribute->set_property_value('name',$attribute_name);
	    next;
	}
	if (/^\s*\(type (\S+)\)/) {
	    $attribute || $self->throw("No attribute defined.");
	    $type = $1;
#	    $attribute->set_property_value('origin',$class) if ($class != $schema);
	    if ($type eq 'INSTANCE') {
		$attribute->set_property_value('type','db_instance_type');
	    } elsif ($type eq 'INTEGER') {
		if ($attribute->get_property_value('name')->[0] eq $DB_ID_NAME) {
		    $attribute->set_property_value('type','db_long_type');
		} else {
		    $attribute->set_property_value('type','db_integer_type');
		}
	    } elsif ($type eq 'STRING') {
		$attribute->set_property_value('type','db_string_type');
	    } elsif ($type eq 'FLOAT') {
		$attribute->set_property_value('type','db_float_type');
	    } elsif ($type eq 'SYMBOL') {
		$attribute->set_property_value('type','db_enum_type');
	    } else {
		$attribute->set_property_value('type','db_string_type');
	    }
	    next;
	}
	if (/^;\+\s*\(allowed-classes ?(.*)\)$/) {
	    $attribute || $self->throw("No attribute defined.");
	    foreach my  $allowed (split(/ /, $1)) {
		next if ($allowed =~ /^:/);
		$allowed = $schema->schema_item_from_cache_or_new($allowed,'SchemaClass');
		$attribute->add_property_value('allowed_classes',$allowed);
	    }
	    next;
	}
	if (/^;\+\s*\(user-facet _identityDefinedBy (ANY|ALL|none)/) {
	    $attribute || $self->throw("No attribute defined.");
	    if ($1 eq 'ANY') {
		$attribute->set_property_value('value_defines_instance','any');
	    } elsif ($1 eq 'ALL') {
		$attribute->set_property_value('value_defines_instance','all');
	    } else {
		$attribute->set_property_value('value_defines_instance',undef);
	    }
	    next;
	}
	if (/^;\+\s*\(user-facet _attributeCategory (MANDATORY|REQUIRED|OPTIONAL|NOMANUALEDIT)/) {
	    $attribute || $self->throw("No attribute defined.");
	    if ($1 eq 'MANDATORY') {
		$attribute->set_property_value('category','MANDATORY');
	    } elsif ($1 eq 'REQUIRED') {
		$attribute->set_property_value('category','REQUIRED');
	    } elsif ($1 eq 'OPTIONAL') {
		$attribute->set_property_value('category','OPTIONAL');
	    } elsif ($1 eq 'NOMANUALEDIT') {
		$attribute->set_property_value('category','NOMANUALEDIT');
	    } else {
		$attribute->set_property_value('category',undef);
	    }
	    next;
	}
	if (/^;\+\s*\(user-facet _databaseColumnSpecification "(.+)"/) {
	    $attribute || $self->throw("No attribute defined.");
	    $attribute->set_property_value('db_col_type',$1);
	    next;
	}
	if (/^;\+\s*\(inverse-slot\s+(.+)\)/) {
	    $attribute || $self->throw("No attribute defined.");
	    my $inverse_attribute = $schema->schema_item_from_cache_or_new($1,'SchemaClassAttribute');
	    $attribute->set_property_value('inverse_slots',$inverse_attribute);
	    next;
	}
	if (/^;\+\s*\(cardinality\s+(\d+)\s+(.+?)\)/) {
	    $attribute || $self->throw("No attribute defined.");
	    $attribute->set_property_value('min_cardinality',$1);
	    $attribute->set_property_value('max_cardinality',$2) if ($2 =~ /^\d+$/);
	    next;
	}
	if (/^\s*\(default\s+"?(.+?)"?\)/) {
	    $attribute || $self->throw("No attribute defined.");
	    if ($attribute->get_property_value('type') eq 'db_instance_type') {
		$self->throw("Can't handle default attribute values which are instances: " . $attribute->get_property_value('name'));
	    }
	    $attribute->set_property_value('default',$1);
	    next;
	}
    }
#    $schema->set_property_value('pont_file_content',$pont_file_content);
    return $schema;
}

sub _clean_classname {
    $ {$_[0]} =~ s/^:/_/;
    $ {$_[0]} =~ s/-+/_/;
}

sub fetch_instances {
    my ($self,$ontology) = @_;
    $ontology && $self->ontology($ontology);
    $self->ontology || $self->throw("Need ontology.");
    my ($id,$class,$attribute,$attribute_value,$hr,$t_hr,$instance);
    while (defined($_ = $self->_readline)){
	chomp;
#	print "<BR />$. =>$_<=\n";
	# Using empty line as a record separator (don't want to count parenthesis)
	if (/^\s*$/) {
	    ($instance,$class,$attribute,$attribute_value,$id) = (undef,undef,undef,undef,undef);
	    next;
	}
	_dehex(\$_);
	/^;/ && next;
#	print join("\t",(map {defined $_ ? $_ : 'undef'} ("HERE",$class,$id,$attribute,$_))), "\n";
	if ($_ =~ /^\(\[(\S+)\] of (\S+)/) {
	    next if ($2 =~ /^:/);
	    ($id,$class) = ($1, $2);
	    # Instances w/o any attribute values are on one line.
	    $class =~ s/\)$//;
#	    print "$class\t$id\n";
#	    print join ("\n",(keys %{$self->ontology->class_attributes($class)})), "\n";
	    $instance = $self->_cache($id);
	    # set class
	    $instance->class($class);
#	    $instance->add_attribute_value2("${class}_id",$id);
#	    $instance->add_attribute_value2("_Protege_id",$id);
	    # Although strictly speaking not true, it will be inflated once all attributes values
	    # (coming from the following lines) have been added.
	    $instance->inflated(1);
	    ($attribute,$attribute_value) = (undef,undef);
	    next;
	}
	$instance || next;
	if ($_ =~ /^\s+\((\S+)[ \"\[]*(.*)/) {
	    next if ($1 =~ /^:/);
	    $attribute = $1;
	    if ((defined $2) && ($2 ne '')) {
		$attribute_value = $2;
		# have to remove these one-by-one and cautiously
		$attribute_value =~ s/\)+$//g;
		$attribute_value =~ s/\]+$//g;
		$attribute_value =~ s/\"+$//g;
		$attribute_value =~ s/\^/ /g;
		$attribute_value =~ s/\\$//s;
		$attribute_value =~ s/\\"/"/g;
		if ($self->ontology->class_attribute_type($class,$attribute) eq 'db_instance_type') {
		    $attribute_value = $self->_cache($attribute_value);
#		    print $attribute_value, "\n";
		}
		$instance->add_attribute_value2($attribute,$attribute_value);
	    }
	    next;
	}
	if ($_ =~ /^\s+(?:\[|\")(.+)(?:\]|\")\)*$/) {
	    next if ($1 =~ /^:/);
	    $attribute || $self->throw("Parsing problem: attribute value '$1' without attribute.");
	    $attribute_value = $1;
	    $attribute_value =~ s/\^/ /g;
	    $attribute_value =~ s/\\$//s;
	    if ($self->ontology->class_attribute_type($class,$attribute) eq 'db_instance_type') {
		$attribute_value = $self->_cache($attribute_value);
#		print $attribute_value, "\n";
	    }
	    $instance->add_attribute_value2($attribute,$attribute_value);
	    next;
	}
	$self->throw("Unable to parse line $.: =>$_<=");
    }
#    $self->check_duplicate_DB_IDs;
    return $self->instance_cache;
}

sub _cache {
    my ($self,$attribute_value) = @_;
    return $self->instance_cache->fetch($attribute_value) ||
	$self->instance_cache->store($attribute_value, GKB::Instance->new
				      (
				       -ONTOLOGY => $self->ontology,
				       -ID => $attribute_value,
				       ));
}

sub ontology {
    my $self = shift;
    if (@_) {
	$self->{'ontology'} = shift;
    }
    return $self->{'ontology'};
}

sub schema {
    my $self = shift;
    if (@_) {
	$self->{'schema'} = shift;
    }
    return $self->{'schema'};
}

sub instance_cache {
    my $self = shift;
    if (@_) {
	$self->{'instance_cache'} = shift;
    }
    return $self->{'instance_cache'};
}

sub debug {
    my $self = shift;
    if (@_) {
	$self->{'debug'} = shift;
    }
    return $self->{'debug'};
}

sub _store_instance {
    my ($self,$i) = @_;
    $i->{'stored_by_ClipsAdaptor'} && return;
    my $ar = $self->_store_instance_get_attribute_instances($i);
    $self->store_instances($ar);
}

sub store_instances {
    my ($self,$instances) = @_;
#    $self->set_id($instances);
    foreach (@{$instances}) {
#	print STDERR "", (caller(0))[3], $_->extended_displayName, "\n";
	$self->_store_instance($_);
    }
}

sub store_instances_shallow {
    my ($self,$instances) = @_;
    my @ghosts;
    foreach my $i (@{$instances}) {
	push @ghosts, @{$self->_store_instance_get_attribute_instances($i)};
    }
    foreach my $i (@ghosts) {
	$self->_store_ghost($i);
    }
}

sub is_excluded_class_attribute {
    my ($self,$cls,$att) = @_;
    return $self->{'exclude'}->{$cls}->{$att};
}

sub _store_instance_get_attribute_instances {
     my ($self,$i) = @_;
     $i->{'stored_by_ClipsAdaptor'} && return [];
     # print $i->extended_displayName, "\n";
#     my $protege_id = $i->create_protege_id;
#     $self->_print("([$protege_id] of ". $i->class);
     $self->_print("([" . $i->create_protege_id . "] of " . $i->class);
     my $displayName = protege_displayName($i);
     $displayName =~ s/\\/\\\\/g;
     $displayName =~ s/\\*"/\\\"/g; #"
     $self->_print("\n\t(_displayName \"$displayName\"\)");
     my @attribute_instances;
     foreach my $att (grep {! /_(displayName|class|internal\d+|timestamp|partial|html|Protege_id)$/} $i->list_valid_attributes) {
	$self->is_excluded_class_attribute($i->class,$att) && next;
	my @vals = @{$i->attribute_value($att)};
	@vals || next;
	next if ((scalar (@vals) == 1) && ($vals[0] eq ""));
	$self->_print("\n\t($att");
	if ($i->attribute_type($att) eq 'db_instance_type') {
	    @vals = map {"[$_]"}
	    map {$_->create_protege_id ||
		     $self->throw("Need Protege id to write clips.")}
	    @vals;
	    push @attribute_instances, @{$i->attribute_value($att)};
	} else {
	    foreach (@vals) {
                s/\\/\\\\/g;
		s/\\*"/\\\"/g; #"
		s/\r\n/\\n/g;
		s/\r/\\n/g;
		s/\n/\\n/g;
		$_ = '"' . $_ . '"';
	    }
        }
	if (@vals > 1) {
	    $self->_print(join("\n\t\t","",@vals), ")");
	} else {
	    $self->_print(" @vals)");
	}
    }
    $self->_print(")\n\n");
    $i->{'stored_by_ClipsAdaptor'} = 1;
    return \@attribute_instances;
}

sub _store_ghost {
    my ($self, $i) = @_;
    $i->{'stored_by_ClipsAdaptor'} && return;
    $self->_print("([" . $i->create_protege_id . "] of " . $i->class . "\n");
    $self->_print(qq/\t($DB_ID_NAME / . $i->db_id . qq/)\n/);
    if ($i->displayName) {
	$self->_print(qq/\t(_displayName "/ . $i->displayName . qq/")\n/);
    }
    $self->_print(qq/\t(__is_ghost TRUE))\n\n/);
    $i->{'stored_by_ClipsAdaptor'} = 1;	  
    return;
}

sub set_id {
    my ($self,$instances) = @_;
    my %h;
    my @pidless;
    foreach my $i (@{$instances}) {
	if ($i->db_id) {
	    $h{$i->db_id} = $i;
	}
	if (my $pid = $i->attribute_value('_Protege_id')->[0]) {
	    $pid =~ s/\D+//g;
	    $h{$pid} = $i;
	} else {
	    push @pidless, $i;
	}
    }
    my $c = 0;
  INST: foreach my $i (@pidless) {
      while ($h{++$c}) { }
      $i->id($c);
      $h{$c} = $i;
  }
}

# just a shortcut really
sub instances {
    my ($self) = @_;
    return $self->fetch_instances->instances;
}

sub _dehex {
    $ {$_[0]} =~ s/%([\dA-Fa-f][\dA-Fa-f])/pack ("C", hex ($1))/eg;
}

sub check_duplicate_DB_IDs {
    my $self = shift;
    my %seen;
    foreach my $i (grep {$_->attribute_value($GKB::Ontology::DB_ID_NAME)->[0]} $self->instance_cache->instances) {
	if (my $val = $i->attribute_value($GKB::Ontology::DB_ID_NAME)->[0]) {
	    $self->throw("Duplicate $GKB::Ontology::DB_ID_NAME '$val'!") if ($seen{$val}++);
	}
    }
}

sub create_protege_project {
    my $self = shift;
    my ($instance_ar,
	$basename,
	$tgz,
	$dir,
	$outfh,
	$shallow,
	$dba
	) = $self->_rearrange(
			      [qw(INSTANCES
				  BASENAME
				  TGZ
				  DIR
				  OUTFH
				  SHALLOW
                                  DBA
				  )],@_
			      );
#    ($instance_ar && @{$instance_ar} && $instance_ar->[0]->isa("GKB::Instance")) ||
#	$self->throw("Need a reference to an array of GKB::Instances. Got '$instance_ar'");
    $basename ||= "PROJECT_$$";
    if ($tgz) {
	$dir = File::Spec->tmpdir;
	chdir $dir;
    } else {
	$dir ||= File::Spec->curdir;
    }
    my $ontology = ($dba) ? $dba->ontology : $instance_ar->[0]->ontology;
    (my $pprj_fc = $ontology->pprj_file_content) =~ s/\w+\.pins/$basename\.pins/gms;
    $pprj_fc =~ s/\w+\.pont/$basename\.pont/gms;
    #my $pprjfile = File::Spec->catfile($dir, "$basename.pprj");
    my $pprjfile = "$basename.pprj";
    $self->_initialize_io(-FILE => ">$pprjfile");
    $self->_print($pprj_fc);
    $self->close;
    #my $pontfile = File::Spec->catfile($dir, "$basename.pont");
    my $pontfile = "$basename.pont";
    $self->_initialize_io(-FILE => ">$pontfile");
    $self->_print($ontology->pont_file_content);
    $self->close;
    #my $pinsfile = File::Spec->catfile($dir, "$basename.pins");
    my $pinsfile = "$basename.pins";
    $self->_initialize_io(-FILE => ">$pinsfile");
    $self->_print($ontology->pins_file_stub, "\n");
    if ($shallow) {
	$self->store_instances_shallow($instance_ar);
    } else {
	$self->store_instances($instance_ar);
    }
    $self->close;
    if ($tgz) {
	require Archive::Tar;
	# For whatever reason I'm unable to write the gzipped stuff to STDOUT.
	# Hence the creation of .tar.gz file possibly followed by reading from it and deleting it. Not good.
	Archive::Tar->create_archive("$basename.tar.gz",9,$pprjfile,$pontfile,$pinsfile);
	unlink($pprjfile,$pontfile,$pinsfile);
	if ($outfh) {
	    local *IN;
	    open (IN, "$basename.tar.gz");
	    local $/ = undef;
	    print $outfh <IN>;
	    close IN;
	    unlink "$basename.tar.gz";
	} else {
	    use File::Copy;
	    use GKB::Config;
	    move("$basename.tar.gz","$GK_TMP_IMG_DIR/$basename.tar.gz");
	}
    }
}

sub protege_displayName {
    my ($i) = @_;
    my $out = $i->displayName;
    if ($i->is_a("Event")) {
	if ($i->attribute_value('species')->[0]) {
	    $out = '[' . GKB::Utils::species_abbreviation($i->Species->[0]) . '] ' . $out;
	}
    } elsif ($i->is_a("PhysicalEntity")) {
	if ($i->is_valid_attribute('species') && $i->Species->[0]) {
	    $out = '[' . GKB::Utils::species_abbreviation($i->Species->[0]) . '] ' . $out;
	}
    } elsif ($i->is_a("ReferenceSequence")) {
	if ($i->Species->[0]) {
	    $out = '[' . GKB::Utils::species_abbreviation($i->Species->[0]) . '] ' . $out;
	}
	if ($i->Description->[0]) {
	    $out .= ' ' . $i->Description->[0];
	}
    } elsif ($i->is_a("CatalystActivity")) {
	if ($i->PhysicalEntity->[0] &&
	    $i->PhysicalEntity->[0]->is_valid_attribute('species')  &&
	    $i->PhysicalEntity->[0]->Species->[0]) {
	    $out = '[' . GKB::Utils::species_abbreviation($i->PhysicalEntity->[0]->Species->[0]) . '] ' . $out;
	}
    }
    return $out;
}

1;
