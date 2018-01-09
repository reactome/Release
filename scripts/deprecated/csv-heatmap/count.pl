while(<>) {
    chomp;
    my ($count) = split ';';
    $total += $count;
}

print $total;

