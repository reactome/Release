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

Copyright (c) 2007 European Bioinformatics Institute and Cold Spring
Harbor Laboratory.

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
    
    return $self;
}

sub interaction_ids {
    my $self = shift;
    my $file = $self->file;

    open IDS, "cut -f14 $file |";
    my @ids;
    while (<IDS>) {
	chomp;
	next if /^\#/;
	s/^\S+://;
	push @ids, $_;
    }
    return \@ids;
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

    unless ($file) {
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
    }
    
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
    
    #print STDERR "IntAct.find_intact_ids_for_uniprot_pair: uniprot1=$uniprot1, uniprot2=$uniprot2\n";
    
    my $data = $self->get_interaction_data;

    my $key = join('|', $uniprot1, $uniprot2);
    warn "$key\n";
    my $ids;
    $data->db_get($key,$ids);
    $ids || return;

    my @ids = grep {/\S+/} split(/\s+/, $ids);
    $logger->info("IntAct.find_intact_ids_for_uniprot_pair: GOT $ids!");
    return \@ids;
}

sub get_interaction_data {
    my $self = shift;
    my $db = $self->interaction_data;
    return $db if $db;

    my $file = $self->get_file();
    (my $db_file = $file) =~ s/txt$/db/;

    $db = BerkeleyDB::Hash->new(-Filename => $db_file, -Flags => DB_CREATE);

    open INT, "cut -f1,2,14 $file |";
    while (<INT>) {
	chomp;
	next if /^\#/;
	my ($id1,$id2,$id3) = grep { s/\S+:// } split "\t";
	#print STDERR "Interaction: $id1,$id2,$id3\n";
	my $key = join('|',sort($id1,$id2));
	my $other_ids;
	$db->db_get($key,$other_ids);
	my $ids = $id3;
	$ids .= " $other_ids" if $other_ids;
	$db->db_put($key,$ids);
    }
    close INT;

    $self->interaction_data($db);
    return $db;
}

sub interaction_data {
    my $self = shift;
    my $hash = shift;
    if ($hash) {
	$self->{_interaction_data} = $hash;
    }
    return $self->{_interaction_data};
}

# Given a hash of interactions, will attempt to get the corresponding
# IntAct IDs.  Uses the IntAct web services, so potentially slow.
sub find_intact_ids_for_interactions {
    my ($self, $interactions) = @_;
    my @db_ids1 = keys(%{$interactions});
    my @db_ids2;
    my $db_id1;
    my $db_id2;
    my $interactor1;
    my $interactor2;
    my $id1;
    my $id2;
    my $interaction;

    foreach $db_id1 (@db_ids1) {
	@db_ids2 = keys(%{$interactions->{$db_id1}});
	foreach $db_id2 (@db_ids2) {
	    $interaction = $interactions->{$db_id1}->{$db_id2};
	    $interactor1 = $interaction->{'interactors'}->[0];
	    $interactor2 = $interaction->{'interactors'}->[1];
	    
	    # TODO: we may also need to get the name of the
	    # reference database in order to build an
	    # unequivocal query.
	    $id1 = $interactor1->identifier->[0];
	    $id2 = $interactor2->identifier->[0];
	    
	    my $intact_ids = $self->find_intact_ids_for_uniprot_pair($id1, $id2);
	    $interaction->{'intact_ids'} = $intact_ids;
	}
    }
}

1;
