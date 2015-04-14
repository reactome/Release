while (<>) {
    chomp;
    my @line = split;
    my $IP = shift @line;
    if ($IP = /(\d+[.-]\d+[.-]\d+[.+]\d+)/) {
	$IP = $1;
    }
    s/^\S+/$IP/;
    print "$_\n";
}
