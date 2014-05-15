package GKB::Release::Steps::EventsAndEntities;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

has '+gkb' => ( default => "gkbdev" );
has '+passwords' => ( default => sub { ['mysql'] } );
has '+directory' => ( default => "$release/events_and_entities" );
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

    # Mapping events to entities
    my @args = ("-user", $user,"-pass", $pass, "-db", $db);
    my @reaction = ("reaction", @args);
    my @pathway = ("pathway", @args);
    
    cmd("Preparing mapping of events to genes and molecules",
    	[
    	    ["perl events2genes.pl @reaction"],
    	    ["perl events2molecules.pl @reaction"],
    	    ["perl events2genes.pl @pathway"],
    	    ["perl events2molecules.pl @pathway"]
    	]
    );
    
    my $download = "$html/download/current";
    my $events = "$download/events.tgz";
    my @files =  map { "$download/$_" } qw/Pathway_2_molecules.txt Pathway_2_genes.txt Reaction_2_molecules.txt Reaction_2_genes.txt/;
    system("tar -cvzf $events @files");
    
    # Mapping entities to events (i.e. pathways)
    cmd("Mapping entities to events",
    	[
    	    ["perl ewas2pathways.pl @args"],
    	    ["perl smallmolecule2pathways.pl @args"],
    	    ["perl ensemblprotein2pathways.pl @args"]
    	]
    );
};

1;
