package GKB::Release::Steps::BiomartUpdate;

use Log::Log4perl qw(get_logger);

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends 'GKB::Release::Step';

#has '+gkb' => ();
has '+passwords' => ( default => sub { ['mysql','sudo'] } );
has '+directory' => ( default => "$release/biomart_update" );
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

Log::Log4perl->init($log_conf);

override 'run_commands' => sub {
    my ($self, $gkbdir) = @_;
    
    my $remote_server = ($self->host eq $biomart_server) ? undef : $biomart_server;
    if (! $self->_biomart_installed($remote_server)) {
	cmd("Installing BioMart",[["./install_biomart.sh"]], {'ssh' => $remote_server});
    }
    
    my $biomart_release_dir = $self->directory;
    
    cmd("Updating biomart database",
        [
	    ["mysqldump --opt -u $user -p$pass $biomartdb > $biomart_release_dir/$biomartdb.dump.beforeupdate.$version"],
	    ["perl martify_reactome.pl -db $db -user $user -pass $pass -bdb $biomartdb -interactions > $biomart_release_dir/martify_reactome.out.$version"],
	    ["mysqldump --opt -u $user -p$pass $biomartdb > $biomart_release_dir/$biomartdb.afterupdate.$version"]
	],
	{'ssh' => $remote_server}
    );

    
    cmd("Importing new parameters into biomart database",[["perl update_reactome_mart.pl -sudo -biomart_version -0.7"]],{'ssh' => $remote_server});
};

sub _biomart_installed {
    my $self = shift;
    my $remote_server = shift;
    
    my $logger = get_logger(caller());
    
    my @mandatory_directories = map {"$gkbdev/BioMart/$_/"}("apache", "biomart-perl", "martj*");

    foreach my $dir (@mandatory_directories) {    
	$logger->debug("Checking if $dir exists");
	return 0 unless directory_exists($dir, $remote_server);
	$logger->debug("$dir exists"); 
    }
    return 1;
}

sub directory_exists {    
    my $dir = shift;
    my $remote_server = shift;

    my $logger = get_logger(caller());
        
    my $command = "ls -d $dir 2>/dev/null";
    my $result;
    unless ($remote_server) {
	$result = `$command`;
    } else {
	$result = `ssh $remote_server $command`;
    }
    return ($result ? 1 : 0);
}


1;
