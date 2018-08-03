#!/usr/local/bin/perl -w
use strict;

use lib "/usr/local/gkb/modules";

use GKB::Config;
use GKB::DBAdaptor;

use autodie;
use Carp;
use List::MoreUtils qw/uniq/;
use Getopt::Long;
use Term::ReadKey;

my ($user, $pass, $host, $db, $output_file, $help);
GetOptions(
  'user=s' => \$user,
  'pass=s' => \$pass,
  'host=s' => \$host,
  'db=s'   => \$db,
  'output=s' => \$output_file,
  'help'   => \$help
);

if ($help) {
    print usage_instructions();
    exit;
}

($output_file = $0) =~ s/\.pl$/\.txt/ unless $output_file;
open (my $output, ">", $output_file);
report("Regulation author\tRegulation id\tRegulation name\n", $output);

my $regulations = get_dba($user, $pass, $host, $db)->fetch_instance(-CLASS => 'Regulation');
foreach my $regulation (@{$regulations}) {
    next unless regulated_entity_is_human($regulation);
    next if $regulation->containedInPathway->[0];
    
    my $regulation_author = get_authors($regulation->created->[0]);
    
    report(
        join("\t",
            $regulation_author,
            $regulation->db_id,
            $regulation->displayName,
        ) . "\n",   
        $output
    );
}
close($output) if $output_file;

sub prompt {
    my $query = shift;
    my $is_password = shift;
    
    print $query;
    
    ReadMode 'noecho' if $is_password; # Don't show keystrokes if it is a password
    my $return = ReadLine 0;
    chomp $return;
    
    ReadMode 'normal';
    print "\n" if $is_password;
    return $return;
}

sub get_dba {
    my $user = shift;
    my $pass = shift;
    my $host = shift;
    my $db = shift;

    return GKB::DBAdaptor->new(
        -dbname => $db // $GKB::Config::GK_DB_NAME,
        -user   => $user // $GKB::Config::GK_DB_USER,
        -host   => $host // $GKB::Config::GK_DB_HOST, # host where mysqld is running                                                                                                                              
        -pass   => $pass // $GKB::Config::GK_DB_PASS,
        -port   => '3306'
    );	
}

sub report {
    my $message = shift;
    my $file_handle = shift // *STDOUT;
    
    print $file_handle $message;
}

sub get_authors {
    my $instance_edit = shift;
    
    return 'Unknown' unless ($instance_edit && $instance_edit->author->[0]);
    
    return join(';', map {$_->displayName} @{$instance_edit->author});
}

sub regulated_entity_is_human {
    my $regulation = shift;
    
    # TODO: A Regulation could regulate multiple things, now that regulatedBy is a 1:N relation between RLEs and Regulation. So how do we determine if 
    # *a* single regulated entity is Human?
    my $regulated_entity = $regulation->regulatedEntity->[0];
#    my $regulator = $regulation->regulator;
#    if ($regulator)
#    {
#    	my $species = $regulator->species->;
#    }
    croak "Regulation instance $regulation->{db_id} has no regulated entity\n" unless $regulated_entity;
    if ($regulated_entity->is_a('Event')) {
        return is_human($regulated_entity);
    } elsif ($regulated_entity->is_a('CatalystActivity')) {
        return is_human($regulated_entity->physicalEntity->[0]);
    } else {
        croak "Regulated entity instance $regulated_entity->{db_id} is of the wrong class: " . $regulated_entity->class . "\n";
    }
}

sub is_human {
    my $instance = shift;
    
    return ($instance->species->[0] &&
            $instance->species->[0]->displayName eq 'Homo sapiens' &&
            !($instance->species->[1]) &&
            !(is_chimeric($instance))
            );
}

sub is_chimeric {
    my $instance = shift;
    
    return $instance->isChimeric->[0] && $instance->isChimeric->[0] eq 'TRUE';
}

sub usage_instructions{
    return <<END;

This script gets all regulation instances from a database
and reports those with an empty contained in pathway attribute.

The output will be a tab delimited file reporting the
regulation instance author, database id, and display name.
    
Usage: perl $0

Options:
    
-user [db_user]         User for source database (default is $GKB::Config::GK_DB_USER)
-pass [db_pass]         Password for source database (default is password for $GKB::Config::GK_DB_USER user)
-host [db_host]         Host for source database (default is $GKB::Config::GK_DB_HOST)
-db [db_name]           Source database (default is $GKB::Config::GK_DB_NAME)
-output [output_file]   File name for script output (default is this script's name with a .txt extension)
-help                   Prints these instructions
    
END
}
