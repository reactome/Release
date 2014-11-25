package GKB::HMDB;

=head1 NAME

GKB::Hmdb

=head1 SYNOPSIS

Methods for mapping uniprot and chebi IDs to HMDB 

=head1 DESCRIPTION

=head1 SEE ALSO

GKB::AddLinks::HMDBMolecules
GKB::AddLinks::HMDBProteins

=head1 AUTHOR

Sheldon McKay E<lt>sheldon.mckay@gmail.com<gt>

Copyright (c) 2014 Ontario Institute for Cancer Research

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

use strict;
use CGI 'url';
use common::sense;
use Bio::Root::Root;
use GKB::Config;
use HTTP::Tiny;
use File::Basename;
use Data::Dumper;
use Cwd;

use vars '@ISA';
@ISA = qw(Bio::Root::Root);

use constant REFPROTEINS  => url(-base => 1) . '/ReactomeRESTfulAPI/RESTfulWS/getUniProtRefSeqs';
use constant REFMOLECULES => url(-base => 1) . '/ReactomeRESTfulAPI/RESTfulWS/getReferenceMolecules';
use constant METABOLITES  => 'http://www.hmdb.ca/downloads/hmdb_metabolites.zip';
use constant PROTEINS     => 'http://www.hmdb.ca/downloads/hmdb_proteins.zip';

sub new {
    my $class = shift;
    my $self  = bless {}, $class;
    my $dba   = shift || $self->throw("The GKB::HMDB class requires a DBA object");
    $self->dba($dba);
    return $self;
}

sub dba {
    my $self = shift;
    my $dba  = shift;
    $self->{dba} = $dba if $dba;
    return $self->{dba};
}

sub fetch_proteins {
    my $self = shift;
    my @proteins  = $self->fetch_url_contents(REFPROTEINS);
    my $path = $self->get_xml_files(PROTEINS,'proteins');
    my $uniprot2hmdb = $self->identifier_to_accession($path, 'proteins');
    my $uniprot2entities = $self->entity_to_identifier(@proteins);

    # Can be > 1 entity/uniprot and > 1 hmdb/uniprot
    my %hmdb;
    for my $uniprot_id (keys %$uniprot2entities) {
        my $hmdb_ids = $uniprot2hmdb->{$uniprot_id} || next;
        my @instances = map {$self->get_cached_instance($_)} @{$uniprot2entities->{$uniprot_id}};
        for my $hmdb_id (@$hmdb_ids) {
            for my $instance (@instances) {
                push @{$hmdb{$hmdb_id}}, $instance;
            }
        }
    }

    my $total = keys %hmdb;
    say STDERR "I am dealing with a total of about $total HMDB protein IDS!";
    return \%hmdb;
}

sub fetch_molecules {
    my $self = shift;
    my @molecules  = $self->fetch_url_contents(REFMOLECULES);
    my $path = $self->get_xml_files(METABOLITES,'metabolites');
    my $chebi2hmdb = $self->identifier_to_accession($path, 'metabolites');
    my $chebi2entities = $self->entity_to_identifier(@molecules);

    # Can be > 1 entity/chebi and > 1 hmdb/chebi
    my %hmdb;
    for my $chebi_id (keys %$chebi2entities) {
	my $hmdb_ids = $chebi2hmdb->{$chebi_id};
	my @instances = map {$self->get_cached_instance($_)} @{$chebi2entities->{$chebi_id}};
	for my $hmdb_id (@$hmdb_ids) {
	    for my $instance (@instances) {
		push @{$hmdb{$hmdb_id}}, $instance;
	    }
	}
   }
    
    my $total = keys %hmdb;
    say STDERR "I am dealing with a total of about $total HMDB metabolite IDS!";
    return \%hmdb;
}

sub get_instance {
    my $self  = shift;
    my $db_id = shift;
    return $self->dba->fetch_instance_by_db_id($db_id)->[0];
}

sub get_cached_instance {
    my $self  = shift;
    my $db_id = shift;
    
    $self->{cached_instance}->{$db_id} ||= $self->get_instance($db_id);

    return $self->{cached_instance}->{$db_id};
}

# Here is where we grab the XML files that provide
# the chebi->HMDB mapping
sub get_xml_files {
    my $self  = shift;
    my $url   = shift;
    my $class = shift;

    my $dir = "/tmp/$class";
    my $cwd = getcwd;

    # Download and unpack the XML (there are 40+K) only
    # if it was not already done within the past week
    my $id_file = "$dir/ids.txt";
    unless (-e $id_file && -M $id_file < 7) {
	system "rm -fr $dir" if -d $dir;
	mkdir $dir or die $!;
	chdir $dir;
	
	my ($zip) = $url =~ m!/([^/]+)$!;

	system "wget $url > /dev/null 2>\&1";
	
	if ( -e $zip) {
	    system "unzip $zip >/dev/null";
	}
	else {
	    die "Could not find $zip";
	}

	$self->filter_xml_files($dir,$class);
    }

    chdir $cwd;

    return $dir;
}

# We only want xml files that have chebi_ids or uniprot Ids in them
sub filter_xml_files {
    my $self  = shift;
    my $path  = shift;
    my $class = shift;

    my $id = $class eq 'metabolites' ? 'chebi_id' : 'uniprot_id';
    my $regex = "<$id>[^<]+";
    system "grep -P '$regex' $path/HMDB*.xml > $path/ids.txt";
    system "rm -f $path/*xml $path/*zip";
    return $path;
}


sub entity_to_identifier {
    my $self  = shift;
    my @lines = @_;

    my %map;
    for my $line (@lines) {
	chomp $line;
	my ($db_id,$identifier) = $line =~ /^(\S+)\s+\S+:(\S+)$/;
	push @{$map{$identifier}}, $db_id;
    }

    return \%map;
}


# grabs plain text output from RESTful API
sub fetch_url_contents {
    my $self = shift;
    my $url = shift;

    my $response = HTTP::Tiny->new->get($url);

    if ($response->{success}) {
        return split("\n",$response->{content});
    }
    else {
        $self->throw(join("\t",$response->{status},$response->{reason}));
    }
}

sub identifier_to_accession {
    my $self  = shift;
    my $path  = shift;
    my $class = shift;

    my $id = $class eq 'metabolites' ? 'chebi_id' : 'uniprot_id';
    my %id;

    # Too much overhead parsing XML.  Let's just grab the info we want
    # the filename is overloaded with the accession
    open IN, "$path/ids.txt" or die $!;
    while (<IN>) {
	chomp;
	my ($file,$identifier) = split;
	$file =~ s/:$//;
	$identifier =~ s/^\S+>(\S+)<\S+$/$1/;
	next unless $file;
	my $accession = basename($file,'.xml');
	push @{$id{$identifier}},$accession;
    } 

    return \%id;
}


1;


