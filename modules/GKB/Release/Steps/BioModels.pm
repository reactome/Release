package GKB::Release::Steps::BioModels;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

my $models2pathways_file = 'models2pathways.tsv';

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
});

override 'run_commands' => sub {
    my ($self, $gkbdir) = @_;

    # Backup database and run biomodels script
    $self->cmd("Backup database", [["mysqldump --opt -u$user -p$pass $db > $db\_before_biomodels.dump"]]);
    my @results = $self->cmd("Running BioModels script", [["perl biomodels.pl -db $db > biomodels_$version.out 2> biomodels_$version.err"]]);

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

override 'post_step_tests' => sub {
    my $self = shift;

    my @errors = super();
    push @errors, _check_referrer_count_for_biomodels_reference_database();
    return @errors;
};

override 'archive_files' => sub {
    my ($self, $version) = @_;

    # arguments passed to this method are implicitly passed to the superclass method by Moose
    # https://metacpan.org/pod/release/DOY/Moose-2.0604/lib/Moose/Manual/MethodModifiers.pod#OVERRIDE-AND-SUPER
    my $archive_directory = super();
    system "cp --backup=numbered $models2pathways_file $archive_directory";
};

sub _check_referrer_count_for_biomodels_reference_database {
    my @errors;
    my $current_biomodels_referrer_count = _get_biomodels_referrer_count($db, \@errors);
    my $previous_biomodels_referrer_count = _get_biomodels_referrer_count($previous_db, \@errors);
    return @errors if @errors;

    if ($current_biomodels_referrer_count == 0) {
        return "Biomodels reference database has no referrers";
    }

    if ($current_biomodels_referrer_count < $previous_biomodels_referrer_count) {
        return "Biomodels reference database has fewer referrers compared to the previous release: $version - $current_biomodels_referrer_count; $prevver - $previous_biomodels_referrer_count";
    }

    releaselog("Biomodels external links added successfully: $version - $current_biomodels_referrer_count; $prevver - $previous_biomodels_referrer_count");
}

sub _get_biomodels_referrer_count {
    my $database_name = shift;
    my $errors = shift;

    my $biomodels_reference_database_name = 'BioModels Database';
    my $biomodels_reference_database_instances = get_dba($database_name)->fetch_instance_by_attribute('ReferenceDatabase', [['_displayName', [$biomodels_reference_database_name]]]);
    if (!$biomodels_reference_database_instances || (scalar @{$biomodels_reference_database_instances} == 0)) {
        push @{$errors}, "Reference database '$biomodels_reference_database_name' does not exist";
        return;
    }

    if (scalar @{$biomodels_reference_database_instances} > 1) {
        push @{$errors}, "There is more than one reference database instance for $biomodels_reference_database_name.  Please verify its referrer count manually";
        return;
    }

    return scalar @{$biomodels_reference_database_instances->[0]->reverse_attribute_value('referenceDatabase')};
}

1;
