package GKB::Release::Steps::Orthodiagrams;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

#has '+gkb' => ();
has '+passwords' => ( default => sub { ['mysql'] } );
has '+directory' => ( default => "$release/orthodiagrams" );
has '+user_input' => (default => sub {
    {
        'person_id' => {
            'query' => 'Please enter your person instance db id:',
            'hide_keystrokes' => 0
        }
    }
});
has '+mail' => (default => sub {
    my $self = shift;
    return {
        'to' => '',
        'subject' => $self->name,
        'body' => '',
        'attachment' => ''
    };
});

override 'run_commands' => sub {
    my ($self, $gkbdir) = @_;

    my $host = $self->host;
    my $person_id = $self->user_input->{'person_id'}->{'response'};
    $self->cmd("Running ELV tool to generate diagrams for predicted pathway",
        [
            ["mysqldump --opt -u $user -p$pass $db > $db\_before_pathway_diagram.dump"],
            ["./WebELVTool/runWebELVTool.sh $host $db $user $pass $port $person_id > runWebELVTool.$version.out 2> runWebELVTool.$version.err"]
        ]
    );

};

override 'post_step_tests' => sub {
    my ($self) = shift;

    my @errors = super();
    my $pathway_diagram_count_error = _check_pathway_diagram_count($db, $previous_db);
    push @errors, $pathway_diagram_count_error if $pathway_diagram_count_error;
    return @errors;
};

sub _check_pathway_diagram_count {
    my $current_db = shift;
    my $previous_db = shift;

    my $current_pathway_diagram_count = get_dba($current_db)->class_instance_count('PathwayDiagram');
    my $previous_pathway_diagram_count = get_dba($previous_db)->class_instance_count('PathwayDiagram');

    my $pathway_diagram_count_change = $current_pathway_diagram_count - $previous_pathway_diagram_count;
    return "Pathway Diagram count has gone down to $current_pathway_diagram_count for version $version " .
        " from $previous_pathway_diagram_count for version $prevver" if $pathway_diagram_count_change < 0;
}

1;
