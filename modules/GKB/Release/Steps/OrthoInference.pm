package GKB::Release::Steps::OrthoInference;

use GKB::Config_Species;

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
has '+user_input' => (default => sub {{'skip_list_verified' => {'query' => "Has the normal event skip list been verified for version $version (y/n):"}}});

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

override 'post_step_tests' => sub {
    my $self = shift;
    
    my @errors = super();
    my @species_instance_count_errors = _check_orthoinferred_instance_count_for_all_species();
    push @errors, @species_instance_count_errors if @species_instance_count_errors;
    
    return @errors;
};

sub _check_orthoinferred_instance_count_for_all_species {
    my @errors;
    foreach my $species (@species) {
        my $species_name = $species_info{$species}->{'name'}->[0];
        next if $species_name =~ /Homo sapiens/i;
        
        my $current_species_count = _get_species_count($db, $species_name);
        my $previous_species_count = _get_species_count("test_reactome_$prevver", $species_name);
        
        if ($current_species_count < $previous_species_count) {
            push @errors, "$species_name has fewer instances compared to the previous release: $version - $current_species_count; $prevver - $previous_species_count";
            next;
        }
        releaselog("$species_name orthoinference successful: $version - $current_species_count; $prevver - $previous_species_count");
    }
    return @errors;
}

sub _get_species_count {
    my $db = shift;
    my $species_name = shift;
    
    my $species_instance = get_dba($db)->fetch_instance_by_attribute('Species', [['_displayName', [$species_name]]])->[0];
    return scalar @{$species_instance->reverse_attribute_value('species')};
}

1;
