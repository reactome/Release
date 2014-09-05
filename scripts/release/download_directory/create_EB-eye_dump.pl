#!/usr/local/bin/perl -w

# I think it's more user-friendly if the user will not have to practise any
# symlink tricks. Hence the BEGIN block. If the location of the script or
# libraries changes, this will have to be changed.

BEGIN {
    my @a = split('/',$0);
    pop @a;
    push @a, ('..','..','modules');
    my $libpath = join('/', @a);
    unshift (@INC, $libpath);
}

use strict;
use GKB::Utils;
use GKB::Config;
$NO_SCHEMA_VALIDITY_CHECK = 0;

use Log::Log4perl qw/get_logger/;
Log::Log4perl->init(\$LOG_CONF);

my %event_instructions = 
(
 -INSTRUCTIONS => {
     'ReactionlikeEvent' => {'attributes' => [qw(input output catalystActivity)]},
     'Pathway' => {'attributes' => [qw(hasEvent)]},
     'CatalystActivity' => {'attributes' => [qw(physicalEntity)]},
     'Complex' => {'attributes' => [qw(hasComponent)]},
#     'ConceptualEvent' => {'attributes' => [qw(hasSpecialisedForm)]},
#     'EquivalentEventSet' => {'attributes' => [qw(hasMember)]},
     'EntitySet' => {'attributes' => [qw(hasMember)]},
     'Polymer' => {'attributes' => [qw(repeatedUnit)]},
     'EntityWithAccessionedSequence' => {'attributes' => [qw(referenceEntity)]},
     'SimpleEntity' => {'attributes' => [qw(referenceEntity)]}
 },
 -OUT_CLASSES => ['ReferenceEntity']
);

my %months =
(
 '01' => 'JAN',
 '02' => 'FEB',
 '03' => 'MAR',
 '04' => 'APR',
 '05' => 'MAY',
 '06' => 'JUN',
 '07' => 'JUL',
 '08' => 'AUG',
 '09' => 'SEP',
 '10' => 'OCT',
 '11' => 'NOV',
 '12' => 'DEC'
);

my ($dba,$release_num,$release_date) = get_db_connection();

my $ar = $dba->fetch_instance_by_remote_attribute('Event',[['stableIdentifier','IS NOT NULL',[]]]);

@{$ar} || die "No instances to dump\n";

#printf qq(<?xml version="1.0" encoding="ISO-8859-1"?>
printf qq(<?xml version="1.0" encoding="UTF-8"?>
<database>
<name>%s</name>
<description>%s is a curated knowledgebase of biological pathways</description>
<release>%d</release>
<release_date>%s</release_date>
<entry_count>%d</entry_count>
<entries>
), $PROJECT_NAME, $PROJECT_NAME, $release_num,$release_date, scalar(@{$ar});

my $logger = get_logger(__PACKAGE__);
foreach my $i (@{$ar}) {
    $logger->info("i=$i");
    printf qq(<entry id="%s" acc="%s.%i">\n), 
    $i->StableIdentifier->[0]->Identifier->[0],
    $i->StableIdentifier->[0]->Identifier->[0],
    $i->StableIdentifier->[0]->identifierVersion->[0];
    (my $name = $i->Name->[0]) =~ s/<\/?su(bp)>//gi;
    escape(\$name);
    printf qq(<name>%s</name>\n), $name;
    if ($i->Summation->[0] && $i->Summation->[0]->Text->[0]) {
	(my $description = $i->Summation->[0]->Text->[0]) =~ s/<.+?>//g;
	escape(\$description);
	printf qq(<description>%s</description>\n), $description;
    }
    if ($i->Authored->[0] && $i->Authored->[0]->Author->[0]) {
	my @tmp;
	foreach my $p (@{$i->Authored->[0]->Author}) {
#	    print STDERR $p->extended_displayName, "\n";
	    my $str = $p->Surname->[0];
	    (my $initials = $p->Initial->[0]) =~ s/\W//g;
	    $str .= ' ' . join('', map{"$_."} split(//,$initials));
	    push @tmp, $str;
	}
	printf qq(<authors>%s</authors>\n), join(', ', @tmp);
    }
    creation_and_modification_dates($i);
    event_crosslinks($i);
    additional_fields($i);
    print qq(</entry>\n);
}

print qq(</entries>
</database>
);

sub event_crosslinks {
    my $i = shift;
    my $ar = $i->follow_class_attributes(%event_instructions);
    print qq(<cross_references>\n);
    foreach my $re (grep {$_->ReferenceDatabase->[0]} @{$ar}) {
	printf qq(<ref dbname="%s" dbkey="%s"/>\n), $re->ReferenceDatabase->[0]->displayName, $re->Identifier->[0];
    }
    if ($i->is_valid_attribute('catalystActivity') && $i->CatalystActivity->[0]) {
	foreach my $ca (@{$i->CatalystActivity}) {
	    if (my $a = $ca->Activity->[0]) {
		if (defined $a && defined $a->Accession && defined $a->Accession->[0]) {
			printf qq(<ref dbname="GO" dbkey="GO:%s"/>\n), $a->Accession->[0];
		}
	    }
	}
    }
    if (my $g = $i->GoBiologicalProcess->[0]) {
	if (defined $g && defined $g->Accession && defined $g->Accession->[0]) {
		printf qq(<ref dbname="GO" dbkey="GO:%s"/>\n), $g->Accession->[0];
	}
    }
    foreach my $s (@{$i->Species}) {
	if (defined $s && defined $s->CrossReference && defined $s->CrossReference->[0]) {
		printf qq(<ref dbname="taxonomy" dbkey="%i"/>\n), $s->CrossReference->[0]->Identifier->[0];
	}
    }
    print qq(</cross_references>\n);
}

sub additional_fields {
    my $i = shift;
    print qq(<additional_fields>\n);
    foreach my $s (@{$i->Species}) {
	printf qq(<field name="organism">%s</field>\n), $s->displayName;
    }
    print qq(</additional_fields>\n);    
}

sub creation_and_modification_dates {
    my $i = shift;
    my $ds;
    print qq(<dates>\n);
    if ($i->Created->[0] && $i->Created->[0]->DateTime->[0] && ($i->Created->[0]->DateTime->[0] !~ /^0000/)) {
	$ds = get_formatted_date_string($i->Created->[0]->DateTime->[0]);
	printf qq(<date type="creation" value="%s"/>\n), $ds;
    }
    if ($i->Modified->[-1] && $i->Modified->[-1]->DateTime->[0]) {
	$ds = get_formatted_date_string($i->Modified->[-1]->DateTime->[0]);
    }
    printf qq(<date type="last_modification" value="%s"/>\n), $ds;
    print qq(</dates>\n);
}

sub get_formatted_date_string {
    my $in = shift;
    
    my $logger = get_logger(__PACKAGE__);
    
    if (!(defined $in)) {
        $logger->warn("create_EB-eye_dump.get_formatted_date_string: input is undef!!");
        return "00-00-0000";
    }
    my ($yyyy,$mm,$dd) = $in =~ /^(\d{4})-?(\d{2})-?(\d{2})/;
    my $mon = $months{$mm};
    return sprintf "%02d-%s-%04d", $dd, $mon, $yyyy; 
}

sub get_db_connection {
    my $logger = get_logger(__PACKAGE__);
    
    @ARGV || die "Usage: $0 RELEASE_NUMBER -db STABLE_ID_DB [-user ... -host ... -port ... -pass ...]\n";
    my $dba = GKB::Utils::get_db_connection();
    my $release_num = shift @ARGV;
    $release_num || die "Need release number\n";
    $logger->info("get_db_connection: release_num=$release_num");
    my $release = $dba->fetch_instance_by_attribute('ReactomeRelease',[['num',[$release_num]]])->[0] || die "No release with number $release_num\n";
    my $release_date = get_formatted_date_string($release->DateTime->[0]);
    $logger->info("get_db_connection: release_date=$release_date");
    my $db_name = $release->releaseDbParams->[0]->DbName->[0];
    $logger->info("get_db_connection: db_name=$db_name");
    $dba->instance_cache->clean;
    $dba->execute("USE $db_name");
    $dba->fetch_schema;
    #$dba->table_type($dba->fetch_table_type('Ontology'));
    $dba->fetch_parameters;
    return ($dba,$release_num,$release_date);
}

sub escape {
    my $ref = shift;
    $ {$ref} =~ s/&/&amp;/sg;
    $ {$ref} =~ s/</&lt;/sg;
    $ {$ref} =~ s/>/&gt;/sg;
    $ {$ref} =~ s/"/&quot;/sg;
    $ {$ref} =~ s/'/&apos;/sg;
    $ {$ref} =~ s/\342\200\234/&quot;/sg;
    $ {$ref} =~ s/\342\200\235/&quot;/sg;
    $ {$ref} =~ s/\342\200\231/&quot;/sg;
}
