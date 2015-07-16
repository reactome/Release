package GKB::Release::Steps::ClearData;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

has '+gkb' => ( default => "gkbdev" );
#has '+passwords' => ();
has '+directory' => (default => "$release");
has '+mail' => ( default => sub { 
					my $self = shift;
					return {
						'to' => 'curation,internal',
						'subject' => $self->name,
						'body' => "",
						'attachment' => ""
					};
				}
);

my $resp;
do {	
	# Ask if a run has been done
	print "Has a run been done before?\n";
	print "(A \"run\" covers the part of the release procedure starting after step 30 and ending before step 140, i.e. generating the release databases.)\n\n"; 
	print "y/n:";

	chomp($resp = <STDIN>); 

} while ($resp ne "y" && $resp ne "n");

override 'run_commands' => sub {
    my ($self, $gkbdir) = @_;
        
    # If a run has been done before
    if ($resp eq "y") {
	
	# Drop the following created databases
	foreach my $database ("$db","$db\_myisam","$db\_dn") {
	    $self->cmd("Dropping database $db",[["mysql -e drop database $database"]]);
	}

	# Attempt to list the stable identifiers database dump and if present, recreate database    
	my $return = system("ls generate_stable_ids/test_reactome_stable_identifiers_$version.dump");
	if ($return == 0) {
	    $self->cmd("Recreating stable identifiers database",
	        [
	            ["mysql -e drop database test_reactome_stable_identifiers"],
		    ["mysql -e create database test_reactome_stable_identifiers"],
		    ["cat generate_stable_ids/test_reactome_stable_identifiers_$version.dump | mysql test_reactome_stable_identifiers"]
		]
	    );
	}
    }
};

1;
