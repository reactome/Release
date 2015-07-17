package GKB::Release::Steps::BioModels;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

has '+gkb' => ( default => "gkbdev" );
has '+passwords' => ( default => sub { ['mysql'] } );
has '+directory' => ( default => "$release/biomodels" );
has '+mail' => ( default => sub { 
					my $self = shift;
					return {
						'to' => '',
						'subject' => $self->name,
						'body' => "",
						'attachment' => ""
					};
				}
);

override 'run_commands' => sub {
	my ($self, $gkbdir) = @_;

    # Backup database and run biomodels script
    $self->cmd("Backup database", [["mysqldump --opt -u$user -p$pass $db > $db\_before_biomodels.dump"]]);
    my @results = $self->cmd("Running BioModels script", [["perl biomodels.pl -db $db > biomodels_$version.out"]]);
    
    my $exit_code = ($results[0])->{'exit_code'};
    # Backup the database
    if ($exit_code == 0) {
    	$self->cmd("Backing up database $db",
    		[
    			["mysqldump --opt -u$user -p$pass $db > $db\_after_biomodels.dump"]
    		]
    	);
    }
};

1;
