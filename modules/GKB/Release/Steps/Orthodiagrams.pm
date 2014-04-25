package GKB::Release::Steps::Orthodiagrams;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

#has '+gkb' => ();
has '+passwords' => ( default => sub { ['mysql'] } );
has '+directory' => ( default => "$release/orthodiagrams" );
has '+user_input' => (default => sub { {'person_id' => {'query' => 'Please enter your person instance db id:',
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
    cmd("Running ELV tool to generate diagrams for predicted pathway",
        [
	    ["mysqldump --opt -u $user -p$pass $db > $db\_before_pathway_diagram.dump"],
            ["./WebELVTool/runWebELVTool.sh $host $db $user $pass 3306 $person_id > runWebELVTool.out.$version"]
        ]
    );
   
};
 
1;
