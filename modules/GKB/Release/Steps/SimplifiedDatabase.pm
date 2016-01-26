package GKB::Release::Steps::SimplifiedDatabase;

use GKB::Config;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

has '+gkb' => ( default => "gkbdev" );
has '+passwords' => ( default => sub { [] } );
has '+directory' => ( default => "$release/simplified_database" );
has '+mail' => ( default => sub { 
					my $self = shift;
					return {
						'to' => '',
						'subject' => $self->name,
						'body' => "",
						'attachment' => ''
					};
				}
);
has '+user_input' => (default => sub {{'overwrite' => {'query' => 'Overwrite simplified database if it exists (y/n):'}}});

override 'run_commands' => sub {
    my ($self, $gkbdir) = @_;

    my $overwrite = $self->user_input->{'overwrite'}->{'response'} =~ /^y/i ? '-overwrite' : '';
    $self->cmd("Creating simplified database",[["perl simplified_db.pl -source_db $db $overwrite"]]);
};

override 'post_step_tests' => sub {
    my $self = shift;
    
    my @errors = super();
    push @errors, _check_row_count_for_all_database_tables();
    return @errors;
};

sub _check_row_count_for_all_database_tables {
    my @errors;
        
    my %current_database_table_to_row_count = _get_database_table_to_row_count("$db\_dn");
    my %previous_database_table_to_row_count = _get_database_table_to_row_count("test_reactome_$prevver\_dn");
    
    foreach my $database_table (keys %previous_database_table_to_row_count) {    
        my $previous_row_count = $previous_database_table_to_row_count{$database_table};
        my $current_row_count = $current_database_table_to_row_count{$database_table};
        if (!$current_row_count) {
            push @errors, "$database_table does not exist or has no rows";
            next;
        }
        if ($current_row_count < $previous_row_count) {
            push @errors, "$database_table has fewer rows compared to the previous release: $version - $current_row_count; $prevver - $previous_row_count";
            next;
        }
    }

    return @errors;
}

sub _get_database_table_to_row_count {
    my $database_name = shift;
    
    my %database_table_to_row_count;
    foreach my $database_table (_get_tables($database_name)) {
        $database_table_to_row_count{$database_table} = _get_row_count($database_name, $database_table);
    }
    return %database_table_to_row_count;
}

sub _get_tables {
    my $database_name = shift;
    
    return _execute_query($database_name, 'show tables');
}

sub _get_row_count {
    my $database_name = shift;
    my $table_name = shift;
    
    return _execute_query($database_name, "select count(*) from $table_name");
}

sub _execute_query {
    my $database_name = shift;
    my $query = shift;
    
    return `mysql -u $GKB::Config::GK_DB_USER -p$GKB::Config::GK_DB_PASS -sN -e 'use $database_name; $query'`;
}

1;
