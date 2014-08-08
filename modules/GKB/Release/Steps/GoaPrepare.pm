package GKB::Release::Steps::GoaPrepare;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

has '+gkb' => ( default => "gkbdev" );
has '+passwords' => ( default => sub { ['mysql'] } );
has '+directory' => ( default => "$release/goa_prepare" );
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
    
    my $host = $self->host;
       
    my $go = "GO_submission/go";
    my $utildir = "$go/software/utilities";
    my @args = ("-user", $user, "-pass", $pass, "-host", $host, "-db", $db, "-date", $date, "-debug");
   
    cmd("Updating GO ontology folder from GO SVN",[["svn --ignore-externals update $go"]]);
    cmd("Preparing GO submission",
    	[
    		["perl goa_submission.pl @args"],
    		["perl goa_submission_stats.pl gene_association.reactome gene_association.reactome.stats"],
    		["rm -f $go/gene-associations/submission/gene_association.reactome"],
    		["mv gene_association.reactome $go/gene-associations/submission"],
		["perl $utildir/filter-gene-association.pl -e -v 2 -p nocheck -i $go/gene-associations/submission/gene_association.reactome > gofilter.report 2>&1"]
    	]
    );    
};

1;