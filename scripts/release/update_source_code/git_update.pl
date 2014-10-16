#!/usr/local/bin/perl
use strict;
use warnings;

`git stash`;
`git pull`;
`git stash pop`;