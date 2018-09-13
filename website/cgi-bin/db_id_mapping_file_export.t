#!/usr/bin/perl
use strict;
use warnings;

use Test::More;

require_ok('db_id_mapping_file_export.cgi');
ok(1, "testing works");
is(get_reactome_version(), 65, "get Reactome version from content service");

done_testing();