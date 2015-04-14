#!/usr/bin/perl -w
use common::sense;
use XML::Simple;

# A script to flatten the Pathway hierachy to link
# all sub-pathways to the top-level parent.  This
# script ignores non-pathway entities.
# Sheldon Mckay <sheldon.mckay@gmail.com>

my $parent;

while (my $species = <>) {
    chomp $species;

    say STDERR $species;

    $species =~ s/\s+/\+/;

    my $xml = "/tmp/$$.xml";
    system "curl http://localhost/ReactomeRESTfulAPI/RESTfulWS/pathwayHierarchy/$species > $xml";
    
    my $data = XMLin($xml);
    
    my $pathways = $data->{Pathway};
    if (ref $pathways ne 'ARRAY') {
	$pathways = $pathways->{Pathway};
    }
    my @pathways = @$pathways;;
    
    for my $pathway (@pathways) {
	$parent = $pathway->{dbId};
	recurse($pathway);
    }
    
}

sub recurse {
    my $p  = shift;
    my $id = $p->{dbId};
    my $children = $p->{Pathway};
    my $name = $p->{displayName};
    say "$parent\t$id\t$name";
   
    # recurse down all levels.  Are you scared? I am.
    my @children = get_kids($p);
    for (@children) {
	recurse($_);
    }
}

sub get_kids {
    my $p = shift;
    my $sub = $p->{'Pathway'} or return ();
    
    if (ref $sub eq 'ARRAY') {
	return @$sub;
    }
    elsif (ref $sub eq 'HASH') {
	my $kids = $sub->{Pathway};
	if ($kids) {
	    return get_kids($kids) if ref $kids eq 'HASH';
	    return @$kids if ref $kids eq 'ARRAY';
	}
    }
    
    return ();
}





