package GKB::Release::Steps::ELV;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

#has '+gkb' => ();
has '+passwords' => ( default => sub { ['mysql'] } );
has '+directory' => ( default => "$html/entitylevelview/pathway_diagram_statics" );
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

    if ($gkbdir eq "gkbdev") {
        $self->cmd("Backing up previous version of ELV directory and running ELV tool for current version",
        	[
        		["tar -zcvf test_reactome_$prevver.tgz test_reactome_$prevver"],
        		["rm -rf test_reactome_$prevver"],
		        ["mysqldump --opt -u$user -p$pass $db > $dumpdir/$db\_before_pathway_diagram.dump"],
        		["sh /usr/local/gkbdev/WebELVTool/runWebELVTool.sh reactomedev $db $user $pass 3306 $self->directory 349401 > runWebELVTool.$version.out"]
        	]
        );
    } elsif ($gkbdir eq "gkb_prod") {
        $self->cmd("Copying over ELV directory from reactomedev",
        	[
        		["scp -r reactomedev.oicr.on.ca:$tmp/test_reactome_$version.tgz ."],
        		["tar -zxvf $db.tgz"]
        	]
        );
    } else {
    	my $prod;
    	($prod = $self->directory) =~ s/gkb/gkb_prod/;
    	$self->cmd("Linking gkb to gkb_prod ELV directory",
        	[
        		["ln -s $prod/$db ."],
        		["rm gk_current"],
        		["ln -s $db gk_current"]
        	]
        );
    }
};
 
1;
