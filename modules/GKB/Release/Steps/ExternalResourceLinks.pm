package GKB::Release::Steps::ExternalResourceLinks;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

has '+gkb' => ( default => 'gkbdev' );
has '+passwords' => ( default => sub { ['mysql'] } );
has '+directory' => ( default => "$release/add_links" );
has '+user_input' => (default => sub {
    {
        'person_id' => {'query' => 'Please enter your person instance db id:','hide_keystrokes' => 0},
        'configuration' => {
            'query' => 'Please choose configuration: download only (d), insertion only (i), full (f):',
            'hide_keystrokes' => 0
        }
    }
});
has '+mail' => ( default => sub {
    my $self = shift;
    return {
        'to' => '',
        'subject' => $self->name,
        'body' => '',
        'attachment' => $self->directory . '/AddLinks/scripts/logs/addlinks.log'
    };
});

override 'run_commands' => sub {
    my ($self) = @_;

    my $person_id = $self->user_input->{'person_id'}->{'response'};
    my $configuration = get_configuration($self->user_input->{'configuration'}->{'response'});

    # Backup database and run add links script
    $self->cmd('Backup database', [["mysqldump --opt -u$user -p$pass $db > $db\_before_addlinks.dump"]]);
    my @results = $self->cmd('Running add links script', [
        ["perl setup_add_links.pl -user $user -pass $pass -db $db -person_id $person_id -configuration $configuration " .
         "> setup_add_links_$version.out 2> setup_add_links_$version.err"]
    ]);

    my $exit_code = ($results[0])->{'exit_code'};
    # Backup the database or else drop and remake the database if the add links script fails
    if ($exit_code == 0) {
        $self->cmd("Backing up database $db",
            [
                ["mysqldump --opt -u$user -p$pass $db > $db\_after_addlinks.dump"]
            ]
        );
    }
};

override 'post_step_tests' => sub {
    my $self = shift;

    my @errors = super();
    push @errors, _check_referrer_count_for_all_reference_databases();
    return @errors;
};

sub get_configuration {
    my $user_selection = shift;

    if ($user_selection =~ /^d/i) {
        return 'download_only';
    } elsif ($user_selection =~ /^i/i) {
        return 'insertion_only';
    } elsif ($user_selection =~ /^f/i) {
        return 'full';
    } else {
        confess "Unknown configuration: $user_selection";
    }
}

sub _check_referrer_count_for_all_reference_databases {
    my @errors;

    my %current_reference_database_referrer_count = _get_reference_database_to_referrer_count($db, \@errors);
    my %previous_reference_database_referrer_count = _get_reference_database_to_referrer_count($previous_db, \@errors);

    foreach my $reference_database_name (sort keys %previous_reference_database_referrer_count) {
        my $previous_referrer_count = $previous_reference_database_referrer_count{$reference_database_name};
        my $current_referrer_count = $current_reference_database_referrer_count{$reference_database_name};
        if (!$current_referrer_count) {
            push @errors, "$reference_database_name does not exist or has no referrers";
            next;
        }
        if ($current_referrer_count < $previous_referrer_count) {
            push @errors, "$reference_database_name has fewer referrers compared to the previous release: " .
                "$version - $current_referrer_count; $prevver - $previous_referrer_count";
            next;
        }
        releaselog("$reference_database_name external links added successfully: " .
            "$version - $current_referrer_count; $prevver - $previous_referrer_count");
    }
    return @errors;
}

sub _get_reference_database_to_referrer_count {
    my $database_name = shift;
    my $errors = shift;

    my %reference_database_to_referrer_count;
    foreach my $reference_database_instance (_get_reference_database_instances($database_name)) {
        my $reference_database_name = _get_name(@{$reference_database_instance->name});
        my $referrer_count = scalar @{$reference_database_instance->reverse_attribute_value('referenceDatabase')};
        if (exists $reference_database_to_referrer_count{$reference_database_name}) {
            push @{$errors}, "There is more than one reference database instance for $reference_database_name. " .
                'Please verify its referrer count manually';
            next;
        }
        $reference_database_to_referrer_count{$reference_database_name} = $referrer_count;
    }
    return %reference_database_to_referrer_count;
}

sub _get_reference_database_instances {
    my $database_name = shift;

    return @{get_dba($database_name)->fetch_instance(-CLASS => 'ReferenceDatabase')};
}

sub _get_name {
    my @names = @_;

    if (scalar @names == 0) {
        return;
    }

    if (scalar @names == 1) {
        return $names[0];
    }

    return _get_longest(@names);
}

# Taken from answer to http://stackoverflow.com/questions/4182010/
sub _get_longest {
    my @values = @_;

    my $max_length = -1;
    my $max_ref;
    foreach my $value (@values) {
        if (length $value > $max_length) {
            $max_length = length $value;
            $max_ref = \$value;
        }
    }
    return ${$max_ref};
}

1;
