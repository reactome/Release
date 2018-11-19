#!/usr/local/bin/perl -w 

use common::sense;

use lib "/usr/local/gkb/modules";

use GKB::WebUtils;
use GKB::Config;


my $wu = GKB::WebUtils->new_from_cgi();
my $dba = $wu->dba;

my @ids;
my $sth = $dba->prepare('SELECT DB_ID FROM DatabaseObject WHERE _class = "Pathway" ORDER BY DB_ID');
$sth->execute;
while (my $ary = $sth->fetchrow_arrayref) {
    push @ids, $ary->[0];
}

my $num = @ids;
say "Total of $num pathways";

for my $id (@ids) {
    print "processing pathway $id\n";
    my $basename = join('_', $PROJECT_NAME, $id);

    my $tarball = "/usr/local/gkb/website/html/img-tmp/$basename.tar.gz";
    next if -e $tarball && -M $tarball < 14; 

    $wu->create_protege_project_wo_orthologues($basename,[$id]);
}


