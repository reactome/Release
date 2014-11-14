package GKB::IntAct;

=head1 NAME

GKB::IntAct

=head1 SYNOPSIS

Methods for querying the IntAct protein-protein interaction
database.

=head1 DESCRIPTION

=head1 SEE ALSO

GKB::Utils

=head1 AUTHOR

David Croft E<lt>croft@ebi.ac.ukE<gt>
Sheldon McKay E<lt>sheldon.mckay@gmail.com<gt>

Copyright (c) 2007 European Bioinformatics Institute and Cold Spring
Harbor Laboratory, 2014 Ontario Institute for Cancer Research

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

use vars qw(@ISA $AUTOLOAD %ok_field);
use strict;
use Bio::Root::Root;
use Data::CTable;
use Data::Dumper;
use List::Compare;
use LWP::UserAgent;
use GKB::Config;
use SOAP::Lite;
use BerkeleyDB;
use Cwd;
use Log::Log4perl qw/get_logger/;
Log::Log4perl->init(\$LOG_CONF);

@ISA = qw(Bio::Root::Root);

# Create a new instance of this class
# Is this the right thing to do for a subclass?
sub new {
    my($pkg, @args) = @_;
    my $self = bless {}, $pkg;
    
    $self->get_interaction_data;
    return $self;
}

# Gets the path to the PSIMITAB interaction file.  If this
# file has not been set already, then try to get the file from
# the IntAct FTP site and put it into a temporary directory -
# in the worst case, this will be /tmp.
sub get_file {
    my ($self) = @_;

    my $logger = get_logger(__PACKAGE__);
    
    my $file = $self->file;
    return $file if $file;

    my $dir = $GK_TMP_IMG_DIR || '/tmp';
    my $cwd = getcwd;
    chdir $dir;

    my $url = 'ftp://ftp.ebi.ac.uk/pub/databases/intact/current/psimitab/intact.zip';
    
    $file = "intact.txt";
    my $zip  = "intact.zip";
    system "rm -f intact*";
    
    system "wget $url > /dev/null 2>&1";
    
    $logger->info("IntAct.get_file: NEW file=$file");
    
    if ( -e $zip) {
	system "unzip $zip";
	$self->file("$dir/$file");
	$logger->info('file ' . `ls $file`);
	$logger->info('zip' . `ls $zip`);
	$logger->info("FILE " . $self->file);
    } 
    else {
	$logger->error_die("IntAct.get_file: could not find $zip");
    }
    
    chdir $cwd;

    
    return "$dir/$file";
}

sub file {
    my $self = shift;
    my $file = shift;
    $self->{_file} = $file if $file;
    return $self->{_file};
}

# Given a pair of UniProt IDs, will attempt to get the corresponding
# IntAct IDs.
sub find_intact_ids_for_uniprot_pair {
    my ($self, $uniprot1, $uniprot2) = @_;
    
    my $logger = get_logger(__PACKAGE__);
    
    my $data = $self->get_interaction_data;

    my $key = join('|', $uniprot1, $uniprot2);
    my $ids;
    $data->db_get($key,$ids);
    $ids || return;

    my @ids = grep {/\S+/} split(/\s+/, $ids);
    $logger->info("IntAct.find_intact_ids_for_uniprot_pair: GOT $ids for $uniprot1, $uniprot2");
    return \@ids;
}

sub get_interaction_data {
    my $self = shift;
    my $db = $self->interaction_data();
    return $db if $db;

    my $file = $self->get_file();
    (my $db_file = $file) =~ s/txt$/db/;
    (my $path = $db_file) =~ s!/[^/]+$!!;
    
    system "rm -f $path/*db";

    $db = BerkeleyDB::Hash->new(-Filename => $db_file, -Flags => DB_CREATE) or die $!;

    open INT, "cut -f1,2,14 $file |";
    while (<INT>) {
	chomp;
	next if /^\#/;
	my ($id1,$id2,$id3) = grep { s/\S+:// } split "\t";
	my $key = join('|',sort($id1,$id2));
	my $other_ids;
	next unless $db && $key && $id1 && $id2;
	$db->db_get($key,$other_ids);
	my $ids = $id3;
	$ids .= " $other_ids" if $other_ids;
	next unless $ids;
	$db->db_put($key,$ids);
	$db->db_put($id1,1);
	$db->db_put($id2,1);
    }
    close INT;
    die "The db file is missing" unless $db;

    $self->interaction_data($db);
    return $db;
}

sub is_in_interaction {
    my $self = shift;
    my $rs   = shift;
    my $uniprot = $rs->identifier->[0] || return 0;
    my $db = $self->get_interaction_data();
    my $ok;
    $db->db_get($uniprot,$ok);
    return $ok;
}

sub interaction_data {
    my $self = shift;
    my $hash = shift;
    if ($hash) {
	$self->{_interaction_data} = $hash;
    }
    return $self->{_interaction_data};
}

1;
