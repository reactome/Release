#!/usr/bin/perl -w
use strict;
use common::sense;

while (<>) {
    chomp;
    say $_;
    system "./investigate_stable_ids.pl '$_'";
}
