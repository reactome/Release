=head1 NAME


GKB::BioMart

=head1 SYNOPSIS

A package of subroutines for exporting from Reactome to BioMart.

I made a change to create_biomart_columns in order to deal with Nelson's missing attribute problem.

=head1 DESCRIPTION


=head1 SEE ALSO

GKB::DBAdaptor

=head1 AUTHOR

David Croft E<lt>croft@ebi.ac.ukE<gt>

Copyright (c) 2006 European Bioinformatics Institute and Cold Spring
Harbor Laboratory.

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

package GKB::BioMart;

use GKB::Config;
use GKB::DBAdaptor;
use GKB::FileUtils;
use GKB::BioMart::ColDef;
use GKB::BioMart::ColDefCollection;
use GKB::BioMart::DynamicLinkInstances;
use GKB::BioMart::Utils;
use GKB::BioMart::TSVFileReader;
use GKB::BioMart::CannedQueries;
use GKB::Utils::Timer;
use GKB::FrontPage3;
use LWP::UserAgent;
use strict;
use vars qw(@ISA $AUTOLOAD %ok_field);
use Bio::Root::Root;
use Data::Dumper;

@ISA = qw(Bio::Root::Root);

my $BIOMART_MAIN_TABLE_NAME_EXTENSION = "__main";
my $BIOMART_DIMENSION_TABLE_NAME_EXTENSION = "__dm";
#my $utils = GKB::BioMart::Utils->new();

# List the object variables here, so that they can be checked
for my $attr
    (qw(
    reactome_host
    reactome_port
	reactome_db_name
	reactome_user
	reactome_pass
	reactome_dba
    biomart_host
    biomart_port
	biomart_db_name
	biomart_user
	biomart_pass
	instance_ignored_attributes
	expanded_instance_ignored_attributes
	instance_ignored_composite_attributes
	max_expanded_instance_depth
	instance_substitute_attribute
	instance_db_ids
	max_main_row_count
	attributes_hash
	allow_multiple_hash
	link_instances
	link_main_instances
	multiple_dataset_flag
	modcol_atts
	modcol_substitutes
	expand_multiple_attribute_name_hash
	expand_multiple_attribute_name_list_hash
	expand_multiple_attribute_value_hash
	dimension_export_attribute_hash
	cgi
	webutils
	utils
	part_of_attribute_hash
	canonical_depth_hash
	table_name_map
	timer
	) ) { $ok_field{$attr}++; }

sub AUTOLOAD {
    my $self = shift;
    my $attr = $AUTOLOAD;
    $attr =~ s/.*:://;
    return unless $attr =~ /[^A-Z]/;  # skip DESTROY and all-cap methods
    $self->throw("invalid attribute method: ->$attr()") unless $ok_field{$attr};
    $self->{$attr} = shift if @_;
    return $self->{$attr};
}  

sub new {
    my($pkg) = @_;

    my $self = bless {}, $pkg;
   	
   	my %ignored = ();
   	my %expanded_ignored = ();
   	my %composite_ignored = ();
   	my %multiple_attributes = ();
   	my $link_instances = GKB::BioMart::DynamicLinkInstances->new(); # default
   	my @modcol_atts = ();
   	my @modcol_substitutes = ();
   	my %expand_multiple_attribute_name_hash = ();
   	my %expand_multiple_attribute_name_list_hash = ();
   	my %expand_multiple_attribute_value_hash = ();
   	my %dimension_export_attribute_hash = ();
   	my $utils = GKB::BioMart::Utils->new();
	my %part_of_attribute_hash = ();
	my %canonical_depth_hash = ();
	my %table_name_map = ();
   	my $timer = GKB::Utils::Timer->new();
   	
   	$self->instance_ignored_attributes(\%ignored);
   	$self->expanded_instance_ignored_attributes(\%expanded_ignored);
   	$self->instance_ignored_composite_attributes(\%composite_ignored);
   	$self->allow_multiple_hash(\%multiple_attributes);
   	$self->max_expanded_instance_depth(1);
   	$self->link_instances($link_instances);
   	$self->multiple_dataset_flag(0);
   	$self->modcol_atts(\@modcol_atts);
   	$self->modcol_substitutes(\@modcol_substitutes);
   	$self->expand_multiple_attribute_name_hash(\%expand_multiple_attribute_name_hash);
   	$self->expand_multiple_attribute_name_list_hash(\%expand_multiple_attribute_name_list_hash);
   	$self->expand_multiple_attribute_value_hash(\%expand_multiple_attribute_value_hash);
   	$self->dimension_export_attribute_hash(\%dimension_export_attribute_hash);
   	$self->utils($utils);
   	$self->part_of_attribute_hash(\%part_of_attribute_hash);
   	$self->canonical_depth_hash(\%canonical_depth_hash);
	$self->table_name_map(\%table_name_map);
	$self->timer($timer);
	
    return $self;
}

sub set_cgi {
    my ($self, $cgi) = @_;
    
    $self->cgi($cgi);
}

sub set_webutils {
    my ($self, $webutils) = @_;
    
    $self->webutils($webutils);
}

# Sets the Reactome attributes that will *not* be included when
# constructing the BioMart dataset table.
# Argument is a reference to a hash.
sub set_instance_ignored_attributes {
    my ($self, $attributes) = @_;
    
    $self->instance_ignored_attributes($attributes);
    $self->link_instances->instance_ignored_attributes($attributes);
} 

# Sets the Reactome attributes in addition to instance_ignored_attributes
# that will *not* be included from expanded attributes when constructing
# the BioMart dataset table.
# Argument is a reference to a hash.
sub set_expanded_instance_ignored_attributes {
    my ($self, $attributes) = @_;
    
    $self->expanded_instance_ignored_attributes($attributes);
    $self->link_instances->expanded_instance_ignored_attributes($attributes);
}

# Sets the Reactome *composite* attributes that will *not* be included when
# constructing the LinkInstances dataset table.
# Argument is a reference to a hash.
sub set_instance_ignored_composite_attributes {
    my ($self, $attributes) = @_;
    
#    print STDERR "BioMart.set_instance_ignored_composite_attributes: entered\n";
    
    $self->instance_ignored_composite_attributes($attributes);
    $self->link_instances->instance_ignored_composite_attributes($attributes);
}

# Sets the Reactome attributes that will be allowed even
# if they are multiple, contrary to the normal behaviour,
# wich would be to exclude these attributes from BioMart
# tables.
# Argument is a reference to a hash.
sub set_allow_multiple_attributes {
    my ($self, $attributes) = @_;
    
    $self->allow_multiple_hash($attributes);
} 

# Set the maximum allowed depth for expanding single-valued
# instance attributes
sub set_max_expanded_instance_depth {
    my ($self, $max_expanded_instance_depth) = @_;
    
    $self->max_expanded_instance_depth($max_expanded_instance_depth);
} 

# Use this if you are testing: sets the max number of rows that
# will be inserted into main tables.  If you use, say, 100, then
# the script will run pretty quickly - but of course, the resulting
# tables will be incomplete.  By default, all main table rows
# will be inserted.  Using this subroutine with an undefined
# argument will result in the default behaviour.
sub set_max_main_row_count {
    my ($self, $max_main_row_count) = @_;
    
    $self->max_main_row_count($max_main_row_count);
} 

# If an attribute is of type instance, and it doesn't get expanded,
# what should we replace it with?  This is one possibility: specify
# an attribute name from within the instance itself and pull the
# value out of that instance and use that.  Likely candidates for
# this would be DB_ID or stableIdentifier.
sub set_instance_substitute_attribute {
    my ($self, $instance_substitute_attribute) = @_;
    
    $self->instance_substitute_attribute($instance_substitute_attribute);
} 

sub set_link_instances {
    my ($self, $link_instances) = @_;
    
    $self->link_instances($link_instances);
    $self->link_instances->set_instance_ignored_attributes($self->instance_ignored_attributes);
    $self->link_instances->set_expanded_instance_ignored_attributes($self->expanded_instance_ignored_attributes);
    $self->link_instances->set_instance_ignored_composite_attributes($self->instance_ignored_composite_attributes);
    $self->link_instances->set_dba($self->reactome_dba);
}

# This flag creates one dataset per main table if set to
# a non-zero value.
sub set_multiple_dataset_flag {
    my ($self, $multiple_dataset_flag) = @_;
    
    $self->multiple_dataset_flag($multiple_dataset_flag);
}

# Sets database parameters for the Reactome database.
# If no BioMart database parameters have been specified,
# default values for these will also be set.
sub set_reactome_db_params {
    my ($self, $host, $port, $db_name, $user, $pass) = @_;
    
    $self->reactome_host($host);
    $self->reactome_port($port);
    $self->reactome_db_name($db_name);
    $self->reactome_user($user);
    $self->reactome_pass($pass);
    
    # Set BioMart DB params, if not already set
    if (!(defined $self->biomart_host) && !(defined $self->biomart_port) && !(defined $self->biomart_user) && !(defined $self->biomart_pass)) {
    	$self->biomart_host($self->reactome_host);
    	$self->biomart_port($self->reactome_port);
    	if (!(defined $self->biomart_db_name)) {
    		$self->biomart_db_name($self->reactome_db_name . "_mart");
    	}
    	$self->biomart_user($self->reactome_user);
    	$self->biomart_pass($self->reactome_pass);
    }
}

# Sets database parameters for the BioMart database.
# Overwites any previously set BioMart database parameters.
sub set_biomart_db_params {
    my ($self, $host, $port, $db_name, $user, $pass) = @_;
    
    $self->biomart_host($host);
    $self->biomart_port($port);
    $self->biomart_db_name($db_name);
    $self->biomart_user($user);
    $self->biomart_pass($pass);
}

# Set only the BioMart database name.  This can be used to
# override the name that would otherwise be generated automatically
# by "set_reactome_db_params".
sub set_biomart_db_name {
    my ($self, $biomart_db_name) = @_;
    
    $self->biomart_db_name($biomart_db_name);
}

# Sets the hash of DB_IDs to limit the Reactome instances that will
# be used for creating the main table.  This will be undef by default,
# meaning that all DB_IDs will be used.  This is really only here for
# testing purposes, and is not recommended for normal use.
sub set_instance_db_ids {
    my ($self, $instance_db_ids) = @_;
    
    $self->instance_db_ids($instance_db_ids);
}

# Sets the parameters associated with modified, additional
# columns.
sub set_modcol {
    my ($self, $modcol_atts, $modcol_substitutes) = @_;
    
    print STDERR "Biomart.set_modcol: lets get setting then\n";
    print STDERR "Biomart.set_modcol: modcol_atts[0]=" . $modcol_atts->[0] . "\n";
    print STDERR "Biomart.set_modcol: modcol_atts[1]=" . $modcol_atts->[1] . "\n";
    
    if (!$modcol_atts) {
    	print STDERR "Biomart.set_modcol: WARNING - modcol_atts not defined!!\n";
    	return;
    }
    if (!$modcol_substitutes) {
    	print STDERR "Biomart.set_modcol: WARNING - modcol_substitutes not defined!!\n";
    	return;
    }
    if (scalar(@{$modcol_atts}) != scalar(@{$modcol_substitutes})) {
    	print STDERR "Biomart.set_modcol: WARNING - modified column class count (" . scalar(@{$modcol_atts}) . ") is different from modified column attribute count (" . scalar(@{$modcol_substitutes}) . ")!!\n";
    	return;
    }
    
    $self->modcol_atts($modcol_atts);
    $self->modcol_substitutes($modcol_substitutes);
}

# Used to modify the way that Mart table names are generated.  Normally,
# table names are constructed based on Reactome instance names.  But,
# as time goes by, these instance names can change, as a result of
# data model changes.  So, you can use this hash to make new instance
# classes compatible with an existing Mart web interface, by mapping
# them onto their 'old' names.
sub set_table_name_map {
    my ($self, $table_name_map) = @_;
    
    $self->table_name_map($table_name_map);
}

# Establish connection to Reactome database
sub create_reactome_dba {
    my ($self) = @_;
    
	my $dba = GKB::DBAdaptor->new(-port => $self->reactome_port,
								  -user => $self->reactome_user,
								  -host => $self->reactome_host,
								  -pass => $self->reactome_pass,
								  -dbname => $self->reactome_db_name);
    
    $self->reactome_dba($dba);
    $self->link_instances->set_dba($dba);
}

# Create new Mart database if one doesn't already exist,
# and establish a connection to it.  You may optionally
# provide a stem, which will be added to the name of the
# database to be created.
sub create_biomart_dba {
    my ($self, $stem) = @_;
    
    if (!(defined $stem)) {
    	$stem = '';
    } else {
    	$stem = lc($stem);
    	$stem =~ s/^_*/_/;
    }

	my $dba = GKB::DBAdaptor->new(-port => $self->biomart_port,
								  -user => $self->biomart_user,
								  -host => $self->biomart_host,
								  -pass => $self->biomart_pass,
								  -no_ontology_flag => 1);

	my $db_name = $self->biomart_db_name;
	if (defined $stem) {
		if ($db_name =~ /_mart$/) {
			my $stem_mart = $stem . "_mart";
			$db_name =~ s/_mart$/$stem_mart/;
		} else {
			$db_name .= $stem;
		}
	}
	my $statement = "CREATE DATABASE IF NOT EXISTS " . $db_name;
    $dba->execute($statement);
    $dba->db_name($db_name);
    
    $statement = "USE " . $db_name;
    $dba->execute($statement);
    
    return $dba;
}

# Close the databases
sub close_dbas {
	my ($self, $col_def_collection_hash) = @_;
	
	$self->reactome_dba->db_handle->disconnect;
	
	if ($col_def_collection_hash) {
		my $col_def_collection;
		my $class;
		foreach $class (keys(%{$col_def_collection_hash})) {
			$col_def_collection = $col_def_collection_hash->{$class};
			$col_def_collection->close_dbas();
		}
	}
}

# Takes data from a Reactome database and generates a
# corresponding BioMart database.  Requires a list of
# Reactome instance class names and a list of
# corresponding attribute names, one per instance
# class.  One BioMart dataset will be created per
# instance class, and the attribute names will be
# used as the primary keys, so make sure you choose
# an attribute whose values are unique for this
# purpose.  The dimsnsion classes determine which
# dimension tables will be constructed, and if they
# have dimension attributes, which dimension table
# attributes will be incorporated into the main table.
# NN: Added new agument ($dimension_directions) with information on which hieracrchy direction is to be followed
sub generate_biomart_database {
    my ($self, $reactome_instance_classes, $reactome_attributes, $dimension_classes, $dimension_attributes, $dimension_directions) = @_;
    
    print STDERR "Biomart.generate_biomart_database: entered\n";
    
    if (!$reactome_instance_classes) {
    	print STDERR "Biomart.generate_biomart_database: WARNING - reactome_instance_classes not defined!!\n";
    	return;
    }
    if (!$reactome_attributes) {
    	print STDERR "Biomart.generate_biomart_database: WARNING - reactome_attributes not defined!!\n";
    	return;
    }
    if (scalar(@{$reactome_instance_classes}) != scalar(@{$reactome_attributes})) {
    	print STDERR "Biomart.generate_biomart_database: WARNING - main class count (" . scalar(@{$reactome_instance_classes}) . ") is different from main attribute count (" . scalar(@{$reactome_attributes}) . ")!!\n";
    	return;
    }
    # TODO: NN: Need to do a similar error checking as as done below so that my '$dimension_directions' sent over from martify_reactome.PL
    # to this sub as an additional argument is checked for consistency the same way the 'if' and 'else' statements are doing below
    if (($dimension_classes && !$dimension_attributes) || (!$dimension_classes && $dimension_attributes)) {
    	print STDERR "Biomart.generate_biomart_database: WARNING - both dimension_classes and dimension_attributes must be defined!!\n";
    	return;
    } else {
    	if (scalar(@{$dimension_classes}) != scalar(@{$dimension_attributes})) {
    		print STDERR "Biomart.generate_biomart_database: WARNING - dimension class count (" . scalar(@{$dimension_classes}) . ") is different from main attribute count (" . scalar(@{$dimension_attributes}) . ")!!\n";
    		return;
    	}
    }
    
	$self->create_reactome_dba();
	
	my $utils = $self->utils();
	
	# This evil black magic is necessary because the subroutine
	# in Ontology that gets the attributes for an instance class
	# can do funny things if you call it more than once.  These
	# calls will cache the attributes in $utils.
	$utils->sanitize_class_attributes($self->reactome_dba, $reactome_instance_classes);
	$utils->sanitize_class_attributes($self->reactome_dba, $dimension_classes);
	
	my $i;
	my $j;
	my @all_classes = ();

	# Create initial dimension table (empty) ColDefCollections
	my %col_def_collection_hash = ();
	# Create initial main table (empty) ColDefCollections
	for ($i=0; $i<scalar(@{$reactome_instance_classes}); $i++) {
		my $reactome_instance_class = $reactome_instance_classes->[$i];
		my $reactome_primary_key_attribute = $reactome_attributes->[$i];
		
    	my $col_def_collection = GKB::BioMart::ColDefCollection->new();
    	$col_def_collection->reactome_instance_class($reactome_instance_class);
    	$col_def_collection->attribute_key($reactome_primary_key_attribute);
    	my $dba;
    	if ($self->multiple_dataset_flag) {
    		$dba = $self->create_biomart_dba($reactome_instance_class);
    	} else {
    		$dba = $self->create_biomart_dba();
    	}
		$col_def_collection->add_dba($reactome_instance_class, $dba);

		$col_def_collection_hash{$reactome_instance_class} = $col_def_collection;
		
		push(@all_classes, $reactome_instance_class);
	}
    
	for ($i=0; $i<scalar(@{$dimension_classes}); $i++) {
		my $dimension_key = $dimension_attributes->[$i];
		my $dimension_class = $dimension_classes->[$i];
		# NN: Get the link direction for each main class
		my $dimension_direction = $dimension_directions->[$i];
		
    	my $col_def_collection = GKB::BioMart::ColDefCollection->new();
    	$col_def_collection->reactome_instance_class($dimension_class);
    	# NN: Add the dimension_direction to the dimension class collection
		if (defined $dimension_direction && !($dimension_direction eq '')) {
    		$col_def_collection->dimension_direction($dimension_direction);
		}
		if (defined $dimension_key && !($dimension_key eq '')) {
    		$col_def_collection->attribute_key($dimension_key);
		}
		# Put in one DBA for each of the main classes
		for ($j=0; $j<scalar(@{$reactome_instance_classes}); $j++) {
			my $reactome_instance_class = $reactome_instance_classes->[$j];
			my $dba = $col_def_collection_hash{$reactome_instance_class}->get_dba($reactome_instance_class);
			$col_def_collection->add_dba($reactome_instance_class, $dba);
			
			if (!$self->multiple_dataset_flag) {
				last;
			}
		}
		
		$col_def_collection_hash{$dimension_class} = $col_def_collection;
		
		push(@all_classes, $dimension_class);
	}
    
	# Create expanded columns.
	# First, for each column to be expanded, collect together
	# all unique values for the name attribute
	my @expand_multiple_attribute_classes = keys(%{$self->expand_multiple_attribute_name_hash});
	foreach my $expand_multiple_attribute_class (@expand_multiple_attribute_classes) {
		# Loop over both main and dimension classes
		for (my $i=0; $i<scalar(@all_classes); $i++) {
			my $class = $all_classes[$i];
				
			print STDERR "Biomart.generate_biomart_database: class=$class\n";
			print STDERR "Biomart.generate_biomart_database: expand_multiple_attribute_class=$expand_multiple_attribute_class\n";
			
			# Compare main/dimension class with the class to be expanded
			# and do something if they are the same, otherwise skip.
			if (!($self->reactome_dba->ontology->is_a($class, $expand_multiple_attribute_class))) {
				next;
			}
			
			$self->expand_multiple_attribute_name_list_hash->{$expand_multiple_attribute_class} = $self->generate_expand_multiple_attribute_name_list($expand_multiple_attribute_class);
				
			last;
		}
	}

	# Create main tables
	my %link_class_hash = ();
	$self->link_main_instances(GKB::BioMart::StaticLinkInstances->new());
    $self->link_main_instances->set_instance_ignored_attributes($self->instance_ignored_attributes);
    $self->link_main_instances->set_expanded_instance_ignored_attributes($self->expanded_instance_ignored_attributes);
    $self->link_main_instances->set_instance_ignored_composite_attributes($self->instance_ignored_composite_attributes);
    $self->link_main_instances->set_dba($self->reactome_dba);
	for (my $i=0; $i<scalar(@{$reactome_instance_classes}); $i++) {
		my $reactome_instance_class = $reactome_instance_classes->[$i];
		my $reactome_primary_key_attribute = $reactome_attributes->[$i];
		
		print STDERR "BioMart.generate_biomart_database: reactome_instance_class=$reactome_instance_class\n";
		
		my $col_def_collection = $col_def_collection_hash{$reactome_instance_class};

		# Extract column definitions for the given main instance
		# class.
		$self->create_biomart_columns($col_def_collection, $reactome_instance_class, $reactome_primary_key_attribute, "", 0);
		
		my $key_col_name = $self->create_key_column($reactome_instance_class, $reactome_primary_key_attribute);
    	my $attribute_column_definitions = $self->create_key_column_definitions($reactome_instance_class, $key_col_name);

		my $key_col_name_in_other_main_tables = $key_col_name;
		my $attribute_column_definitions_in_other_main_tables = $attribute_column_definitions;
		if ($self->multiple_dataset_flag) {
			# Don't put attributes with the _key extension into
			# other main tables if the tables are being split
			# across multiple datasets.
			$key_col_name_in_other_main_tables =~ s/_key$//;
			$attribute_column_definitions_in_other_main_tables = "$key_col_name_in_other_main_tables INT(10)";
			
			# Duplicate the key column without the _key extension -
			# this is used for linking main tables.
			my $duplicate_key_col_def = GKB::BioMart::ColDef->new($key_col_name_in_other_main_tables, $key_col_name_in_other_main_tables, $attribute_column_definitions_in_other_main_tables);
			$duplicate_key_col_def->put_info("link_direction", "to");
			$col_def_collection->add($duplicate_key_col_def);
		}
		
		# Look for unidirectional dependancies between main tables,
		# and insert indexing where this occurs
		for (my $j=$i+1; $j<scalar(@{$reactome_instance_classes}); $j++) {
			my $link_instance_class = $reactome_instance_classes->[$j];
			my @valid_paths = $self->link_main_instances->generate_path_source_class_to_target_class($reactome_instance_class, $link_instance_class, 1);

			if (scalar(@valid_paths)<1) {
				next;
			}
			
			# Since these are invented columns, give them identical
			# Reactome and BioMart names.
			my $link_col_def_collection = $col_def_collection_hash{$link_instance_class};
			my $link_col_def = GKB::BioMart::ColDef->new($key_col_name_in_other_main_tables, $key_col_name_in_other_main_tables, $attribute_column_definitions_in_other_main_tables);
			$link_col_def->put_info("link_class", $reactome_instance_class);
			$link_col_def->put_info("link_direction", "from");
			$link_col_def_collection->add($link_col_def);
		}
			
		# Assume there are dependancies between all main tables
		# and all dimension tables, and insert indexing where this occurs
		foreach my $dimension_class (sort {$a cmp $b} @{$dimension_classes}) {
			# Since these are invented columns, give them identical
			# Reactome and BioMart names.
			my $dimension_col_def_collection = $col_def_collection_hash{$dimension_class};
			my $dimension_col_def = GKB::BioMart::ColDef->new($key_col_name, $key_col_name, $attribute_column_definitions);
			$dimension_col_def_collection->add($dimension_col_def);
		}
			
		# Note which main tables need back-coupling from which
		# dimension tables and create appropriate column definitions.
		my $dimension_export_attributes;
		my $dimension_export_attribute;
		foreach my $dimension_class (sort {$a cmp $b} @{$dimension_classes}) {
			
			print STDERR "BioMart.generate_biomart_database: dimension_class=$dimension_class\n";
			
			$dimension_export_attributes = $self->dimension_export_attribute_hash->{$dimension_class};
			foreach $dimension_export_attribute (@{$dimension_export_attributes}) {
				# Since these are invented columns, give them identical
				# Reactome and BioMart names.
				my $key_col_name;
				my $boolean_col_name;
		    	($key_col_name, $boolean_col_name) = $self->create_export_attribute_col_names($dimension_class, "_$dimension_export_attribute");
				
				print STDERR "BioMart.generate_biomart_database: key_col_name=$key_col_name, boolean_col_name=$boolean_col_name\n";
				
				my $col_def1 = GKB::BioMart::ColDef->new($key_col_name, $key_col_name, $key_col_name . " LONGTEXT default NULL");
				$col_def_collection->add($col_def1);
#				my $col_def2 = GKB::BioMart::ColDef->new($boolean_col_name, $boolean_col_name, $boolean_col_name . " BIGINT(1) default NULL");
				my $col_def2 = GKB::BioMart::ColDef->new($boolean_col_name, $boolean_col_name, $boolean_col_name . " INT(1) UNSIGNED default NULL");
				$col_def_collection->add($col_def2);
			}
		}
	
		
		# Create a canonical column, if appropriate
		if (defined $self->part_of_attribute_hash->{$reactome_instance_class} && defined $self->canonical_depth_hash->{$reactome_instance_class}) {
			# Since these are invented columns, give them identical
			# Reactome and BioMart names.
			my $col_def = GKB::BioMart::ColDef->new("is_canonical", "is_canonical", "is_canonical INT(1) UNSIGNED default NULL");
			$col_def_collection->add($col_def);
		}

		# Create expanded columns.
		# Now generate the expanded columns
		$self->create_expanded_columns($reactome_instance_class, $col_def_collection);
		
		# Go through the listed modified columns and add appropriately
		# named additional columns to the list.
		$self->create_modcols($col_def_collection);
	}
		
	# Need to loop again to do this, because extra columns may have been
	# added.
	for (my $i=0; $i<scalar(@{$reactome_instance_classes}); $i++) {
		my $reactome_instance_class = $reactome_instance_classes->[$i];
		my $col_def_collection = $col_def_collection_hash{$reactome_instance_class};
		
		my @reactome_atts = $col_def_collection->get_reactome_atts();
		
		$self->create_biomart_table($reactome_instance_class, $BIOMART_MAIN_TABLE_NAME_EXTENSION, $col_def_collection);
	}
		
	# Build the dimension tables
	foreach my $dimension_class (sort {$a cmp $b} @{$dimension_classes}) {
	   	my $col_def_collection = $col_def_collection_hash{$dimension_class};

		$self->create_biomart_columns($col_def_collection, $dimension_class, undef, "", 0);
	
		# Now generate the expanded columns
		$self->create_expanded_columns($dimension_class, $col_def_collection);
		
		# Go through the listed modified columns and add appropriately
		# named additional columns to the list.
		$self->create_modcols($col_def_collection);
	
		$self->create_biomart_table($dimension_class, $BIOMART_DIMENSION_TABLE_NAME_EXTENSION, $col_def_collection);
	}
	
	# Create hashes connecting primary keys to DB_IDs
	my %primary_key_hashes = ();
	for ($i=0; $i<scalar(@{$reactome_instance_classes}); $i++) {
		my $reactome_instance_class = $reactome_instance_classes->[$i];
		my %primary_key_hash = $self->generate_primary_key_hash($reactome_instance_class, $col_def_collection_hash{$reactome_instance_class});
		$primary_key_hashes{$reactome_instance_class} = \%primary_key_hash;
	}
	
	# Precompute the linked instances between main tables
	my %main_table_link_hash = $self->link_main_tables($reactome_instance_classes, \%col_def_collection_hash, \%primary_key_hashes);
	
	# Load data from Reactome into BioMart tables
	for ($i=0; $i<scalar(@{$reactome_instance_classes}); $i++) {
		my $reactome_instance_class = $reactome_instance_classes->[$i];
		$self->load_biomart_dataset_table($reactome_instance_class, $dimension_classes, \%col_def_collection_hash, $primary_key_hashes{$reactome_instance_class}, \%main_table_link_hash);
	}
	
	# Close the databases
	$self->close_dbas(\%col_def_collection_hash);
}

# For a given instance type, as specified by $class, generate a
# list of names for the column to be expanded.
sub generate_expand_multiple_attribute_name_list {
    my ($self, $class) = @_;
    
	my $utils = $self->utils();
	my $db_ids;
	my $db_id;
	my $expand_multiple_attribute_name = $self->expand_multiple_attribute_name_hash->{$class};
	my %expand_multiple_attribute_column_valid_name_hash = ();

	my $expand_multiple_attribute_root_name = $utils->get_root_composite_reactome_attribute($expand_multiple_attribute_name);
	my $expand_multiple_attribute_non_root_name = $utils->get_non_root_composite_reactome_attribute($expand_multiple_attribute_name);
	print STDERR "Biomart.generate_expand_multiple_attribute_name_list: expand_multiple_attribute_root_name=$expand_multiple_attribute_root_name\n";
	my @attribute_classes = $self->reactome_dba->ontology->class_attribute_allowed_classes($class, $expand_multiple_attribute_root_name);
	print STDERR "Biomart.generate_expand_multiple_attribute_name_list: attribute_classes=@attribute_classes\n";
	my $attribute_class;
	foreach $attribute_class (@attribute_classes) {
		$db_ids = $self->reactome_dba->fetch_db_ids_by_class($attribute_class);
		foreach $db_id (@{$db_ids}) {
			my $instance = $self->reactome_dba->fetch_instance_by_db_id($db_id)->[0];
			my $name = $utils->get_nested_attribute_value_from_instance($self->reactome_dba, $instance, $expand_multiple_attribute_non_root_name);
			print STDERR "Biomart.generate_expand_multiple_attribute_name_list: name=$name\n";
			$expand_multiple_attribute_column_valid_name_hash{$name} = $name;
		}
	}

## Old and slow way of doing things, may also miss some potential
## column names, which can screw up the web code.  The advantage
## is that it only gets you the column names that are relevant.
#	# Pull
#	# out all valid values for instances of the
#	# class for this column.
#	$db_ids = $self->reactome_dba->fetch_db_ids_by_class($class);
#				
#	print STDERR "Biomart.generate_expand_multiple_attribute_name_list: scalar(db_ids)=" . scalar(@{$db_ids}) . "\n";
#			
#	# Loop over all those instances and pull out some kind of information
#	# for generating the expanded columns
#	foreach $db_id (@{$db_ids}) {
#					
#		print STDERR "Biomart.generate_expand_multiple_attribute_name_list: db_id=$db_id\n";
#					
#		my $instance = $self->reactome_dba->fetch_instance_by_db_id($db_id)->[0];
#					
#		if (!(defined $instance)) {
#			print STDERR "Biomart.generate_expand_multiple_attribute_name_list: WARNING - instance is undef for db_id=$db_id!\n";
#			next;
#		}
#					
#		if ($expand_multiple_attribute_name =~ /_displayName/) {
#			print STDERR "Biomart.generate_expand_multiple_attribute_name_list: about to do a get_nested_attribute_value_from_instance\n";
#		}
#				
#		# E.g. if expand_multiple_attribute_name='referenceDatabase._displayName',
#		# then the following will get the name of a reference database for you.
#		my $name = $utils->get_nested_attribute_value_from_instance($self->reactome_dba, $instance, $expand_multiple_attribute_name);
#		if (defined $name && !($name eq '')) {
#			print STDERR "Biomart.generate_expand_multiple_attribute_name_list: INSERTING name=$name\n";
#						
#			$expand_multiple_attribute_column_valid_name_hash{$name} = $name;
#		}
#	}
			
	# This gets us a list of all the valid attribute names for
	# this main/dimension class.  E.g. for ReferenceDNASequence,
	# it would get the names of all appropriate gene databases.
	my @expand_multiple_attribute_name_list = keys(%expand_multiple_attribute_column_valid_name_hash);
				
	print STDERR "Biomart.generate_expand_multiple_attribute_name_list: expand_multiple_attribute_name_list=@expand_multiple_attribute_name_list\n";

	return \@expand_multiple_attribute_name_list;
}

# Adds additional columns to a table being created.  The contents
# of these additional columns will be derived from existing columns,
# allowing modifications to the original data, e.g. by string
# substitution.
sub create_modcols {
    my ($self, $col_def_collection) = @_;
    
    my @modcol_atts = @{$self->modcol_atts};
    my @modcol_substitutes = @{$self->modcol_substitutes};

	# Go through the listed modified columns and add appropriately
	# named additional columns to the list.
	my $modcol_att;
	my %missing_modcol_atts = ();
	while ($modcol_att = shift(@modcol_atts)) {
		my $modcol_substitute = shift(@modcol_substitutes);
			
#		print STDERR "Biomart.create_modcols: modcol_att=$modcol_att, modcol_substitute=$modcol_substitute\n";
			
		my $col_def1 = $col_def_collection->fetch_by_reactome_app($modcol_att);
			
		if (defined $col_def1) {
			my $biomart_col_name = $col_def1->biomart_col_name;
			my $biomart_col_def = $col_def1->biomart_col_def;
				
#			print STDERR "Biomart.create_modcols: biomart_col_name=$biomart_col_name\n";
				
			# Figure out a nice name for the new column, based
			# on the old column name, plus the substitution
			# rule in $modcol_substitute.
			my $new_col_name = '';
			if (scalar($modcol_substitute) =~ /ARRAY/) {
				# We have a substitution rule, where the 0th
				# element is the "search" term, and the 1st
				# is the replace term.
				if ($modcol_substitute->[1] =~ /[_0-9a-zA-Z]/) {
					$new_col_name = $modcol_substitute->[1];
					$new_col_name =~ s/[^_0-9a-zA-Z]//g;
					$new_col_name .= $biomart_col_name;
				} else {
					$new_col_name = "_" . $biomart_col_name;
				}
			} else {
				# We have a substitution rule which uses "%s" to
				# indicate the position in a text string where the
				# value from the original column should be inserted
				$new_col_name = $modcol_substitute;
				$new_col_name =~ s/\%s/$biomart_col_name/g;
				$new_col_name = lc($new_col_name);
				$new_col_name =~ s/[^0-9A-Za-z]+/_/g;
			}
				
#			print STDERR "Biomart.create_modcols: new_col_name=$new_col_name\n";
				
			# If the new column name is unique, add it to the
			# col def collection.
			if ($new_col_name eq $modcol_att) {
				print STDERR "Biomart.create_modcols: WARNING - new_col_name=$new_col_name is the same as the original column name, skipping!\n";
			} else {
#				print STDERR "Biomart.create_modcols: adding new column\n";
					
				$biomart_col_def =~ s/$biomart_col_name/$new_col_name/;
				my $col_def2 = GKB::BioMart::ColDef->new($new_col_name, $new_col_name, $biomart_col_def);
				$col_def2->put_info("Original column name", $modcol_att);
				$col_def2->put_info("Substitution rule", $modcol_substitute);
				$col_def_collection->add($col_def2);
			}
               } else {
                       if (!(defined $missing_modcol_atts{$modcol_att})) {
                               print STDERR "Biomart.create_modcols: cant deal with attribute $modcol_att right now, adding it to list of atts to try again later\n";
 
                               # If we can't deal with the attribute the first
                               # time around, push it onto the end of the list and try
                               # again later - it could be that the appropriate attribute
                               # needed is a modcol attribute that hasn't yet been
                               # created!
                               push(@modcol_atts, $modcol_att);
                               push(@modcol_substitutes, $modcol_substitute);
                               $missing_modcol_atts{$modcol_att} = $modcol_att;
                       } else {
                               print STDERR "Biomart.create_modcols: WARNING - col_def1 is undef AND missing_modcol_atts is defined\n";
                       }
 
#                       # Diagnostics
#                       my @reactome_atts = $col_def_collection->get_reactome_atts();
#                       print STDERR "Biomart.create_modcols: reactome_atts=@reactome_atts\n";
			
			
		}
	}
}

# Adds additional columns to a table being created.  The contents
# of these additional columns will be derived from existing columns,
# allowing modifications to the original data, e.g. by string
# substitution.
sub old_create_modcols {
    my ($self, $col_def_collection) = @_;
    
    my @modcol_atts = @{$self->modcol_atts};
    my @modcol_substitutes = @{$self->modcol_substitutes};

	# Go through the listed modified columns and add appropriately
	# named additional columns to the list.
	for (my $j=0; $j<scalar(@{$self->modcol_atts}); $j++) {
		my $modcol_att = $self->modcol_atts->[$j];
		my $modcol_substitute = $self->modcol_substitutes->[$j];
			
#		print STDERR "Biomart.create_modcols: modcol_att=$modcol_att, modcol_substitute=$modcol_substitute\n";
			
		my $col_def1 = $col_def_collection->fetch_by_reactome_app($modcol_att);
			
		if (defined $col_def1) {
			my $biomart_col_name = $col_def1->biomart_col_name;
			my $biomart_col_def = $col_def1->biomart_col_def;
				
#			print STDERR "Biomart.create_modcols: biomart_col_name=$biomart_col_name\n";
				
			# Figure out a nice name for the new column, based
			# on the old column name, plus the substitution
			# rule in $modcol_substitute.
			my $new_col_name = '';
			if (scalar($modcol_substitute) =~ /ARRAY/) {
				# We have a substitution rule, where the 0th
				# element is the "search" term, and the 1st
				# is the replace term.
				if ($modcol_substitute->[1] =~ /[_0-9a-zA-Z]/) {
					$new_col_name = $modcol_substitute->[1];
					$new_col_name =~ s/[^_0-9a-zA-Z]//g;
					$new_col_name .= $biomart_col_name;
				} else {
					$new_col_name = "_" . $biomart_col_name;
				}
			} else {
				# We have a substitution rule which uses "%s" to
				# indicate the position in a text string where the
				# value from the original column should be inserted
				$new_col_name = $modcol_substitute;
				$new_col_name =~ s/\%s/$biomart_col_name/g;
				$new_col_name = lc($new_col_name);
				$new_col_name =~ s/[^0-9A-Za-z]+/_/g;
			}
				
#			print STDERR "Biomart.create_modcols: new_col_name=$new_col_name\n";
				
			# If the new column name is unique, add it to the
			# col def collection.
			if ($new_col_name eq $modcol_att) {
				print STDERR "Biomart.create_modcols: WARNING - new_col_name=$new_col_name is the same as the original column name, skipping!\n";
			} else {
#				print STDERR "Biomart.create_modcols: adding new column\n";
					
				$biomart_col_def =~ s/$biomart_col_name/$new_col_name/;
				my $col_def2 = GKB::BioMart::ColDef->new($new_col_name, $new_col_name, $biomart_col_def);
				$col_def2->put_info("Original column name", $modcol_att);
				$col_def2->put_info("Substitution rule", $modcol_substitute);
				$col_def_collection->add($col_def2);
			}
		}
	}
}

# Create expanded columns.
sub create_expanded_columns {
    my ($self, $class, $col_def_collection) = @_;
    
	print STDERR "Biomart.create_expanded_columns: reactome_instance_class=$class\n";
				
	my $utils = $self->utils();
	
	my @expand_multiple_attribute_columns = keys(%{$self->expand_multiple_attribute_name_hash});
	@expand_multiple_attribute_columns = sort(@expand_multiple_attribute_columns);
	foreach my $expand_multiple_attribute_column (@expand_multiple_attribute_columns) {
		
		print STDERR "Biomart.create_expanded_columns: expand_multiple_attribute_column=$expand_multiple_attribute_column\n";
		
		# We need to work out the MySQL type of the
		# value attribute, because this is what needs to go
		# into the biomart_col_def.
		my $expand_multiple_attribute_value = $self->expand_multiple_attribute_value_hash->{$expand_multiple_attribute_column};
		
		print STDERR "Biomart.create_expanded_columns: expand_multiple_attribute_value=$expand_multiple_attribute_value\n";
		
		my $expand_multiple_attribute_name;
		if ($self->reactome_dba->ontology->is_a($class, $expand_multiple_attribute_column)) {
			my $biomart_col_def = $self->create_biomart_attribute_column_definitions_recursive($class, $expand_multiple_attribute_value) . " default NULL";
		
			print STDERR "Biomart.create_expanded_columns: biomart_col_def=$biomart_col_def\n";
		
			my @expand_multiple_attribute_name_list = @{$self->expand_multiple_attribute_name_list_hash->{$expand_multiple_attribute_column}};
			foreach $expand_multiple_attribute_name (@expand_multiple_attribute_name_list) {
				my $expand_multiple_attribute_root_name = $utils->get_root_composite_reactome_attribute($self->expand_multiple_attribute_name_hash->{$expand_multiple_attribute_column});
				my $new_biomart_col_name = $self->create_expanded_biomart_col_name($expand_multiple_attribute_name, $expand_multiple_attribute_root_name);
				my $new_biomart_col_def = $new_biomart_col_name . " $biomart_col_def";
				
				print STDERR "Biomart.create_expanded_columns: new_biomart_col_name=$new_biomart_col_name, new_biomart_col_def=$new_biomart_col_def\n";
				
				my $col_def2 = GKB::BioMart::ColDef->new($new_biomart_col_name, $new_biomart_col_name, $new_biomart_col_def);
				$col_def2->put_info("Unexpanded column name", $expand_multiple_attribute_column);
				$col_def2->put_info("Original value", $expand_multiple_attribute_name);
				$col_def_collection->add($col_def2);
			}
		}
	}
}

sub create_expanded_biomart_col_name {
    my ($self, $expand_multiple_attribute_name, $biomart_col_name) = @_;

	$expand_multiple_attribute_name =~ s/[^a-zA-Z0-9]+/_/g;
	my $new_biomart_col_name = lc($biomart_col_name) . "_" . lc($expand_multiple_attribute_name);

	return $new_biomart_col_name;
}

my %value_column_name_hash = (); # cache
my %bool_column_name_hash = (); # cache
# Creates column names for a main table, which will be used
# for filtering for values taken out of a dimension table.
# These column names are for the value and the boolean columns
# respectively, and will be returned as a list with two members.
sub create_export_attribute_col_names {
    my ($self, $reactome_class, $extension) = @_;
    
    # If the user has provided a mapping from modern instance
    # class names to older instance class names, apply it, to
    # maintain compatibility with the web code.
	my $mapped_reactome_class = $self->table_name_map->{$reactome_class};
	if (!(defined $mapped_reactome_class)) {
		$mapped_reactome_class = $reactome_class;
	}
	
	if (defined $extension) {
		$mapped_reactome_class .= $extension;
	}
	
#	print STDERR "BioMart.create_export_attribute_col_names: reactome_class=$reactome_class, mapped_reactome_class=$mapped_reactome_class\n";
	
    my $value_column_name = $value_column_name_hash{$mapped_reactome_class};
    my $bool_column_name = $bool_column_name_hash{$mapped_reactome_class};
    if (!(defined $value_column_name)) {
	    my $value_column_terminator = "_concat";
	    my $bool_column_prefix = "has_";
	    my $bool_column_terminator = "_info_bool";
	    my $stem = lc($mapped_reactome_class);
	    $stem =~ s/\./_/g;
	    # Keep on snipping bits off until the total is under 64.
	    # MySQL doesn't like column names with more than 64 characters.
	    while ((length($stem) + length($value_column_terminator) >= 64) ||
	    	   (length($bool_column_prefix) + length($stem) + length($bool_column_terminator) >= 64)) {
	    	$stem =~ s/^[^_]*_//;
	    }
	    
#		print STDERR "BioMart.create_export_attribute_col_names: stem=$stem\n";
	
	    $value_column_name = $stem . $value_column_terminator;
	    $bool_column_name = $bool_column_prefix . $stem . $bool_column_terminator;
	    
	    $value_column_name_hash{$mapped_reactome_class} = $value_column_name;
	    $bool_column_name_hash{$mapped_reactome_class} = $bool_column_name;
    }
    
    return ($value_column_name, $bool_column_name);
} 

# Creates a BioMart dataset table based on the named Reactome
# instance class and attribute to be used as a primary key.
# Returns a reference to a hash, mapping Reactome attribute
# name onto BioMart attribute name for the corresponding BioMart
# table.  If a Reactome attribute was not mapped, then no
# corresponding entry will appear in this hash.
sub create_biomart_table {
    my ($self, $reactome_instance_class, $biomart_table_name_extension, $col_def_collection) = @_;
    
	print STDERR "BioMart.create_table: reactome_instance_class=$reactome_instance_class\n";
	
	my $col_defs_string = $col_def_collection->create_biomart_col_def_sql();
	my $drop_statement;
	my $create_statement;
	my $class;
	
	# Create table in all DBAs for this ColDefCollection
	foreach $class ($col_def_collection->get_dba_keys()) {
	    my $biomart_dba = $col_def_collection->get_dba($class);
		
		
		print STDERR "BioMart.create_table: class=$class, biomart_dba->db_name=" . $biomart_dba->db_name . "\n";

	    my $table_name = $self->create_biomart_table_name($reactome_instance_class, $class, $biomart_table_name_extension);
	    if ($table_name =~ /^meta_/) {
	    	print STDERR "BioMart.create_table: WARNING - table name ($table_name) starts with meta_, this may conflict with tables created by the BioMart editor!!\n";
	    }
	    	
		$drop_statement = "DROP TABLE $table_name";
		
#		print STDERR "BioMart.create_table: drop statement=$drop_statement\n";

		# Drop the table, if it already esits.
	    if ($biomart_dba->exists_table($table_name)) {
			$biomart_dba->execute($drop_statement);
	    }
	    
		# NN truncate table name
		my $length_of_table_name = length($table_name);
		
		# Trim the class ($lc_class) name so that its no more than 28 char long to avoid hitting the 64 chars max for MySQL for table name	
		if (length($table_name) > 64) {
			$table_name = substr $table_name, 3;
		}
		
		$create_statement = "CREATE TABLE $table_name $col_defs_string ENGINE=MyISAM";
		
		print STDERR "BioMart.create_table: statement=$create_statement\n";
	
		$biomart_dba->execute($create_statement);
	}
	
	print STDERR "\n\n";
}

# Create column BioMart definitions from the given Reactome instance
# class and all of its subclasses.  If $reactome_attribute is defined, then use it to create
# a BioMart key column definition.
#
# Results are stored in a hash, $col_def_collection, that maps from
# Reactome attribute names to BioMart dataset column names
# and SQL column definitions.
sub create_biomart_columns {
    my ($self, $col_def_collection, $reactome_instance_class, $reactome_primary_key_attribute, $reactome_composite_super_attribute, $expanded_instance_depth) = @_;
    
      my @instance_classes = $self->reactome_dba->ontology->children($reactome_instance_class);
      push(@instance_classes, $reactome_instance_class);

      print STDERR "BioMart.create_biomart_columns: reactome_instance_class=$reactome_instance_class, instance_classes=@instance_classes\n";

      foreach my $instance_class (@instance_classes) {
      $self->create_biomart_columns_single_instance_class($col_def_collection, $instance_class, $reactome_primary_key_attribute, $reactome_composite_super_attribute, $expanded_instance_depth);
      }
}
 
# Create column BioMart definitions from the given Reactome instance
# class.  If $reactome_attribute is defined, then use it to create
# a BioMart key column definition.
#
# Results are stored in a hash, $col_def_collection, that maps from
# Reactome attribute names to BioMart dataset column names
# and SQL column definitions.
sub create_biomart_columns_single_instance_class {
    my ($self, $col_def_collection, $reactome_instance_class, $reactome_primary_key_attribute, $reactome_composite_super_attribute, $expanded_instance_depth) = @_;

	my $utils = $self->utils();
	
	my @attributes = $utils->list_instance_class_attributes($self->reactome_dba, $reactome_instance_class);
    my $root_composite_reactome_attribute = $utils->get_root_composite_reactome_attribute($reactome_primary_key_attribute);
    
    # Set up the attribute stems.  These are used when attributes
    # are pulled out of instance attributes, in the process of
    # "flattening out" a Reactome table.
    my $reactome_attribute_precursor = "";
    my $biomart_attribute_precursor = "";
    if ($reactome_composite_super_attribute) {
    	$reactome_attribute_precursor = "$reactome_composite_super_attribute.";
    	$biomart_attribute_precursor = $reactome_composite_super_attribute;
    	$biomart_attribute_precursor =~ s/\./_/g;
    	$biomart_attribute_precursor .= "_";
    }
    
	my $attribute_column_definitions;
	my $biomart_attribute;
	# Now deal with attributes of the instance class
    foreach my $attribute (@attributes) {
    	if ($self->instance_ignored_attributes->{$attribute}) {
    		next;
    	}
    	if ($expanded_instance_depth && $self->expanded_instance_ignored_attributes->{$attribute}) {
    		next;
    	}
		my $new_reactome_composite_super_attribute = $attribute;
		if ($reactome_composite_super_attribute) {
			$new_reactome_composite_super_attribute = "$reactome_composite_super_attribute.$attribute";
		}
     	if ($self->instance_ignored_composite_attributes->{$new_reactome_composite_super_attribute}) {
    		next;
    	}
    	
    	$attribute_column_definitions = undef;
    	$biomart_attribute = $biomart_attribute_precursor;
    	if (defined $root_composite_reactome_attribute && $attribute eq $root_composite_reactome_attribute) {
    		# Deal with the key column
			$biomart_attribute .= $self->create_key_column($reactome_instance_class, $reactome_primary_key_attribute);
    		$attribute_column_definitions = $self->create_key_column_definitions($reactome_instance_class, $reactome_primary_key_attribute);
    		$attribute_column_definitions =~ s/$attribute/$biomart_attribute/g;
    		$attribute = $reactome_primary_key_attribute;
    	} elsif (!$self->allow_multiple_hash->{$attribute} && $self->reactome_dba->ontology->is_multivalue_class_attribute($reactome_instance_class, $attribute)) {
			# Multi-valued attributes will normally be ignored
		} else {
			# Deal with all other attributes
			if ($self->reactome_dba->ontology->is_instance_type_class_attribute($reactome_instance_class, $attribute)) {
				# If the attribute is an instance, we check to see
				# if we can pull any single-values attributes out
				# of the instance.  This is where things get recursive.
				my @new_reactome_instance_classes = $self->reactome_dba->ontology->list_allowed_classes($reactome_instance_class, $attribute);
				my $new_reactome_instance_class = $new_reactome_instance_classes[0]; # TODO: deal with all classes

				if (defined $new_reactome_instance_class &&
					!($reactome_instance_class eq $new_reactome_instance_class) &&
					$self->max_expanded_instance_depth>$expanded_instance_depth) {
					# Allow recursion if the attribute's instance class
					# is not the same as the parent's instance class.
					# This is intended to stop infinite recursion.
#					my $new_reactome_composite_super_attribute = $attribute;
#					if ($reactome_composite_super_attribute) {
#						$new_reactome_composite_super_attribute = "$reactome_composite_super_attribute.$attribute";
#					}
						
					# Recursion is cooler than body surfing
#					my $local_col_def_ref;
					$self->create_biomart_columns($col_def_collection, $new_reactome_instance_class, $reactome_primary_key_attribute, $new_reactome_composite_super_attribute, $expanded_instance_depth+1);
				} else {
					# We may be able to substitute some attribute of the
					# instance attribute for the instance itself.
					if (defined $self->instance_substitute_attribute &&
						$self->reactome_dba->ontology->is_valid_class_attribute($new_reactome_instance_class, $self->instance_substitute_attribute) &&
						!$self->reactome_dba->ontology->is_multivalue_class_attribute($new_reactome_instance_class, $self->instance_substitute_attribute)) {

						$biomart_attribute .= $attribute;
						$attribute_column_definitions = "$biomart_attribute " . $self->create_biomart_attribute_column_definitions($new_reactome_instance_class, $self->instance_substitute_attribute) . " default NULL";
						$attribute .= "." . $self->instance_substitute_attribute;
					}
				}
			} else {
				# Attribute has a simple value, e.g. string.
				$biomart_attribute .= $attribute;
				$attribute_column_definitions = "$biomart_attribute " . $self->create_biomart_attribute_column_definitions($reactome_instance_class, $attribute) . " default NULL";
			}
		}
	    if ($attribute_column_definitions) {
		    my $col_def = GKB::BioMart::ColDef->new($reactome_attribute_precursor . $attribute, $biomart_attribute, $attribute_column_definitions);
	        $col_def_collection->add($col_def);
	    }
    }
}

# Given a Reactome instance class name and attribute name,
# generate the appropriate SQL to create a corresponsing
# column in the BioMart table.
sub create_biomart_attribute_column_definitions {
    my ($self, $reactome_instance_class, $reactome_attribute) = @_;
    
    my $t = $self->reactome_dba->ontology->class_attribute_type($reactome_instance_class, $reactome_attribute);
    
    my $type = $self->reactome_dba->ontology->class_attribute_db_col_type($reactome_instance_class, $reactome_attribute) || eval "\$self->reactome_dba->$t";
    return $type;
}

# Given a Reactome instance class name and attribute name,
# generate the appropriate SQL to create a corresponsing
# column in the BioMart table.
# This is the Clever Trevor version, that can deal with
# compound attributes.
sub create_biomart_attribute_column_definitions_recursive {
    my ($self, $reactome_instance_class, $composite_reactome_attribute) = @_;
    
#    print STDERR "BioMart.create_biomart_attribute_column_definitions_recursive: reactome_instance_class=$reactome_instance_class, composite_reactome_attribute=$composite_reactome_attribute\n";
    	
	my $utils = $self->utils();
	
    my $starting_key = $utils->get_root_composite_reactome_attribute($composite_reactome_attribute);
    my @allowed_classes = $self->reactome_dba->ontology->class_attribute_allowed_classes($reactome_instance_class, $starting_key);
    
    # TODO: should really deal with all allowed classes
    if (defined $allowed_classes[0]) {
    	return $self->create_biomart_attribute_column_definitions_recursive2($allowed_classes[0], $composite_reactome_attribute, $starting_key);
    } else {
    	return $self->create_biomart_attribute_column_definitions_recursive2($reactome_instance_class, $composite_reactome_attribute, undef);
    }
}

sub create_biomart_attribute_column_definitions_recursive2 {
    my ($self, $reactome_instance_class, $composite_reactome_attribute, $starting_reactome_attribute) = @_;
    
#    print STDERR "BioMart.create_biomart_attribute_column_definitions_recursive2: reactome_instance_class=$reactome_instance_class, composite_reactome_attribute=$composite_reactome_attribute, starting_reactome_attribute=$starting_reactome_attribute\n";
    	
	my $utils = $self->utils();
	
    my $new_composite_reactome_attribute = $utils->get_non_root_composite_reactome_attribute($composite_reactome_attribute);
    
    if ($new_composite_reactome_attribute) {
	    my $current_key = $utils->get_root_composite_reactome_attribute($composite_reactome_attribute);
	    my @allowed_classes = $self->reactome_dba->ontology->class_attribute_allowed_classes($reactome_instance_class, $current_key);
	    my $new_reactome_instance_class = $allowed_classes[0]; # TODO: should really deal with all allowed classes
	    return $self->create_biomart_attribute_column_definitions_recursive2($new_reactome_instance_class, $new_composite_reactome_attribute, $starting_reactome_attribute);
    } else {
	    my $t = $self->reactome_dba->ontology->class_attribute_type($reactome_instance_class, $composite_reactome_attribute);
	    my $type = $self->reactome_dba->ontology->class_attribute_db_col_type($reactome_instance_class, $composite_reactome_attribute) || eval "\$self->reactome_dba->$t";
	    return $type;
    }
}

#
# Given the name of a Reactome instance class, generate a
# table name of the corresponding BioMart table.
sub create_biomart_table_name {
	my ($self, $reactome_instance_class, $main_class, $biomart_table_name_extension) = @_;
	
	my $lc_class = $reactome_instance_class;
	if (defined $self->table_name_map->{$lc_class}) {
		$lc_class = $self->table_name_map->{$lc_class};
	}
	$lc_class = lc($lc_class);
	
	print STDERR "BioMart.create_biomart_table_name: reactome_instance_class=$reactome_instance_class, lc_class=$lc_class\n";

	my $biomart_dataset_name_extension = "";
	if (defined $main_class) {
		$biomart_dataset_name_extension = lc($main_class);
	} else {
		$biomart_dataset_name_extension = "unknown";
	}
	
	# NN
	if (length($lc_class) > 28) {
		$lc_class = substr $lc_class, 3;
	}

	return $biomart_dataset_name_extension . "__" . $lc_class . $biomart_table_name_extension;
}

# Creates a BioMart column name for the table key column.
sub create_key_column {
    my ($self, $reactome_instance_class, $reactome_primary_key_attribute) = @_;
    
	my $utils = $self->utils();
	
    my $root_composite_reactome_attribute = $utils->get_root_composite_reactome_attribute($reactome_primary_key_attribute);
	my $biomart_attribute = lc($reactome_instance_class) . "_" . $root_composite_reactome_attribute . "_key";
	
	return $biomart_attribute;
}

# Creates a column definition for the BioMart key column,
# based on the instance class of the corresponding Reactome
# table and attribute.
#
# This was supposed to be a recursive subroutine, that
# descends down to the bottom of the supplied composite
# attribute, but currenly, it always returns INT.
sub create_key_column_definitions {
    my ($self, $reactome_instance_class, $composite_reactome_attribute) = @_;
    
	my $utils = $self->utils();
	
    my $starting_key = $utils->get_root_composite_reactome_attribute($composite_reactome_attribute);
    my @allowed_classes = $self->reactome_dba->ontology->class_attribute_allowed_classes($reactome_instance_class, $starting_key);
    
    # TODO: should really deal with all allowed classes
    return $self->create_key_column_definitions2($allowed_classes[0], $composite_reactome_attribute, $starting_key);
}

# The recursive part of create_key_column_definitions.
sub create_key_column_definitions2 {
    my ($self, $reactome_instance_class, $composite_reactome_attribute, $starting_reactome_attribute) = @_;
    
	my $utils = $self->utils();
	
    my $new_composite_reactome_attribute = $utils->get_non_root_composite_reactome_attribute($composite_reactome_attribute);
    
    if ($new_composite_reactome_attribute) {
	    my $current_key = $utils->get_root_composite_reactome_attribute($composite_reactome_attribute);
	    my @allowed_classes = $self->reactome_dba->ontology->class_attribute_allowed_classes($reactome_instance_class, $current_key);
	    my $new_reactome_instance_class = $allowed_classes[0]; # TODO: should really deal with all allowed classes
	    return $self->create_key_column_definitions2($new_reactome_instance_class, $new_composite_reactome_attribute, $starting_reactome_attribute);
    } else {
	    return "$starting_reactome_attribute INT(10), KEY $starting_reactome_attribute ($starting_reactome_attribute)";
    }
}

# Generates a hash with primary keys as keys and corresponding
# DB_IDs as values.  Missing or non-numeric primary keys will
# not be added to this hash.
sub generate_primary_key_hash {
    my ($self, $reactome_instance_class, $col_def_collection) = @_;
    
    my $reactome_primary_key_attribute = $col_def_collection->attribute_key;

    my $db_ids = $self->reactome_dba->fetch_db_ids_by_class($reactome_instance_class);
    
    # Associate DB_IDs with primary keys.  Missing or non-numeric
    # primary keys will not be added to this hash.
    my $reactome_primary_key_value;
    my $instances;
    my $instance;
    my $db_id;
    my %primary_key_hash = ();
    foreach $db_id (@{$db_ids}) {
    	# This is mainly used for test purposes, allows a developer
    	# to supply a list of DB_IDs that he/she wants to filter on.
    	if ($self->instance_db_ids && !(defined $self->instance_db_ids->{$db_id})) {
    		next;
    	}
    	
    	$instances = $self->reactome_dba->fetch_instance_by_db_id($db_id);
    	$instance = $instances->[0];
    	
    	$reactome_primary_key_value = $self->create_key_column_value($instance, $reactome_primary_key_attribute);
    	
    	if (defined $reactome_primary_key_value) {
    		$primary_key_hash{$reactome_primary_key_value} = $db_id;
    	}
    }
    
    my @primary_keys = sort {$a <=> $b} keys(%primary_key_hash);
    
    # Limits the number of instances to be considered - useful
    # for test purposes.
    if (defined $self->max_main_row_count) {
    	my $max_main_row_count = $self->max_main_row_count;
    	my $main_row_count = 0;
	    my $reactome_primary_key_value;
	    my %new_primary_key_hash = ();
	    foreach $reactome_primary_key_value (@primary_keys) {
	    	$new_primary_key_hash{$reactome_primary_key_value} = $primary_key_hash{$reactome_primary_key_value};
	    	
	    	if ($main_row_count==$max_main_row_count) {
	    		last;
	    	}
	    	
	    	$main_row_count++;
	    }
	    
	    %primary_key_hash = %new_primary_key_hash;
    }
        
    return %primary_key_hash;
}

# Compute links between main tables.  Results put into link_hash.
# link_hash must not be undef.
sub link_main_tables {
    my ($self, $reactome_instance_classes, $dataset_col_def_collection_hash, $primary_key_hashes) = @_;
    
	my %main_table_link_hash = ();
	for (my $i=0; $i<scalar(@{$reactome_instance_classes}); $i++) {
		my $reactome_instance_class = $reactome_instance_classes->[$i];
		my $col_def_collection = $dataset_col_def_collection_hash->{$reactome_instance_class};
		my $primary_key_hash = $primary_key_hashes->{$reactome_instance_class};
		
	    # Loop over remaining main classes and pick out those linking to other
	    # main tables
	    my @link_classes = ();
		for (my $j=$i+1; $j<scalar(@{$reactome_instance_classes}); $j++) {
			my $link_instance_class = $reactome_instance_classes->[$j];
			
			my @valid_paths = $self->link_main_instances->generate_path_source_class_to_target_class($reactome_instance_class, $link_instance_class, 1);

			if (scalar(@valid_paths)>0) {
				push(@link_classes, $link_instance_class);
			}
		}
			    	
	    my $db_ids = $self->reactome_dba->fetch_db_ids_by_class($reactome_instance_class);
	    
	    my $db_id;
	    my $instances;
	    my $instance;
	    my $link_class;
	   	my $link_col_def;
	    my @linked_instances;
	    my $linked_instance;
	    my $reactome_primary_key_value;
	    
	    my @primary_keys = sort {$a <=> $b} keys(%{$primary_key_hash});
	    
	    my %tmp = ();
	    $main_table_link_hash{$reactome_instance_class} = \%tmp;
	    
	    # Find the linked instances in the other main tables
	    # and note them in a hash
	    foreach $reactome_primary_key_value (@primary_keys) {
	    	$db_id = $primary_key_hash->{$reactome_primary_key_value};
	    	$instances = $self->reactome_dba->fetch_instance_by_db_id($db_id);
	    	$instance = $instances->[0];
	    	
	     	my $link_db_id;
			foreach $link_class (@link_classes) {
	    		@linked_instances = $self->link_main_instances->fetch_linked_instances($instance, $link_class, 1);
	    		
	    		foreach $linked_instance (@linked_instances) {
	    			$link_db_id = $linked_instance->db_id();
	    			$main_table_link_hash{$reactome_instance_class}->{$link_db_id} = $reactome_primary_key_value;
	    		}
	    	}
	    }
	}
    
    return %main_table_link_hash;
}

# Copies over columns from the Reactome table to the corresponding
# BioMart dataset table.  The $col_def_collection variable specifies which
# columns will be copied over and also maps Reactome column name to
# BioMart volumn name.
sub load_biomart_dataset_table {
    my ($self, $reactome_instance_class, $dimension_classes, $col_def_collection_hash, $primary_key_hash, $link_hash) = @_;
    
    my $col_def_collection = $col_def_collection_hash->{$reactome_instance_class};
    my $reactome_primary_key_attribute = $col_def_collection->attribute_key;
    my $table_name = $self->create_biomart_table_name($reactome_instance_class, $reactome_instance_class, $BIOMART_MAIN_TABLE_NAME_EXTENSION);

#    my $db_ids = $self->reactome_dba->fetch_db_ids_by_class($reactome_instance_class);
#    
#    print STDERR "BioMart.load_biomart_dataset_table: reactome_instance_class=$reactome_instance_class, db_ids=$db_ids\n";
#    print STDERR "BioMart.load_biomart_dataset_table: scalar db_ids=" . scalar(@{$db_ids}) . "\n";
    
    # BioMart needs dataset keys to be numerically ordered, so do
    # a sort on them first.
    my @primary_keys = sort {$a <=> $b} keys(%{$primary_key_hash});
    
    # Insert rows into tables.  The outer loop inserts into
    # the main table, the inner loop inserts into the dimension
    # tables.  There's also some extra stuff to deal with
    # the back coupling between dimension and main tables,
    # whereby dimension table key attributes can be stored in the main
    # table.
    my $dimension_class;
    my $main_row_count = 0;
    my $max_main_row_count = (-1);
    if (defined $self->max_main_row_count) {
    	$max_main_row_count = $self->max_main_row_count;
    }
    
    # Loop over attributes and pick out those linking to other
    # main tables
    my @link_from_col_defs = $col_def_collection->fetch_by_info_regexp("link_direction", "from");
    my @link_to_col_defs = $col_def_collection->fetch_by_info_regexp("link_direction", "to");
    my $link_to_col_def = undef;
    if (scalar(@link_to_col_defs)>0) {
    	# This is used to fill the duplicate key column,
    	# if there is one.
    	$link_to_col_def = $link_to_col_defs[0];
    }
    my $link_col_def;
    my $link_class;
    
    # Get the BioMart column name for the main table's primary key.
    # This is the name that the dimension tables will know this key
    # under.
    my $biomart_primary_key = $self->create_key_column($reactome_instance_class, $reactome_primary_key_attribute);
    
#    print STDERR "BioMart.load_biomart_dataset_table: biomart_primary_key=$biomart_primary_key\n";

    my $reactome_primary_key_value;
    my $instances;
    my $instance;
    my $db_id;
    # The "extra values" hashes are a kludge, used to pass on
    # values that are not extracted from a Reactome instance, but
    # which we would like to see in the BioMart table.
    my %extra_values_hash;
    my %dimension_extra_values_hash;
    foreach $reactome_primary_key_value (@primary_keys) {
    	# This is mainly useful for test purposes, when you don't
    	# want to do a (time consuming) run over all rows in the
    	# main table.
    	if ($main_row_count==$max_main_row_count) {
    		last;
    	}
    	
    	$db_id = $primary_key_hash->{$reactome_primary_key_value};
    	
    	# This is mainly used for test purposes, allows a developer
    	# to supply a list of DB_IDs that he/she wants to filter on.
    	if ($self->instance_db_ids && !(defined $self->instance_db_ids->{$db_id})) {
    		next;
    	}
    	
#    	print STDERR "BioMart.load_biomart_dataset_table: reactome_primary_key_value=$reactome_primary_key_value\n";
			
    	$instances = $self->reactome_dba->fetch_instance_by_db_id($db_id);
    	$instance = $instances->[0];
    	%extra_values_hash = ();
    	%dimension_extra_values_hash = ();
    	
    	# Pass on primary key value to dimension table
    	$dimension_extra_values_hash{$biomart_primary_key} = $reactome_primary_key_value;

#    	print STDERR "BioMart.load_biomart_dataset_table: instance=$instance\n";
    	
    	# Find linked target instances and insert BioMart dimension
    	# table rows for them.  Do this first, because we may need
    	# to feed information from the dimension table back to the
    	# main table.
    	my $dimension_table_name;
    	my @path_list;
    	my $dimension_key;
    	my $dimension_key_value;
    	my $key_col_name;
    	my $boolean_col_name;
    	my $dimension_export_attributes;
    	my $dimension_export_attribute;
    	# NN: Add new attribute 'dimension_direction'
    	my $dimension_direction;
    	foreach $dimension_class (@{$dimension_classes}) {
			$dimension_table_name = $self->create_biomart_table_name($dimension_class, $reactome_instance_class, $BIOMART_DIMENSION_TABLE_NAME_EXTENSION);
			$dimension_key = $col_def_collection_hash->{$dimension_class}->attribute_key;
			# NN: Add 'dimension_direction' to the collection hash
			$dimension_direction = $col_def_collection_hash->{$dimension_class}->dimension_direction;
			$dimension_export_attributes = $self->dimension_export_attribute_hash->{$dimension_class};
			
    		my %dimension_export_attribute_key_value_string = ();
    		my %dimension_export_attribute_key_value_string_hash = (); # stops repeated IDs
    		
    		# Finds all instances of type $dimension_class that are
    		# linked to the main class instance.
    		# NN: Added the '$dimension_direction' argument to be passed to fetch_linked_instances so that direction can be determined.
    		# there is no risk of breaking things if there are any other subs calling this method coz if they don't provide this 3rd argument, then 'undef' is used  		

			my @dimension_instances = $self->link_instances->fetch_linked_instances($instance, $dimension_class, $dimension_direction);
			if (scalar(@dimension_instances)<1) {
				# Even if there are no values to be added to the
				# dimension table, add an empty row.  Mart needs
				# this.  If it is absent, you can get missing rows
				# in your martview results tables.
#				print STDERR "BioMart.load_biomart_dataset_table: lawks, we have an empty row, dimension_class=$dimension_class\n";
				$self->load_biomart_row($dimension_class, $BIOMART_DIMENSION_TABLE_NAME_EXTENSION, $col_def_collection_hash->{$dimension_class}, undef, $dimension_table_name, \%dimension_extra_values_hash);
#				print STDERR "BioMart.load_biomart_dataset_table: done the empty row, dimension_table_name=$dimension_table_name\n";
			} else {
				@dimension_instances = sort(@dimension_instances); # for repeatability
				
				# Loop over dimension class instances and add them to the
				# appropriate Mart dimension table.  Also, gather together
				# any attribute values that should be passed back to the
				# main class.
				foreach my $dimension_instance (@dimension_instances) {
					# Write the dimension table row into the
					# database and retrieve the value for the
					# dimension key column.
					my %attribute_value_hash = $self->load_biomart_row($dimension_class, $BIOMART_DIMENSION_TABLE_NAME_EXTENSION, $col_def_collection_hash->{$dimension_class}, $dimension_instance, $dimension_table_name, \%dimension_extra_values_hash);
					$dimension_key_value = $attribute_value_hash{$dimension_key};
					
					# Build up the values that will be
					# passed to the main table.
					foreach $dimension_export_attribute (@{$dimension_export_attributes}) {
						$dimension_key_value = $attribute_value_hash{$dimension_export_attribute};
						
						# Build up the key values that will be
						# passed to the main table.
						if (defined $dimension_key_value && !($dimension_key_value eq 'NULL') && !(defined $dimension_export_attribute_key_value_string_hash{$dimension_export_attribute}{$dimension_key_value})) {
							if (defined $dimension_export_attribute_key_value_string{$dimension_export_attribute}) {
								$dimension_export_attribute_key_value_string{$dimension_export_attribute} .= ', ';
							} else {
								$dimension_export_attribute_key_value_string{$dimension_export_attribute} = '';
							}
							$dimension_export_attribute_key_value_string{$dimension_export_attribute} .= $dimension_key_value;
							$dimension_export_attribute_key_value_string_hash{$dimension_export_attribute}{$dimension_key_value} = $dimension_key_value;
						}
					}
				}
				
				# Pass values gathered from the dimension table
				# over to the main table.
				foreach $dimension_export_attribute (@{$dimension_export_attributes}) {
			    	($key_col_name, $boolean_col_name) = $self->create_export_attribute_col_names($dimension_class, "_$dimension_export_attribute");
					if (defined $dimension_export_attribute_key_value_string{$dimension_export_attribute}) {
						$dimension_export_attribute_key_value_string{$dimension_export_attribute} =~ s/'//g; #'
						$dimension_export_attribute_key_value_string{$dimension_export_attribute} = "'" . $dimension_export_attribute_key_value_string{$dimension_export_attribute} . "'"; #'
						
			    		if ($dimension_export_attribute_key_value_string{$dimension_export_attribute} eq "''") {
			    			# This situation should probably never arise,
			    			# but just in case...
			    			$extra_values_hash{$key_col_name} = 'NULL';
			    			$extra_values_hash{$boolean_col_name} = '0';
			    		} else {
			    			$extra_values_hash{$key_col_name} = $dimension_export_attribute_key_value_string{$dimension_export_attribute};
			    			$extra_values_hash{$boolean_col_name} = '1';
			    		}
					} else {
			    		$extra_values_hash{$key_col_name} = 'NULL';
			    		$extra_values_hash{$boolean_col_name} = '0';
					}
				}
			}
    	}
		
		# Use this hash to carry links from other main tables.
    	foreach $link_col_def (@link_from_col_defs) {
    		$link_class = $link_col_def->get_info("link_class");
	    	if ($link_hash->{$link_class}->{$db_id}) {
	    		$extra_values_hash{$link_col_def->reactome_att} = $link_hash->{$link_class}->{$db_id};
	    	}
    	}
		
    	# Pass on primary key value to main table
    	$extra_values_hash{$reactome_primary_key_attribute} = $reactome_primary_key_value;
		if (defined $link_to_col_def) {
			# Duplicate main key column if necessary.
			$extra_values_hash{$link_to_col_def->reactome_att} = $reactome_primary_key_value;
		}
		
		# Check to see if this is a canonical instance
		if (defined $self->part_of_attribute_hash->{$reactome_instance_class} && defined $self->canonical_depth_hash->{$reactome_instance_class}) {
			$extra_values_hash{"is_canonical"} = '0'; # default
			
			my $test_instance = $instance;
			my $part_of_attribute = $self->part_of_attribute_hash->{$reactome_instance_class};
			my $canonical_depth = $self->canonical_depth_hash->{$reactome_instance_class};

			my $reverse_attribute_value;
			for (my $i=0; $i<$canonical_depth; $i++) {
				$reverse_attribute_value = $test_instance->reverse_attribute_value($part_of_attribute);

				# TODO: there may be more than one reverse attribute value, we ought to explore recursively
				if (scalar(@{$reverse_attribute_value})==0) {
					# Nothing above this instance in the part-of hierarchy, this
					# is our rather cheap definition of canonical.
					$extra_values_hash{"is_canonical"} = '1';
					last;
				} elsif (!(defined $reverse_attribute_value->[0])) { # unlikely, but just in case
					# Nothing above this instance in the part-of hierarchy, this
					# is our rather cheap definition of canonical.
					$extra_values_hash{"is_canonical"} = '1';
					last;
				} elsif (!($reverse_attribute_value->[0]->is_a($reactome_instance_class))) {
					last;
				}
				
				$test_instance = $reverse_attribute_value->[0];
			}
		}
				
    	# Insert a row into the main table.
    	$self->load_biomart_row($reactome_instance_class, $BIOMART_MAIN_TABLE_NAME_EXTENSION, $col_def_collection, $instance, $table_name, \%extra_values_hash);
    
    	$main_row_count++;
    }
}

# Takes the given instance and inserts the appropriate attributes
# into a BioMart table.  The extra_values_hash allows you to pass
# key/value pairs that will also be inserted into the BioMart table,
# in addition to the values gleaned from the instance.  If dimension_key
# corresponds to a valid Reactome attribute name, then the subroutine
# will return the corresponding value.  Otherwise, it will return undef.
my $main_table_counter = 0; # controls diagnostic printouts
sub load_biomart_row {
    my ($self, $reactome_class, $table_name_extension, $col_def_collection, $instance, $table_name, $extra_values_hash) = @_;
    
    my %attribute_value_hash = ();

    my $values = "";
    my $value;
    my $modcol_att = undef;
    my $expand_multiple_attribute_att = undef;
    my $modcol_substitute;
    my $search_term;
    my $replace_term;
    
	my $utils = $self->utils();
	
    foreach my $attribute (@{$col_def_collection->create_reactome_atts()}) {
	    if (!(defined $attribute)) {
	    	print STDERR "BioMart.load_biomart_row: WARNING - attribute is undef, aborting insert into $table_name\n";
			return %attribute_value_hash;
	    }

    	if ($values) {
    		$values .= ", ";
    	}
    	$value = undef;
    	
    	if ($extra_values_hash) {
    		$value = $extra_values_hash->{$attribute};
    	}
    	
    	if (defined $value) {
    		# If value has already been assigned, don't do anything further
    	} else {
	    	# Check to see if the attribute is actually a copy of some
	    	# other attribute, where some kind of editing is needed.
	    	my @col_defs = $col_def_collection->fetch_by_reactome_app($attribute);
	    	my $col_def = $col_defs[0]; # should be exactly one element
	    	$modcol_att = $col_def->get_info("Original column name");
	    	if (defined $modcol_att) {
	    		# Get the value of the attribute to be copied
	    		if (defined $instance) {
	    			$value = $utils->get_nested_attribute_value_from_instance($self->reactome_dba, $instance, $modcol_att);
	    		} else {
	    			$value = $extra_values_hash->{$modcol_att};
	    		}
	    		$value = $self->convert_reactome_to_biomart_value($value);
	    		
	    		# "Edit" the value by doing a substitution.  %s is used
	    		# as a placeholder for the value, I copied this idea from
	    		# BioMart.
				if (!($value eq 'NULL')) {
					$value =~ s/^'//; #'
					$value =~ s/'$//; #'
			    	$modcol_substitute = $col_def->get_info("Substitution rule");
				    if (scalar($modcol_substitute) =~ /ARRAY/) {
						# We have a substitution rule, where the 0th
						# element is the "search" term, and the 1st
						# is the replace term.
			    		$search_term = $modcol_substitute->[0];
			    		$replace_term = $modcol_substitute->[1];
			    		
#			    		print STDERR "BioMart.load_biomart_row: search_term=|$search_term|, replace_term=|$replace_term|\n";

			    		$value =~ s/$search_term/$replace_term/g;
			    	} else {
						# We have a substitution rule which uses "%s" to
						# indicate the position in a text string where the
						# value from the original column should be inserted
						$modcol_substitute =~ s/\%s/$value/g;
						$value = $modcol_substitute;
			    	}
				}
	    	} else {
	    		$expand_multiple_attribute_att = $col_def->get_info("Unexpanded column name");
		    	if (defined $expand_multiple_attribute_att) {
		    		# The current column is one of a set of columns
		    		# "expanded" from $attribute by looking for
		    		# all of the values of a given attribute and creating
		    		# one new column per value.
	    			my $expand_multiple_attribute_original_value = $col_def->get_info("Original value");

					my $expand_multiple_attribute_name = $self->expand_multiple_attribute_name_hash->{$expand_multiple_attribute_att};
					my $expand_multiple_attribute_value = $self->expand_multiple_attribute_value_hash->{$expand_multiple_attribute_att};

					my $expand_multiple_names = $utils->get_nested_attribute_value_from_instance($self->reactome_dba, $instance, $expand_multiple_attribute_name, 1);
					my $expand_multiple_name;
					if (defined $expand_multiple_names) {
						# Loop over all of the name attribute values until
						# we find one matching the name of the expanded
						# column.  That was a lie.  Actually, it has to
						# match the original value that was used to generate
						# the column name, I tried to make things easier for
						# you to understand.
						foreach $expand_multiple_name (@{$expand_multiple_names}) {
							
							print STDERR "BioMart.load_biomart_row: expand_multiple_name=$expand_multiple_name, expand_multiple_attribute_original_value=$expand_multiple_attribute_original_value\n";
							
							if (lc($expand_multiple_name) eq lc($expand_multiple_attribute_original_value)) {
					
								my $expand_multiple_value = $utils->get_nested_attribute_value_from_instance($self->reactome_dba, $instance, $expand_multiple_attribute_value);
								
								print STDERR "BioMart.load_biomart_row: expand_multiple_value=$expand_multiple_value\n";
								
								$value = $expand_multiple_value;
								
								last;
							}
						}
					}
		    	}
	    	}
	    	
    		if (!(defined $value)) {
				$value = "";

		    	# This is actually the "regular" case, where the value is
		    	# extracted from the instance, using the attribute name
		    	# as a key.  This gets done if all of the above things
		    	# fail to find a value for the named attribute.
		    	$value = $utils->get_nested_attribute_value_from_instance($self->reactome_dba, $instance, $attribute);
		    	
		    	# Having been burned by UniProt entries containing newlines
		    	# and tabs, tame these troublesome characters.
		    	if (defined $value) {
		    		$value =~ s/[\t\n]+/ /g;
		    	}
    		}
    	}
    	
    	# Stash all attribute values - this hash will be returned by
    	# the subroutine.
    	$attribute_value_hash{$attribute} = $value;
    	
    	# Make value suitable for insertion into an SQL table
		$value = $self->convert_reactome_to_biomart_value($value);
		
    	$values .= $value;
    }
    
    my $attributes = $col_def_collection->create_biomart_attribute_sql();
    my $statement;
    
	# Insert into table in all DBAs for this ColDefCollection
	my $class;
	foreach $class ($col_def_collection->get_dba_keys()) {
	    my $biomart_dba = $col_def_collection->get_dba($class);
	    $table_name = $self->create_biomart_table_name($reactome_class, $class, $table_name_extension);
    	$statement = "INSERT INTO $table_name ($attributes) VALUES ($values)";

#		if ($table_name_extension eq $BIOMART_MAIN_TABLE_NAME_EXTENSION) {
#	    	print STDERR "BioMart.load_biomart_row: statement=$statement\n";
#		}
    
	    $biomart_dba->execute($statement);
	}
    
    return %attribute_value_hash;
}

# Tkes a value and makes it suitable for putting into
# an SQL insert statement.  If it is undefined, then
# it ise set to NULL.  Any internal single quotes will
# be removed, and flanking single-quotes will be added.
sub convert_reactome_to_biomart_value {
    my ($self, $reactome_value) = @_;
    
    my $biomart_value;
	if (!(defined $reactome_value) || $reactome_value eq "NULL") {
		$biomart_value = "NULL";
	} else {
		$biomart_value = $reactome_value;
		$biomart_value =~ s/'//g; #'# TODO: maybe we should use BLOB type?
		$biomart_value = "'" . $biomart_value . "'";
	}

	return $biomart_value;
}

# Since the key column may actually be part of a nested instance,
# we may need to recurse into that instance in order to get at it.
# Also strips out any non-numerical characters from the key.  If
# the key contains no numerical characters, prints a warning and
# returns undef.
sub create_key_column_value {
    my ($self, $instance, $reactome_attribute) = @_;
    
	my $utils = $self->utils();
	
#	if ($reactome_attribute =~ /_displayName/) {
#		print STDERR "Biomart.create_key_column_value: about to do a get_nested_attribute_value_from_instance\n";
#	}
					
    my $value = $utils->get_nested_attribute_value_from_instance($self->reactome_dba, $instance, $reactome_attribute);
    if (defined $value) {
    	$value =~ s/[^0-9]//g;
    	if ($value eq '') {
    		print STDERR "BioMart.create_key_column_value: WARNING - value=$value contains no numeric information!\n";
    		$value = undef;
    	}
    }

    return $value;
}

my %database_optimizations = (
	'Complex' => [
		# Christina's optimizations
		"create fulltext index _displayname on complex__complex__main ( _displayname )",
		"create index pubmedidentifier on complex__literaturereference__dm ( pubmedidentifier )",
		"create index referencedatabase_chebi on complex__referencemolecule__dm ( referencedatabase_chebi (32) )",
#		"create index referencedatabase_ensembl on complex__referencednasequence__dm ( referencedatabase_ensembl (32) )",
		"create index referencedatabase_entrez_gene on complex__referencednasequence__dm ( referencedatabase_entrez_gene (32) )",
		"create index referencedatabase_kegg_gene on complex__referencednasequence__dm ( referencedatabase_kegg_gene (32) )",
		"create index referencedatabase_uniprot on complex__referencepeptidesequence__dm ( referencedatabase_uniprot (32) )",
		"create index referencedatabase_wormbase on complex__referencepeptidesequence__dm ( referencedatabase_wormbase (32) )",
		"create fulltext index species__displayname on complex__complex__main ( species__displayname )",
		"create index stableidentifier_identifier on complex__complex__main ( stableidentifier_identifier (32) )",
		"create index stableidentifier_identifierversion on complex__complex__main ( stableidentifier_identifierversion (32) )",
		# Nelson's optimizations
#		"CREATE INDEX stableIdentifier_id ON complex__complex__main (stableIdentifier_identifier (32))",
		"CREATE INDEX stableIdentifier_id ON complex__itywithaccessionedsequence__dm (stableIdentifier_identifier (32))",
#		"CREATE INDEX ensembl_id ON complex__referencednasequence__dm (referencedatabase_ensembl (32))",
		"CREATE INDEX uniprot_id ON complex__referencepeptidesequence__dm (referencedatabase_uniprot (32))",
	],
	'Interaction' => [
		# Christina's optimizations
		"create fulltext index species_displayname on interaction__interaction__main ( species_displayname )",
		"create index type on interaction__interaction__main ( type (32) )",
		"create index value on interaction__entrez__dm ( value (32) )",
		"create index value on interaction__gene__dm ( value (32) )",
		"create index value on interaction__id_complex_db_id__dm ( value (32) )",
		"create index value on interaction__id_complex_stable_id__dm ( value (32) )",
		"create index value on interaction__id_intact__dm ( value (32) )",
		"create index value on interaction__id_pathway_db_id__dm ( value (32) )",
		"create index value on interaction__id_pathway_stable_id__dm ( value (32) )",
		"create index value on interaction__id_reaction_db_id__dm ( value (32) )",
		"create index value on interaction__id_reaction_stable_id__dm ( value (32) )",
		"create index value on interaction__protein_cme_genome_project__dm ( value (32) )",
		"create index value on interaction__protein_dictybase__dm ( value (32) )",
		"create index value on interaction__protein_ensembl__dm ( value (32) )",
		"create index value on interaction__protein_flybase__dm ( value (32) )",
		"create index value on interaction__protein_tigr__dm ( value (32) )",
		"create index value on interaction__protein_uniprot__dm ( value (32) )",
		"create index value on interaction__protein_wormbase__dm ( value (32) )",
		"create index value on interaction__pubmed__dm ( value (32) )",      
	],
	'Pathway' => [
		# Christina's optimizations
		"create index db_id on pathway__complex__dm ( db_id )",
		"create index db_id on pathway__reaction__dm ( db_id )",
		"create fulltext index _displayname on pathway__pathway__main ( _displayname )",
		"create index gobiologicalprocess_accession on pathway__pathway__main ( gobiologicalprocess_accession (32) )",
		"create index is_canonical on pathway__pathway__main ( is_canonical )",
		"create index pubmedidentifier on pathway__literaturereference__dm ( pubmedidentifier )",
		"create index referencedatabase_chebi on pathway__referencemolecule__dm ( referencedatabase_chebi (32) )",
#		"create index referencedatabase_ensembl on pathway__referencednasequence__dm ( referencedatabase_ensembl (32) )",
		"create index referencedatabase_entrez_gene on pathway__referencednasequence__dm ( referencedatabase_entrez_gene (32) )",
		"create index referencedatabase_kegg_gene on pathway__referencednasequence__dm ( referencedatabase_kegg_gene (32) )",
		"create index referencedatabase_uniprot on pathway__referencepeptidesequence__dm ( referencedatabase_uniprot (32) )",
		"create index referencedatabase_wormbase on pathway__referencepeptidesequence__dm ( referencedatabase_wormbase (32) )",
		"create fulltext index species__displayname on pathway__pathway__main ( species__displayname )",
		"create index stableidentifier_identifier on pathway__complex__dm ( stableidentifier_identifier (32) )",
		"create index stableidentifier_identifier on pathway__pathway__main ( stableidentifier_identifier (32) )",
		"create index stableidentifier_identifier on pathway__reaction__dm ( stableidentifier_identifier (32) )",
		"create index stableidentifier_identifierversion on pathway__pathway__main ( stableidentifier_identifierversion (32) )",
		# Nelson's optimizations
		"CREATE INDEX stableIdentifier_id ON pathway__itywithaccessionedsequence__dm (stableIdentifier_identifier (32))",
#		"CREATE INDEX stableIdentifier_id ON pathway__pathway__main (stableIdentifier_identifier (32))",
#		"CREATE INDEX stableIdentifier_id ON pathway__complex__dm (stableIdentifier_identifier (32))",
#		"CREATE INDEX stableIdentifier_id ON pathway__reaction__dm (stableIdentifier_identifier (32))",
#		"CREATE INDEX ensembl_id ON pathway__referencednasequence__dm (referencedatabase_ensembl (32))",
		"CREATE INDEX uniprot_id ON pathway__referencepeptidesequence__dm (referencedatabase_uniprot (32))",
	],
	'Reaction' => [
		# Christina's optimizations
		"create index catalystactivity_activity_accession on reaction__reaction__main ( catalystactivity_activity_accession (32) )",
		"create fulltext index catalystactivity__displayname on reaction__reaction__main ( catalystactivity__displayname )",
		"create index db_id on reaction__complex__dm ( db_id )",
		"create fulltext index _displayname on reaction__reaction__main ( _displayname )",
		"create index gobiologicalprocess_accession on reaction__reaction__main ( gobiologicalprocess_accession (32) )",
		"create index pubmedidentifier on reaction__literaturereference__dm ( pubmedidentifier )",
		"create index referencedatabase_chebi on reaction__referencemolecule__dm ( referencedatabase_chebi (32) )",
		"create index referencedatabase_ensembl on reaction__referencednasequence__dm ( referencedatabase_ensembl (32) )",
		"create index referencedatabase_entrez_gene on reaction__referencednasequence__dm ( referencedatabase_entrez_gene (32) )",
		"create index referencedatabase_kegg_gene on reaction__referencednasequence__dm ( referencedatabase_kegg_gene (32) )",
		"create index referencedatabase_uniprot on reaction__referencepeptidesequence__dm ( referencedatabase_uniprot (32) )",
		"create index referencedatabase_wormbase on reaction__referencepeptidesequence__dm ( referencedatabase_wormbase (32) )",
		"create fulltext index species__displayname on reaction__reaction__main ( species__displayname )",
		"create index stableidentifier_identifier on reaction__complex__dm ( stableidentifier_identifier (32) )",
		"create index stableidentifier_identifier on reaction__reaction__main ( stableidentifier_identifier (32) )",
		"create index stableidentifier_identifierversion on reaction__reaction__main ( stableidentifier_identifierversion (32) )",
		# Nelson's optimizations
		"CREATE INDEX stableIdentifier_id ON reaction__itywithaccessionedsequence__dm (stableIdentifier_identifier (32))",
#		"CREATE INDEX stableIdentifier_id ON reaction__reaction__main (stableIdentifier_identifier (32))",
#		"CREATE INDEX stableIdentifier_id ON reaction__complex__dm (stableIdentifier_identifier (32))",
#		"CREATE INDEX ensembl_id ON reaction__referencednasequence__dm (referencedatabase_ensembl (32))",
		"CREATE INDEX uniprot_id ON reaction__referencepeptidesequence__dm (referencedatabase_uniprot (32))",
	],
	'EntityWithAccessionedSequence' => [
		# Nelson's optimizations
		"CREATE INDEX uniprot_id ON entitywithaccessionedsequence__itywithaccessionedsequence__main (referencedatabase_uniprot_concat (10))",
		"CREATE INDEX psimodid ON entitywithaccessionedsequence__itywithaccessionedsequence__main (mod_hasmodifiedresidue_psimod_identifier (10))",
		"CREATE INDEX stableIdentifier_id ON entitywithaccessionedsequence__itywithaccessionedsequence__main (stableIdentifier_identifier (32))",
		"CREATE INDEX modifiedResidue_refSeq_id ON entitywithaccessionedsequence__itywithaccessionedsequence__main (hasModifiedResidue_referenceSequence_identifier (10))",
		"CREATE INDEX stableIdentifier_id ON entitywithaccessionedsequence__complex__dm (stableIdentifier_identifier (32))",
		"CREATE INDEX stableIdentifier_id ON entitywithaccessionedsequence__pathway__dm (stableIdentifier_identifier (32))",
		"CREATE INDEX stableIdentifier_id ON entitywithaccessionedsequence__reaction__dm (stableIdentifier_identifier (32))",
		"CREATE INDEX ensembl_id ON entitywithaccessionedsequence__referencednasequence__dm (referencedatabase_ensembl (32))",
		"CREATE INDEX uniprot_id ON entitywithaccessionedsequence__referencepeptidesequence__dm (referencedatabase_uniprot (32))",
		"CREATE INDEX uniprot_id_psimod_id ON entitywithaccessionedsequence__itywithaccessionedsequence__main (referencedatabase_uniprot_concat (10), mod_hasmodifiedresidue_psimod_identifier (10))",
		"CREATE INDEX complex_stableId_concat ON entitywithaccessionedsequence__itywithaccessionedsequence__main (complex_stableidentifier_identifier_concat (60))",
		"CREATE INDEX refdbase_ensembl_concat ON entitywithaccessionedsequence__itywithaccessionedsequence__main (referencednasequence_referencedatabase_ensembl_concat (66))",
	],
);

sub optimize_databases {
    my ($self, $reactome_instance_classes) = @_;
    
    my $reactome_instance_class;
    foreach $reactome_instance_class (@{$reactome_instance_classes}) {
    	$self->optimize_database($reactome_instance_class)
    }
    
    $self->nelsons_column_type_changes();
}

sub optimize_database {
    my ($self, $reactome_instance_class) = @_;
    
    my $biomart_dba;
    if ($self->multiple_dataset_flag) {
    	$biomart_dba = $self->create_biomart_dba($reactome_instance_class);
    } else {
    	$biomart_dba = $self->create_biomart_dba();
    }
    
    my $statement;
    my $statements = $database_optimizations{$reactome_instance_class};
    if (defined $statements) {
	    foreach $statement (@{$statements}) {
	    	eval {
	    	    $biomart_dba->execute($statement);
	    	};
	    }
	}
}

sub nelsons_column_type_changes {
    my ($self) = @_;

	my @changes = (
		"ALTER TABLE entitywithaccessionedsequence__itywithaccessionedsequence__main CHANGE stableIdentifier_identifier stableIdentifier_identifier VARCHAR(20)",
		"ALTER TABLE entitywithaccessionedsequence__complex__dm CHANGE stableIdentifier_identifier stableIdentifier_identifier VARCHAR(20)",
		"ALTER TABLE entitywithaccessionedsequence__itywithaccessionedsequence__main CHANGE mod_hasmodifiedresidue_psimod_identifier mod_hasmodifiedresidue_psimod_identifier VARCHAR(13)",
		"ALTER TABLE entitywithaccessionedsequence__pathway__dm CHANGE stableIdentifier_identifier stableIdentifier_identifier VARCHAR(20)",
		"ALTER TABLE entitywithaccessionedsequence__reaction__dm CHANGE stableIdentifier_identifier stableIdentifier_identifier VARCHAR(20)",
		"ALTER TABLE entitywithaccessionedsequence__referencednasequence__dm CHANGE referencedatabase_ensembl referencedatabase_ensembl VARCHAR(25)",
		"ALTER TABLE pathway__itywithaccessionedsequence__dm CHANGE stableIdentifier_identifier stableIdentifier_identifier VARCHAR(20)",
		"ALTER TABLE pathway__pathway__main CHANGE stableIdentifier_identifier stableIdentifier_identifier VARCHAR(20)",
		"ALTER TABLE pathway__complex__dm CHANGE stableIdentifier_identifier stableIdentifier_identifier VARCHAR(20)",
		"ALTER TABLE pathway__reaction__dm CHANGE stableIdentifier_identifier stableIdentifier_identifier VARCHAR(20)",
#		"ALTER TABLE pathway__referencednasequence__dm CHANGE referencedatabase_ensembl referencedatabase_ensembl VARCHAR(20)",
#		"ALTER TABLE entitywithaccessionedsequence__referencepeptidesequence__dm CHANGE referencedatabase_uniprot referencedatabase_uniprot VARCHAR(10)",
		"ALTER TABLE pathway__referencepeptidesequence__dm CHANGE referencedatabase_uniprot referencedatabase_uniprot VARCHAR(40)",
		"ALTER TABLE complex__complex__main CHANGE stableIdentifier_identifier stableIdentifier_identifier VARCHAR(20)",
		"ALTER TABLE complex__itywithaccessionedsequence__dm CHANGE stableIdentifier_identifier stableIdentifier_identifier VARCHAR(20)",
#		"ALTER TABLE complex__referencednasequence__dm CHANGE referencedatabase_ensembl referencedatabase_ensembl VARCHAR(20)",
#		"ALTER TABLE complex__referencepeptidesequence__dm CHANGE referencedatabase_uniprot referencedatabase_uniprot VARCHAR(40)",
		"ALTER TABLE reaction__itywithaccessionedsequence__dm CHANGE stableIdentifier_identifier stableIdentifier_identifier VARCHAR(20)",
		"ALTER TABLE reaction__reaction__main CHANGE stableIdentifier_identifier stableIdentifier_identifier VARCHAR(20)",
		"ALTER TABLE reaction__complex__dm CHANGE stableIdentifier_identifier stableIdentifier_identifier VARCHAR(20)",
#		"ALTER TABLE reaction__referencednasequence__dm CHANGE referencedatabase_ensembl referencedatabase_ensembl VARCHAR(20)",
#		"ALTER TABLE reaction__referencepeptidesequence__dm CHANGE referencedatabase_uniprot referencedatabase_uniprot VARCHAR(40)",
		"ALTER TABLE entitywithaccessionedsequence__itywithaccessionedsequence__main CHANGE hasModifiedResidue_referenceSequence_identifier hasModifiedResidue_referenceSequence_identifier VARCHAR(20)",
	);
	
    my $biomart_dba = $self->create_biomart_dba();
    
	foreach my $statement (@changes) {
	    eval {
	        $biomart_dba->execute($statement);
	    };
	}
}


######################################################################
###   This stuff is for reading from flat files into a Mart   ########
######################################################################

# Creates a Mart dataset from a flat file.  The flat file should
# be tab-separated.  You need to specify the desired dataset name
# explicitly as the first argument.  You can read in from one or
# more files, by provoding a regular expression for the filename.
# This should supply the complete path for the file.  If you wish
# to create an extra Mart column that contains the filename from
# which the data originated, use the 2filename_col_name2 argument.
# The "filename_col_map" can be used for providing instructions for
# converting the filename into something a bit more biologically meaningful.
# You need to specify a "field_list" if the files do not contain a
# header line.  If you have columns in the input data that contain
# multiple values, you can convert these into dimension tables via
# the "dimension_table_columns" variable, which is a reference to a hsh
# keyed by column name, with 3-element array refs as values.  Each
# array ref contains the name of the target dimension table, plus
# a string of stuff to delete (e.g.  "ENSEMBL:") at the beginning of the entry
# the separator (most commonly, this will be a comma).
#
# expand_multiple_attribute_name	Allows a single column to be split up according to the value in another column
# row_filter						Takes the form [row_name, comparator, value].  Used to select the
#									rows to be used.
#
# If you want to leave anything unspecified, use an undef.
sub dataset_from_files {
    my ($self, $dataset, $filename_regexp, $filename_col_name, $filename_col_map, $field_list, $dimension_table_columns, $start_row, $expand_multiple_attribute_names, $row_filter) = @_;
    
#    print STDERR "BioMart.dataset_from_files: dimension_table_columns=" . Data::Dumper->Dumpxs([$dimension_table_columns], ["$dimension_table_columns"]) . "\n";

    my $tsv_file_reader = GKB::BioMart::TSVFileReader->new($filename_regexp);
    $tsv_file_reader->filename_col_name($filename_col_name);
    $tsv_file_reader->filename_col_map($filename_col_map);
    $tsv_file_reader->field_list($field_list);
    $tsv_file_reader->start_row($start_row);
    
    $field_list = $tsv_file_reader->get_headers();
    my $primary_key_attribute = $tsv_file_reader->row_num_col_name;
    
    print STDERR "BioMart.dataset_from_files: number of fields is " . scalar(@{$field_list}) . "\n";
    
    my $interaction_instance_class = "Interaction";
    my $col_def_collection = GKB::BioMart::ColDefCollection->new();
    $col_def_collection->reactome_instance_class($interaction_instance_class);
    $col_def_collection->attribute_key($primary_key_attribute);
    my $dba;
    if ($self->multiple_dataset_flag) {
    	$dba = $self->create_biomart_dba($interaction_instance_class);
    } else {
    	$dba = $self->create_biomart_dba();
    }
	$col_def_collection->add_dba($interaction_instance_class, $dba);
	
#    print STDERR "BioMart.dataset_from_files: start_row=$start_row\n";
    
	# Add columns to col_def_collection
	my $biomart_col_name;
	my $key_col_name;
	my $pooled_col_name;
	my $attribute_column_definitions;
	my $key_col_def = undef;
	my $field;
	my $boolean_col_name;
	foreach $field (@{$field_list}) {
		
#		print STDERR "BioMart.dataset_from_files: dealing with field=$field\n";
		
		my $col_def;
		if ($field eq $primary_key_attribute) {
			$key_col_name = $self->create_key_column($interaction_instance_class, $primary_key_attribute);
	    	$attribute_column_definitions = $self->create_key_column_definitions2($interaction_instance_class, $key_col_name, $key_col_name);
			$col_def = GKB::BioMart::ColDef->new($field, $key_col_name, $attribute_column_definitions);
			$key_col_def = $col_def; # preserve this for creating dimension tables
		} else {
			if (defined $dimension_table_columns->{$field}) {
				# For columns that are known to be multi-valued,
				# create a pair of columns, one for the values and one
				# for a boolean flag, that says of values are
				# present or not.
		    	($key_col_name, $boolean_col_name) = $self->create_export_attribute_col_names(lc($field));
				
#				print STDERR "BioMart.generate_biomart_database: key_col_name=$key_col_name, boolean_col_name=$boolean_col_name\n";
				
				my $col_def1 = GKB::BioMart::ColDef->new($field, $key_col_name, $key_col_name . " TEXT default NULL");
				$col_def_collection->add($col_def1);
				my $col_def2 = GKB::BioMart::ColDef->new($boolean_col_name, $boolean_col_name, $boolean_col_name . " INT(1) UNSIGNED default NULL");
				$col_def_collection->add($col_def2);
				
				# Also create a column to hold possibly pooled data
				# from multiple columns
				$pooled_col_name = $dimension_table_columns->{$field}->[0];
				if (defined $pooled_col_name &&
				    !($pooled_col_name eq '') &&
				    !(defined $col_def_collection->fetch_by_reactome_app($pooled_col_name))) {
					my $col_def3 = GKB::BioMart::ColDef->new($pooled_col_name, $pooled_col_name, $pooled_col_name . " TEXT default NULL");
					$col_def_collection->add($col_def3);
				}
			} else {
				$biomart_col_name = lc($field);
				$biomart_col_name =~ s/[^0-9A-Za-z]+/_/g;
				$col_def = GKB::BioMart::ColDef->new($field, $biomart_col_name, "$biomart_col_name TEXT default NULL");
			}
		}
		$col_def_collection->add($col_def);
	}
	
	# If a column is to be expanded into multiple columns, add
	# the extra columns as well
	my @expand_multiple_attribute_names_columns = (); # lists names of expanded columns
	my $expand_multiple_attribute_name;
    my $new_column;
    my $term;
	my $null_term_new_column = undef; 
	my $i;
	my $j;
    my $postfix;
	if (defined $expand_multiple_attribute_names) {
		for ($j=0; $j<scalar(@{$expand_multiple_attribute_names}); $j++) {
			$expand_multiple_attribute_name = $expand_multiple_attribute_names->[$j];
			my @expand_multiple_attribute_columns = ();
			
			print STDERR "BioMart.dataset_from_files: expand_multiple_attribute_name=" . Data::Dumper->Dumpxs([$expand_multiple_attribute_name], ["$expand_multiple_attribute_name"]) . "\n";
			
			for ($i=0; $i<scalar(@{$expand_multiple_attribute_name->[2]}); $i++) {
				$field = $expand_multiple_attribute_name->[1]; # second attribute column
				
				$term = $expand_multiple_attribute_name->[2]->[$i]; # This is the selecion term
				$postfix = undef;
				if (scalar($term) =~ /ARRAY/) {
					$postfix = $term->[1];
					$term = $term->[0];
					
					print STDERR "BioMart.dataset_from_files: postfix=$postfix\n";
				}
				
				print STDERR "BioMart.dataset_from_files: INITIAL field=$field\n";
				
				if (defined $postfix) {
					$field .= "_$postfix";
				} else {
					if ($term eq '') {
						$term = "no_selection";
					} else {
						$term =~ s/[^a-zA-Z0-9]+/_/g;
						$term =~ s/^_+//;
						$term =~ s/_+$//;
						$term = lc($term);
					}
					
					$field .= "_$term";
				}
				
				$term = $col_def_collection->generate_unique_reactome_att($term);
				
				# Create and store a column definition, based on
				# a column name derived from the term
				my $col_def = GKB::BioMart::ColDef->new($field, $field, $field . " TEXT default NULL");
				$col_def_collection->add($col_def);
				
				print STDERR "BioMart.generate_biomart_database: FINAL field=$field\n";
			
				# Create a list of column names, with a on-to-one
				# corespondence with the list of terms
				push(@expand_multiple_attribute_columns, $field);
			}
			
			# Store the names that we just invented for these
			# columns, for quick reference later.
			push(@expand_multiple_attribute_names_columns, \@expand_multiple_attribute_columns);
		}
	}
	
	# Go through the listed modified columns and add appropriately
	# named additional columns to the list.
	$self->create_modcols($col_def_collection);
	
	# Create main table
    $self->create_biomart_table($interaction_instance_class, $BIOMART_MAIN_TABLE_NAME_EXTENSION, $col_def_collection);
    my $table_name = $self->create_biomart_table_name($interaction_instance_class, $interaction_instance_class, $BIOMART_MAIN_TABLE_NAME_EXTENSION);

	# Create dimension tables
    my %dimension_table_names;
	foreach $field (@{$field_list}) {
		if (defined $dimension_table_columns->{$field} &&
		    defined $dimension_table_columns->{$field}->[0] &&
		    !($dimension_table_columns->{$field}->[0] eq '')) {
#			print STDERR "BioMart.generate_biomart_database: REGULAR dimension_table_columns->{$field}=" . $dimension_table_columns->{$field}->[0] . "\n";
			
			$dimension_table_names{$field} = $dimension_table_columns->{$field}->[0];
		}
	}
	# Create additional dimension tables for expanded
	# column
	for ($j=0; $j<scalar(@expand_multiple_attribute_names_columns); $j++) {
		print STDERR "BioMart.generate_biomart_database: j=$j\n";
		foreach $field (@{$expand_multiple_attribute_names_columns[$j]}) {
			print STDERR "BioMart.generate_biomart_database: EXPANDED field=$field\n";
			if (defined $dimension_table_columns->{$field} &&
			    defined $dimension_table_columns->{$field}->[0] &&
			    !($dimension_table_columns->{$field}->[0] eq '')) {
				print STDERR "BioMart.generate_biomart_database: EXPANDED dimension_table_columns->{field}=" . $dimension_table_columns->{$field}->[0] . "\n";
				
				$dimension_table_names{$field} = $dimension_table_columns->{$field}->[0];
			}
		}
	}
	
    my $dimension_col_def_collection = GKB::BioMart::ColDefCollection->new();
    $dimension_col_def_collection->reactome_instance_class($interaction_instance_class);
    $dimension_col_def_collection->attribute_key($primary_key_attribute);
	$dimension_col_def_collection->add_dba($interaction_instance_class, $dba);
    my %dbas = ();
    my $value_col_def = GKB::BioMart::ColDef->new("value", "value", "value TEXT default NULL");
    $dimension_col_def_collection->add($value_col_def);
    if (defined $key_col_def) {
	    $dimension_col_def_collection->add($key_col_def);
    }
#    my $key_col_def = GKB::BioMart::ColDef->new($primary_key_attribute, $col_def_collection->col_defs_hash->{$primary_key_attribute}->biomart_col_name, $col_def_collection->col_defs_hash->{$primary_key_attribute}->biomart_col_def);
	my $dimension_table_name;
	foreach $dimension_table_name (values(%dimension_table_names)) {
#		print STDERR "BioMart.dataset_from_files: dimension_table_name=$dimension_table_name\n";
		
    	$self->create_biomart_table($dimension_table_name, $BIOMART_DIMENSION_TABLE_NAME_EXTENSION, $dimension_col_def_collection);
	}
	
	if (defined $row_filter && scalar(@{$row_filter})!=3) {
		print STDERR "BioMart.dataset_from_files: WARNING - wrong number of values (" . scalar(@{$row_filter}) . " in row_filter, expected 3, ignoring!\n";
		$row_filter = undef;
	}
	
	# Copy data from files into Martified database
    my $row;
    my %extra_values_hash;
    my $col_def;
    my %values;
    my $value;
    my $old_value;
    my @value_list;
    my $dirt_string;
    my $separator;
    my $class;
    my %dimension_values;
    my $term_match_flag;
    my $process_list;
    # Loop over all rows in all files.  tsv_file_reader->get_row
    # will move seamlesslessly from one file to the next.
    while (defined ($row = $tsv_file_reader->get_row())) {
		print STDERR "BioMart.dataset_from_files: got row from file\n";
#		print STDERR "BioMart.dataset_from_files: row=" . Data::Dumper->Dumpxs([$row], ["$row"]) . "\n";
		
		# Filter rows.  If the filter column's value fails the
		# filter test criterion, then skip this row.
		if (defined $row_filter) {
			if ($row_filter->[1] eq '<') {
				$value = $row->{$row_filter->[0]};
				if (defined $value && !($value < $row_filter->[2])) {
					next;
				}
			}
		}
			
		# Null out the values for this row, so that we start
		# with a clean slate
		%values = ();
		
		print STDERR "BioMart.dataset_from_files: getting values\n";

		foreach $field (@{$field_list}) {
			$value = $row->{$field};
			
#			print STDERR "BioMart.dataset_from_files: STARTING value=$value\n";
			
			$process_list = $dimension_table_columns->{$field};
			if (defined $value && defined $process_list) {
				# Process value to remove grot
				$dirt_string = $process_list->[1];
				if (defined $dirt_string && !($dirt_string eq '')) {
					$value =~ s/$dirt_string//g;
				}
				# Replace whatever separator was used in the file with commas
				$separator = $process_list->[2];
				if (defined $separator && !($separator eq '')) {
					$value =~ s/$separator/,/g;
				}
			}
			
#			print STDERR "BioMart.dataset_from_files: WITH SEPARATOR value=$value\n";
			
			# put value into hash
			$values{$field} = $value;
			
			# Create derivative values in columns associated
			# with dimension tables.
			if (defined $dimension_table_columns->{$field}) {
#				print STDERR "BioMart.dataset_from_files: creating a mighty dimension column\n";
			
				($key_col_name, $boolean_col_name) = $self->create_export_attribute_col_names(lc($field));
				
				if (defined $value && !($value eq '')) {
					# This column contains 1 if the value
					# is defined, 0 otherwise.
					$values{$boolean_col_name} = 1;
					
#					print STDERR "BioMart.dataset_from_files: creating pooled column\n";
			
					# A pooled column takes the contents of multiple
					# other columns, e.g. 'proteins' contains 'protein1'
					# and 'protein2'.
					$pooled_col_name = $dimension_table_columns->{$field}->[0];
					if (defined $values{$pooled_col_name}) {
#						print STDERR "BioMart.dataset_from_files: there is already a value for pooled_col_name=$pooled_col_name\n";

						# Only insert if value is unique (it isn't always,
						# e.g. pubmed)
						# TODO: replace comma with actual separator
						if (!($values{$pooled_col_name} eq $value) &&
							!($values{$pooled_col_name} =~ /^$value,/) &&
							!($values{$pooled_col_name} =~ /,$value,/) &&
							!($values{$pooled_col_name} =~ /,$value$/)
							) {
							$values{$pooled_col_name} .= $dimension_table_columns->{$field}->[2] . $value;
					}
					} else {
#						print STDERR "BioMart.dataset_from_files: setting value for pooled_col_name=$pooled_col_name to $value\n";

						$values{$pooled_col_name} = $value;
					}
					
#					print STDERR "BioMart.dataset_from_files: pooled column value=" . $values{$pooled_col_name} . "\n";
				} else {
					$values{$boolean_col_name} = 0;
				}
			}
		}
		
		print STDERR "BioMart.dataset_from_files: create expanded column values\n";

		# Copy values into expanded columns, if
		# appropriate
		if (defined $expand_multiple_attribute_names) {
			# Loop over all columns that are to be expanded.
			for ($j=0; $j<scalar(@{$expand_multiple_attribute_names}); $j++) {
				$expand_multiple_attribute_name = $expand_multiple_attribute_names->[$j];
				
				print STDERR "BioMart.dataset_from_files: source column=" . $expand_multiple_attribute_name->[0] . "\n";

				# Get the value from the source column that we want to expand
				$value = $values{$expand_multiple_attribute_name->[0]};
				
				if (defined $value) {
					print STDERR "BioMart.dataset_from_files: value=$value\n";
				} else {
					print STDERR "BioMart.dataset_from_files: value is not defined!\n";
				}

				if (!(defined $value)) {
					# If the value is not defined, we have no
					# basis for assigning an expanded column.
					# so skip it!
					next;
				}
				
				$old_value = $value;
				
				# TODO: we need to break up at separators first, assign
				# to appropriate columns, and rejoin with commas where necessary.
				$term_match_flag = 0;
				# Loop over the columns that we wish to expand into
				for ($i=0; $i<scalar(@{$expand_multiple_attribute_names_columns[$j]}); $i++) {
					$term = $expand_multiple_attribute_name->[2]->[$i];
					if (scalar($term) =~ /ARRAY/) {
						$postfix = $term->[1];
						$term = $term->[0];
					} else {
						$postfix = undef;
					}
					
#					print STDERR "BioMart.dataset_from_files: term=$term\n";
					
					if ($term eq '') {
						next;
					}
					# $term is used to pull out values appropriate for
					# the current expanded column.
					if ($value =~ /$term/) {
						$value = $values{$expand_multiple_attribute_name->[1]};
						
						$process_list = $dimension_table_columns->{$expand_multiple_attribute_names_columns[$j]->[$i]};
						if (defined $value && defined $process_list) {
							$separator = $process_list->[2];
							
							# Just pull out those parts of the value that
							# contain the term
							if ($expand_multiple_attribute_name->[0] eq $expand_multiple_attribute_name->[1]) {
								my @value_list = split(/$separator/, $value);
								$value = '';
								foreach my $value_element (@value_list) {
									if ($value_element =~ /$term/) {
										if (!($value eq '')) {
											$value .= $separator;
										}
										$value .= $value_element;
									}
								}
							}
							
							# Clean out grot
							$dirt_string = $process_list->[1];
							if (defined $dirt_string && !($dirt_string eq '')) {
								$value =~ s/$dirt_string//g;
							}
							# Replace whatever separator was used in the file with commas
							if (defined $separator && !($separator eq '')) {
								$value =~ s/$separator/,/g;
							}
						}
						
						$values{$expand_multiple_attribute_names_columns[$j]->[$i]} = $value;
						$term_match_flag = 1;
#						last; # !!!!!!!!!!!!!!!!!!!!!!!!!
					}
				
					# Restore the good old value, because we may have
					# messed around with it a bit in the lines above.
					$value = $old_value;
				}
				
				# If none of the terms match the value, plop it
				# into the column for which no term has been specified.
				if (!$term_match_flag && defined $null_term_new_column) {
#					print STDERR "BioMart.dataset_from_files: null_term_new_column=$null_term_new_column\n";
					
					$values{$null_term_new_column} = $value;
				}
			}
		}
		
		print STDERR "BioMart.dataset_from_files: insert into database\n";

		# Create a main table row
    	$self->load_biomart_row($interaction_instance_class, $BIOMART_MAIN_TABLE_NAME_EXTENSION, $col_def_collection, undef, $table_name, \%values);
		
		# Create dimension table rows
		foreach $field ($col_def_collection->get_reactome_atts()) {
			if (!(defined $dimension_table_names{$field})) {
				next;
			}
			$class = $dimension_table_names{$field};
			
#			print STDERR "BioMart.dataset_from_files: field=$field, class=$class\n";
			
			$dimension_table_name = $self->create_biomart_table_name($class, $interaction_instance_class, $BIOMART_DIMENSION_TABLE_NAME_EXTENSION);
			
			$value = $values{$field};
 
# 			print STDERR "BioMart.dataset_from_files: dimension_table_name=$dimension_table_name, value=$value\n";
			
			$dimension_values{$primary_key_attribute} = $values{$primary_key_attribute};
			
			if (defined $value) {
				@value_list = split(/,/, $value);
				
#				if (scalar(@value_list)<1) {
#					print STDERR "BioMart.dataset_from_files: value_list is empty for field=$field\n";
#				}
				
				foreach $value (@value_list) {
					$dimension_values{"value"} = $value;
		    		$self->load_biomart_row($class, $BIOMART_DIMENSION_TABLE_NAME_EXTENSION, $dimension_col_def_collection, undef, $dimension_table_name, \%dimension_values);
				}
			} else {
				$dimension_values{"value"} = undef;
	    		$self->load_biomart_row($class, $BIOMART_DIMENSION_TABLE_NAME_EXTENSION, $dimension_col_def_collection, undef, $dimension_table_name, \%dimension_values);
			}
		}
    }
}

######################################################################
###   This stuff is for creating BioMart canned queries   ############
######################################################################

# Prints tha appropriate page for dealing with the current stage of
# a canned query.
sub print_canned_query_page {
    my ($self) = @_;
    my $cgi = $self->cgi;
	my @params = $cgi->param();
	my $preview_flag = 0;
	foreach my $name (@params) {
		if ($name =~ /^CANNED_QUERY_DATA_INPUT__PREVIEW/) {
			$preview_flag = 1;
			last;
		}
	}

    # Choose the page to present, depending on the parameters passed
    # from the previous page.
    if ($cgi->param('CANNED_QUERY_DATA_INPUT__SUBMIT')) {
    	# 3) Pass query to martservice
		$self->print_canned_query_results_page();
    } elsif ($cgi->param('CANNED_QUERY_SELECTER__GO') ||
    		 $cgi->param('CANNED_QUERY_DATA_INPUT__EXAMPLE') ||
    		 $preview_flag) {
    	# 2) Enter data
		$self->print_canned_query_data_input_page();
    } else {
    	# 1) START: choose canned query
		$self->print_canned_query_selecter_page();
    }
}

# Presents a selecter for canned queries, plus the regular
# BioMart GUI.
sub print_canned_query_selecter_page {
    my ($self, $url) = @_;
    my $cgi = $self->cgi;
    $self->print_start_page();
    my $canned_queries = GKB::BioMart::CannedQueries->new();
	my $choice = $cgi->param('CANNED_QUERY_SELECTER__CHOICE');
	if (!$choice) {
    	$choice = $canned_queries->names->[0];
	}
    
    # Print a form to do canned queries
#    print "<DIV STYLE=\"font-size:9pt;font-weight:bold;text-align:center;color:red;padding-top:10px;\">Reactome BioMart currently offline, pending relocation to a new server</DIV><BR>\n";
    # It seems that the canned queries can out a huge load on the
    # host machine, even if the BioMart engine is hosted remotely.
    # So, experimenting with a can-free BioMart page.
#    print $cgi->start_form(-method=>"post", -action=>"/cgi-bin/mart", -enctype=>"multipart/form-data"); 
#    print "<table align=\"center\" cellpadding=\"5\" cellspacing=\"0\" border=\"0\">";
#    print "<tr>";
#    print "<td>";
#    print "<P><b>Canned query: </b>";
#    print "</td>";
#    print "<td>";
#    print $cgi->scrolling_list(-size=>'1', -name=>'CANNED_QUERY_SELECTER__CHOICE', -values=>$canned_queries->names, -labels=>$canned_queries->name_to_name_hash, -default=>$choice);
#    print "</td>";
#    print "<td>";
#    print $cgi->submit(-name=>'CANNED_QUERY_SELECTER__GO', -value=>'Go!');
#    print "</td>";
#    print "</tr>";
#    print "</table>";
#    print $cgi->endform;
    
    # Create a frame with BioMart in it.  Provide a default
    # action if no frames are available.
    if (!defined $url) {
    	$url = $MART_URL;
    }
	print "<IFRAME align=\"top\" marginheight=\"0\" width=\"100%\" height=\"2000\" scrolling=\"no\" frameborder=\"0\" src=\"$url\">";
	print "<head>\n";
	print "<head>\n";
	# If the browser doesn't support frames, then jump to the Reactome BioMart
	# server page.
	print "<META HTTP-EQUIV=\"refresh\" CONTENT=\"0;URL=$url\">";
	print "</head>\n";
	print "</head>\n";
	print "</IFRAME>\n";
    
    $self->print_end_page();
}

# Allows the user to enter or upload data and then submit a query.
sub print_canned_query_data_input_page {
    my ($self) = @_;
    my $cgi = $self->cgi;
    $self->print_start_page();
    my $canned_queries = GKB::BioMart::CannedQueries->new();
    my $choice = $cgi->param("CANNED_QUERY_SELECTER__CHOICE");     
    my $canned_query = $canned_queries->get_canned_query($choice);  
    my $federated_query = $canned_query->federated_query;  
    my $filter = $canned_query->filter; 
    my $input_style = '';     
    my $no_of_querys = 1; 
    
    # For federated queries get the number of queries (N)   
	if (defined $federated_query) {
		$no_of_querys = scalar(@{$federated_query});     	   	     	
	}  
       
  	my $canned_query_data_input_example  = $cgi->param("CANNED_QUERY_DATA_INPUT__EXAMPLE");  	
	
	# If this page has already been called, and the user wanted to see an example, set this flag.
    my @text_area_content = ();
    my $temp_selecter_reactome_attribute = undef;    
  	
  	# Loop through the first dataset (N)  	
	for (my $j=0; $j<$no_of_querys; $j++) {
		
		# Normal query (N)
		my $local_canned_query = $canned_query;
		
		# Federated query (N)
		if (defined $federated_query) {
			$local_canned_query = $federated_query->[$j];
		}
	    my $missing_data_flag = 0;
	    		
		# TODO: this should be indexed, it isnt a flag really!
		if (defined $local_canned_query->selecter_reactome_attribute) {
			$temp_selecter_reactome_attribute = $local_canned_query->selecter_reactome_attribute;
		}
		my $loopcount = 0;	
		$filter = $local_canned_query->filter; 
		
		# Repeat as many times as there are filters
		for (my $i=0; $i<scalar(@{$filter}); $i++) {
			# keep track of the dataset (j) and the filter (i)
			my $ij = $i . "_" . $j;	
			my $canned_query_data_input_preview  = $cgi->param("CANNED_QUERY_DATA_INPUT__PREVIEW_$ij");			
			$text_area_content[$j][$i] = '';
			if (defined $canned_query_data_input_example && !($canned_query_data_input_example eq "")) {	    		    	
			    $text_area_content[$j][$i] = $local_canned_query->example->[$i];			   			    
			} else {
				if ($canned_query_data_input_preview) {	
		  			my $content_from_file = '';
					my $ij = $i . "_" . $j;			    		
					my $fh = $cgi->upload("CANNED_QUERY_DATA_INPUT__FILE_$ij");
					if ($fh) {
						while (<$fh>) {
							$content_from_file .= $_;
						}
					}
					$text_area_content[$j][$i] = $content_from_file;
					
				}
			}
		  	
		    # If this page has already been called but the user has not entered any data, then this flag should be set to 1.		   
		   	$ij = $i . "_" . $j;
			if ($missing_data_flag = $cgi->param('CANNED_QUERY_DATA_INPUT__SUBMIT') && !($cgi->param("CANNED_QUERY_DATA_INPUT__DATA_$ij") || $cgi->upload("CANNED_QUERY_DATA_INPUT__FILE_$ij"))) {
			    $missing_data_flag = 1;
			}
			$loopcount++;
		}    
	    
		if ($missing_data_flag) {
		    $input_style = ' STYLE="color:red;"';
		}  
	}	
	
              
    my $selecter_reactome_attribute = $temp_selecter_reactome_attribute;
    my $fixed_width_cols = 90;
    my $fixed_width = $fixed_width_cols * 8;
    
    print $cgi->start_form(-method=>"post", -action=>"/cgi-bin/mart", -enctype=>"multipart/form-data");
    print $cgi->hidden('CANNED_QUERY_SELECTER__CHOICE', $choice);   	
   	print "<P><b>$choice</b><P>";
    
	if (!$selecter_reactome_attribute) {
	    	print $cgi->submit(-name=>'CANNED_QUERY_DATA_INPUT__EXAMPLE', -value=>'Show example');
	}
	
    # Begin DATASET LOOP 2   	
    for (my $j=0; $j<$no_of_querys; $j++) {   		
		if (defined $federated_query) {
	  		$filter = $federated_query->[$j]->filter;  			
		}
    	my $filter_array = $filter;
	
		# Repeat as many times as there are filters
		for (my $i=0; $i<scalar(@{$filter_array}); $i++) {	
			# Hide filter input areas for the second dataset as they are redundant
			if ($j<1) {	    	
			print "<P><P>";
			print "<table cellpadding=\"2\" width=\"$fixed_width\" cellspacing=\"0\" border=\"0\">";
			print "<tr>";
			print "<td align=\"left\">";    			
	    	# A bad implementation, until i think of a neater way
		    if (defined $federated_query) {
		    	print "<DIV$input_style>" . $federated_query->[$j]->input->[$i] . ":</DIV>";
			} else {    				
		    	print "<DIV$input_style>" . $canned_query->input->[$i] . ":</DIV>";
		    }  		    		
			print "</td>";
			print "<td align=\"right\">";
				    
			print "</td>";
			print "</tr>";
			print "</table>";
			if ($selecter_reactome_attribute) {
				# TODO: it might be worth caching these instance
				# names, rather than extracting them from BioMart
				# every time.
		    	my @instance_names = $self->get_instance_names_from_mart($selecter_reactome_attribute->[$i]);
		    	my %instance_name_name_hash = ();
		    	my $default_instance_name = '';
		    	foreach my $instance_name (@instance_names) {
		    		if ($instance_name eq 'Homo sapiens') {
		    			# Ahem, this is rather a hack
		    			$default_instance_name = $instance_name;
		    		}
		    		$instance_name_name_hash{$instance_name} = $instance_name;
				}
					    	
			    unshift(@instance_names, '');
			    $instance_name_name_hash{''} = "[NONE]";
					    	    	
				print $cgi->scrolling_list(-size=>'1', -name=>"CANNED_QUERY_DATA_INPUT__DATA_$i" . "_$j", -values=>\@instance_names, -labels=>\%instance_name_name_hash, -default=>$default_instance_name);
			} else {
				# Use straight HTML - CGI has a bug that stops you from preloading
				# data into a textarea.					
				my $ij = $i . "_" . $j;	
							
				print "<textarea name=\"CANNED_QUERY_DATA_INPUT__DATA_$ij\" tabindex=\"2\" rows=\"5\" cols=\"$fixed_width_cols\">" . $text_area_content[$j][$i] . "</textarea>";										 	
				print "<P>" . $cgi->filefield(-name=>"CANNED_QUERY_DATA_INPUT__FILE_$ij", -size=>'20') . "  " . $cgi->submit(-name=>"CANNED_QUERY_DATA_INPUT__PREVIEW_$ij", -value=>'Preview file content');
									
				    
				    	}
			}
		}
	}    	
		
    print "<P>" . $cgi->submit(-name=>'CANNED_QUERY_DATA_INPUT__SUBMIT', -value=>'Run query') . "  " . $cgi->defaults();    
    print $cgi->endform;    
    $self->print_end_page();    
}

# Submits a query to Mart and prints results.
sub print_canned_query_results_page {
    my ($self) = @_;
    my $cgi = $self->cgi;
    my $choice = $cgi->param('CANNED_QUERY_SELECTER__CHOICE');
	my $canned_queries = GKB::BioMart::CannedQueries->new();	
    my $canned_query = $canned_queries->get_canned_query($choice);        
    my $dataset = $canned_query->dataset;     
    my $filter = $canned_query->filter;    
    my $attribute_internalname = $canned_query->attribute_internalname;     
    my $filter_to_use = $canned_query->filter_to_use; 
    
    # By default only one canned query is run, but if federated query 
    # is defined, the piece of code is run as many times as there are querys
    # in the federated query.
    my $no_of_querys = 1;
    my $federated_query = $canned_query->federated_query;
    if (defined $federated_query) {
    	 $no_of_querys = scalar(@{$federated_query});    	 
    }  		

   	# Combine DATA and CANNED_QUERY to make the query.		
	my %params = ();
	$params{'VIRTUALSCHEMANAME'} = 'default';
	$params{'VISIBLEPANEL'} = 'resultspanel';
	$params{'Results'} = 'get_results_button';
	my $temp_attributes_string = '';
	my $filters = '';		
	my $data_from_dataset1_filter1 = '';
	
	# Dataset loop
	for (my $j=0; $j<scalar($no_of_querys); $j++) {
		if (defined $federated_query) {
    	   $dataset = $federated_query->[$j]->dataset; 
    	   $filter = $federated_query->[$j]->filter;    	   
    	   $attribute_internalname = $federated_query->[$j]->attribute_internalname; 
    	   # Always use the first 'filter_to_use' i.e. that for the first query
    	   $filter_to_use = $federated_query->[0]->filter_to_use; 	
    	}     
    
    	# Add a separator bar to $filters after looping through the first dataset, otherwise move on
		if ($filters) {
			$filters .= "|";				
		}
    
    	my $first_filter_flag = 1;	
		my $loop_counter = 0;
		my $filter_no_add = 0;    	       
    	my $filter_array = $filter;	
		my $filterloop_counter_t = 0; 
	
		# Filter area data collector loop:loops as many times as there are filters
		# and for each filter either read in data from file or textarea.
		for (my $i=0; $i<scalar(@{$filter_array}); $i++) {
			my $data = '';
		    my $ij = $i . "_" . $j;    	
			# Extract DATA from file, if necessary
			my $fh = $cgi->upload("CANNED_QUERY_DATA_INPUT__FILE_$ij");		
			if ($fh) {
				while (<$fh>) {
					$data .= $_;								
				}
				if (!$data) {
					$self->print_error("File " . $cgi->param("CANNED_QUERY_DATA_INPUT__FILE_$ij") . " contains no valid data!!");				
				}
			} else {
				$data = $cgi->param("CANNED_QUERY_DATA_INPUT__DATA_$ij");	
			}
			$filterloop_counter_t++;	
						
			# Temporarily store data from the first filter of the first dataset to be passed onto the first filter of the second dataset.
			if (defined $federated_query) {
				# Store the data in a temp to be passed on to the second dataset
				if ($j==0 && $i==0) {
					$data_from_dataset1_filter1 = $data;
				}
				
				# Automatically pass filter values from the first dataset to the second dataset.
				# This is to save the user the effort of entering the same shared values e.g. P30304 UniProt ID for the 1st & 2nd datasets.
				# The assumption here is that we are dealing with a federated query where the two datasets share the same attributes e.g. UniProt IDs,
				# and in order to speed up query execution, both datasets need to have filter values.
				if ($dataset =~ /COSMIC/) {
					# Assumption here is that the first dataset is "entitywithaccessionedsequence" aka 'Protein' dataset.
					$data = $data_from_dataset1_filter1;
					$filter = ["uniprot_swissprot","samp_gene_mutated"];
					if ($i==1) {
						$data = "y";
					}
				}
				
				if ($dataset eq 'pride') {
					# Assumption here is that the first dataset is "entitywithaccessionedsequence" aka 'Protein' dataset.
					$data = $data_from_dataset1_filter1;
					$filter = ["mod_loc_sptr_ac_filter"];
				} 
			}

			# Add the $data from filter1-dataset1 to filter1-dataset2 for specific datasets
			# This implementation doesn't work for all Datasets because in the case of 'All modified proteins in a Pathway/Complex/Reaction'
			# canned queries, the filter1-dataset1 data input isn't the same as that for the filter1-dataset2.
			# in the Pathway/Complex/Reaction datasets, the input is either Pathway/Complex/Reaction stable ids, while in dataset2
			# no filter is required, and even if there is a need to specify, then its Uniprot IDs.
			if (!($data eq '')) {						
				#separator for the filters		
				if (!$first_filter_flag) {
		    		$filters .= "|";    					
				}					    		
				$filters .= $self->make_mart_filter($choice, $data, "\n", '', $filter_no_add, $dataset, $filter_to_use, $filter);
			}	
					
			$first_filter_flag = 0;
			$filter_no_add++;		
			$loop_counter++;				
		}   		    	    
			
		# Add a separator bar to $temp_attributes_string after looping through the first dataset, otherwise move on
		if ($temp_attributes_string) {
			$temp_attributes_string .= "|";				
		}
			
		$temp_attributes_string .= $self->make_mart_attributes($choice, $dataset, $attribute_internalname, $j);			
	}
		
	$params{'ATTRIBUTES'} = $temp_attributes_string; 
	$params{'FILTERS'} = $filters;
	my $content = $self->extract_content_from_url($MART_URL, \%params);	
	$content =~ /href="([^\"]*)"/; # Ooooh, ugly!
	my $url = $1;
	
	$self->print_canned_query_selecter_page($url);
}

sub make_mart_attributes {
    my ($self, $name, $dataset, $attribute_internalname, $j) = @_;    
    my $canned_queries = GKB::BioMart::CannedQueries->new();
    my $canned_query = $canned_queries->get_canned_query($name);
    my $federated_query = $canned_query->federated_query;
    my @attributes = ();
    if (defined $federated_query) {
    	  @attributes = @{$federated_query->[$j]->attributes};   	 
    } else {
    	@attributes = @{$canned_query->attributes};
    }      
    
	my $attribute_string = '';
	my $first_item_flag = 1;
	foreach my $attribute (@attributes) {
    	if (!$first_item_flag) {
    		$attribute_string .= "|";
    	}
    	
    	$attribute_string .= "$dataset.default.$attribute_internalname.$attribute";		 
		
		$first_item_flag = 0;
	}
	
	return $attribute_string;
}

sub make_mart_filter {
    my ($self, $name, $data, $separator, $quote, $filter_no_add, $dataset, $filter_to_use, $filter) = @_;    
    my $canned_queries = GKB::BioMart::CannedQueries->new();
    my $canned_query = $canned_queries->get_canned_query($name);
    
    # Assume that the "data" (list of identifiers, species
    # names or whatever) are separated by newlines
    my @data_lines = split("\n", $data);
    
    if (!defined $separator) {
    	$separator = ",";
    }
    if (!defined $quote) {
    	$quote = "\"";
    }        
#    ##test
    my @filters = @{$filter};         
#    ##    
    my $filter_string = '';
	my $first_filter_item_flag = 1;
	  my $loop_counter_c = 0;   
	for (my $i=0; $i<scalar(@data_lines); $i++) {     	
    	if (!$first_filter_item_flag) {
    		$filter_string .= "|";
    	}
	   	$filter_string .= "$dataset.default.$filter_to_use." . $filters[$filter_no_add] . ".";		
		$filter_string .= $quote;
		my $first_item_flag = 1;
		foreach my $data_line (@data_lines) {
		    # Get rid of leading and trailing spaces, newlines,# etc.	    	
		    $data_line =~ s/\s+$//;
		    $data_line =~ s/^\s+//;
		    if ($data_line eq '') {
		    	# Ignore empty lines
		    	next;
		    }	    	
		    if (!$first_item_flag) {
		    	$filter_string .= $separator;
		    }		    	
		    $filter_string .= $data_line;			
			$first_item_flag = 0;
		}    
		$filter_string .= $quote;	
		$first_filter_item_flag = 0;
		$loop_counter_c++;
    }         
	return $filter_string;	
}

sub print_start_page {
    my ($self) = @_;

    my $cgi = $self->cgi;
    my $webutils = $self->webutils;
    #print $cgi->header;
    print $cgi->start_html(-style => {-src => '/stylesheet.css'}, -title => "$PROJECT_NAME Mart");
    #print $webutils->navigation_bar;
    print $self->front_page->get_header();
    print "<H1 CLASS=\"frontpage\">BioMart</H1>\n";
}

sub print_end_page {
    my ($self) = @_;

    my $cgi = $self->cgi;
    #my $webutils = $self->webutils;
    
    #print $webutils->make_footer;
    print $self->front_page->get_footer();
    print $cgi->end_html;
}

sub front_page {
    my $self = shift;
    $self->{front_page} ||= GKB::FrontPage3->new("BioMart", "/stylesheet.css");
    return $self->{front_page};
}

sub print_error {
    my ($self, $message) = @_;

    $self->print_start_page();
    
    print "<p><b>ERROR: </b>$message";
    
    $self->print_end_page();
    
    exit;
}

# Given a string as input, in the form of:
#
# <dataset name>.<attribute_name>
#
# find all corresponding attribute values and return
# as an array.
sub get_instance_names_from_mart {
    my ($self, $selecter_reactome_attribute) = @_;

	# Split input string into dataset and attribute name
    my ($dataset, $att) = split(/\./, $selecter_reactome_attribute);
    
    # Build XML query
    my $query = '';
	$query .= "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
	$query .= "<!DOCTYPE Query>";
	$query .= "<Query  virtualSchemaName = \"default\" header = \"1\" count = \"\" softwareVersion = \"0.5\" >";
			
	$query .= "		<Dataset name = \"$dataset\" interface = \"default\" >";
	$query .= "	      		<Attribute name = \"$att\" />";
	$query .= "		</Dataset>";
	$query .= "</Query>";
	
	# Run query
	my %params = ();
	$params{'query'} = $query;
	my $martservice_url = $MART_URL;
	$martservice_url =~s/martview/martservice/;
	my $content = $self->extract_content_from_url($martservice_url, \%params);
	
	# Break query results into lines and create a hash
	# of the values
	my %instance_name_hash = ();
	my @lines = split(/\n/, $content);
	my $first_line_flag = 1;
	foreach my $line (@lines) {
		if (!$first_line_flag) {
			$instance_name_hash{$line} = $line;
		} else {
			$first_line_flag = 0;
		}
	}
	
	# Return the keys of the hash, which sould contain
	# unique values for the attribute.
    return keys(%instance_name_hash);
}

# Treats the given URL as a web service, and tries to get
# something out of it.  params should
# be a reference to a hash of name/value pairs that will
# be passed as hidden parameters to the target URL.
sub extract_content_from_url {
    my ($self, $url, $params) = @_;
    
    my $param_string = "";
    foreach my $param (keys(%{$params})) {
    	if ($param_string eq "") {
    		$param_string .= "?";
    	} else {
    		$param_string .= "&";
    	}
    	$param_string .= "$param=" . $params->{$param};
    }
    $url .= $param_string;
    
    my $ua = LWP::UserAgent->new();
    my $response = $ua->post($url, $params);
    my $content = "";
    
    if (defined $response) {
		$content = $response->content;
    }

    return $content;
}

1;

