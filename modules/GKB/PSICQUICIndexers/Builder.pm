=head1 NAME

GKB::PSICQUICIndexers::Builder

=head1 SYNOPSIS

=head1 DESCRIPTION

This is an abstract class for inserting links into a Reactome database.
It provides an abstract method, buildPart, which should be implemented
in subclasses, and which does the actual hard work of inserting links.
It also provides a number of utility methods, which may be used by
subclasses.  Additionally, it provides setter methods, which are used
by the Director class to pass on various parameters.

It forms part of the Builder design pattern.

=head1 SEE ALSO

GKB::PSICQUICIndexers::Director

Subclasses:
GKB::PSICQUICIndexers::ReactomeBuilder
GKB::PSICQUICIndexers::ReactomeFIBuilder

=head1 AUTHOR

David Croft E<lt>croft@ebi.ac.ukE<gt>

Copyright (c) 2012 European Bioinformatics Institute and Cold Spring
Harbor Laboratory.

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

package GKB::PSICQUICIndexers::Builder;

use Data::Dumper;
use GKB::Config;
use GKB::Utils::Timer;
use strict;
use vars qw(@ISA $AUTOLOAD %ok_field);
use Bio::Root::Root;
use Log::Log4perl qw/get_logger/;
Log::Log4perl->init(\$LOG_CONF);

@ISA = qw(Bio::Root::Root);

# List the object variables here, so that they can be checked
# 
for my $attr
    (qw(
    builder_params
    timer
    timer_message
    termination_status
    class_name
    index_dir_name
    current_download_dir
    mitab_path
    gzipped_mitab_path
    index_path
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
   	
   	$self->clear_variables();
	my $timer = GKB::Utils::Timer->new();
   	$self->timer($timer);
   	$self->timer_message("Builder execution time (seconds): ");
   	$self->termination_status(undef);
   	$self->current_download_dir(undef);
   	$self->gzipped_mitab_path(undef);
   	$self->index_path(undef);

    return $self;
}

sub clear_variables {
    my ($self) = @_;
    
   	$self->builder_params(undef);
   	$self->timer(undef);
   	$self->timer_message(undef);
}

# Needed by subclasses to gain access to class variables defined in
# this class.
sub get_ok_field {
	return %ok_field;
}

# Sets the BuilderParams object that will be used to pass parameters
# from a Director object to individual Builder objects.
# Also creates a local InstanceEdit, to be used in any instances created
sub set_builder_params {
	my ($self, $builder_params) = @_;
	
	$self->builder_params($builder_params);
}

sub buildPart {
	my ($self) = @_;
	
	my $logger = get_logger(__PACKAGE__);
	
	$logger->info("entered");
	
	$self->timer->start($self->timer_message);
	
	$self->build_indexes();
	
	$self->clean_up();
	
	$self->timer->stop($self->timer_message);
	$self->timer->print();
}

# Get a directory where file can be stored temporarily, e.g. a
# place where things downloaded from the internet can be
# processed.
sub get_tmp_dir {
	my ($self) = @_;
	
	my $tmp_dir = "/tmp";
	if (defined $GK_TMP_IMG_DIR && (-e $GK_TMP_IMG_DIR) && (-d $GK_TMP_IMG_DIR)) {
		$tmp_dir = $GK_TMP_IMG_DIR;
	}
	
	return $tmp_dir;
}

sub print_termination_status {
	my ($self) = @_;
	
	if (defined $self->termination_status) {
		if (defined $self->class_name) {
		    print $self->class_name . ": " . $self->termination_status . "\n";
		} else {
		    print $self->termination_status . "\n";
		}
	}
}

sub get_current_download_dir {
	my ($self) = @_;
	
	my $logger = get_logger(__PACKAGE__);
	
	if (defined $self->current_download_dir) {
	    return $self->current_download_dir;
	}
	
	my $release_num = $self->builder_params->get_release_num();
	my $gkb_root_dir = $self->builder_params->get_gkb_root_dir();
	
	if (!(defined $release_num)) {
	    $logger->warn("release number is undef");
	    $self->termination_status("release number is undef");
	    return undef;		
	}
	if (!(defined $gkb_root_dir)) {
	    $logger->warn("GKB directory is undef");
	    $self->termination_status("GKB directory is undef");
	    return undef;		
	}

	my $current_download_dir = "$gkb_root_dir/website/html/download/$release_num";
	if (!(-e $current_download_dir)) {
	    $logger->warn("missing directory: $current_download_dir");
	    $self->termination_status("missing directory: $current_download_dir");
	    return undef;		
	}
	
	$self->current_download_dir($current_download_dir);
	
	return $current_download_dir;
}

sub create_index_path {
    my ($self) = @_;

    my $logger = get_logger(__PACKAGE__);
    
    if (defined $self->index_path) {
	return $self->index_path;
    }
    
    my $current_download_dir = $self->get_current_download_dir();
    my $status;
    
    my $indexes_path = "$current_download_dir/psicquic_indexes";
    if (!(-e $indexes_path)) {
	$status = system("mkdir $indexes_path");
	if ($status != 0) {
	    $logger->warn("could not make directory $indexes_path");
	    $self->termination_status("could not make directory $indexes_path");
	    return undef;					
	}
    }

    my $index_path = "$indexes_path/" . $self->index_dir_name;
    if (!(-e $index_path)) {
	$status = system("mkdir $index_path");
	if ($status != 0) {
	    $logger->warn("could not make directory $index_path");
	    $self->termination_status("could not make directory $index_path");
	    return undef;					
	}
    }

    $self->index_path($index_path);

    return $index_path;
}

sub get_mitab_path {
    my ($self) = @_;

    my $logger = get_logger(__PACKAGE__);

    $logger->error_die("this subroutine must be explicitly defined in the inheriting class");
}

sub create_mitab_path {
    my ($self) = @_;

    my $logger = get_logger(__PACKAGE__);

    if (defined $self->mitab_path) {
	return $self->mitab_path;
    }
    
    my $mitab_path = $self->get_mitab_path();

    $logger->info("INITIAL mitab_path=$mitab_path");

    my $command;
    my $status;
    my $gzipped_mitab_path = undef;
    if ($mitab_path =~ /\.gz$/) {
	$gzipped_mitab_path = $mitab_path;
	$mitab_path =~ s/\.gz$//;
    } elsif (!(-e $mitab_path)) {
	$gzipped_mitab_path = "$mitab_path.gz";		
    }
    if (defined $gzipped_mitab_path) {
	if (!(-e $gzipped_mitab_path)) {
	    $logger->warn("missing gzipped MITAB file: $gzipped_mitab_path");
	    $self->termination_status("missing gzipped MITAB file: $gzipped_mitab_path");
	    return undef;		
	}
	$status = system("gunzip $gzipped_mitab_path");
	if ($status != 0) {
	    $logger->warn("could not unzip $gzipped_mitab_path");
	    $self->termination_status("could not unzip $gzipped_mitab_path");
	    return undef;					
	}
	$self->gzipped_mitab_path($gzipped_mitab_path);
    }
	
    if (!(-e $mitab_path)) {
	$logger->warn("missing MITAB file: $mitab_path");
    	$self->termination_status("missing MITAB file: $mitab_path");
    	return undef;		
    }
    if (-s $mitab_path == 0) {
    	$logger->warn("zero length MITAB file: $mitab_path");
    	$self->termination_status("zero length MITAB file: $mitab_path");
    	return undef;		
    }

    $self->mitab_path($mitab_path);

    $logger->info("FINAL mitab_path=$mitab_path");

    return $mitab_path;
}

sub build_indexes {
	my ($self) = @_;
	
	my $logger = get_logger(__PACKAGE__);
	
	my $status;
	my $index_path = $self->create_index_path();
	my $mitab_path = $self->create_mitab_path();
	my $create_war_flag = $self->builder_params->get_create_war_flag();
	
	# Get the pom.xml file
	my $cd_command = "cd $index_path";
	my $checkout_command = "git clone https://github.com/PSICQUIC/psicquic-webservice.git";
	my $command = "$cd_command; $checkout_command";
	$logger->info("checkout, $command=$command");
	$status = system($command);
	if ($status != 0) {
	    $logger->warn("could not run Subversion checkout");
	    $self->termination_status("could not run Subversion checkout");
	}
	
	# Do the indexing
	my $compile_command = "mvn -e clean compile -P createIndex -D psicquic.index=$index_path -D mitabFile=$mitab_path -D hasHeader=true";
	$command = "$cd_command/psicquic-webservice; $compile_command";
	$logger->info("indexing, $command=$command");
	$status = system($command);
	if ($status != 0) {
	    $logger->warn("could not run Maven compile");
	    $self->termination_status("could not run Maven compile");
	}
	
	if ($create_war_flag) {
	    # Create a WAR file, this is a nice-to-have rather than being really essential.
	    my $package_command = "mvn clean package -D psicquic.index=$index_path";
	    $command = "$cd_command/psicquic-webservice; $package_command";
	    $logger->info("packaging, $command=$command");
	    $status = system($command);
	    if ($status != 0) {
		$logger->warn("could not run Maven packaging");
		$self->termination_status("could not run Maven packaging");
	    }
	} else {
	    # Get rid of the code, we don't need it anymore
	    my $delete_command = "rm -rf psicquic-webservice";
	    $command = "$cd_command; $delete_command";
	    $logger->info("deleting, $command=$command");
	    $status = system($command);
	    if ($status != 0) {
		$logger->warn("could not delete psicquic-webservice");
		$self->termination_status("could not delete psicquic-webservice");
	    }
	}
	
	# Compress everything.
	my $compress_command = "tar zcvf " . $self->index_dir_name . ".tgz " . $self->index_dir_name;
	$command = "$cd_command/..; $compress_command";
	$logger->info("compressing, $command=$command");
	$status = system($command);
	if ($status != 0) {
	    $logger->warn("could not compress " . $self->index_dir_name);
	    $self->termination_status("could not compress " . $self->index_dir_name);
	    return;
	}
	
	# Delete original indexes etc.
	my $rm_command = "rm -rf " . $self->index_dir_name;
	$command = "$cd_command/..; $rm_command";
	$logger->info("delete, $command=$command");
	$status = system($command);
	if ($status != 0) {
	    $logger->warn("could not delete " . $self->index_dir_name);
	    $self->termination_status("could not delete " . $self->index_dir_name);
	    return;
	}
}

sub clean_up {
    my ($self) = @_;
    
    my $logger = get_logger(__PACKAGE__);
    
    my $status;
    if (defined $self->gzipped_mitab_path) {
	$status = system("gzip " . $self->mitab_path);
	if ($status != 0) {
	    $logger->warn("could not gzip " . $self->mitab_path);
	    $self->termination_status("could not gzip " . $self->mitab_path);
	}
    }
}

1;

