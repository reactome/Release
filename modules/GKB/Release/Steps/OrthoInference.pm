package GKB::Release::Steps::OrthoInference;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

has '+gkb' => ( default => "gkbdev" );
has '+passwords' => ( default => sub { ['mysql'] } );
has '+directory' => ( default => "$release/orthoinference" );
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
has '+user_input' => (default => sub {{'skip_list_verified' => {'query' => 'Has the normal event skip list been verified for version $version (y/n):'}}});

override 'run_commands' => sub {
    my ($self, $gkbdir) = @_;
    
    my $skip_list_verified = $self->user_input->{'skip_list_verified'}->{'response'} =~ /^y/i;
    
    die "Skip list must be verified before running the orthoinference process" unless $skip_list_verified;
    
    $self->cmd("Creating orthopredictions and backing up database",
    	[
            ["mkdir -p $version"],
            ["perl wrapper_ortho_inference.pl -r $version -user $user -pass $pass > $version/wrapper_ortho_inference.out"],
            ["rm -f ../website_files_update/report_ortho_inference.txt"],
            ["ln $release/orthoinference/$version/report_ortho_inference_$db.txt ../website_files_update/report_ortho_inference.txt"],
            ["mysqldump --opt -u$user -p$pass $db > $db\_after_ortho.dump"]
	]
    );
};

1;
