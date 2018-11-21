package GKB::Release::Steps::UniprotUpdate;

use GKB::Release::Utils;
use GKB::Release::Config;

use Moose;
extends qw/GKB::Release::Step/;

has '+gkb' => ( default => "gkbdev" );
has '+passwords' => ( default => sub { ['mysql'] } );
has '+directory' => ( default => "$release/uniprot_update" );
has '+mail' => ( default => sub { 
    my $self = shift;
    return {
        'to' => 'curation',
        'subject' => $self->name,
        'body' => "",
        'attachment' => $self->directory . "/uniprot.wiki"
    };
});

override 'run_commands' => sub {
    my ($self) = @_;

    #$self->cmd("Setting all UniProt files to have group permissions", [["echo $sudo | sudo -S chgrp gkb *"]]);
 
    $self->cmd("Backing up database and downloading necessary files",
          [
              ["mysqldump -u$user -p$pass -h$gkcentral_host --lock_tables=FALSE $gkcentral > $gkcentral.dump"],
              ["curl -Ss -o uniprot_sprot.xml.gz ftp://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/complete/uniprot_sprot.xml.gz"],
              ["curl -Ss -o uniprot-reviewed_no.list.gz \"https://www.uniprot.org/uniprot/?query=reviewed%3Ano&compress=yes&format=list\""]
          ]
    ); 

    my @args = ("-db", $gkcentral, "-host", $gkcentral_host, "-user", $user, "-pass", $pass);
    $self->cmd("Running uniprot perl script",[["perl uniprot_xml2sql_isoform.pl @args > uniprot.out 2> uniprot.err"]]);
};

1;

