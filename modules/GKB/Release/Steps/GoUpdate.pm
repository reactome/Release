package GKB::Release::Steps::GoUpdate;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

has '+gkb' => ( default => "gkbdev" );
has '+passwords' => ( default => sub { ['mysql', 'sudo'] } );
has '+directory' => ( default => "$release/go_update" );
has '+mail' => ( default => sub {
					my $self = shift;
					return {
						'to' => 'curation',
						'subject' => $self->name,
						'body' => "",
						'attachment' => $self->directory . "./archive/$version/go_update_logs_R$version.tgz"
					};
				}
);

override 'run_commands' => sub {
	my ($self) = @_;

    $self->cmd("Setting all GO files to have group permissions", [["echo $sudo | sudo -S chgrp gkb *"]]);

    $self->cmd("Backing up database",[["mysqldump -u$user -p$pass -h$gkcentral_host --lock_tables=FALSE $gkcentral > $gkcentral\_after_uniprot_update.dump"]]);

    my @args = ("-db", $gkcentral, "-host", $gkcentral_host, "-user", $user, "-pass", $pass);

    $self->cmd("Running GO obsolete update script",[["perl go_obo_update.pl @args > go.out 2> go.err"]]);
    $self->cmd("Running EC number update script",[["perl addEcNumber2Activity_update.pl @args > ec_number.out 2> ec_number.err"]]);

    foreach my $class ("GO_MolecularFunction", "GO_BiologicalProcess", "GO_CellularComponent", "PhysicalEntity", "CatalystActivity") {
        $self->cmd("Updating $class display names",[["perl updateDisplayName.pl @args -class $class > update_$class.out 2> update_$class.err"]]);
    }
};

# override 'run_commands' => sub {
# 	my ($self) = @_;
# 	$self->cmd("Backing up database",[["mysqldump -u$user -p$pass -h$gkcentral_host --lock_tables=FALSE $gkcentral > $gkcentral\_before_go_update.dump"]]);
#     $self->cmd("Running GO Update script",
#     	[
#  			["perl run_GO_Update.pl $version  > go.out 2> go.err"]
#     	]
#  	);
# 	$self->cmd("Backing up database",[["mysqldump -u$user -p$pass -h$gkcentral_host --lock_tables=FALSE $gkcentral > $gkcentral\_after_go_update.dump"]]);
# };

1;
