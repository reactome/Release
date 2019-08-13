package GKB::DBAdaptor;

=head1 NAME

GKB::DBAdaptor -- Database adaptor for GKB

=head1 SYNOPSIS

=head1 DESCRIPTION

A Perl module for storing/retrieving GKB::Instance:s in/from a relational database
and also for creating this database in the first place.
This module has been created specifically to be used with MySQL and is hence unlikely
to work with other rdbms although one can indeed specify an alternative driver.

Provided that you have your ontology/schema as GKB::Ontology object you can create
the database like this:

my $dba = GKB::DBAdaptor->new
    (
     -user   => 'mysql_user',
     -host   => 'localhost', # host where mysqld is running
     -pass   => 'password for accessing mysql server',
     -ontology => $ontology # GKB::Ontology object
     );

my $db_name = 'your_database_name';

# Drop the database if it exists already
$dba->execute("DROP DATABASE IF EXISTS $db_name");
# Recreate it
$dba->execute("CREATE DATABASE IF NOT EXISTS $db_name");
# Select it
$dba->execute("USE $db_name");
# create tables and store ontology/schema
$dba->create_tables;

If you have created the database already and want to access it use following
construct:

my $dba = GKB::DBAdaptor->new
    (
     -dbname => 'your_database_name',
     -user   => 'mysql_user',
     -host   => 'localhost', # host where mysqld is running
     -pass   => 'password for accessing mysql server',
     );

By giving the database name you bypass the requirement for ontology which is assumed
to be stored in the database already and fetched from there.

If your database server is listening to another port than default 3306 give new also
"-PORT => 'port number'" as arguments.

Once you are a lucky owner of a GKB::DBAdaptor object you can start using it for
storing and fetching GKB::Instance:s.

To store an instance in db do:

    $dba->store($instance);

As a result of that the newly-stored instance now has database internal identifier
a.k.a db_id ($instance->db_id) which is a unique identifier for an instance within
the given database. Also, a flag is set on the isntance indicating that it is
newly stored ($instance->newly_stored).

If you suspect that the instance may have been stored already but are too lazy to
check it do:

    $dba->store_if_necessary($instance);

This method uses the "defining attributes" of a class to check 1st if there is anything
"identical" stored in the database already. For example, in the current GKB ontology
class 'Complex' has 'hasComponent' and 'compartment' attributes defined as "defining
attributes". This means that 2 complexes are considered identical if they have identical
components and compartment. If it so happens that something identical has been
stored already the instance is given db_id of the already-stored instance, however,
the newly_stored flag is not set.

There are quite a few methods for retrieving instances from database. As a general rule
all of those methods return reference to an array containing the matching instances
or being empty (in case no matching instances were found).

If you know the db_id you can fetch the instance like this:

    my $ar = $dba->fetch_instance_by_db_id($db_id);
    my $instance;
    if (@{$ar}) {
	$instance = $ar->[0];
    }

Or in a slightly less safe manner:

    my $instance = $dba->fetch_instance_by_db_id($db_id)->[0];

However, knowing the db_id is a luxury you have only after finding your way into
the database via some other route. Hence there are functions for finding and fetching
instances by class and/or attribute value.

For example, to fetch instances of class 'Complex' with attribute 'name' matching
(but not necessarily being identical to) strings 'origin recognition' or 'ORC', do:

    my $ar = $dba->fetch_instance(-CLASS => 'Complex',
				  -QUERY => [{-ATTRIBUTE => 'name',
					      -VALUE => ['origin recognition', 'ORC'],
					      -OPERATOR => 'REGEXP'
					      }]
				  );

The '-VALUE' can be a mysql (not Perl!) regular expression.
You can also omit '-CLASS' in which case all the classes with attribute 'name' will
be searched.
You can also omit '-ATTRIBUTE' in which case all the "sensible" attributes of all
classes will be searched.
If you omit '-QUERY' (but still define '-CLASS') you will get all the instances of
the given class, i.e.:

    $dba->fetch_instance(-CLASS => 'Event');

will return you all instances of class 'Event'.

What about less trivial queries. Say you want to fetch all PhysicalEntities which
localise in the nucleus and have name 'ORC'. The location of a PhysicalEntity is
indicated by 'compartment' attribute. However, the valid values for this attribute
are not strings but  GKB::Instance:s with class 'GenericPhysicalEntity' which is the
GKB equivalent of GO cellular_component. So you need to fetch the 'GenericPhysicalEntity'
with name 'nucleus' 1st:
    my $ar1 = $dba->fetch_instance(-CLASS => 'GenericPhysicalEntity',
				   -QUERY => [{-ATTRIBUTE => 'name',
					       -VALUE => ['nucleus']
					       }]
				   );
    @{$ar1} || $self->throw("No GenericPhysicalEntities with name 'nucleus'.");
    (scalar(@{$ar1}) > 1) && $self->throw("More than 1 GenericPhysicalEntity with name 'nucleus'.");

And then use this instance\'s db_id to query for PhysicalEntities:

    my $ar2 = $dba->fetch_instance(-CLASS => 'PhysicalEntity',
				   -QUERY => [{-ATTRIBUTE => 'name',
					       -VALUE => ['ORC']
					       },
					      {-ATTRIBUTE => 'compartment',
					       -VALUE => [$ar1->[0]->db_id]
					       }
					       ]
				   );

There is also a specialised function for doing this kind of "nested" queries called
fetch_instance_with_nested_query. The other way of doing the previous query would be:

    my $query1 = {-ATTRIBUTE => 'name', -VALUE => 'nucleus'};
    my $query2 = {-ATTRIBUTE => 'compartment', -VALUE => [$query1]);
    my $ar = $dba->fetch_instance_with_nested_query(-CLASS => 'PhysicalEntity',
						    -QUERY => [$query2,
							       {-ATTRIBUTE => 'name',
								-VALUE => 'ORC'}
							       ]
						    );
The important thing to note here is that fetch_instance_with_nested_query does not
allow you to omit '-ATTRIBUTE' or '-CLASS'.


By default all the instances are fetched without "reverse attributes". I know, this begs
for a bit of an explanation. Basically reverse attributes (or reverse attribute values rather)
of a given instance are instances which have the given instance as value of some attribute.
I try to explain it with an example:
Class 'Complex' has an attribute 'hasComponent' the values of which are instances of class
'PhysicalEntity'. Hence, PhysicalEntity has reverse attribute 'hasComponent' the values of
which are instances of Complex which contain this PhysicalEntity as a component.
A concrete example would Complex with name 'ORC'. One of the values of it\'s 'hasComponent'
attribute is PhysicalEntity with name 'Orc1'. Hence PhysicalEntities with name 'Orc1'
also has reverse attribute 'hasComponent' with value Complex having name 'ORC'.

Now, somehow I can see you, dear reader, shaking your head in disbelief and thinking that
wouldn\'t it be easier to have a "real" attribute with name 'componentOf' (or something
like that) ;).

There are several reasons for not doing so:
- "Over-crowding" certain instances: Take for example instances of class Organism. Every
Event and majority of PhysicalEntities have an attribute 'organism' the value of which
is an instance of this class (say, with name "Hs"). Hence, if we were to follow the route
of having "proper attributes" instead of reverse ones we should attach another attribute
(or set of them) to Organism for holding those values. So, every time you were to fetch
an Organism (say, for getting it\'s name or whatever) you would get back the requested
Organism instance with enormous amount of stuff that you don\'t really need and the
response would reach you more slowly. Orgasnism would by no means be the only class with
this kind of heavily loaded instances.
- More multiple-value attributes. Basically all of the reverse attributes would be multi-value
"real" attributes which means that there would be more (left) joins. Initially I indeed
wrote code which fetched attributes and reverse attributes on the "same trip to the
database" and it turned out that executing those sql queries was just prohibitively slow.
It was by far faster to run 2 (or more) sql queries (1st fetching the attributes and then
separately reverse attributes) than to have one super-query.
- Memory cycles. Having "2-way links" creates memory cycles in Perl. (Not that it always
matters.)

Anyway, having attributes and reverse attributes really matter when storing the instance:
reverse attributes are not stored, but that shouldn\'t really matter since value of the
reverse attribute of a given instance should have an attribute with the given instance as
an attribute value.

Also, from user point of view fetching instances with reverse attributes is not much
different from fetching instances wihout them: just give "-REFERER => 1" to
fetch_instance or fetch_instance_with_nested_query as another key-value pair, i.e.
to fetches all instances of class Complex with name 'ORC' (exact match) with reverse
attributes attached do:

    my $ar = $dba->fetch_instance(-CLASS => 'Complex',
				  -QUERY => [{-ATTRIBUTE => 'name',
					      -VALUE => ['ORC'],
					      }],
				  -REFERER => 1
				  );

If you forgot to ask for reverse attributes or did not get them (e.g. by doing
fetch_instance_by_db_id which does not attach reverse attributes) you can have them
added by doing:

    $dba->attach_reverse_attributes_to_instance($instance);

Just bear in mind that this creates cyclical references. If you want to steer clear from
them you can ask for instances which refer to the cuurent instance, i.e. have it as
a value of some attribute:

    my $ar = $dba->fetch_referer_by_instance($instance);



One peculiar feature of GKB::DBAdaptor is that it always comes with an attached
GKB::InstanceCache object which is accessible via instance_cache function.
Every instance that is fetched (either fully, i.e. inflated form or as a "shell")
is stored in cache. This means that (provided that you start off with an empty
cache) you can get all the instances "associated" with your query by just looking
into the cache:

    my $ar = $dba->fetch_instance_by_db_id($db_id);
    foreach my $instance ($dba->instance_cache->instances) {
	# skip the instance that was asked for
	($instance->db_id == $db_id) && next;
	# do something with each instance
	...
    }

DBAdaptor stores instances in cache with their db_id:s as keys. Hence you can get
all the db_id:s of cached instances by:

    foreach my $db_id ($dba->instance_cache->keys) {
	# do something
    }

You can also check if an instance with a given db_id has been cached already by
doing:

    my $instance = $dba->instance_cache->fetch($db_id);

This is especially handy when doing recursive fetching over all or limited attributes
and/or classes in order to find out if given 2 instances can be "linked" somehow.

    $dba->instance_cache->empty;
    my @attributes = ('input','output', 'hasComponent');
    my @classes = ('Event', 'PhysicalEntity');
    $dba->follow_attributes_and_reverse_attributes(-INSTANCE => $instance1,
						   -ATTRIBUTES => \@attributes,
						   -CLASSES => \@classes
						   );
    if($dba->instance_cache->fetch($instance2->db_id)) {
	print "Instances ".$instance1->class.":".$instance1->db_id. " and ".$instance2->class.":".$instance2->db_id.
	    " can be linked over attributes ".join(',',@attributes)." and classes".join(',',@classes)."\n";
	# Do something to delineate all (or just shortest or whatever) links and
	# do something useful with them.
	...
    }

If you just want to use the connection that this class provides to
MySQL, without all the ontology/Protege stuff, specify the no_ontology_flag
option in DBAdaptor->new:

my $dba = GKB::DBAdaptor->new
    (
     -user   => 'mysql_user',
     -host   => 'localhost', # host where mysqld is running
     -pass   => 'password for accessing mysql server',
     -no_ontology_flag => 1
     );

Most of the functionality of this class won't work anymore, but
you can still use the following vey handy subroutines:

drop_database
execute
prepare

=head1 SEE ALSO

=head1 AUTHOR

Imre Vastrik E<lt>vastrik@ebi.ac.ukE<gt>

Copyright (c) 2002 European Bioinformatics Institute and Cold Spring
Harbor Laboratory.

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

use vars qw(@ISA $AUTOLOAD %ok_field);
use strict;

use GKB::Instance;
use GKB::InstanceCache;
use GKB::Ontology;
use GKB::MatchingInstanceHandler;
use GKB::Utils;
use Storable qw(nfreeze thaw);
use Bio::Root::Root;
use DBI;
use Data::Dumper;
use GKB::Config;
#qw($NO_SCHEMA_VALIDITY_CHECK);

@ISA = qw(Bio::Root::Root);

## get/set methods to be handled by AUTOLOAD
for my $attr
    (qw(
	db_internal_id_type
	db_integer_type
	db_string_type
	db_float_type
	port
	host
	user
	db_handle
	driver
	ontology
	primary_key_type
	instance_cache
	db_name
	debug
	matching_instance_handler
	table_type
	) ) { $ok_field{$attr}++; }

## Table and column names for schema storage/retrieval. Have to be hard-wired.
my $SCHEMATABLE = 'Ontology';
my $SCHEMACOL = 'ontology';
my $CONFIG_TABLE = 'Parameters';

sub new {
    my($pkg, @args) = @_;
#    print join("\n", map {defined $_ ? $_ : 'undefined'} @args), "\n";
    my $self = bless {}, $pkg;
    my (
	$db,
	$host,
	$driver,
	$user,
	$password,
	$port,
	$int_id_type,
	$string_type,
	$integer_type,
	$ontology,
	$no_ontology_flag,
	$cache,
	$debug,
	$db_float_type,
	$do_not_index,
	$matching_instance_handler,
	$table_type
        ) = $self->_rearrange([qw(
				  DBNAME
				  HOST
				  DRIVER
				  USER
				  PASS
				  PORT
				  DB_INTERNAL_ID_TYPE
				  DB_STRING_TYPE
				  DB_INTEGER_TYPE
				  ONTOLOGY
				  NO_ONTOLOGY_FLAG
				  CACHE
				  DEBUG
				  DB_FLOAT_TYPE
				  DO_NOT_INDEX
				  MATCHING_INSTANCE_HANDLER
				  TABLE_TYPE
				  )],@args);
    $driver ||= 'mysql'; $self->driver($driver);
    $host ||= 'localhost'; $self->host($host);
    $port ||= 3306; $self->port($port);
    $self->db_integer_type($integer_type || 'INTEGER(10)');
    $self->db_string_type($string_type || 'TEXT');
    $self->db_float_type($db_float_type || 'DOUBLE');
    my $dsn = "DBI:$driver:host=$host;port=$port"; # Imre's original
    if ($db) {
	$dsn .= ";database=$db";
	$self->db_name($db);
    }

    my $dbh = eval {
        DBI->connect(
            $dsn,$user,$password,
            {
                'mysql_ssl' => 1,
                'mysql_enable_utf8' => 1,
                'RaiseError' => 1,
            }
        );
    };

    if ($@ || !$dbh) {
    	my $throw_string = "Could not connect to database ";
    	if (defined $db) {
    		$throw_string .= "$db";
    	} else {
    		$throw_string .= "undef";
    	}
    	$throw_string .= " user ";
     	if (defined $user) {
    		$throw_string .= "$user";
    	} else {
    		$throw_string .= "undef";
    	}
    	$throw_string .= " using [$dsn] as a locator.\n";
    	if (defined $@) {
    		$throw_string .= "$@\n";
    	}
    	$self->throw($throw_string);
    }
    $self->debug($debug);
    #$self->debug(1);
    $self->db_handle($dbh);
    if (!$no_ontology_flag) {
	    # If db name defined assume that it contains the schema. Otherwise need ontology (schema).
	if ($db) {
	    $self->fetch_schema;
	    $self->table_type($self->fetch_table_type($SCHEMATABLE));
	    $self->fetch_parameters;
	} else {
	    $ontology || $self->throw("Need ontology.");
	    $ontology && $self->ontology($ontology);
	    $self->table_type($table_type || 'MyISAM');
	}
	$self->db_internal_id_type('INTEGER(10) UNSIGNED');
	$self->primary_key_type('INTEGER(10) NOT NULL AUTO_INCREMENT PRIMARY KEY');
	$cache ||= GKB::InstanceCache->new(-DEBUG => $debug);
	$self->instance_cache($cache);
	if ($do_not_index && ref $do_not_index) {
	    @{$self->{'do_not_index'}->{@{$do_not_index}}} = @{$do_not_index};
	}
	$self->matching_instance_handler($matching_instance_handler || new GKB::MatchingInstanceHandler);
    }
    #$self->execute("SET NAMES 'utf8'");
    return $self;
}

sub db_instance_type {
    my $self = shift;
    return $self->db_internal_id_type;
}

### Function: prepare
# Wrapper around DBI's prepare
# Returns: statement handle
###
sub prepare{
   my ($self,$string) = @_;
   $self->debug && print "", (caller(0))[3], "\n";
#   $self->debug && print "$string\n";
   return $self->db_handle->prepare($string);
}

### Function: execute
# Wrapper around DBI's prepare and execute
# Returns: statement handle, return value
###
sub execute {
    my ($self, $statement, @values) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    $self->debug && print "$statement\n" . join("\n", @values). "\n";
    my ($sth,$res);
    if ($self->debug) {
		eval {
		    $sth = $self->prepare($statement);
		    $res = $sth->execute(@values);
		};
		if ($@) {
		    $self->throw("<PRE>\nQuery:\n$statement\n" . join("\n", @values). "\nresulted in:\n$@\n</PRE>\n");
		}
    } else {
		$sth = $self->prepare($statement);
		$res = $sth->execute(@values);
    }
##    $self->debug && print "mysql_insertid\t",$sth->{mysql_insertid}, "\n";
    return ($sth, $res);
}

sub instance_from_hash {
    my $self = shift;
    my ($href,$class,$id) = @_;
    unless ($href && ref($href) eq 'HASH' && $class && $id) {
	$self->throw("Usage: \$dba->instance_from_hash(\$hash_ref, \$class, \$db_id)");
    }
    $self->_instance_from_hash(@_);
}


sub _instance_from_hash {
    my ($self,$t_hr,$class,$id) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    my $instance = $self->instance_cache->fetch($id) ||
	$self->instance_cache->store($id,GKB::Instance->new
				     (
				      -ONTOLOGY => $self->ontology,
				      -CLASS => $class,
				      -ID => $id,
				      -DB_ID => $id,
				      -DBA => $self,
				      ));
    # Since ALL attributes have been fetched, set the values of empty attributes to "empty".
    # (undef will create empty attribute value)
    foreach my $k ($instance->list_valid_attributes) {
	$instance->attribute_value($k, ($t_hr->{'attribute'}->{$k}) ? @{$t_hr->{'attribute'}->{$k}} : undef);
    }
    undef %{$t_hr};
    return $instance;
}

sub AUTOLOAD {
    my $self = shift;
    my $attr = $AUTOLOAD;
    $attr =~ s/.*:://;
    return unless $attr =~ /[^A-Z]/;  # skip DESTROY and all-cap methods
    $self->throw("invalid attribute method: ->$attr()") unless $ok_field{$attr};
    $self->{$attr} = shift if @_;
    return $self->{$attr};
}

sub DESTROY {
   my ($self) = @_;
   $self->debug && print "", (caller(0))[3], "\n";
   $self->db_handle && $self->db_handle->disconnect;
   $self->db_handle(undef);
}

sub create_database {
    my ($self,$name) = @_;
    $name || $self->throw("Need name to create a database.");
    $self->db_name && $self->throw("This adaptor is already connected to database " . $self->db_name .
				   ". Use another instance of DBAdaptor to create another db.");
    $self->execute("CREATE DATABASE IF NOT EXISTS $name");
    $self->db_name($name);
    $self->execute("USE $name");
    $self->create_tables;
}

sub drop_database {
    my ($self) = @_;
    if ($self->db_name) {
	$self->execute("DROP DATABASE IF EXISTS " . $self->db_name);
	$self->db_name(undef);
    }
}

### Function: store_schema
# Creates a table for ontology, serializes (Storable::nfreeze)
# and stores it.
###
sub store_schema {
    my ($self) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    $self->ontology || $self->throw("Need ontology.");
    my $statement = "DROP TABLE IF EXISTS $SCHEMATABLE";
    $self->execute($statement);
    $statement = "CREATE TABLE IF NOT EXISTS $SCHEMATABLE ($SCHEMACOL LONGBLOB) ENGINE=" .
	$self->table_type;
    $self->execute($statement);
    my $serialized = nfreeze($self->ontology);
    $statement = "INSERT INTO $SCHEMATABLE($SCHEMACOL) VALUES(?)";
    $self->execute($statement,$serialized);
    $self->_store_schema;
}

### Function: _store_schema
# Stores the data model in a manner understandable by the Java API.
###
sub _store_schema {
    my $self = shift;
    my $pont_file_content = $self->ontology->pont_file_content;
    require IO::String;
    my $io = IO::String->new($pont_file_content);
    require GKB::ClipsAdaptor;
    my $c = GKB::ClipsAdaptor->new2(-FH => $io);
    my $s = $c->fetch_schema;
    $s->set_property_value('_timestamp',$self->ontology->timestamp);
    require GKB::SchemaAdaptor;
    my $sa = GKB::SchemaAdaptor->new($self->db_handle);
    $sa->store_schema($s);
    # To prevent conection to db being lost due to $sa gino out of scope.
    $sa->db_handle(undef);
}

### Function: fetch_schema
# Retrieves and "de-serializes" (Storable::thaw) ontology from db
###
sub fetch_schema {
    my ($self) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    my $statement = "SELECT $SCHEMACOL FROM $SCHEMATABLE";
    my ($sth,$res) = $self->execute($statement);
    if (my $ar = $sth->fetchall_arrayref) {
	my $ontology = thaw($ar->[-1]->[0]);
	$self->ontology($ontology);
    } else {
	$self->throw("Couldn't fetch schema from database.");
    }
}

### Function: create_tables
# Creates tables based on ontology.
###
sub create_tables {
    my ($self) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    $self->store_schema;
    foreach my $class ($self->ontology->list_classes) {
	$self->create_class_tables($class);
    }
    $self->create_config_table;
}

sub create_class_tables {
    my ($self,$class) = @_;
    my @tmp = $self->ontology->is_root_class($class) ?
	"$DB_ID_NAME " . $self->primary_key_type :
	"$DB_ID_NAME " . $self->db_internal_id_type . " PRIMARY KEY";
#    $tmp[0] .= " PRIMARY KEY";
    foreach my $attribute (sort {$a cmp $b} grep {$_ ne $DB_ID_NAME} $self->ontology->list_own_attributes($class)) {
	if ($self->ontology->is_multivalue_class_attribute($class,$attribute)) {
	    $self->create_multivalue_attribute_table($class,$attribute);
	} else {
	    # single value attribute
            push @tmp,@{$self->create_attribute_column_definitions($class,$attribute)};
	}
    }
    my $statement =
	"CREATE TABLE IF NOT EXISTS `$class` (\n  " .
	join(",\n  ", @tmp) . "\n)\nENGINE=" . $self->table_type . "\n";
#    $self->debug && print "$statement\n";
    $self->execute($statement);
}

sub create_attribute_column_definitions {
    my ($self,$class,$attribute) = @_;
    my $t = $self->ontology->class_attribute_type($class,$attribute);
    my $type = $self->ontology->class_attribute_db_col_type($class,$attribute) || eval "\$self->$t";
    my @tmp;
    push @tmp, "$attribute $type";
    if (lc($self->table_type) eq 'myisam') {
	if ($type =~ /TEXT/i) {
	    push @tmp, "FULLTEXT ($attribute)";
	} elsif ($type =~ /BLOB/i) {
	    # don't index
	} else {
	    push @tmp, "INDEX ($attribute)";
	    if ($type =~ /VARCHAR/i) {
		push @tmp, "FULLTEXT $ {attribute}_fulltext ($attribute)";
	    }
	}
    } else {
	if ($type =~ /TEXT/i) {
	    push @tmp, "INDEX ($attribute(10))";
	} elsif ($type =~ /BLOB/i) {
            # don't index
	} else {
	    push @tmp, "INDEX ($attribute)";
	}
    }
    if ($self->ontology->is_instance_type_class_attribute($class,$attribute)) {
	push @tmp, $attribute . '_class VARCHAR(64)';
    }
    return \@tmp;
}

sub create_multivalue_attribute_table {
    my ($self,$class,$attribute) = @_;
    my $t = $self->ontology->class_attribute_type($class,$attribute);
    my $type = $self->ontology->class_attribute_db_col_type($class,$attribute) || eval "\$self->$t";
    my $statement =
	"CREATE TABLE IF NOT EXISTS `$ {class}_2_$ {attribute}` (\n" .
	"  $DB_ID_NAME " . $self->db_internal_id_type . ",\n" .
	"  INDEX ($DB_ID_NAME),\n" .
	"  $ {attribute}_rank INTEGER(10) UNSIGNED,\n";
    my $ar = $self->create_attribute_column_definitions($class,$attribute);
    $statement .= join(",\n", @{$ar}) . "\n)\nENGINE=" . $self->table_type . "\n";
#    $self->debug && print "$statement\n";
    $self->execute($statement);
}

### Function: store
# Stores GKB::Instance in db and sets it's db_id (comes from mysql_insertid).
# Is recursive, i.e. if the instance has other instances as attributes
# it stores also these since their db_id is necessary to store the
# 1st instance.
# Also sets $instance->{'newly_stored'} which can be used to check if the
# instance was really stored or just given db_id from "identical" instance
# stored already.
# Arguments:
# 1) object instance, required
# 2) "boolean", optional, true value forces storage of instance even if its
#    DB_ID is set.
# 3) "boolean", optional, true value sets the $store_func to \&store_if_necessary
#    to allow storing only the current instance while storing_if_necessar
#    the value instances. Not necessary when called from GKB::DBAdaptor::store_if_necessary
# Returns: integer db internal id
###
sub store {
    my ($self,$i,$force_store,$use_store_if_necessary) = @_;
    $self->debug && print "<PRE>", join("\n", $i->extended_displayName, (caller(0))[3],$self->stack_trace_dump), "</PRE>\n";
    # This is to catch attribute instances of instances which do not happen to
    # have defining attributes.
    my $store_func = ($use_store_if_necessary || ((caller(1))[3] and ((caller(1))[3] eq 'GKB::DBAdaptor::store_if_necessary')))
	? \&store_if_necessary
	    : \&store;
    my $o = $self->ontology || $self->throw("Need ontology.");
    ($i && ref($i) && ($i->isa("GKB::Instance"))) ||
	$self->throw("Need instance, got '$i'!");
    $i->db_id && ! $force_store && return $i->db_id;
    my $class = $i->class;
    unless ($i->attribute_value('_class')->[0]) {
	$i->attribute_value('_class',$class);
    }
    if (! defined $i->attribute_value('_displayName')->[0]) {
	eval {
	    $i->displayName;
	}; $@ && $self->warn("Problems setting the displayName:\n$@");
    }
    my $db_id = $i->db_id;
#    print qq(<PRE>) . (caller(0))[3] . "\t" . $i->id_string . qq(</PRE>\n);
    $i->is_being_stored(1);
    my @later;
    foreach my $ancestor ($o->list_ancestors($class), $class) {
	my (@multiatts,@values);
	my $statement = "INSERT INTO $ancestor SET $DB_ID_NAME=?";
	push @values, ($db_id || undef);
	foreach my $attribute (grep {$_ ne $DB_ID_NAME} $o->list_own_attributes($ancestor)) {
	    if (@{$i->attribute_value($attribute)}) {
		if ($o->is_multivalue_class_attribute($ancestor,$attribute)) {
		    push @multiatts, $attribute;
		} else {
		    if ($o->is_instance_type_class_attribute($ancestor,$attribute)) {
			# This is to avoid problems with circular references in the instances
			if ($ancestor eq $o->root_class) {
			    push @later, $attribute;
			    next;
			}
			$statement .= ",$attribute=?";
			push @values, $self->$store_func($i->attribute_value($attribute)->[0]);
			$statement .= ",${attribute}_class=?";
			push @values, $i->attribute_value($attribute)->[0]->class;
		    } else {
			$statement .= ",$attribute=?";
			push @values, $i->attribute_value($attribute)->[0];
		    }
		}
	    }
	}
	my $sth = $self->prepare($statement);
	$self->debug && print "<PRE>", join("\n", $statement, map {defined $_ ? $_ : 'NULL'} @values), "</PRE>\n";
#	eval {
	my $res = $sth->execute(@values);
#	}; $@ && $self->throw("Got $@ when storing " . $i->extended_displayName);
	unless ($o->parents($ancestor)) {
	    $db_id = $sth->{mysql_insertid};
	    $i->db_id($db_id);
#	    $self->debug && print "$class\tdb_id\t$db_id\n";
	}
	foreach my $attribute (@multiatts) {
	    my $rank = 0;
	    foreach my $e (@{$i->attribute_value($attribute)}) {
		my ($statement,@values);
		if ($o->is_instance_type_class_attribute($ancestor,$attribute)) {
		    my $db_id3 = $self->$store_func($e);
		    $statement = "INSERT INTO $ {ancestor}_2_$attribute ($DB_ID_NAME,${attribute}_rank,$attribute,${attribute}_class) VALUES(?,?,?,?)";
		    @values = ($db_id,$rank,$db_id3,$e->class);
		} else {
		    defined $e || $self->warn($i->id . " has undefined attribute '$attribute'\n");
		    $statement = "INSERT INTO $ {ancestor}_2_$attribute ($DB_ID_NAME,${attribute}_rank,$attribute) VALUES(?,?,?)";
		    @values = ($db_id,$rank,$e);
		}
		my $sth = $self->prepare($statement);
		$self->debug && print "<PRE>", join("\n", $statement, @values), "</PRE>\n";
		my $res = $sth->execute(@values);
		$rank++;
	    }
	}
    }
    if (@later) {
	my $statement = "UPDATE " . $self->ontology->root_class . " SET ";
	my (@set,@values);
	foreach my $attribute (@later) {
	    push @set, "$attribute=?";
	    push @values, $self->$store_func($i->attribute_value($attribute)->[0]);
	    push @set, "${attribute}_class=?";
	    push @values, $i->attribute_value($attribute)->[0]->class;
	}
	$statement .= join(",",@set) . " WHERE $DB_ID_NAME=?";
	push @values, $db_id;
	my $sth = $self->prepare($statement);
	$self->debug && print "<PRE>", join("\n", $statement, @values), "</PRE>\n";
	my $res = $sth->execute(@values);
    }
    $i->is_being_stored(undef);
    $i->newly_stored(1);
    return $db_id;
}

sub _handle_sql_query_results {
    my ($self,$sth,$thing2get,$instructions,$incl_referer) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    my ($t_hr,$id,@out,$class,$hr);
#    $self->debug && print Data::Dumper->Dumpxs([$instructions], ["$instructions"]);
    my $tmp = shift @{$instructions};
    my $id_col = $tmp->{'id_col'};
    my $class_col = $tmp->{'class_col'};
    while (my $ar = $sth->fetchrow_arrayref) {
	if ($t_hr && ($id ne $ar->[$id_col])) {
	    # new instance
	    my $i = $self->_instance_from_hash($t_hr,$class,$id);
	    unless ($thing2get eq $class) {
		#inflate instance if it's not "full". Would happen if not fetching instances of a "leaf" class
		$self->inflate_instance($i,$incl_referer);
	    }
	    $i->inflated(1);
	    push @out, $i;
	}
	$class = $ar->[$class_col];
	$id = $ar->[$id_col];
	foreach my $instr (@{$instructions}) {
	    $t_hr = &{$instr->{'method'}}($self,$t_hr,$ar,$instr);
	}
    }
    if ($id) {
#	$self->debug && print Data::Dumper->Dumpxs([$t_hr], ["$t_hr"]);
	my $i = $self->_instance_from_hash($t_hr,$class,$id);
	if ($thing2get ne $class) {
	    #inflate instance if it's not "full". Would happen if not fetching instances of a "leaf" class
	    $self->inflate_instance($i,$incl_referer);
	} else {
	    $i->inflated(1);
	}
	push @out, $i;
    }
    return \@out;
}

sub _handle_ranked_instance_attribute {
    my ($self,$t_hr,$ar,$instr) = @_;
#    $self->debug && print "", (caller(0))[3], "\n";
    if (defined $ar->[$instr->{'class'}]) {
	$t_hr->{'attribute'}->{$instr->{'attribute'}}->[$ar->[$instr->{'rank'}]] =
	    $self->instance_cache->fetch($ar->[$instr->{'value'}]) ||
		$self->instance_cache->store($ar->[$instr->{'value'}], GKB::Instance->new
					      (
					       -ONTOLOGY => $self->ontology,
					       -CLASS => $ar->[$instr->{'class'}],
					       -DB_ID => $ar->[$instr->{'value'}],
					       -DBA => $self,
					       ));
    }
    return $t_hr;
}

sub _handle_single_instance_attribute {
    my ($self,$t_hr,$ar,$instr) = @_;
#    $self->debug && print "", (caller(0))[3], "\t",join("\t", $instr->{'attribute'},$instr->{'class'},$instr->{'value'}), "\n";
    if (defined $ar->[$instr->{'class'}]) {
	$t_hr->{'attribute'}->{$instr->{'attribute'}}->[0] =
	    $self->instance_cache->fetch($ar->[$instr->{'value'}]) ||
		$self->instance_cache->store($ar->[$instr->{'value'}], GKB::Instance->new
					      (
					       -ONTOLOGY => $self->ontology,
					       -CLASS => $ar->[$instr->{'class'}],
					       -DB_ID => $ar->[$instr->{'value'}],
					       -DBA => $self,
					       ));
    }
    return $t_hr;
}

sub _handle_ranked_value_attribute {
    my ($self,$t_hr,$ar,$instr) = @_;
#    $self->debug && print "", (caller(0))[3], "\n";
    if (defined $ar->[$instr->{'rank'}]) {
	$t_hr->{'attribute'}->{$instr->{'attribute'}}->[$ar->[$instr->{'rank'}]] = $ar->[$instr->{'value'}];
    }
    return $t_hr;
}

sub _handle_single_value_attribute {
    my ($self,$t_hr,$ar,$instr) = @_;
#    $self->debug && print "", (caller(0))[3], "\n";
    if (defined $ar->[$instr->{'value'}]) {
	$t_hr->{'attribute'}->{$instr->{'attribute'}}->[0] = $ar->[$instr->{'value'}];
    }
    return $t_hr;
}

### Function: inflate_instance
# Attaches attributes to a GKB::Instance which has only it's db_id and class set.
# These instances result when one fetches an instance which has other instances
# as attributes. Thoseother instances, by default, come with their class and internal
# id (db_id) (and ontology) only.
# Arguments:
# 1) object instance. Required. The thing that needs to be inflated
# 2) "Boolean" include reverse attributes. Optional. Use a true value if you want to attach reverse
# attributes  (i.e. instances which have this instance as an attribute to it) to the
# instance. Returns: object GKB::Instance(the very same gives as argument).
###
sub inflate_instance {
    my ($self,$instance,$incl_referer) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    ($instance && ref($instance) && ($instance->isa("GKB::Instance"))) ||
	$self->throw("Can't inflate '$instance'.");
    $instance->db_id || $self->throw("Can't inflate instance '$instance' without db_id.");
    my $thing2get = $instance->class;

    foreach my $ancestor ($self->ontology->list_ancestors($thing2get), $thing2get) {
	my @single_value_atts = $self->ontology->list_singlevalue_own_attributes($ancestor);
	if ($single_value_atts[0]) {
	    $self->load_attribute_values($instance,$single_value_atts[0]);
	}
	foreach my $att ($self->ontology->list_multivalue_own_attributes($ancestor)) {
	    $self->load_attribute_values($instance,$att);
	}
    }

    $incl_referer && $self->attach_reverse_attributes_to_instance($instance);
    $instance->inflated(1);
    return $instance;
}

### Function: fetch_referer_by_instance
# Fetches all instances which have the given instance as an attribute.
# Arguments:
# 1) object instance. Required.
# Returns: array reference. The array is empty if no instances were found.
###
sub fetch_referer_by_instance {
    my ($self,$instance) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    ($instance && ref($instance) && ($instance->isa("GKB::Instance"))) ||
	$self->throw("Need GK::Instance, got '$instance'.");
    $instance->db_id || $self->throw("Instance has to have db_id in order for you to be able to use this method.");
    my $hr = $self->_fetch_referer_by_instance($instance);
#    return [map {values %{$_}} values %{$hr}];
    my %seen;
    return [grep {!$seen{$_->db_id}++} map {values %{$_}} values %{$hr}];
}

sub _fetch_referer_by_instance {
    my ($self,$instance) = @_;
    $instance->db_id || $self->throw("Instance has to have db_id in order for you to be able to use this method.");
    $self->debug && print "", (caller(0))[3], "\n";
    my %th;
    foreach my $ar (@{$self->ontology->class_referers_by_attribute_origin($instance->class)}) {
	my ($thing2get,$bywhat) = @{$ar};
	$self->debug && print "Fetching $thing2get by $bywhat having value " . $instance->db_id . "\n";
	foreach my $r (@{$self->fetch_instance_by_attribute($thing2get,[[$bywhat,[$instance->db_id]]])}) {
	    $th{$bywhat}->{$r->db_id} = $r;
	}
    }
    return \%th;
}

### Function: attach_reverse_attributes_to_instance
# Attaches reverse attribute instances to the given instance.
# Reverse attribute instancres are instances which have the give instance
# as an attribute.
###
sub attach_reverse_attributes_to_instance {
    my ($self,$instance) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    $instance->reverse_attributes_attached && return;
    $instance->db_id || $self->throw("Instance has to have db_id in order for you to be able to use this method.");
    my $hr = $self->_fetch_referer_by_instance($instance);
    $instance->{'reverse_attribute'} = {};
    foreach my $k (keys %{$hr}){
	$instance->add_reverse_attribute_value($k, [values %{$hr->{$k}}]);
    }
    $instance->reverse_attributes_attached(1);
}

### Function: fetch_instance_by_db_id
# Fetches an instance by internal identifier (db_id)
# Arguments:
# 1) integer db internal id. Required.
# 2) string class. Optional. If you know the "final" class of the requested instance
# you'll save one trip to the database, i.e. the instance can be retrieved by one query.
# Otherwise the instance will be retrieved with 2 queries. Anyway, it's all happening
# under the bonnet.
# Returns: reference to an array (sic!). Array is empty if the instance was not found.
###
sub fetch_instance_by_db_id {
    my ($self,$db_id,$class) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    $class ||= $self->ontology->root_class;
    return $self->fetch_instance_by_attribute($class,[[$GKB::Ontology::DB_ID_NAME,[$db_id]]]);
}

### Fuction: fetch_instance_by_attribute
# Fetches instances by an attribute value.
# Arguments:
# 1) string class name. Required. Has to be a valid class name for current ontology.
# 2) arrayref query. The format is following:
# [[$attribute,[$value1, $value2],$is_regexp]].
#
#   Examples:
#   [['text', ['Cdc', 'Cdk'],1]]
#   fetches instances containing strings 'Cdc' OR 'Cdk' in their attribute 'text'.
#   [['name', ['Cdc2'],0],['name', ['Cdk1'],0]]
#   fetches instances with names 'Cdc2' AND 'Cdk1' (attribute 'name' can have multiple
#   values)
#
# 3) "Boolean" include reverse attributes. Optional. Use a true value if you want to attach reverse
# attributes  (i.e. instances which have this instance as an attribute to it) to the
# instance.
# Returns: reference to an array (sic!). Array is empty if the instance was not found.
###
sub fetch_instance_by_attribute {
    my ($self,$thing2get,$query,$incl_referer) = @_;
    $self->debug && print "==>", (caller(0))[3], "\n";
    $self->ontology->class($thing2get) || $self->throw("Don't know how to fetch '$thing2get'.");
    $query || $self->throw("Need query.");
    my @out;

    if ($query && (scalar(@{$query}) == 1) && (! $query->[0]->[2]) && ($query->[0]->[0] eq $DB_ID_NAME)) {
		my @tmp;
		foreach my $val (@{$query->[0]->[1]}) {
		    if (my $i = $self->instance_cache->fetch($val)) {
				$self->debug && print "Got from cache $i\n";
#				# inflate it if necessary
#				$i->inflated || $self->inflate_instance($i,$incl_referer);
				$i->{'reverse_attribute'} && !$incl_referer && delete($i->{'reverse_attribute'});
				push @out, $i;
				next;
		    }
		    push @tmp, $val;
		}
		if (@tmp) {
		    $query->[0]->[1] = \@tmp;
		} else {
		    # Single-value query, which was in cache already
		    return \@out;
		}
    }
    my ($sth,$instructions) = $self->_create_minimal_instancefetching_sql($thing2get,$query);

    my $ar = $self->_handle_sql_query_results2($sth,$instructions);

    if ($incl_referer) {
		foreach my $instance (@{$ar}) {
		    $self->attach_reverse_attributes_to_instance($instance);
		}
    }
    push @out, @{$ar};
    $self->debug && print "<==", (caller(0))[3], "\n";
    return \@out;
}

sub fetch_instance_by_subclass_attribute1 {
    my ($self,$thing2get,$query) = @_;
    $self->debug && print "==>", (caller(0))[3], "\n";
    my $o = $self->ontology;
    $o->class($thing2get) || $self->throw("Don't know how to fetch '$thing2get'.");
    $query || $self->throw("Need query.");
    my %h;
    foreach my $subq (@{$query}) {
	my $att = $subq->[0];
	map {$h{$_}++} grep {$o->is_own_attribute($_,$att)} ($o->list_classes_with_attribute($att,$thing2get));
    }
    my %subclasses;
    CLS: foreach my $cls (keys %h) {
	foreach my $subq (@{$query}) {
	    my $att = $subq->[0];
	    next CLS unless ($o->is_valid_class_attribute($cls,$att));
	}
	$subclasses{$cls} = 1;
    }
    my @out;
    foreach my $cls (keys %subclasses) {
	push @out, @{$self->fetch_instance_by_attribute($cls,$query)};
    }
    return \@out;
}

sub fetch_instance_by_subclass_attribute2 {
    my ($self,$thing2get,$query) = @_;
    $self->debug && print "==>", (caller(0))[3], "\t$thing2get\n", Dumper($query), "\n";
    my $o = $self->ontology;
    $o->class($thing2get) || $self->throw("Don't know how to fetch '$thing2get'.");
    $query || $self->throw("Need query.");
    my %h;
    foreach my $subq (@{$query}) {
#	print qq(<PRE>@{$subq}</PRE>\n);
	if ($subq->[3]) {
	    $h{$thing2get} = 1;
	} else {
	    my $att = $subq->[0];
	    map {$h{$_} = 1} ($o->list_classes_with_attribute($att,$thing2get));
	}
    }
#   print qq(<PRE>), join("\n", keys %h), qq(</PRE>\n);
    my %subclasses;
    CLS: foreach my $cls (keys %h) {
	foreach my $subq (@{$query}) {
	    unless ($subq->[3]) {
		my $att = $subq->[0];
		next CLS unless ($o->is_valid_class_attribute($cls,$att));
	    }
	}
	$subclasses{$cls} = 1;
    }
    %subclasses || return [];
#    print qq(<PRE>), join("\n", keys %subclasses), qq(</PRE>\n);
    my ($instructions,@union,@bindvalues);
    foreach my $cls (keys %subclasses) {
#	print qq(<PRE>$cls</PRE>\n);
	my ($sql,$values);
	($sql,$values,$instructions) = $self->_create_minimal_instancefetching_sql_bindvalues_and_instructions($cls,$query);
	push @union,$sql;
	push @bindvalues,@{$values};
    }
    my $stmt;
    if (@union > 1) {
	$stmt = '(' . join(') UNION (', @union) . ')';
    } else {
	$stmt = $union[0];
    }
    $stmt .= ' ORDER BY _displayName';
    my $sth = $self->prepare($stmt);
    $self->debug && print join("\n", $stmt, @bindvalues), "\n";
#    print "<PRE>\n", join("\n", $stmt, @{$values}), "\n</PRE>\n";
    my $res = $sth->execute(@bindvalues);
    return $self->_handle_sql_query_results2($sth,$instructions);
}

sub fetch_instance_by_subclass_attribute {
    my ($self,$thing2get,$query) = @_;
    $self->debug && print "==>", (caller(0))[3], "\t$thing2get\n", Dumper($query), "\n";
    my $o = $self->ontology;
    $o->class($thing2get) || $self->throw("Don't know how to fetch '$thing2get'.");
    $query || $self->throw("Need query.");
    my (%h);
    foreach my $subq (@{$query}) {
#	print qq(<PRE>@{$subq}</PRE>\n);
	my $att = $subq->[0];
	if ($subq->[3]) {
	    map {$h{$_} = 1} ($o->list_classes_with_reverse_attribute($att,$thing2get));
	} else {
	    map {$h{$_} = 1} ($o->list_classes_with_attribute($att,$thing2get));
	}
    }
#   print qq(<PRE>), join("\n", keys %h), qq(</PRE>\n);
    my %subclasses;
    CLS: foreach my $cls (keys %h) {
	foreach my $subq (@{$query}) {
	    my $att = $subq->[0];
	    if ($subq->[3]) {
		next CLS unless ($o->is_valid_class_reverse_attribute($cls,$att));
	    } else {
		next CLS unless ($o->is_valid_class_attribute($cls,$att));
	    }
	}
	$subclasses{$cls} = 1;
    }
    %subclasses || return [];
#    print qq(<PRE>), join("\n", keys %subclasses), qq(</PRE>\n);
    my ($instructions,@union,@bindvalues);
    foreach my $cls (keys %subclasses) {
#	print qq(<PRE>$cls</PRE>\n);
	my ($sql,$values);
	($sql,$values,$instructions) = $self->_create_minimal_instancefetching_sql_bindvalues_and_instructions($cls,$query);
	push @union,$sql;
	push @bindvalues,@{$values};
    }
    my $stmt;
    if (@union > 1) {
	$stmt = '(' . join(') UNION (', @union) . ')';
    } else {
	$stmt = $union[0];
    }
    $stmt .= ' ORDER BY _displayName';
    my $sth = $self->prepare($stmt);
    $self->debug && print join("\n", $stmt, @bindvalues), "\n";
#    print "<PRE>\n", join("\n", $stmt, @{$values}), "\n</PRE>\n";
    my $res = $sth->execute(@bindvalues);
    return $self->_handle_sql_query_results2($sth,$instructions);
}

sub fetch_instance_DB_ID_by_subclass_attribute {
    my ($self,$thing2get,$query) = @_;
    my $ar = $self->fetch_instance_by_subclass_attribute($thing2get,$query);
    my @out;
    map {push @out, $_->db_id} @{$ar};
    return \@out;
}

sub fetch_instance_DB_ID_by_attribute {
    my ($self,$thing2get,$query) = @_;
#    $self->debug && print $self->stack_trace_dump;
    $self->debug && print "==>", (caller(0))[3], "\n";
    $self->ontology->class($thing2get) || $self->throw("Don't know how to fetch '$thing2get'.");
    $query || $self->throw("Need query.");
    my (@select,@instructions);
    my ($from,$where,$join,$values) = $self->_from_where_join_values($thing2get,$query);
    my $root_class = $self->ontology->root_class;
    push @select, "$root_class.$DB_ID_NAME";
    my $statement = "SELECT DISTINCT " . join(",\n",@select) . "\nFROM " . join(",\n",@{$from});
    $statement .= "\n" . join("\n",@{$join}) if (@{$join});
    $statement .= "\nWHERE " . join("\nAND ",@{$where}) if (@{$where});
    $statement .= "\nORDER BY $root_class.$DB_ID_NAME";
    my $sth = $self->prepare($statement);
    $self->debug && print join("\n", $statement, @{$values}), "\n";
#    print "<PRE>\n", join("\n", $statement, @{$values}), "\n</PRE>\n";
    my $res = $sth->execute(@{$values});
    my @out;
    while (my $ar = $sth->fetchrow_arrayref) {
	push @out, $ar->[0];
    }
    $self->debug && print "<==", (caller(0))[3], "\n";
    return \@out;
}

sub fetch_instance_by_attribute_simple {
    return fetch_instance_by_attribute(@_);
}

### Function: fetch_instance
# Fetches instances from db. Expects arguments as -KEY => VALUE pairs.
# -CLASS => string class. Class has to be valid class for current ontology.
# -QUERY => arrayref query. The format is following:
# [[$attribute,[$value1, $value2],$is_regexp]].
#   Examples:
#   [['text', ['Cdc', 'Cdk'],1]]
#   fetches instances containing strings 'Cdc' OR 'Cdk' in their attribute 'text'.
#   [['name', ['Cdc2'],0],['name', ['Cdk1'],0]]
#   fetches instances with names 'Cdc2' AND 'Cdk1' (attribute 'name' can have multiple
#   values)
# If not set, retrieves all the instances of the given class.
# NOTE: at least one of -QUERY and -CLASS has to be set.
# -DEPTH => integer depth. Optional. Recursion depth, how far to follow attribute
# instances. Default is 0 , i.e. only the instance of interest is fetched.
# Negative number means that the recursion goes as deep as it can, i.e. everything
# "linked" to the instance of interest is fetched.
# -REFERER => "Boolean" include reverse attributes. If set to "true" value attaches
# reverse attributes to the instance(s) (including to the "attribute isntances").
# Probably not a good idea to use in conjunction with  negative depth because it will
# retrieve everything from the db unless that is what you indeed want to do.
# Returns: reference to an array (sic!). Array is empty if the instance was not found.
###
sub fetch_instance {
    my ($self,@args) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    my ($class,$query,$incl_referer,$depth) =
	$self->_rearrange([qw(
			      CLASS
			      QUERY
			      REFERER
			      DEPTH
			      )],@args);
    ($query || $class) || $self->throw("Need at least either -QUERY or -CLASS.");
    if ($query && ref($query) && ($query->[0] =~/HASH/)) {
	$query = $self->_transform_query($query);
    }
    my $ar;
    if ($class && ! $query) {
	$ar = $self->fetch_all_class_instances_as_shells($class);
    } elsif ($query && (! $class || ! $query->[0]->[0])) {
	$ar = $self->fetch_instance_wo_class_or_attribute($class,$query,$incl_referer,$incl_referer);
    } else {
	$ar = $self->fetch_instance_by_attribute($class,$query,$incl_referer);
    }

    if ($depth) {
	# not checking for definedness here because if $depth == 0 we don't need to get here
	# anyway
 	$depth = undef if ($depth < 0);
	foreach my $instance (@{$ar}) {
	    $self->follow_attributes_and_reverse_attributes(-INSTANCE => $instance,
							    -DEPTH => $depth,
							    -REFERER => $incl_referer);
	}
    }
    return $ar;
}

sub _transform_query {
    my ($self,$query) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
#    print Data::Dumper->Dumpxs([$query], ["$query"]);
    my @tmp;
    foreach my $hr (@{$query}) {
	my ($att,$val,$operator) =
	    $self->_rearrange([qw(
				  ATTRIBUTE
				  VALUE
				  OPERATOR
				  )],%{$hr});
	ref($val) || $self->throw("'-VALUE' has to be array ref, got '$val'");
	if (ref($val) && ($val->[0] =~ /HASH/)) {
	    $val = $self->_transform_query($val);
	}
	push @tmp, [$att,$val,$operator];
    }
    return \@tmp;
}

### Function: follow_attributes_and_reverse_attributes
# Recursively fetches and attaches attribute instances and reverse attribute
# instances to a given instance. Expects arguments as -KEY => VALUE pairs.
# -INSTANCE => object instance. Required. The instance to be used as
# the starting point.
# -DEPTH => integer depth. Recursion depth. By default goes as far as it can
# get.
# -ATTRIBUTES => arrayref attribute list. Optional.
# List of attributes to follow. By default follows all the attributes.
# -CLASSES => arrayref class list. Optional.
# List of classes to follow. By default follows all the classes.
# -OUT_CLASSES => arrayref class list. Optional.
# List of classes the instances of which you want to be returned.
# Returns: 2 array refs:
# 1. real terminal instances, i.e. those which do not have "right" values for
# listed attributes.
# 2. instances which were the stopping point due to reaching the depth limit.
###
sub follow_attributes_and_reverse_attributes {
    my ($self,@args) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    my ($instance,$attributes,$max_depth,$classes,$out_classes) =
	$self->_rearrange([qw(
			     INSTANCE
			     ATTRIBUTES
			     DEPTH
			     CLASSES
			     OUT_CLASSES
			     )],@args);
    ($instance && ref($instance) && $instance->isa("GKB::Instance")) ||
	$self->throw("Need GKB::Instance, got '$instance'.");
    if ($attributes) {
	(ref($attributes) && ($attributes =~ /ARRAY/)) ||
	    $self->throw("Need a reference to an array containing attributes, got '$attributes'.");
    }
    my %classes;
    if ($classes) {
	(ref($classes) && ($classes =~ /ARRAY/)) ||
	    $self->throw("Need a reference to an array containing classes, got '$classes'.");
	@classes{@{$classes}} = @{$classes};
	foreach my $cls (@{$classes}) {
	    @classes{$self->ontology->descendants($cls)} = $self->ontology->descendants($cls);
	}
    }
    $instance->inflated || $self->inflate_instance($instance);
    $instance->reverse_attributes_attached || $self->attach_reverse_attributes_to_instance($instance);
    my %seen;
    my @t = ($instance);
    my $cur_depth = 0;
    my @out;
    while ((@t > 0) && ((! defined $max_depth) || ($cur_depth < $max_depth))) {
	my @instances = @t;
	@t = ();
	$cur_depth++;
	$self->debug && print "Fetching attributes at depth $cur_depth\n";
	while (my $i1 = shift @instances) {
	    my $flag;
	    my @attributes = ($attributes) ? @{$attributes} : $i1->list_set_attributes;
	    foreach my $att (@attributes) {
		if ($i1->is_valid_attribute($att)) {
		    foreach my $i2 (@{$i1->attribute_value($att)}) {
			(ref($i2) && $i2->isa("GKB::Instance")) || next;
			%classes && ($classes{$i2->class} || next);
			$self->debug && print join(" ",("Fetching:",$i1->class,$i1->db_id,$att,$i2->class,$i2->db_id)), "\n";
			next if $seen{$i2->db_id}++;
			$flag = 1;
			#$i2->inflated || $self->inflate_instance($i2,1);
			#$i2->reverse_attributes_attached || $self->attach_reverse_attributes_to_instance($i2);
			push @t, $i2;
		    }
		}
	    }
	    @attributes = ($attributes) ? @{$attributes} : $i1->list_reverse_attributes;
	    foreach my $att (@attributes) {
		if ($i1->is_valid_reverse_attribute($att)) {
		    foreach my $i2 (@{$i1->referer_value($att)}) {
			(ref($i2) && $i2->isa("GKB::Instance")) || next;
			%classes && $classes{$i2->class} || next;
			$self->debug && print join(" ",("Fetching:",$i1->class,$i1->db_id,$att,$i2->class,$i2->db_id)), "\n";
			next if $seen{$i2->db_id}++;
			$flag = 1;
			# iclude referers when inflating
			#($i2->inflated && $i2->reverse_attributes_attached) || $self->inflate_instance($i2,1);
			push @t, $i2;
		    }
		}
	    }
	    # store the "terminal" instance
	    $flag || push @out, $i1;
	}
    }
    # if @t contains anything we've reached the max depth.
    if ($out_classes) {
        return $self->_trim_output($out_classes, \@out, \@t, {});
    } else {
        return \@out, \@t;
    }
}

### Function: follow_attributes
# Recursively Fetches and attaches attribute instances to a given instance.
# Expects arguments as -KEY => VALUE pairs.
# -INSTANCE => object instance. Required. The instance to be used as
# the starting point.
# -DEPTH => integer depth. Recursion depth. By default goes as far as it can
# get.
# -ATTRIBUTES => arrayref attribute list. Optional.
# List of attributes to follow. By default follows all the attributes.
# -CLASSES => arrayref class list. Optional.
# List of classes to follow. By default follows all the classes.
# -REFERER => "boolean"  include reverse attributes.
# -OUT_CLASSES => arrayref class list. Optional.
# List of classes the instances of which you want to be returned.
# Returns: 2 array refs and a hash ref:
# 1. real terminal instances, i.e. those which do not have "right" values for
# listed attributes.
# 2. instances which were the stopping point due to reaching the depth limit.
# 3. Hash of instances (keyed by db_id) which were on the path.
###
sub follow_attributes {
    my ($self,@args) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    my ($instance,$attributes,$max_depth,$incl_referer,$classes,$out_classes) =
	$self->_rearrange([qw(
			     INSTANCE
			     ATTRIBUTES
			     DEPTH
			     REFERER
			     CLASSES
			     OUT_CLASSES
			     )],@args);
    ($instance && ref($instance) && $instance->isa("GKB::Instance")) ||
	$self->throw("Need GKB::Instance, got '$instance'.");
    if ($attributes) {
	(ref($attributes) && ($attributes =~ /ARRAY/)) ||
	    $self->throw("Need a reference to an array containing attributes, got '$attributes'.");
    }
    my %classes;
    if ($classes) {
	(ref($classes) && ($classes =~ /ARRAY/)) ||
	    $self->throw("Need a reference to an array containing classes, got '$classes'.");
	@classes{@{$classes}} = @{$classes};
	foreach my $cls (@{$classes}) {
	    @classes{$self->ontology->descendants($cls)} = $self->ontology->descendants($cls);
	}
    }
    $instance->inflated || $self->inflate_instance($instance);
    $incl_referer && !$instance->reverse_attributes_attached &&
	$self->attach_reverse_attributes_to_instance($instance);
    my %seen;
    my @t = ($instance);
    my $cur_depth = 0;
    my @out;
    my %follow_path;
    while ((@t > 0) && ((! defined $max_depth) || ($cur_depth < $max_depth))) {
	my @instances = @t;
	@t = ();
	$cur_depth++;
	$self->debug && print "Fetching attributes at depth $cur_depth\n";
	while (my $i1 = shift @instances) {
	    my @attributes = ($attributes) ? @{$attributes} : $i1->list_set_attributes;
	    my $flag;
	    foreach my $att (@attributes) {
		if ($i1->is_valid_attribute($att)) {
		    $i1->attribute_value($att) || do {
#			$Data::Dumper::Maxdepth = 3;
#			print Data::Dumper->Dumpxs([$i1], ["$i1"]);
			$self->throw("Got 'undef' when fetching attribute '$att' value on " . $i1->id_string);

		    };
		    foreach my $i2 (@{$i1->attribute_value($att)}) {
			(ref($i2) && $i2->isa("GKB::Instance")) || next;
			%classes && ($classes{$i2->class} || next);
			$self->debug && print join(" ",("Fetching:",$i1->class,$i1->db_id,$att,$i2->class,$i2->db_id)), "\n";
			$flag = 1;
			next if $seen{$i2->db_id}++;
			$i2->inflated || $self->inflate_instance($i2,$incl_referer);
			$incl_referer && ! $i2->reverse_attributes_attached && $self->attach_reverse_attributes_to_instance($i2);
			push @t, $i2;
		    }
		}
	    }
	    # store the "terminal" instance
	    $flag || push @out, $i1;
	    $follow_path{$i1->db_id} = $i1;
	}
    }
    # if @t contains anything we've reached the max depth.
    if ($out_classes) {
	return $self->_trim_output($out_classes, \@out, \@t, \%follow_path);
    } else {
	return \@out, \@t, \%follow_path;
    }
}

sub _trim_output {
    my ($self,$out_classes,$ar1,$ar2,$hr) = @_;
    my %out_classes;
    map {$out_classes{$_}++} map {$_, $self->ontology->descendants($_)} @{$out_classes};
    my @a1 = grep {$out_classes{$_->class}} @{$ar1};
    my @a2 = grep {$out_classes{$_->class}} @{$ar2};
    my %h;
    map {$h{$_->db_id} = $_} grep {$out_classes{$_->class}} values %{$hr};
    return \@a1, \@a2, \%h;
}

### Function: follow_reverse_attributes
# Recursively Fetches and attaches reverse attribute instances to a given
# instance. Expects arguments as -KEY => VALUE pairs.
# -INSTANCE => object instance. Required. The instance to be used as
# the starting point.
# -DEPTH => integer depth. Recursion depth. By default goes as far as it can
# get.
# -ATTRIBUTES => arrayref attribute list. Optional.
# List of attributes to follow. By default follows all the attributes.
# -CLASSES => arrayref class list. Optional.
# List of classes to follow. By default follows all the classes.
# -OUT_CLASSES => arrayref class list. Optional.
# List of classes the instances of which you want to be returned.
# Returns: 2 array refs:
# 1. real terminal instances, i.e. those which do not have "right" values for
# listed attributes.
# 2. instances which were the stopping point due to reaching the depth limit.
# 3. Hash of instances (keyed by db_id) which were on the path.
###
sub follow_reverse_attributes {
    my ($self,@args) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    my ($instance,$attributes,$max_depth,$classes,$out_classes) =
	$self->_rearrange([qw(
			      INSTANCE
			      ATTRIBUTES
			      DEPTH
			      CLASSES
			      OUT_CLASSES
			     )],@args);
    ($instance && ref($instance) && $instance->isa("GKB::Instance")) ||
	$self->throw("Need GKB::Instance, got '$instance'.");
    if ($attributes) {
	(ref($attributes) && ($attributes =~ /ARRAY/)) ||
	    $self->throw("Need a reference to an array containing attributes, got '$attributes'.");
    }
    my %classes;
    if ($classes) {
	(ref($classes) && ($classes =~ /ARRAY/)) ||
	    $self->throw("Need a reference to an array containing classes, got '$classes'.");
	@classes{@{$classes}} = @{$classes};
	foreach my $cls (@{$classes}) {
	    @classes{$self->ontology->descendants($cls)} = $self->ontology->descendants($cls);
	}
    }
    my %seen;
    my @t = ($instance);
    my $cur_depth = 0;
    my @out;
    my %follow_path;
    while ((@t > 0) && ((! defined $max_depth) || ($cur_depth < $max_depth))) {
	my @instances = @t;
	@t = ();
	$cur_depth++;
	$self->debug && print "Fetching referers at depth $cur_depth\n";
	while (my $i1 = shift @instances) {
	    my @attributes = ($attributes) ? @{$attributes} : $i1->list_valid_reverse_attributes;
	    my $flag;
	    $self->debug && print $i1->id_string, "\n";
	    foreach my $att (@attributes) {
		if ($i1->is_valid_reverse_attribute($att)) {
		    foreach my $i2 (@{$i1->reverse_attribute_value($att)}) {
			(ref($i2) && $i2->isa("GKB::Instance")) || next;
			%classes && ($classes{$i2->class} || next);
			$self->debug && print join(" ",("Fetching:",$i1->class,$i1->db_id,$att,$i2->class,$i2->db_id)), "\n";
			$flag = 1;
			next if $seen{$i2->db_id}++;
			push @t, $i2;
		    }
		}
	    }
	    # store the "terminal" instance
	    $flag || push @out, $i1;
	    $follow_path{$i1->db_id} = $i1;
	}
    }
#    map {print join("\t","<B>",$_->class,$_->db_id,$_->attribute_value('_displayName')->[0],"</B>"),"\n"} @out;
    # if @t contains anything we've reached the max depth.
    if ($out_classes) {
        return $self->_trim_output($out_classes, \@out, \@t, \%follow_path);
    } else {
        return \@out, \@t, \%follow_path;
    }
}


### Function: follow_reverse_attributes2
# Here for backward compatibiliy. Silly.
###
sub follow_reverse_attributes2 {
    my ($self,@args) = @_;
    return $self->follow_reverse_attributes(@args);
}


###Function: fetch_instance_wo_class_or_attribute
# Internal function really. Just use fetch_instance for the functionality.
###
sub fetch_instance_wo_class_or_attribute {
    my ($self,$class,$query,$incl_referer) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    my @out;
    foreach my $q (@{$query}) {
	if (! $q->[2] && (scalar(@{$q->[1]}) == scalar(grep {/^\d+$/} @{$q->[1]}))) {
	    $q->[0] = $GKB::Ontology::DB_ID_NAME;
	    push @out, @{$self->fetch_instance_by_attribute(($class || $self->ontology->root_class),[$q],$incl_referer)};
	}
#	@out && return \@out;
	foreach my $cls ($class || $self->ontology->list_classes) {
	    # If class is given we want to check over all it's attributes, if not, only over
	    # own attributes.
	    foreach my $attribute ($class ? $self->ontology->list_class_attributes($cls) :
				   $self->ontology->list_own_attributes($cls)) {
		if ($q->[0]) {
		    if ($q->[0] eq $attribute) {
			push @out, @{$self->fetch_instance_by_attribute($cls,[$q],$incl_referer)};
		    }
		    next;
		}
		$self->ontology->is_string_type_class_attribute($cls,$attribute) || next;
		# skip '_class' and '_protege_id'
		next if ($attribute =~ /^_/);
		push @out, @{$self->fetch_instance_by_attribute($cls,[[$attribute,$q->[1],$q->[2]]],$incl_referer)};
	    }
	}
    }
    my %seen;
    @out = grep {!$seen{$_->db_id}++} @out;
    return \@out;
}

### Function: store_if_necessary
# Checks if something "identical" to the given instance has been stored in db
# already. This is done querying for instances which have same "defining attribute"
# values. For example, for CanonicalMolecule the defining attributes are
# 'referenceDatabase' and 'identifier' (accession number/swissprot id). If an instance with
# given 'referenceDatabase' and 'identifier' values is found in the db the instance-to-be-
# stored is considered stored already. It is given db_id of the stored instance.
# NOTE: Throws if multiple instances matching the  instance-to-be-stored is found.
# Unlike store $instance->{'newly_stored'} does not get set and this can be used to check
# if the instance was really stored or just given db_id from "identical" instance
# stored already.
# Returns: integer db internal id
# NOTE: please do not change this function unless you really really know what you are doing.
###
sub store_if_necessary {
    my ($self,$i) = @_;
    return 0 unless $i;
    $self->debug && print "<PRE>", join("\n", $i->extended_displayName, (caller(0))[3],$self->stack_trace_dump), "</PRE>\n";
    (ref($i) && $i->isa("GKB::Instance")) || $self->throw("Need GKB::Instance, got '$i'.");
    $i->db_id && return $i->db_id;
    my @tmp;
    if (my @j = $self->ontology->list_class_defining_attributes($i->class)) {
	if (my $query = $self->_make_query_to_retrieve_identical_instance($i)) {
	    # Check if the instance has been stored meanwhile. This can happen due to circular references
	    $i->db_id && return $i->db_id;
	    my $ar = $self->_fetch_instance_with_defining_attributes_only($i->class,$query);
	    foreach my $i2 (@{$ar}) {
		push @tmp, $i2 if ($i->reasonably_identical($i2));
	    }
	} else {
	    $self->warn("No defining attribute values for instance " . $i->id_string . "!");
	}
	if (@tmp) {
	    $i->identical_instances_in_db(\@tmp);
	    $self->matching_instance_handler->handle_matching_instances($i,$self);
	} else {
	    # Not stored before. Store now.
	    $self->store($i);
	}
    } else {
	# Not stored before. Store now.
	$self->store($i);
    }
    return $i->db_id;
}

# Make a query which should fetch only the instances matching all the attribute
# values which need checking.
sub _make_query_to_retrieve_identical_instance {
    my ($self,$i) = @_;
    my $o = $self->ontology;
    $self->debug && print "", (caller(0))[3], "\n";
    $self->debug &&
	print join("\t",($i->class,($i->id || 'undef'),($i->db_id || 'undef')))," Checking 'all+any': ",
#	join(", ",(keys %{$o->class($i->class)->{'check'}->{'all'}},
#		   keys %{$o->class($i->class)->{'check'}->{'any'}})), "\n";
        join(", ",($o->list_class_defining_attributes($i->class))), "\n";
    my @query;
#    foreach my $attribute (keys %{$o->class($i->class)->{'check'}->{'all'}}) {
    foreach my $attribute ($o->list_class_attributes_with_defining_type($i->class,'all')) {
	my %seen;
	foreach my $val (@{$i->attribute_value($attribute)}) {
	    if ($o->class($i->class)->{'attribute'}->{$attribute}->{'type'} eq 'db_instance_type') {
		$val->db_id || $self->store_if_necessary($val);
		$val->db_id || $self->throw("Need db_id for instance '" . $val->extended_displayName . "'");
		next if $seen{$val->db_id}++;
		push @query, [$attribute, [$val->db_id]];
	    } else {
		next if $seen{$val}++;
		push @query, [$attribute, [$val]];
	    }
	}
    }

#    foreach my $attribute (keys %{$o->class($i->class)->{'check'}->{'any'}}) {
    foreach my $attribute ($o->list_class_attributes_with_defining_type($i->class,'any')) {
	my @values;
	my %seen;
	foreach my $val (@{$i->attribute_value($attribute)}) {
	    if ($o->class($i->class)->{'attribute'}->{$attribute}->{'type'} eq 'db_instance_type') {
		$val->db_id || $self->store_if_necessary($val);
		$val->db_id || $self->throw("Need db_id for instance '" . $val->extended_displayName . "'");
		next if $seen{$val->db_id}++;
		push @values, $val->db_id;
	    } else {
		next if $seen{$val}++;
		push @values, $val;
	    }
	}
	@values && push @query, [$attribute, \@values];
    }
    if (@query) {
	return \@query;
    } else {
	return undef;
    }
#    (@query) ? return \@query : return undef;
}

# This function just works in the context of store_if_necessary. The instances retrieved from
# the database have been retrieved on the basis of "defining attributes".
sub _are_reasonably_identical {
    my ($self,$i1,$i2) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    my $o = $self->ontology;
#    foreach my $attribute (keys %{$o->class($i1->class)->{'check'}->{'all'}}) {
    foreach my $attribute ($o->list_class_attributes_with_defining_type($i1->class,'all')) {
	# just check if the number of attribute values is same. Are there cases where we need to
	# check order?
	return unless (scalar(@{$i1->attribute_value($attribute)}) == scalar(@{$i2->attribute_value($attribute)}));
	# Also have to check the composition. For example, if you've 1st stored a heterodimer AB and
	# then try to store homodimer AA, the latter would "match" the former unless you check composition.
	my (%seen1,%seen2);
	if ($i1->is_instance_type_attribute($attribute)) {
	    map {$seen1{$_->db_id}++} @{$i1->attribute_value($attribute)};
	    map {$seen2{$_->db_id}++} @{$i2->attribute_value($attribute)};
	} else {
	    map {$seen1{$_}++} @{$i1->attribute_value($attribute)};
	    map {$seen2{$_}++} @{$i2->attribute_value($attribute)};
	}
	return unless (scalar(keys %seen1) == scalar(keys %seen2));
	# Then check the number of each component since AAAB is different from AABB
	map {($seen1{$_} == $seen2{$_}) || return } map {$seen2{$_} ? $_ : return } keys %seen1;
    }
    return 1;
}

### Function: fetch_instance_with_nested_query
# Now this is a really bastard function to explain...
# Fetches instances by attribute instances' attribute instances'...
# attribute instances' value. For example, to Complexes
# which have component (attribute 'hasComponent') which is derived
# (attribute 'canonicalMolecule') from swissprot id ORC1_HUMAN can be retrieved
# in following manner:
# my $q1 = {-ATTRIBUTE => 'identifier', -VALUE => ['ORC1_HUMAN']};
# my $q2 = {-ATTRIBUTE => 'canonicalMolecule', -VALUE => [$q1]};
# my $q3 = {-ATTRIBUTE => 'hasComponent', -VALUE => [$q2]};
# my $results_ref = $dba->fetch_instance_with_nested_query(-CLASS => 'Complex',
#				                           -QUERY => [$q3]);
# Neat, eh ? ;)
# What happens under the bonnet:
# - All classes which are allowed classes for attribute 'canonicalMolecule' and
# which have attribute 'identifier' are searched for 'ORC1_HUMAN' as 'identifier'.
# - All classes which are allowed classes for attribute 'hasComponent' and
# which have attribute 'canonicalMolecule' are searched for db_id(s) from the
# previous query as 'canonicalMolecule'.
# - Class 'Complex' is searched for db_id(s) from the previous query as 'hasComponent'.
# Returns: reference to an array (sic!). Array is empty if the instance was not found.
###
sub fetch_instance_with_nested_query {
    my ($self,@args) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
#    print Data::Dumper->Dumpxs([@args], ["@args"]);
    my ($class,$query,$incl_refer) =
	$self->_rearrange([qw(
			      CLASS
			      QUERY
			      REFERER
			      )],@args);

    ($query && ref($query)) || $self->throw("Need query.");
    $class || $self->throw("Need class.");
    if ($query && ref($query) && ($query->[0] =~/HASH/)) {
	$query = $self->_transform_query($query);
    }
    #print "$class\n", Data::Dumper->Dumpxs([$query], ["$query"]);
    my @tmp;
    foreach my $q (@{$query}) {
	my @db_ids;
	($q->[1] && ref($q->[1])) || $self->throw("'-VALUE' has to be array ref, got '$q'");
	if ($q->[1]->[0] && ref($q->[1]->[0])) {
	    # nested query
	    if (my $cls = $q->[3]) {
		push @db_ids,
		@{$self->fetch_instance_DB_ID_with_nested_query(-CLASS => $cls,
								-QUERY => $q->[1])};
	    } else {
		foreach my $cls2 ($self->ontology->list_classes_with_attribute($q->[0],$class)) {
		    foreach my $cls ($self->ontology->list_allowed_classes($cls2,$q->[0])) {
			$q->[1]->[0]->[0] || $self->throw("Need attribute");
			if ($q->[1]->[0]->[3]) {
			    push @db_ids, @{$self->fetch_instance_DB_ID_with_nested_query(-CLASS => $cls, -QUERY => $q->[1])};
			} elsif ($self->ontology->is_valid_class_attribute($cls,$q->[1]->[0]->[0])) {
			    push @db_ids,
			    @{$self->fetch_instance_DB_ID_with_nested_query(-CLASS => $cls,
									    -QUERY => $q->[1])};
			} else {
			    foreach my $dsc (grep {$self->ontology->is_valid_class_attribute($_,$q->[1]->[0]->[0])} $self->ontology->descendants($cls)) {
				push @db_ids, @{$self->fetch_instance_DB_ID_with_nested_query(-CLASS => $dsc, -QUERY => $q->[1])};
			    }
			}
		    }
		}
	    }
            if (@db_ids) {
		my @a = @{$q};
		$a[1] = \@db_ids;
		push @tmp, \@a;
	    }
	} else {
	    push @tmp, $q;
	}
    }
    if (@tmp == @{$query}) {
	return $self->fetch_instance_by_subclass_attribute($class,\@tmp);
    } else {
	# one or more of the nested queries produced empty result and hence the whole query should do so.
	return [];
    }
}

sub fetch_instance_DB_ID_with_nested_query {
    my ($self,@args) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
#    print Data::Dumper->Dumpxs([@args], ["@args"]);
    my ($class,$query) =
	$self->_rearrange([qw(
			      CLASS
			      QUERY
			      )],@args);

    ($query && ref($query)) || $self->throw("Need query.");
    $class || $self->throw("Need class.");
    if ($query && ref($query) && ($query->[0] =~/HASH/)) {
	$query = $self->_transform_query($query);
    }
    #print "$class\n", Data::Dumper->Dumpxs([$query], ["$query"]);
    my @tmp;
    foreach my $q (@{$query}) {
	my @db_ids;
	($q->[1] && ref($q->[1])) || $self->throw("'-VALUE' has to be array ref, got '$q'");
	if ($q->[1]->[0] && ref($q->[1]->[0])) {
	    # nested query
	    if (my $cls = $q->[3]) {
		push @db_ids,
		@{$self->fetch_instance_DB_ID_with_nested_query(-CLASS => $cls,
								-QUERY => $q->[1])};
	    } else {
		foreach my $cls ($self->ontology->list_allowed_classes($class,$q->[0])) {
		    $q->[1]->[0]->[0] || $self->throw("Need attribute");
		    if ($q->[1]->[0]->[3]) {
			push @db_ids, @{$self->fetch_instance_DB_ID_with_nested_query(-CLASS => $cls, -QUERY => $q->[1])};
		    } elsif ($self->ontology->is_valid_class_attribute($cls,$q->[1]->[0]->[0])) {
			push @db_ids,
			@{$self->fetch_instance_DB_ID_with_nested_query(-CLASS => $cls,
									-QUERY => $q->[1])};
		    } else {
			foreach my $dsc (grep {$self->ontology->is_valid_class_attribute($_,$q->[1]->[0]->[0])} $self->ontology->descendants($cls)) {
			    push @db_ids, @{$self->fetch_instance_DB_ID_with_nested_query(-CLASS => $dsc, -QUERY => $q->[1])};
			}
		    }
		}
	    }
            if (@db_ids) {
		my @a = @{$q};
		$a[1] = \@db_ids;
		push @tmp, \@a;
	    }
	} else {
	    push @tmp, $q;
	}
    }
    if (@tmp == @{$query}) {
#	return $self->fetch_instance_DB_ID_by_subclass_attribute($class,\@tmp);
	return $self->fetch_instance_DB_ID_by_attribute($class,\@tmp);
    } else {
	# one or more of the nested queries produced empty result and hence the whole query should do so.
	return [];
    }
}

### Funtion: fetch_instance_by_remote_attribute
# Example:
# fetch_instance_by_remote_attribute('Reaction',
#                                    [
#                                     ['input.referenceEntity.identifier','=',['P12345','P12346']]
#                                    ]);
# Fetches all reactions with input PhysicalEntity with referenceEntity with identifier
# P12345 or P12346.
# Can also do:
# fetch_instance_by_remote_attribute('DatabaseObject',
#                                    [
#                                     ['input.referenceEntity.identifier','=',['P12345','P12346']]
#                                    ]);
# This will fetch all instances of classes with attribute 'input' and matching the rest.
# If the query is impossible, i.e. no classes with specified criteria can be found, reference to an
# empty array is returned.
###
sub fetch_instance_by_remote_attribute {
    my ($self,$class,$query) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    (ref($query) eq 'ARRAY') || $self->throw("Need an array reference, got '$query'.");
    my @translated_query;
    foreach my $ar (@{$query}) {
	push @translated_query, &GKB::Utils::translate_query_chain(@{$ar});
    }
    return $self->fetch_instance_with_nested_query(-CLASS => $class, -QUERY => \@translated_query);
}

sub _fetch_instance_with_defining_attributes_only {
    my ($self,$thing2get,$query) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    $self->ontology->class($thing2get) || $self->throw("Don't know how to fetch '$thing2get'.");
    my %hash;
    my $query_chunk = 4;
    my $max;
#    $self->debug(1);
    while (@{$query}) {
	$max++;
	my @tmp = splice(@{$query}, 0, $query_chunk);
	my ($sth,$instructions) =
	    $self->_create_minimal_instancefetching_sql($thing2get,\@tmp);
	my $ar = $self->_handle_sql_query_results2($sth,$instructions);
	# if a "subquery" did not fetch anything it means that also the whole query can't possibly
	# retrieve anything. Hence return reference to an empty array.
	@{$ar} || return $ar;
	foreach (@{$ar}) {
	    $hash{$_->db_id}++;
	}
    }
    my @out = map{$self->instance_cache->fetch($_)} grep{$hash{$_} == $max} keys(%hash);
#    $self->debug(undef);
    return \@out;
}

sub _create_minimal_instancefetching_sql_bindvalues_and_instructions {
    my ($self,$thing2get,$query) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    $self->ontology->class($thing2get) || $self->throw("Don't know how to fetch '$thing2get'.");
    my (@select,@instructions);
    my ($from,$where,$join,$values) = $self->_from_where_join_values($thing2get,$query);
    my $root_class = $self->ontology->root_class;
    push @select, "$root_class.$DB_ID_NAME";
    push @select, "$root_class._class";
    push @instructions, {'id_col' => 0, 'class_col' => 1};

    push @select, "$root_class._displayName";
    push @instructions, {'method'=> \&_handle_single_value_attribute,
			 'attribute' => '_displayName',
			 'value' => $#select};

    my $statement = "SELECT DISTINCT " . join(",\n",@select) . "\nFROM " . join(",\n",@{$from});
    $statement .= "\n" . join("\n",@{$join}) if (@{$join});
    $statement .= "\nWHERE " . join("\nAND ",@{$where}) if (@{$where});
    return ($statement,$values,\@instructions);
}

sub _create_minimal_instancefetching_sql {
    my ($self,$thing2get,$query) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    $self->ontology->class($thing2get) || $self->throw("Don't know how to fetch '$thing2get'.");
    my (@select,@instructions);
    my ($from,$where,$join,$values) = $self->_from_where_join_values($thing2get,$query);
    my $root_class = $self->ontology->root_class;
    push @select, "$root_class.$DB_ID_NAME";
    push @select, "$root_class._class";
    push @instructions, {'id_col' => 0, 'class_col' => 1};

    push @select, "$root_class._displayName";
    push @instructions, {'method'=> \&_handle_single_value_attribute,
			 'attribute' => '_displayName',
			 'value' => $#select};

#    my $statement = "SELECT DISTINCT " . join(",\n",@select) . "\nFROM " . join(",\n",@{$from}); # Works with MySQL 4 only
    my $statement = "SELECT DISTINCT " . join(",\n",@select) . "\nFROM (" . join(",\n",@{$from}) . ")"; # MySQL 5 compliant
    $statement .= "\n" . join("\n",@{$join}) if (@{$join});
    $statement .= "\nWHERE " . join("\nAND ",@{$where}) if (@{$where});
    $statement .= "\nORDER BY $root_class.$DB_ID_NAME";

    my $sth = $self->prepare($statement);

    $self->debug && print join("\n", $statement, @{$values}), "\n";
#    print "<PRE>\n", join("\n", $statement, @{$values}), "\n</PRE>\n";
    my $res = $sth->execute(@{$values});

    return ($sth,\@instructions);
}

sub fetch_DB_ID_class_displayName {
    my ($self,$thing2get,$query,$orderByDisplayName) = @_;
    my ($statement,$bindvalues) = $self->_create_DB_ID_class_displayName_fetching_sql
	($thing2get,$query,$orderByDisplayName);
    my $sth = $self->prepare($statement);
    my $res = $sth->execute(@{$bindvalues});
    return $sth;
}

sub _create_DB_ID_class_displayName_fetching_sql {
    my ($self,$thing2get,$query,$orderByDisplayName) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    $self->ontology->class($thing2get) || $self->throw("Don't know how to fetch '$thing2get'.");
    my (@select);
    my ($from,$where,$join,$bindvalues) = $self->_from_where_join_values($thing2get,$query);
    my $root_class = $self->ontology->root_class;
    push @select, "$root_class.$DB_ID_NAME";
    push @select, "$root_class._class";
    push @select, "$root_class._displayName";
    my $statement = "SELECT DISTINCT " . join(",\n",@select) . "\nFROM " . join(",\n",@{$from});
    $statement .= "\n" . join("\n",@{$join}) if (@{$join});
    $statement .= "\nWHERE " . join("\nAND ",@{$where}) if (@{$where});
    if ($orderByDisplayName) {
	$statement .= "\nORDER BY $root_class.$DB_ID_NAME";
    }
    return ($statement,$bindvalues);
}

### Function: update
# Overwrites an instance in database.
###
sub update {
    my ($self,$i) = @_;
    $self->debug && print "<PRE>", join("\n", $i->extended_displayName, (caller(0))[3],$self->stack_trace_dump), "</PRE>\n";
    my $o = $self->ontology || $self->throw("Need ontology.");
#    ($i && ref($i) && ($i =~ /Instance/)) || $self->throw("Need instance, got '$i'!");
    ($i && ref($i) && $i->isa("GKB::Instance")) || $self->throw("Need instance, got '$i'!");
    $i->inflated || $self->throw("Instance not inflated. Forgot to set the flag?");
    my $db_id = $i->db_id || $self->throw("Instance has to have db_id for updating.");
    my $class = $i->class;
    my $old_class = $self->fetch_single_attribute_value_by_db_id($db_id,$o->root_class,'_class')->[0]->[0];

    unless ($old_class) {
	warn "$class instance $db_id has no old class.  Did you call update on a new instance?";
	$old_class ||= $class;
    }

    $self->delete_by_db_id($i->db_id);
    $self->store($i,1,1);
    unless ($class eq $old_class) {
	$self->debug && print "<PRE>$class != $old_class</PRE>\n";
	my %seen;
	foreach my $ar (@{$self->ontology->class_referers_by_attribute_origin($class)}) {
	    my ($cls,$att,$is_multivalue_att) = @{$ar};
	    $seen{$att}->{$cls}++;
	    my $table_name;
	    if ($is_multivalue_att) {
		$table_name = "$ {cls}_2_$att";
	    } else {
		$table_name = $cls;
	    }
	    my $statement = qq(UPDATE $table_name SET $ {att}_class=? WHERE $att=?);
	    my $sth = $self->prepare($statement);
	    $self->debug && print "<PRE>", join("\n", $statement, $class, $i->db_id), "</PRE>\n";
	    $sth->execute($class,$i->db_id);
	}
	foreach my $ar (@{$self->ontology->class_referers_by_attribute_origin($old_class)}) {
	    my ($cls,$att,$is_multivalue_att) = @{$ar};
	    next if $seen{$att}->{$cls};
	    my $table_name;
	    if ($is_multivalue_att) {
		$table_name = "$ {cls}_2_$att";
	    } else {
		$table_name = $cls;
	    }
	    my $statement = qq(UPDATE $table_name SET $ {att}_class=? WHERE $att=?);
	    my $sth = $self->prepare($statement);
	    $self->debug && print "<PRE>", join("\n", $statement, $class, $i->db_id), "</PRE>\n";
	    $sth->execute($class,$i->db_id);
	}
    }
    return $i->db_id;
}

# Method for updateing single attribute values. Uses 'UPDATE' rather than 'DELETE' and 'INSERT'.
sub update_attribute {
    my ($self,$i,$attribute) = @_;
    $self->debug && print "<PRE>", join("\n", $i->extended_displayName, (caller(0))[3],$self->stack_trace_dump), "</PRE>\n";
    $i || $self->throw("Need instance");
    $attribute|| $self->throw("Need attribute");
    $i->is_valid_attribute($attribute) || $self->throw("Not a valid attribute ('$attribute') for this class.");
    my $db_id = $i->db_id || $self->throw("Instance has to have db_id for updating.");
    my $db_id_name = $GKB::Ontology::DB_ID_NAME;
    my $origin = $i->attribute_origin($attribute);
    if ($i->is_multivalue_attribute($attribute)) {
	my $delete_statement = "DELETE FROM $ {origin}_2_$attribute WHERE $db_id_name=$db_id";
	$self->debug && print "<PRE>$delete_statement</PRE>\n";
	$self->execute($delete_statement);
	my $rank = 0;
	foreach my $e (@{$i->attribute_value($attribute)}) {
	    my ($statement,@values);
	    if ($i->is_instance_type_attribute($attribute)) {
		my $db_id3 = $self->store_if_necessary($e);
		$statement = "INSERT INTO $ {origin}_2_$attribute ($db_id_name,${attribute}_rank,$attribute,${attribute}_class) VALUES(?,?,?,?)";
		@values = ($db_id,$rank,$db_id3,$e->class);
	    } else {
		defined $e || $self->warn($i->id . " has undefined attribute '$attribute'\n");
		$statement = "INSERT INTO $ {origin}_2_$attribute ($db_id_name,${attribute}_rank,$attribute) VALUES(?,?,?)";
		@values = ($db_id,$rank,$e);
	    }
	    my $sth = $self->prepare($statement);
	    $self->debug && print "<PRE>", join("\n", $statement, map {defined $_ ? $_ : 'undef'} @values), "</PRE>\n";
	    my $res = $sth->execute(@values);
	    $rank++;
	}
    } else {
	my $statement = "UPDATE $origin SET $attribute=?";
	my @values;
	if ($i->is_instance_type_attribute($attribute)) {
	    push @values, $self->store_if_necessary($i->attribute_value($attribute)->[0]);
	    $statement .= ",${attribute}_class=?";
	    push @values, ($i->attribute_value($attribute)->[0]) ? $i->attribute_value($attribute)->[0]->class : undef;
	} else {
	    push @values, $i->attribute_value($attribute)->[0];
	}
	$statement .= " WHERE $db_id_name=?";
	push @values, $db_id;
	my $sth = $self->prepare($statement);
	$self->debug && print "<PRE>", join("\n", $statement, map {defined $_ ? $_ : 'undef'} @values), "</PRE>\n";
	my $res = $sth->execute(@values);
    }
}

# Requires at least one argument, namely, an object of type Instance,
# the instance to be deleted from the database.  Subsequent arguments
# relate to bookkeeping information that you may wish to save about
# the reasons for the deletion - see the comments for _insert__deleted
# for full details.  These arguments may be safely left out.
sub delete {
    my ($self, $i, $replacement_db_id, $reason, $comment) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    my $o = $self->ontology || $self->throw("Need ontology.");
#    ($i && ref($i) && ($i =~ /Instance/)) || $self->throw("Need instance, got '$i'!");
    ($i && ref($i) && $i->isa("GKB::Instance")) || $self->throw("Need instance, got '$i'!");
    my $db_id = $i->db_id || $self->throw("Instance has to have db_id for deleting.");

	# Create a _Deleted instance to record the deletion
	$self->_insert__deleted($db_id, $replacement_db_id, $reason, $comment);

    my $class = $i->class;
    my $db_id_name = $GKB::Ontology::DB_ID_NAME;
    my $statement;
    foreach my $ancestor ($o->list_ancestors($class), $class) {
		$self->execute("DELETE FROM $ancestor WHERE $db_id_name=$db_id");
		foreach my $attribute ($o->list_multivalue_own_attributes($ancestor)) {
		    $self->execute("DELETE FROM $ {ancestor}_2_$attribute WHERE $db_id_name=$db_id");
		}
    }
    unless ((caller(1))[3] and ((caller(1))[3] eq 'GKB::DBAdaptor::update') ||
	    ((caller(1))[3] eq 'GKB::WriteBackDBAdaptor::update')) {
	$self->delete_instance_from_referers($i);
    }
    $self->instance_cache->delete($db_id);
}

sub delete_by_db_id {
    my $self = shift;
    $self->debug && print "", (caller(0))[3], "\n";
    my @out;
    my $delete_from_referers_flag = ((caller(1))[3] and ((caller(1))[3] eq 'GKB::DBAdaptor::update') || ((caller(1))[3] eq 'GKB::WriteBackDBAdaptor::update')) ? undef : 1;
    my $db_id_name = $GKB::Ontology::DB_ID_NAME;
    foreach my $db_id (@_) {
		if (my $class = $self->fetch_single_attribute_value_by_db_id($db_id,$self->ontology->root_class,'_class')->[0]->[0]){
		    foreach my $ancestor ($self->ontology->list_ancestors($class), $class) {
				$self->execute("DELETE FROM $ancestor WHERE $db_id_name=$db_id");
				foreach my $attribute ($self->ontology->list_multivalue_own_attributes($ancestor)) {
				    $self->execute("DELETE FROM $ {ancestor}_2_$attribute WHERE $db_id_name=$db_id");
				}
		    }
		    if ($delete_from_referers_flag) {
				# Want (and have) to go via cache to make sure we have a single copy of every instance.
				my $tmp =
				    $self->instance_cache->fetch($db_id) ||
				    $self->instance_cache->store
				    ($db_id, GKB::Instance->new(-CLASS => $class, -ONTOLOGY => $self->ontology, -DB_ID => $db_id, -DBA => $self));
				$self->delete_instance_from_referers($tmp);
		    }
		} else {
		    push @out, $db_id;
		    $self->warn("No instance with db_id $db_id.");
		}
    }
    return @out;
}

# Exactly the same as delete_by_db_id except that a _Deleted
# instance is created for wach instance deleted.
sub delete_by_db_id_and_insert__deleted {
    my ($self, @db_ids) = @_;

    $self->_insert__deleted(@db_ids);
    $self->delete_by_db_id(@db_ids);
}

sub delete_instance_from_referers {
    my ($self,$i) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    $self->attach_reverse_attributes_to_instance($i);
    foreach my $att ($i->list_set_reverse_attributes) {
	foreach my $ii (@{$i->reverse_attribute_value($att)}) {
	    $ii->inflate;
	    # Since there's only a single copy of every instance, can do '!='
	    my @vals = grep {$_ != $i} @{$ii->attribute_value($att)};
	    @vals ? $ii->attribute_value($att,@vals) : $ii->attribute_value($att,undef);
	    $self->update($ii);
	}
    }
}

# When you delete an instance, record the occurence in a
# new _Deleted instance.  If there is a replacement instance,
# e.g. because you are doing a merge, then you should also supply
# the replacement's DB_ID.  Otherwise, this parameter can be undef.
# It's good practise to supply a reason, from the following list
# of terms:
#
# Killed not replaced
# Merged
# Obsoleted
# Split
#
# If you  use undef for this parameter, then the default reason
# inserted into the database will be "Killed not replaced".
# A comment would be nice, but can also be undef.
sub _insert__deleted {
    my ($self, $deleted_db_id, $replacement_db_id, $reason, $comment) = @_;

    # If no DB_ID has been assigned yet, don't bother to
    # make a note of this deletion.
    if (!(defined $deleted_db_id)) {
    	return;
    }

    # Check to see if _Deleted instance for this DB_ID exists already
    my $query = [["deletedInstanceDB_ID", [$deleted_db_id], 0]];
    my $ar = $self->fetch_instance_by_attribute("_Deleted", $query);
	my $_deleted = $ar->[0];
	if (scalar(@{$ar})>0) {
	    return;
	}

    # Only keep track of  instances of certain classes; ignore
    # all others.
    $ar = $self->fetch_instance_by_db_id($deleted_db_id);
    my $deleted = $ar->[0];
    if (!(defined $deleted)) {
    	return;
    }
    my $deleted_class = $deleted->class();
    if (!$self->ontology->is_class_attribute_allowed_class("_Deleted", "replacementInstances", $deleted_class)) {
    	return;
    }

    # Create the new _Deleted instance.
    my $deleted_display_name = "";
    if (defined $deleted->_displayName && defined $deleted->_displayName->[0]) {
    	$deleted_display_name = " (" . $deleted->_displayName->[0] . ")";
    }

    $_deleted = GKB::Instance->new(-CLASS => "_Deleted",
							    -ONTOLOGY => $self->ontology,
							    '_displayName' => "Deleted record of $deleted_db_id$deleted_display_name");
	$_deleted->inflated(1);
	if (!(defined $reason)) {
		if (defined $replacement_db_id) {
			$reason = "Obsoleted";
		} else {
			$reason = "Killed not replaced";
		}
	}
    $_deleted->attribute_value("deletedInstanceDB_ID", $deleted_db_id);
	$query = [['name','=',[$reason]]];
	$ar = $self->fetch_instance_by_remote_attribute('DeletedControlledVocabulary', $query);
	my $reason_instance = $ar->[0];
	if (!(defined $reason_instance)) {
		# This instance doesn't exist yet, so create it
		$reason_instance = GKB::Instance->new(-CLASS => "DeletedControlledVocabulary",
								    -ONTOLOGY => $self->ontology,
								    '_displayName' => $reason,
								    'name' => $reason);
		$reason_instance->inflated(1);
	}
    $_deleted->attribute_value("reason", $reason_instance);
	if (defined $comment) {
    	$_deleted->attribute_value("curatorComment", $comment);
	}

    # Insert the replacement instance, if available.
    my $replacement = undef;
    my $replacement_class = undef;
    if (defined $replacement_db_id) {
    	$ar = $self->fetch_instance_by_db_id($replacement_db_id);
    	$replacement = $ar->[0];
	    if (defined $replacement) {
	    	$replacement_class = $replacement->class();
	    	if (!$self->ontology->is_class_attribute_allowed_class("_Deleted", "replacementInstances", $replacement_class)) {
	    	}

	    	$_deleted->attribute_value("replacementInstances", $replacement);
	    } else {
	    }
    }

    # Store _Deleted instance
    $self->store($_deleted);
}

# Complements delete_by_db_id; creates a new _Deleted instance for every
# DB_ID on the supplied list.  This is a hack: in theory, the best way
# to do this would be to integrate the creation of _Deleted instances into
# the delete_by_db_id subroutine, but this subroutine gets called somewhere
# and many of the instances it is called upon to delete shouldn't get
# deleted and indeed, don't get deleted.  I do not understand what is going on
# here, so I am separating out the delete functionality from the _Deleted
# functionality.
sub insert__deleted_by_db_id {
    my $self = shift;
    $self->debug && print "", (caller(0))[3], "\n";
    my @out;
    my $delete_from_referers_flag = ((caller(1))[3] and ((caller(1))[3] eq 'GKB::DBAdaptor::update') || ((caller(1))[3] eq 'GKB::WriteBackDBAdaptor::update')) ? undef : 1;
    my $db_id_name = $GKB::Ontology::DB_ID_NAME;
    foreach my $db_id (@_) {
		if (my $class = $self->fetch_single_attribute_value_by_db_id($db_id,$self->ontology->root_class,'_class')->[0]->[0]){
			# Create a _Deleted instance to record the deletion
			$self->_insert__deleted($db_id, undef, "Obsoleted", "Deleted by DBAdaptor.insert__deleted_by_db_id");
		} else {
		    push @out, $db_id;
		    $self->warn("No instance with db_id $db_id.");
		}
    }
    return @out;
}

sub debug {
    my $self = shift;
    if (@_) {
	$self->{'debug'} = shift;
    }
    return $self->{'debug'};
}

### Function: fetch_single_attribute_value
# Fetches all values of a given attribute of a given class.
# Arguments:
# 1) string class name.
# 2) string attribute name.
# 3) "boolean", optional. Set this to true value if you want to fetch only the 1st value
# of multivalue attribute.
# Returns: reference to an array containing references to arrays containing:
# - db_id of the instance this attribute value belongs to.
# - attribute value.
# - attribute rank or undef if single value attribute
# - attribute class or undef if the value is not instance
###
sub fetch_single_attribute_value {
    my ($self,$class,$attribute,$first_only) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    $self->ontology->check_class_attribute($class,$attribute);
    my $db_id_name = $GKB::Ontology::DB_ID_NAME;
    my $origin = $self->ontology->class_attribute_origin($class,$attribute);
    my (@select,@where,@from);
    unless ($origin eq $class) {
	push @from, $class;
	push @where, "A_$$.$db_id_name=$class.$db_id_name";
    }
    if ($self->ontology->is_multivalue_class_attribute($origin,$attribute)) {
	push @select, "A_$$.$db_id_name";
	push @select, "A_$$.$ {attribute}";
	push @select, "A_$$.$ {attribute}_rank";
	push @from, "$ {origin}_2_$ {attribute} AS A_$$";
	push @where, "A_$$.$ {attribute}_rank = 0" if ($first_only);
    } else {
	push @select, "A_$$.$db_id_name";
	push @select, "A_$$.$ {attribute}";
	push @select, "NULL";
	push @from, "$origin AS A_$$";
    }
    if ($self->ontology->is_instance_type_class_attribute($origin,$attribute)) {
#	push @select, "A_$$.$ {attribute}_class";
	push @from, $self->ontology->root_class . " AS R" ;
	push @where, "A_$$.$ {attribute}=R.$db_id_name";
	push @select, "R._class";
    } else {
	push @select, "NULL";
    }
    my $statement =
	"SELECT " . join(",\n",@select) .
	"\nFROM " . join(",\n",@from);
    $statement .= "\nWHERE " . join("\nAND ",@where) if (@where);
    my ($sth,$res) = $self->execute($statement);
    return $sth->fetchall_arrayref;
}

### Function: fetch_single_attribute_value_by_db_id
# Fetches all values of a given attribute of an instance with given db_id.
# Arguments:
# 1) integer db_id
# 2) string class name.
# 3) string attribute name.
# 4) "boolean", optional. Set this to true value if you want to fetch only the 1st value
# of multivalue attribute.
# Returns: reference to an array containing references to arrays containing:
# - attribute value.
# - attribute rank or undef if single value attribute
# - attribute class or undef if the value is not instance
###
sub fetch_single_attribute_value_by_db_id {
    my ($self,$db_id,$class,$attribute,$first_only) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    ($db_id =~ /^\d+$/) || $self->throw("Not a valid-looking db_id: '$db_id'..");
    $self->ontology->check_class_attribute($class,$attribute);
    my $db_id_name = $GKB::Ontology::DB_ID_NAME;
    my $origin = $self->ontology->class_attribute_origin($class,$attribute);
    my @select = ("A_$$.$ {attribute}");
    my ($where,$from) = (" WHERE A_$$.$ {db_id_name}=$db_id", '');
    if ($self->ontology->is_multivalue_class_attribute($origin,$attribute)) {
	push @select, "A_$$.$ {attribute}_rank";
	$from = "$ {origin}_2_$ {attribute} AS A_$$";
	$first_only and  $where .= " AND A_$$.$ {attribute}_rank = 0";
    } else {
	push @select, "NULL";
	$from = "$origin AS A_$$";
    }
    if ($self->ontology->is_instance_type_class_attribute($origin,$attribute)) {
#	push @select, "$ {attribute}_class";
	$from .=  ', ' . $self->ontology->root_class . " AS R" ;
	$where .=  " AND A_$$.$ {attribute}=R.$db_id_name";
	push @select, "R._class";
    } else {
	push @select, "NULL";
    }
    my $statement = "SELECT " . join(",",@select) . " FROM $from $where";
    my ($sth,$res) = $self->execute($statement);
    return $sth->fetchall_arrayref;
}

### Function: fetch_paired_values
# Argument: arrayref in following format
# [[Class1, attribute1],
#  [linking_attribute1,Linking_Class,linking_attribute2],
#  [Class2, attribute2]]
# Here we ask for attribute1,attribute2 (value) pairs paired over the Linking_Class attributes
# linking_attribute1 and linking_attribute2, respectively.
###
sub fetch_paired_values {
    my ($self,$ref) = @_;
    # Check the validity of 1st Class/attribute pair
    my $db_id_name = $GKB::Ontology::DB_ID_NAME;
    $self->ontology->check_class_attribute(@{$ref->[0]});
    my $class1 = $ref->[0]->[0];
    my $attribute1 = $ref->[0]->[1];
    my $origin1 = $self->ontology->class_attribute_origin($class1,$attribute1);
    # Check validity of Linking_Class/linking_attribute pairs
    my $linker_attribute1 = $ref->[1]->[0];
    my $linker_class = $ref->[1]->[1];
    my $linker_attribute2 = $ref->[1]->[2];
    $self->ontology->check_class_attribute($linker_class,$linker_attribute1);
    $self->ontology->check_class_attribute($linker_class,$linker_attribute2);
    my $linker_origin1 = $self->ontology->class_attribute_origin($linker_class,$linker_attribute1);
    my $linker_origin2 = $self->ontology->class_attribute_origin($linker_class,$linker_attribute2);
    # Check the validity of 2nd Class/attribute pair
    $self->ontology->check_class_attribute(@{$ref->[2]});
    my $class2 = $ref->[2]->[0];
    my $attribute2 = $ref->[2]->[1];
    my $origin2 = $self->ontology->class_attribute_origin($class2,$attribute2);
    # Check that the linking is possible
    $self->ontology->is_class_attribute_allowed_class($linker_origin1,$linker_attribute1,$class1) ||
	$self->ontology->is_class_attribute_allowed_class($linker_origin1,$linker_attribute1,$origin1) ||
	$self->throw("Can't link to class '$class1' over class '$linker_class' attribute '$linker_attribute1'.");
    $self->ontology->is_class_attribute_allowed_class($linker_origin2,$linker_attribute2,$class2) ||
	$self->ontology->is_class_attribute_allowed_class($linker_origin2,$linker_attribute2,$origin2) ||
	$self->throw("Can't link to class '$class2' over class '$linker_class' attribute '$linker_attribute2'.");

    my (@select,@from,@where);
    my $linker1_table = ($self->ontology->is_multivalue_class_attribute($linker_origin1,$linker_attribute1)) ?
	"$ {linker_origin1}_2_$ {linker_attribute1}" : $linker_origin1;
    my $linker2_table = ($self->ontology->is_multivalue_class_attribute($linker_origin2,$linker_attribute2))?
	"$ {linker_origin2}_2_$ {linker_attribute2}" : $linker_origin2;
    push @from, $linker1_table;
    # Determine how Linking_Class attributes need to be joined
    unless ($linker1_table eq $linker2_table) {
	push @from, $linker2_table;
	push @where, "$linker1_table.$db_id_name=$linker2_table.$db_id_name";
	if ($self->ontology->is_multivalue_class_attribute($linker_origin1,$linker_attribute1)) {
	    push @where, "$linker_origin1.$db_id_name=$linker1_table.$db_id_name";
	    push @from, $linker_origin1;
	}
	if ($self->ontology->is_multivalue_class_attribute($linker_origin2,$linker_attribute2)) {
	    push @where, "$linker_origin2.$db_id_name=$linker2_table.$db_id_name";
	    push @from, $linker_origin2 unless ($linker_origin1 eq $linker_origin2);
	}
    }
    # Have to use aliases since the same class (and hence table name) can occur more than once.
    if ($self->ontology->is_multivalue_class_attribute($origin1,$attribute1)) {
	push @select, "A1.$ {attribute1}";
	push @from, "$ {origin1}_2_$ {attribute1} AS A1";
	push @where, "A1.$db_id_name=$ {linker1_table}.$ {linker_attribute1}";
	push @where, "A1.$ {attribute1}_rank=0";
    } else {
	push @select, "A1.$attribute1";
	push @from, "$origin1 AS A1";
	push @where, "A1.$attribute1=$ {linker1_table}.$ {linker_attribute1}";
    }
    if ($self->ontology->is_multivalue_class_attribute($origin2,$attribute2)) {
	push @select, "A2.$ {attribute2}";
	push @from, "$ {origin2}_2_$ {attribute2} AS A2";
	push @where, "A2.$db_id_name=$ {linker2_table}.$ {linker_attribute2}";
	push @where, "A2.$ {attribute2}_rank=0";
    } else {
	push @select, "A2.$attribute2";
	push @from, "$origin2 AS A2";
	push @where, "A2.$attribute2=$ {linker2_table}.$ {linker_attribute2}";
    }
    my $statement =
	"SELECT " . join(",\n",@select) .
	"\nFROM " . join(",\n",@from) .
	"\nWHERE " . join("\nAND ",@where);
    my ($sth,$res) = $self->execute($statement);
    return $sth->fetchall_arrayref;
}

sub fetch_multiple_attributes_values {
    my ($self,$class,$attribute_ar,$first_only,$order) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    $order ||= [];
    my %order;
    foreach my $i (0 .. $#{$order}) {
	ref($order->[$i]) || $self->throw("Not an array ref: '$order->[$i]'.");
	$order{$order->[$i]->[0]}->{'order'} = $i;
	$order{$order->[$i]->[0]}->{'desc'} = 1 if (defined $order->[$i]->[1] && uc($order->[$i]->[1]) eq 'DESC');
    }
    my (@select,%from,@where,@order);
    my $db_id_name = $GKB::Ontology::DB_ID_NAME;
    foreach my $attribute (@{$attribute_ar}) {
	$self->ontology->check_class_attribute($class,$attribute);
	my $origin = $self->ontology->class_attribute_origin($class,$attribute);
	if ($self->ontology->is_multivalue_class_attribute($origin,$attribute)) {
	    $from{"$ {origin}_2_$ {attribute}"} = 1;
	    push @select, "$ {origin}_2_$ {attribute}.$attribute";
	    push @where, "$ {origin}_2_$ {attribute}.$db_id_name=$class.$db_id_name";
	    push @where, "$ {origin}_2_$ {attribute}.$ {attribute}_rank=0" if ($first_only);
	    if ($order{$attribute}) {
		push @order, "$ {origin}_2_$ {attribute}.$attribute" .
		    ($order{$attribute}->{'direction'} ? ' DESC' : '');
	    }
	} else {
	    push @select, "$origin.$attribute";
	    unless ($from{$origin}) {
		$from{$origin} = 1;
		push @where, "$origin.$db_id_name=$class.$db_id_name";
		$from{$class} = 1;
	    }
	    if ($order{$attribute}) {
		push @order, "$origin.$attribute" .
		    ($order{$attribute}->{'direction'} ? ' DESC' : '');
	    }
	}
    }
    my $statement =
	"SELECT " . join(",\n",@select) .
	"\nFROM " . join(",\n",keys %from);
    $statement .= "\nWHERE " . join("\nAND ",@where) if (@where);
    $statement .= "\nORDER BY " . join("\n,",@order) if (@order);
    my ($sth,$res) = $self->execute($statement);
    return $sth->fetchall_arrayref;
}

sub store_or_merge {
    my ($self,$instance) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    $self->store_if_necessary($instance);
    unless ($instance->newly_stored) {
	my $stored = $self->instance_cache->fetch($instance->db_id);
	$self->inflate_instance($stored);
	# skip single-value attributes in order not to overwrite new ones with old ones.
	foreach my $att (grep{$instance->is_multivalue_attribute($_)} $stored->list_set_attributes) {
#	    print $att, "\t->", $stored->attribute_value($att)->[0], "<-\n";
	    if ($self->ontology->is_instance_type_class_attribute($stored->class,$att)) {
		my %h;
		map{$h{$_} = 1} map{$self->store_if_necessary($_)} @{$instance->attribute_value($att)};
		map{$instance->add_attribute_value($att, $_)} grep{! $h{$_->db_id}} @{$stored->attribute_value($att)};
	    } else {
		my %h;
		map{$h{uc($_)} = 1} @{$instance->attribute_value($att)};
		map{$instance->add_attribute_value($att, $_)} grep{! $h{uc($_)}} @{$stored->attribute_value($att)};
	    }
	}
	$self->update($instance);
	$self->instance_cache->delete($stored->db_id);
    }
    return $instance->db_id;
}

# Fetches instances which are not values of given class attributes. I.e.
# fetch_instances_which_are_not_class_attribute_values('PhysicalEntity',['Complex','hasComponent'])
# would fetch all PhysicalEntities which are not part of any Complexes.
# fetch...('Actvivity',[qw(Activity instanceOf Activity componentOf)])
# would fetch all the root Activity, i.e. biological_function
sub fetch_instances_which_are_not_class_attribute_values {
    my ($self,$class,$ar) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    my (@left_join,@where);
    my $db_id_name = $GKB::Ontology::DB_ID_NAME;
    for (my $i = 0; $i < @{$ar}; $i += 2) {
	my $cl = $ar->[$i];
	my $att = $ar->[$i + 1];
	$self->ontology->check_class_attribute($cl,$att);
	my $origin = $self->ontology->class_attribute_origin($cl,$att);
	my $alias = "A${i}_$$";
	my $tbl_name = ($self->ontology->is_multivalue_class_attribute($cl,$att)) ?
		"$ {origin}_2_$att" : $origin ;
	push @left_join, "LEFT JOIN $tbl_name $alias ON ($alias.$att=$class.$db_id_name)";
	push @where, "$alias.$att IS NULL";
    }
    my $statement = "SELECT $class.$db_id_name FROM $class\n" . join("\n",@left_join) .
                    "\nWHERE " . join ("\nAND ",@where);
    my ($sth,$res) = $self->execute($statement);
    my $ids = $self->db_handle->selectcol_arrayref($statement);
    return $self->fetch_instance_by_attribute($class,[["$db_id_name",$ids]]);
}

sub class_instance_count {
    my ($self,$class) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    $class ||= $self->ontology->root_class;
    $self->ontology->check_class_attribute($class);
    my $statement = qq(SELECT COUNT(*) FROM $class);
    my ($sth,$res) = $self->execute($statement);
    return $sth->fetchrow_arrayref->[0];
}

sub fetch_all_class_instances_as_shells {
    my ($self,$class) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    my $db_id_name = $GKB::Ontology::DB_ID_NAME;
    my @out;
    $class ||= $self->ontology->root_class;
    foreach my $ar (@{$self->fetch_multiple_attributes_values($class,
							      ["$db_id_name",'_displayName','_class'],
							      undef,
							      [['_displayName']])}) {
	my $instance = $self->instance_cache->fetch($ar->[0]) ||
	    $self->instance_cache->store($ar->[0],
	                                 GKB::Instance->new(-CLASS => $ar->[2],
							    -DB_ID => $ar->[0],
							    -ONTOLOGY => $self->ontology,
							    -DBA => $self,
							    '_displayName' => $ar->[1])
					);
	push @out, $instance;
    }
    return \@out;
}

sub fetch_db_ids_by_class {
    my ($self,$class) = @_;
    my $db_id_name = $GKB::Ontology::DB_ID_NAME;
    $self->ontology->check_class_attribute($class);
    my $statement = qq(SELECT $db_id_name FROM $class);
    return $self->db_handle->selectcol_arrayref($statement);
}

sub delete_by_class {
    my $self = shift;
    $self->debug && print "", (caller(0))[3], "\n";
    foreach (@_) {
	$self->insert__deleted_by_db_id(@{$self->fetch_db_ids_by_class($_)});
	$self->delete_by_db_id(@{$self->fetch_db_ids_by_class($_)});
    }
}

sub load_db_specific_modules {
    my ($self) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    my $package_base_name = 'GKB';
    foreach my $i (@{$self->fetch_instance(-CLASS => 'PerlModule')}) {
	print $i->displayName, "\n";
	my $code = $i->PerlCode->[0];
	$code =~ s/use\s+$package_base_name.+?\n//gms;
	eval $code;
	$@ && $self->warn($@);
    }
}

sub load_attribute_values {
    my ($self,$instance,$attribute) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    $instance->check_attribute($attribute);
    my ($sth,$instructions) = $self->_create_attribute_loading_sql($instance,$attribute);
    my $t_hr = {};
    while (my $ar = $sth->fetchrow_arrayref) {
	foreach my $instr (@{$instructions}) {
	    $t_hr = &{$instr->{'method'}}($self,$t_hr,$ar,$instr);
	}
    }
#    foreach my $k (keys %{$t_hr->{'attribute'}}) {
#	$instance->attribute_value($k,@{$t_hr->{'attribute'}->{$k}});
#    }
    foreach my $k (map {$_->{'attribute'}} @{$instructions}) {
	$instance->attribute_value($k, $t_hr->{'attribute'}->{$k} ? @{$t_hr->{'attribute'}->{$k}} : undef);
    }
}

sub load_single_attribute_values {
    my ($self,$instance,$attribute) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    $instance->db_id || $self->throw("Instance has to have db_id for being able to load attribute values.");
    $instance->check_attribute($attribute);
    my ($sth,$instructions) = $self->_create_single_attribute_loading_sql($instance,$attribute);
    my $t_hr = {};
    while (my $ar = $sth->fetchrow_arrayref) {
	foreach my $instr (@{$instructions}) {
	    $t_hr = &{$instr->{'method'}}($self,$t_hr,$ar,$instr);
	}
    }
    foreach my $k (map {$_->{'attribute'}} @{$instructions}) {
	$instance->attribute_value($k, $t_hr->{'attribute'}->{$k} ? @{$t_hr->{'attribute'}->{$k}} : undef);
    }
}

sub load_class_attribute_values_of_multiple_instances {
    my ($self,$class,$attribute,$instance_ar) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    my @classes = $self->ontology->list_classes_with_attribute($attribute,$class);
    if (@classes > 1) {
	my @classes = grep {$self->ontology->is_own_attribute($_,$attribute)} @classes;
    }
    foreach my $cls (@classes) {
	$self->_load_class_attribute_values_of_multiple_instances($cls,$attribute,$instance_ar);
    }
}

sub _load_class_attribute_values_of_multiple_instances {
    my ($self,$class,$attribute,$instance_ar) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    @{$instance_ar} || return;
    my $o = $self->ontology;
    $o->check_class_attribute($class,$attribute);
    my $origin = $o->class_attribute_origin($class,$attribute) || return; # TMP HACK
    my (@select,$instruction,$from);
    push @select, $DB_ID_NAME;
    if ($o->is_multivalue_class_attribute($origin,$attribute)) {
	$from = "$ {origin}_2_$ {attribute}";
	push @select, $attribute;
	push @select, "$ {attribute}_rank";
	if ($o->is_instance_type_class_attribute($origin,$attribute)) {
	    push @select, "$ {attribute}_class";
	    $instruction = {'method'=> \&_handle_ranked_instance_attribute,
			    'attribute' => $attribute,
			    'value' => 1,
			    'rank' => 2,
			    'class' => 3};
	} else {
	    $instruction = {'method'=> \&_handle_ranked_value_attribute,
			    'attribute' => $attribute,
			    'value' => 1,
			    'rank' => 2};
	}
    } else {
	$from = $origin;
	push @select, $attribute;
	if ($o->is_instance_type_class_attribute($origin,$attribute)) {
	    push @select, "$ {attribute}_class";
	    $instruction = {'method'=> \&_handle_single_instance_attribute,
			    'attribute' => $attribute,
			    'value' => 1,
			    'class' => 2};
	} else {
	    $instruction = {'method'=> \&_handle_single_value_attribute,
			    'attribute' => $attribute,
			    'value' => 1};
	}
    }
    my $statement = "SELECT " . join(",\n",@select) . "\nFROM $from" . "\nWHERE $DB_ID_NAME IN(" . join(",",(('?') x scalar(@{$instance_ar}))) . ")\nORDER BY $DB_ID_NAME";
    my @values = map {$_->db_id} @{$instance_ar};
    my $sth = $self->prepare($statement);
    $self->debug && print join("\n", $statement,@values), "\n";
    my $res = $sth->execute(@values);
    my $id = 0;
    my $i;
    my $t_hr = {};
    while (my $ar = $sth->fetchrow_arrayref) {
	if ($id ne $ar->[0]) {
	    if ($i) {
		$i->attribute_value($attribute,
				    $t_hr->{'attribute'}->{$attribute}
				    ? @{$t_hr->{'attribute'}->{$attribute}}
				    : undef);
	    }
	    $t_hr = {};
	    $id = $ar->[0];
	    $i = $self->instance_cache->fetch($id) ||
		$self->throw("Instance with $DB_ID_NAME $id not found in cache!");
	}
	$t_hr = &{$instruction->{'method'}}($self,$t_hr,$ar,$instruction);
    }
    if ($i) {
	$i->attribute_value($attribute,
			    $t_hr->{'attribute'}->{$attribute}
			    ? @{$t_hr->{'attribute'}->{$attribute}}
			    : undef);
    }
    foreach my $ins (@{$instance_ar}) {
	if ($ins->is_a($origin)) {
	    unless ($ins->is_attribute_value_loaded($attribute)) {
		$ins->attribute_value($attribute,undef);
	    }
	}
    }
}

sub _create_attribute_loading_sql {
    my ($self,$instance,$attribute) = @_;
    $self->debug && print "", (caller(0))[3], "\t$attribute\n";
    my $origin = $instance->attribute_origin($attribute);
    my (@select,@instructions,$from,@values);
    my $db_id_name = $GKB::Ontology::DB_ID_NAME;
    if ($instance->ontology->is_multivalue_class_attribute($origin,$attribute)) {
	$from = "$ {origin}_2_$ {attribute}";
	push @select, $attribute;
	push @select, "$ {attribute}_rank";
	if ($instance->is_instance_type_attribute($attribute)) {
	    push @select, "$ {attribute}_class";
	    push @instructions, {'method'=> \&_handle_ranked_instance_attribute,
				 'attribute' => $attribute,
				 'value' => 0,
				 'rank' => 1,
				 'class' => 2};
	} else {
	    push @instructions, {'method'=> \&_handle_ranked_value_attribute,
				 'attribute' => $attribute,
				 'value' => 0,
				 'rank' => 1};
	}
    } else {
	$from = $origin;
	foreach my $att ($instance->ontology->list_singlevalue_own_attributes($origin)) {
	    push @select, $att;
	    if ($instance->is_instance_type_attribute($att)) {
		push @select, "$ {att}_class";
		my $l = $#select;
		push @instructions, {'method'=> \&_handle_single_instance_attribute,
				     'attribute' => $att,
				     'value' => $l - 1,
				     'class' => $l};
	    } else {
		my $l = $#select;
		push @instructions, {'method'=> \&_handle_single_value_attribute,
				     'attribute' => $att,
				     'value' => $l};
	    }
	}
    }
    my $statement = "SELECT " . join(",\n",@select) . "\nFROM $from" . "\nWHERE $db_id_name=?";
    my $sth = $self->prepare($statement);
    $self->debug && print join("\n", $statement,$instance->db_id), "\n";
    my $res = $sth->execute($instance->db_id);
    return ($sth,\@instructions);
}

sub _create_single_attribute_loading_sql {
    my ($self,$instance,$attribute) = @_;
    $self->debug && print "", (caller(0))[3], "\t$attribute\n";
    my $origin = $instance->attribute_origin($attribute);
    my (@select,@instructions,$from,@values);
    my $db_id_name = $GKB::Ontology::DB_ID_NAME;
    if ($instance->ontology->is_multivalue_class_attribute($origin,$attribute)) {
	$from = "$ {origin}_2_$ {attribute}";
	push @select, $attribute;
	push @select, "$ {attribute}_rank";
	if ($instance->is_instance_type_attribute($attribute)) {
	    push @select, "$ {attribute}_class";
	    push @instructions, {'method'=> \&_handle_ranked_instance_attribute,
				 'attribute' => $attribute,
				 'value' => 0,
				 'rank' => 1,
				 'class' => 2};
	} else {
	    push @instructions, {'method'=> \&_handle_ranked_value_attribute,
				 'attribute' => $attribute,
				 'value' => 0,
				 'rank' => 1};
	}
    } else {
	$from = $origin;
	push @select, $attribute;
	if ($instance->is_instance_type_attribute($attribute)) {
	    push @select, "$ {attribute}_class";
	    push @instructions, {'method'=> \&_handle_single_instance_attribute,
				 'attribute' => $attribute,
				 'value' => 0,
				 'class' => 1};
	} else {
	    push @instructions, {'method'=> \&_handle_single_value_attribute,
				 'attribute' => $attribute,
				 'value' => 0};
	}
    }
    my $statement = "SELECT " . join(",\n",@select) . "\nFROM $from" . "\nWHERE $db_id_name=?";
    my $sth = $self->prepare($statement);
    $self->debug && print join("\n", $statement,$instance->db_id), "\n";
    my $res = $sth->execute($instance->db_id);
    return ($sth,\@instructions);
}

sub load_reverse_attribute_values {
    my ($self,$instance,$attribute) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    $instance->is_valid_reverse_attribute($attribute) ||
	($NO_SCHEMA_VALIDITY_CHECK
	 ? return
	 : $self->throw("'$attribute' is not a valid reverse attribute for " . $instance->id_string)
	 );
    foreach my $class ($instance->ontology->list_allowed_classes_origin_for_class_reverse_attribute($instance->class,$attribute)) {
	$instance->add_reverse_attribute_value
	    ($attribute, $self->fetch_instance_by_attribute($class,[[$attribute,[$instance->db_id]]]));
    }
}

sub fetch_instance_with_query_attributes_only {
    my ($self,$thing2get,$query) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    my ($sth,$instructions) = $self->_create_instancefetching_sql_query_attributes_only($thing2get,$query);
    return $self->_handle_sql_query_results2($sth,$instructions);
}

sub _create_instancefetching_sql_query_attributes_only {
    my ($self,$thing2get,$query) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    $self->ontology->class($thing2get) || $self->throw("Don't know how to fetch '$thing2get'.");
    my (@select,@from,@join,@where,@values,@instructions,%seen);
    my $db_id_name = $GKB::Ontology::DB_ID_NAME;
#    $self->debug && print Data::Dumper->Dumpxs([$query], ["$query"]);
    my $c = 0;
    my $root_class_name = $self->ontology->root_class;
    push @select, "$ {root_class_name}.$db_id_name";
    push @select, "$ {root_class_name}._class";
    push @instructions, {'id_col' => 0, 'class_col' => 1};
    foreach my $q (@{$query}) {
	$c++;
	my $att = $q->[0];
	@{$q->[1]} || $self->throw("Empty value array for attribute '$att'.");
	$self->ontology->class($thing2get)->{'attribute'}->{$att} ||
	    $self->throw("Don't know how to get '$thing2get' by '$att'.");
	my $match_string;
	if ($q->[2] && defined $q->[1]->[0]) {
	    $match_string = " REGEXP ?";
	    push @values, $q->[1]->[0];
	} elsif (scalar(@{$q->[1]}) == 1) {
	    if (! defined $q->[1]->[0] || ($q->[1]->[0] eq 'undef') || (uc($q->[1]->[0]) eq 'NULL')) {
		$match_string = " IS NULL";
	    } elsif (uc($q->[1]->[0]) eq 'NOT NULL') {
		$match_string = " IS NOT NULL";
	    } else {
		$match_string = "= ?";
		push @values, $q->[1]->[0];
	    }
	} else {
	    my @tmp = ('?') x scalar(@{$q->[1]});
	    $match_string = " IN(" . join(",",@tmp) . ")";
	    push @values, @{$q->[1]};
	}
	my $origin = $self->ontology->class_attribute_origin($thing2get,$att);
	if ($self->ontology->is_multivalue_class_attribute($thing2get,$att)) {
	    # This requires bending over backwards.
	    # Have to use aliases to join table to itself.
	    my $alias = "A_$ {c}_$$";
	    push @where, "$alias.$att$match_string";
	    push @join, "LEFT JOIN $ {origin}_2_$att AS $alias ON ($alias.$db_id_name=$root_class_name.$db_id_name)";

	    unless ($seen{"$ {origin}_2_$ {att}"}++) {
		push @select, "$ {origin}_2_$att.$att";
		push @select, "$ {origin}_2_$att.$ {att}_rank";
		push @join, "LEFT JOIN $ {origin}_2_$ {att} ON ($ {origin}_2_$ {att}.$db_id_name=$root_class_name.$db_id_name)";
		if ($self->ontology->is_instance_type_class_attribute($origin,$att)) {
		    push @select, "$ {origin}_2_$att.$ {att}_class";
		    my $l = $#select;
		    push @instructions, {'method'=> \&_handle_ranked_instance_attribute,
					 'attribute' => $att,
					 'value' => $l - 2,
					 'rank' => $l - 1,
					 'class' => $l};
		} else {
		    my $l = $#select;
		    push @instructions, {'method'=> \&_handle_ranked_value_attribute,
					 'attribute' => $att,
					 'value' => $l - 1,
					 'rank' => $l};
		}
	    }
	} else {
	    push @where, "$origin.$att$match_string";
	    # check in order not to retrieve attributes multiple times
	    unless ($seen{$origin}++) {
		push @from, $origin;
		push @where, "$ {origin}.$db_id_name=$root_class_name.$db_id_name";
		foreach my $attribute ($self->ontology->list_singlevalue_own_attributes($origin)) {
		    push @select, "$origin.$attribute";
		    if ($self->ontology->is_instance_type_class_attribute($origin,$attribute)) {
			push @select, "$origin.$ {attribute}_class";
			my $l = $#select;
			push @instructions, {'method'=> \&_handle_single_instance_attribute,
					     'attribute' => $attribute,
					     'value' => $l - 1,
					     'class' => $l};
		    } else {
			my $l = $#select;
			push @instructions, {'method'=> \&_handle_single_value_attribute,
					     'attribute' => $attribute,
					     'value' => $l};
		    }
		}
	    }
	}
    }
    unless ($seen{$root_class_name}++) {
	push @from, $root_class_name;
	foreach my $attribute (grep {! /^(_class|$db_id_name)$/}
			       $self->ontology->list_singlevalue_own_attributes($root_class_name)) {
	    push @select, "$root_class_name.$attribute";
	    if ($self->ontology->is_instance_type_class_attribute($root_class_name,$attribute)) {
		push @select, "$root_class_name.$ {attribute}_class";
		my $l = $#select;
		push @instructions, {'method'=> \&_handle_single_instance_attribute,
				     'attribute' => $attribute,
				     'value' => $l - 1,
				     'class' => $l};
	    } else {
		my $l = $#select;
		push @instructions, {'method'=> \&_handle_single_value_attribute,
				     'attribute' => $attribute,
				     'value' => $l};
	    }
	}
    }
    unless ($seen{$thing2get}++) {
	push @from, $thing2get;
	push @where, "$thing2get.$db_id_name=$root_class_name.$db_id_name";
    }

    # Ensure that the root class name is last so that MySQL 5 left join will work.
    @from = grep {$_ ne $root_class_name} @from;
    push @from, $root_class_name;

    my $statement = "SELECT " . join(",\n",@select) . "\nFROM " . join(",\n",@from);
    $statement .= "\n" . join("\n",@join) if (@join);
    $statement .= "\nWHERE " . join("\nAND ",@where) if (@where);
    $statement .= "\nORDER BY $ {root_class_name}.$db_id_name";
    my $sth = $self->prepare($statement);
    $self->debug && print join("\n", $statement, @values), "\n";
    my $res = $sth->execute(@values);
    return ($sth,\@instructions);
}

### Function: _handle_sql_query_results2
# Method to be used in the context of fetch_instance_with_query_attributes_only.
###
sub _handle_sql_query_results2 {
    my ($self,$sth,$instructions) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    my ($t_hr,$id,@out,$class,$hr);
#    $self->debug && print Data::Dumper->Dumpxs([$instructions], ["$instructions"]);
    my $tmp = shift @{$instructions};
    my $id_col = $tmp->{'id_col'};
    my $class_col = $tmp->{'class_col'};
    while (my $ar = $sth->fetchrow_arrayref) {
		if ($id && ($id ne $ar->[$id_col])) {
		    # new instance
		    my $i = $self->_instance_from_hash2($t_hr,$class,$id,$instructions);
		    push @out, $i;
		}
		$class = $ar->[$class_col];
		$id = $ar->[$id_col];
		foreach my $instr (@{$instructions}) {
		    $t_hr = &{$instr->{'method'}}($self,$t_hr,$ar,$instr);
		}
    }

    if ($id) {
#		$self->debug && print Data::Dumper->Dumpxs([$t_hr], ["$t_hr"]);
		my $i = $self->_instance_from_hash2($t_hr,$class,$id,$instructions);
		push @out, $i;
    }
    return \@out;
}

### Function: _instance_from_hash2
# Method to be used in the context of fetch_instance_with_query_attributes_only.
###
sub _instance_from_hash2 {
    my ($self,$t_hr,$class,$id,$instructions) = @_;
    $self->debug && print "==>", (caller(0))[3], "\n";
    my $instance = $self->instance_cache->fetch($id) ||
	$self->instance_cache->store($id,GKB::Instance->new
				     (
				      -ONTOLOGY => $self->ontology,
				      -CLASS => $class,
				      -ID => $id,
				      -DB_ID => $id,
				      -DBA => $self,
				      ));
    # Here we want to set the values of only those attributes that were indeed fetched.
#    foreach my $k (keys %{$t_hr->{'attribute'}}) {
#	$instance->attribute_value($k, ($t_hr->{'attribute'}->{$k}) ? @{$t_hr->{'attribute'}->{$k}} : undef);
#    }
    foreach my $k (map {$_->{'attribute'}} @{$instructions}) {
	$instance->attribute_value($k, $t_hr->{'attribute'}->{$k} ? @{$t_hr->{'attribute'}->{$k}} : undef);
    }
    undef %{$t_hr};
    $self->debug && print "<==", (caller(0))[3], "\n";
    return $instance;
}

sub replace_strings_with_db_ids_where_appropriate {
    my ($self,$class,$query) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
#    print qq(<PRE>), Data::Dumper->Dumpxs([$query],["$query"]), qq(</PRE>\n);
    $self->ontology->check_class_attribute($class);
    my $results_flag = 1;
    foreach my $q (@{$query}) {
	$self->ontology->check_class_attribute($class,$q->[0]);
	if ($self->ontology->is_instance_type_class_attribute($class,$q->[0])) {
	    if ($q->[2] && ($q->[2] =~ /NULL$/i)) {
		next;
	    }
	    my (@numbers,@strings);
	    # Separate strings from numbers and do the query with the former only
	    # since the latter are likely to be DB_IDs
	    map {/^\d+$/ ? push @numbers, $_ : push @strings, $_} @{$q->[1]};
	    $q->[1] = \@numbers;
	    if (@strings) {
		my %tmp;
		foreach my $cls ($self->ontology->list_allowed_classes($class,$q->[0])) {
		    foreach my $att (grep {! ($_ eq '_Protege_id' or $_ eq '_class')}
				     grep {$self->ontology->is_string_type_class_attribute($cls,$_)}
				     $self->ontology->list_class_attributes($cls)) {
			map {$tmp{$_->db_id}++} @{$self->fetch_instance_by_attribute($cls,[[$att,\@strings,$q->[2]]])};
		    }
		}
		push @{$q->[1]}, keys %tmp;
	    }
	    $q->[2] = '=' unless ($q->[2] eq '!=');
	    $results_flag = 0 unless(@{$q->[1]});
	}
    }
    # If any of the queries produced no results the whole query will produce no results
    # if the queries and AND:ed together as they are now. So, whoever calls this function
    # can check the return value and if it is not 1 just skip doing the whole query if appropriate.
#    print qq(<PRE>), Data::Dumper->Dumpxs([$query],["$query"]), qq(</PRE>\n);
    return $results_flag;
}

sub fetch_identical_instances {
    my ($self,$i) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    (ref($i) && $i->isa("GKB::Instance")) || $self->throw("Need GKB::Instance, got '$i'.");
    $self->debug && print join("\t",($i->class,($i->id || 'undef'),($i->db_id || 'undef'))), "\n";
    if ($i->identical_instances_in_db) {
	return $i->identical_instances_in_db;
    }
    if ($self->{'instances_being_checked'}->{"$i"}) {
	$self->throw("Instance '" . $i->extended_displayName . "' refers (either directly or indirectly) to itself in its defining attributes.");
    }
    my @out;
    $self->{'instances_being_checked'}->{"$i"} = $i;
    if (my $query = $self->_make_query_to_retrieve_identical_instance2($i)) {
	my $ar = $self->_fetch_instance_with_defining_attributes_only($i->class,$query);
	foreach my $i2 (@{$ar}) {
	    next if ($i == $i2);
	    push @out, $i2 if ($i->reasonably_identical($i2));
	}
	$i->identical_instances_in_db(\@out);
    }
    delete $self->{'instances_being_checked'}->{"$i"};
    return \@out;
}

# This method also checks for ->attribute_value(DB_ID) rather than just
# ->db_id
sub _make_query_to_retrieve_identical_instance2 {
    my ($self,$i) = @_;
    my $o = $self->ontology;
    $self->debug && print "", (caller(0))[3], "\n";
    $self->debug &&
	print join("\t",($i->class,($i->id || 'undef'),($i->db_id || 'undef')))," Checking 'all+any': ",
#	join(", ",(keys %{$o->class($i->class)->{'check'}->{'all'}},
#		   keys %{$o->class($i->class)->{'check'}->{'any'}})), "\n";
        join(", ",($o->list_class_defining_attributes($i->class))), "\n";
    my @query;
#    foreach my $attribute (keys %{$o->class($i->class)->{'check'}->{'all'}}) {
    foreach my $attribute ($o->list_class_attributes_with_defining_type($i->class,'all')) {
	my %seen;
	foreach my $val (@{$i->attribute_value($attribute)}) {
	    if ($i->is_instance_type_attribute($attribute)) {
		my $db_id = $val->db_id || $val->attribute_value($DB_ID_NAME)->[0];
		if ($db_id) {
		    next if $seen{$db_id}++;
		    push @query, [$attribute, [$db_id]];
		} else {
		    my $ar = $self->fetch_identical_instances($val);
		    @{$ar} || return;
		    next if $seen{join(":",map {$_->db_id} @{$ar})}++;
		    push @query, [$attribute, [map{$_->db_id} @{$ar}]];
		}
	    } else {
		next if $seen{$val}++;
		push @query, [$attribute, [$val]];
	    }
	}
    }

#    foreach my $attribute (keys %{$o->class($i->class)->{'check'}->{'any'}}) {
    foreach my $attribute ($o->list_class_attributes_with_defining_type($i->class,'any')) {
	my @values;
	my %seen;
	foreach my $val (@{$i->attribute_value($attribute)}) {
	    if ($i->attribute_type($attribute) eq 'db_instance_type') {
		my $db_id = $val->db_id || $val->attribute_value($DB_ID_NAME)->[0];
		if ($db_id) {
		    next if $seen{$db_id}++;
		    push @values, $db_id;
		} else {
		    push @values, map {$_->db_id} grep {! $seen{$_->db_id}++} @{$self->fetch_identical_instances($val)}
		}
	    } else {
		next if $seen{$val}++;
		push @values, $val;
	    }
	}
	if (($i->attribute_type($attribute) eq 'db_instance_type') &&
	    @{$i->attribute_value($attribute)} &&
	    ! @values
	    ) {
	    return;
	}
	@values && push @query, [$attribute, \@values];
    }
    (@query) ? return \@query : return;
}

sub merge_instances {
    my ($self,$i1,$i2) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    ($i1 && $i2) || $self->throw("Need 2 Instances.");
    $i1->merge($i2);

    if ($i1->db_id) {
		$self->update($i1);
    } elsif ($i2->db_id) {
		$i1->db_id($i2->db_id);
		$self->update($i1);
    } else {
		$self->store_if_necessary($i1);
    }

    if ($i2->db_id && ($i2->db_id != $i1->db_id)) {
        # Fetch the referers so that their display name can be reset later.
		my $referers = $self->fetch_referer_by_instance($i2);
		# Break their connection to the instance being merged and deleted.
		map {$_->deflate} @{$referers};
		foreach my $ar (@{$self->ontology->class_referers_by_attribute_origin($i2->class)}) {
		    my $statement;
		    if ($ar->[2]) {
			# multi-value attribute
			$statement = qq(UPDATE $ar->[0]_2_$ar->[1] SET $ar->[1] = ?, $ar->[1]_class = ? WHERE $ar->[1] = ?);
		    } else {
			# single-value attribute
			$statement = qq(UPDATE $ar->[0] SET $ar->[1] = ?, $ar->[1]_class = ? WHERE $ar->[1] = ?);
		    }
#	    	print join("\n",$statement,$i1->db_id,$i1->class,$i2->db_id), "\n";
		    $self->execute($statement,$i1->db_id,$i1->class,$i2->db_id);
		}
		$self->delete($i2, $i1->db_id, "Merged", "DBAdaptor.merge_instances");
		# update the cache
		$self->instance_cache->store($i1->db_id,$i1);
		foreach my $referer (@{$referers}) {
		    $referer->namedInstance;
		    $self->update_attribute($referer,'_displayName');
		}
    }
}

sub fetch_instance_by_single_attribute {
    my ($self,$class,$query) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    my ($from,$where,$join,$values) = $self->_from_where_join_values($class,$query);
    my $root_class = $self->ontology->root_class;
    my $statement =
	"SELECT DISTINCT $root_class.$DB_ID_NAME,$root_class._class,$root_class._displayName\n" .
	"FROM " . join(",\n",@{$from});
    $statement .= "\n" . join("\n",@{$join}) if (@{$join});
    $statement .= "\nWHERE " . join("\nAND ",@{$where}) if (@{$where});
    $statement .= "\nORDER BY $root_class.$DB_ID_NAME";
#    print qq(<PRE>$statement\n$value</PRE>\n);
    my $sth = $self->prepare($statement);
    (@{$values}) ? $sth->execute(@{$values}) : $sth->execute;
    my @out;
    while (my $ar = $sth->fetchrow_arrayref) {
	push @out,  $self->instance_cache->fetch($ar->[0]) ||
	    $self->instance_cache->store($ar->[0], GKB::Instance->new
					 (
					  -ONTOLOGY => $self->ontology,
					  -CLASS => $ar->[1],
					  -DB_ID => $ar->[0],
					  -DBA => $self,
					  '_displayName' => $ar->[2],
					  ));
    }
    return \@out;
}

sub fetch_instance_by_string_type_attribute {
    my ($self,$value,$operator) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    my @out;
    my %seen;
    foreach my $class ($self->ontology->list_classes) {
	foreach my $attribute (grep {! /^_/}
			       grep {$self->ontology->is_string_type_class_attribute($class,$_)}
			       $self->ontology->list_own_attributes($class)) {
	    push @out, grep {! $seen{$_->db_id}++} @{$self->fetch_instance_by_attribute($class,[[$attribute,[$value],$operator]])};
	}
    }
    return \@out;
}

sub fetch_instance_by_string_type_attribute_and_species_db_id {
    my ($self,$value,$operator,$sp_id) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    my @out;
    my %seen;

    # Loop over all ontology classes to see if $value can be found in
    # any instance of the corresponding class.
    foreach my $class ($self->ontology->list_classes) {
    	# For backward compatibility to old data models: get attribute
    	# name for species.
		my $att2;
		if ($sp_id) {
		    if ($self->ontology->is_valid_class_attribute($class,'species')) {
				$att2 = 'species';
		    } elsif ($self->ontology->is_valid_class_attribute($class,'taxon')) {
				$att2 = 'taxon';
		    }
		}

		my @atts = grep {! /^_(P|i|h)/}
		grep {$self->ontology->is_string_type_class_attribute($class,$_)}
		$self->ontology->list_own_attributes($class);
		# Loop over all attributes and create queries for each such that
		# those whose values match $value get added to the list.
		if ($att2) {
		    foreach my $attribute (@atts) {
				push @out, grep {! $seen{$_->db_id}++} @{$self->fetch_instance_by_attribute($class,[[$attribute,[$value],$operator],[$att2,[$sp_id]]])};
				push @out, grep {! $seen{$_->db_id}++} @{$self->fetch_instance_by_attribute($class,[[$attribute,[$value],$operator],[$att2,[],'IS NULL']])};
		    }
		} else {
		    foreach my $attribute (@atts) {
				push @out, grep {! $seen{$_->db_id}++} @{$self->fetch_instance_by_attribute($class,[[$attribute,[$value],$operator]])};
			}
		}
    }
    return \@out;
}

# The same idea as fetch_instance_by_string_type_attribute_and_species_db_id,
# but you also have to specify a reference to an array of allowed classes that
# restricts the search.  Should help to make the search quicker.
sub fetch_instance_by_string_type_attribute_and_species_db_id_by_class {
    my ($self,$value,$operator,$sp_id, $allowed_classes, $forbidden_attributes) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    my @out;
    my %seen;
    my $nice_class_flag;
    my $class;
    my $subclass;
    my @subclasses;

    if (!(defined $forbidden_attributes)) {
    	my %empty = ();
    	$forbidden_attributes = \%empty;
    }

#    push(@{$allowed_classes}, 'DatabaseObject');
    # Loop over all allowed ontology classes to see if $value can be found in
    # any instance of the corresponding class.
    foreach $class (@{$allowed_classes}) {
    	if ($class eq 'DatabaseObject') {
    		# Stops the method from exploring all possible classes
    		@subclasses = ();
    	} else {
    		@subclasses = $self->ontology->descendants($class);
    	}
    	push(@subclasses, $class);
    	foreach $subclass (@subclasses) {
	    	# For backward compatibility to old data models: get attribute
	    	# name for species.
			my $att2;
			if ($sp_id) {
			    if ($self->ontology->is_valid_class_attribute($subclass,'species')) {
					$att2 = 'species';
			    } elsif ($self->ontology->is_valid_class_attribute($subclass,'taxon')) {
					$att2 = 'taxon';
			    }
			}

			my @atts = grep {! /^_(P|i|h)/}
				grep {$self->ontology->is_string_type_class_attribute($subclass,$_)}
				$self->ontology->list_own_attributes($subclass);

			# Loop over all attributes and create queries for each such that
			# those whose values match $value get added to the list.
			if ($att2) {
			    foreach my $attribute (@atts) {
		    		if (!$forbidden_attributes->{$attribute}) {
						push @out, grep {! $seen{$_->db_id}++} @{$self->fetch_instance_by_attribute($subclass,[[$attribute,[$value],$operator],[$att2,[$sp_id]]])};
						push @out, grep {! $seen{$_->db_id}++} @{$self->fetch_instance_by_attribute($subclass,[[$attribute,[$value],$operator],[$att2,[],'IS NULL']])};
		    		}
			    }
			} else {
			    foreach my $attribute (@atts) {
		    		if (!$forbidden_attributes->{$attribute}) {
			    		push @out, grep {! $seen{$_->db_id}++} @{$self->fetch_instance_by_attribute($subclass,[[$attribute,[$value],$operator]])};
		    		}
			    }
			}
    	}
    }

    return \@out;
}

sub fetch_class_instance_by_string_type_attribute {
    my ($self,$class,$value,$operator) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    my @out;
    my %seen;
    foreach my $attribute ($self->ontology->list_string_type_class_attributes($class)) {
	push @out, grep {! $seen{$_->db_id}++} @{$self->fetch_instance_by_attribute($class,[[$attribute,[$value],$operator]])};
    }
    return \@out;
}

sub fetch_class_instance_by_string_type_attribute_and_species_db_id {
    my ($self,$class,$value,$operator,$sp_id) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    my @out;
    my %seen;
    my $att2;
    if ($sp_id) {
	if ($self->ontology->is_valid_class_attribute($class,'species')) {
	    $att2 = 'species';
	} elsif ($self->ontology->is_valid_class_attribute($class,'taxon')) {
	    $att2 = 'taxon';
	}
    }
    if ($att2) {
	foreach my $attribute ($self->ontology->list_string_type_class_attributes($class)) {
	    push @out, grep {! $seen{$_->db_id}++} @{$self->fetch_instance_by_attribute($class,[[$attribute,[$value],$operator],[$att2,[$sp_id]]])};
	    push @out, grep {! $seen{$_->db_id}++} @{$self->fetch_instance_by_attribute($class,[[$attribute,[$value],$operator],[$att2,[],'IS NULL']])};
	}
    } else {
	foreach my $attribute ($self->ontology->list_string_type_class_attributes($class)) {
	    push @out, grep {! $seen{$_->db_id}++} @{$self->fetch_instance_by_attribute($class,[[$attribute,[$value],$operator]])};
	}
    }
	return \@out;
}

# This function is not to be used when there are too many joins
sub fetch_class_instance_by_fulltext_in_boolean_mode {
    my ($self,$class,$value) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    $self->ontology->check_class_attribute($class);
    my (@from,@match,%seen,@where);
    my $root_class = $self->ontology->root_class;
    foreach my $attribute (grep {! /^_/} $self->ontology->list_string_type_class_attributes($class)) {
	my $origin = $self->ontology->class_attribute_origin($class,$attribute);
	if ($self->ontology->is_multivalue_class_attribute($origin,$attribute)) {
	    my $tbl = "$ {origin}_2_$attribute";
	    $self->_throw_if_innodb_table($tbl, "Table '$tbl' does not support fulltext queries.");
	    push @from, $tbl;
	    push @match, "$tbl.$attribute";
	    push @where, "$tbl.$DB_ID_NAME=$root_class.$DB_ID_NAME";
	} else {
	    unless ($seen{$origin}++) {
		$self->_throw_if_innodb_table($origin, "Table '$origin' does not support fulltext queries.");
		push @from, $origin;
		push @where, "$origin.$DB_ID_NAME=$root_class.$DB_ID_NAME" unless ($origin eq $root_class);
	    }
	    push @match, "$origin.$attribute";
	}
    }
    return [] unless (@match);
    push @from, $root_class unless ($seen{$root_class}++);
    my $statement =
	"SELECT DISTINCT $root_class.$DB_ID_NAME,$root_class._class,$root_class._displayName" .
	"\nFROM\n" . join("\n,",@from) .
	"\nWHERE\n" . join("\n AND ", ("MATCH(\n" . join("\n,",@match) . "\n)" . "\nAGAINST(? IN BOOLEAN MODE)"), @where);
#    print qq(<PRE>$statement\n$value</PRE>\n);
    my $sth = $self->prepare($statement);
    $sth->execute($value);
    my @out;
    while (my $ar = $sth->fetchrow_arrayref) {
	push @out,  $self->instance_cache->fetch($ar->[0]) ||
	    $self->instance_cache->store($ar->[0], GKB::Instance->new
					 (
					  -ONTOLOGY => $self->ontology,
					  -CLASS => $ar->[1],
					  -DB_ID => $ar->[0],
					  -DBA => $self,
					  '_displayName' => $ar->[2],
					  ));
    }
    return \@out;
}

sub _from_where_join_values {
    my ($self,$class,$query) = @_;
    (ref($query) eq 'ARRAY') || $self->throw("Need an array reference, got '$query'.");
    my (@from,@where,@join,@values,$alias,%from);
    my $c = 0;
    my $root_class = $self->ontology->root_class;
#    push @from, $root_class;
    $from{$root_class}++;
    foreach my $subquery (@{$query}) {
	(ref($subquery) eq 'ARRAY') || $self->throw("Need an array reference, got '$subquery'.");
	my ($att,$value_ar,$operator,$reverse_attribute_class) = @{$subquery};
	(ref($value_ar) eq 'ARRAY') || $self->throw("Need a reference to value array, got '$value_ar'.");
	$operator ||= '=';
	$operator = uc($operator);
	@{$value_ar} > 1 and $operator eq '=' and $operator = 'IN';
	@{$value_ar} > 1 and $operator eq '!=' and $operator = 'NOT IN';
	if ($reverse_attribute_class) {
	    $self->ontology->is_valid_class_reverse_attribute($class,$att) || $self->throw("Invalid reverse attribute '$att' for class '$class'.");
	    $self->ontology->check_class_attribute($reverse_attribute_class,$att);
	    my $origin = $self->ontology->class_attribute_origin($reverse_attribute_class,$att);
	    my $table = ($self->ontology->is_multivalue_class_attribute($origin,$att)) ? "$ {origin}_2_$att" : $origin;
	    unless($from{$class}++) {
		push @where, "$class.$DB_ID_NAME=$root_class.$DB_ID_NAME";
	    }
	    $alias = "B_$ {c}_$$";
	    if ($operator eq 'IS NULL') {
		push @join, "LEFT JOIN $table AS $alias ON ($root_class.$DB_ID_NAME=$alias.$att)";
		push @where, "$alias.$att IS NULL";
	    } else {
		$from{"$table AS $alias"}++;
		push @where, "$class.$DB_ID_NAME=$alias.$att";
		if ($operator eq 'IS NOT NULL') {
		    #do nothing
		} elsif ($operator eq 'IN' or $operator eq 'NOT IN') {
		    my @tmp = ('?') x scalar(@{$value_ar});
		    push @where, "$alias.$DB_ID_NAME $operator(" . join(",",@tmp) . ")";
		    push @values, @{$value_ar};
		} else {
		    push @where, "$alias.$DB_ID_NAME $operator ?";
		    push @values, $value_ar->[0];
		}
	    }
	} else {
	    $self->ontology->check_class_attribute($class,$att);
	    my $origin = $self->ontology->class_attribute_origin($class,$att);
### The commented bit here is to deal with != queries on multi-value attributes which
# do not work otherwise. However, as this bit hasn't been tested properly either and
# as I don't have time to do it now I leave it out for the time being.
# Species.name != 'homo sapiens' (using extendedsearch) works
# Pathway.species.name != 'homo sapiens' (using remoteattsearch) works in the sense that
# it return al pathways w/o non-human species but also those which have human in addition
# to something else.
# Pathway.species != 'homo sapiens' (using extendedsearch)
# and
# Reaction.orthologousEvent.species.name != 'homo sapiens' (using remoteattsearch)
# are something that I have to investigate further
#	    if ($self->ontology->is_multivalue_class_attribute($origin,$att) &&
#		(($operator eq 'NOT IN') || ($operator eq '!='))) {
#		$value_ar = $self->fetch_instance_DB_ID_by_attribute($class,[[$att,$value_ar,'=']]);
#		$att = $DB_ID_NAME;
#		$origin = $root_class;
#	    }
###
	    $alias = "B_$ {c}_$$";
	    my $table = ($self->ontology->is_multivalue_class_attribute($origin,$att)) ? "$ {origin}_2_$att" : $origin;
#	    push @from, "$table AS $alias";
	    $from{"$table AS $alias"}++;
	    push @where, "$alias.$DB_ID_NAME=$root_class.$DB_ID_NAME";
	    if ($operator eq 'IS NULL') {
#		pop @from;
		delete $from{"$table AS $alias"};
		pop @where;
		push @where, "$alias.$att IS NULL";
		push @join, "LEFT JOIN $table AS $alias ON ($alias.$DB_ID_NAME=$root_class.$DB_ID_NAME)";
	    } elsif ($operator eq 'IS NOT NULL') {
		push @where, "$alias.$att IS NOT NULL";
	    } elsif ($operator eq 'IN' or $operator eq 'NOT IN') {
		my @tmp = ('?') x scalar(@{$value_ar});
		push @where, "$alias.$att $operator(" . join(",",@tmp) . ")";
		push @values, @{$value_ar};
	    } elsif ($operator eq 'MATCH') {
		$self->_throw_if_innodb_table($table, "Table '$table' does not support fulltext queries.");
		push @where, "MATCH($alias.$att) AGAINST(?)";
		push @values, $value_ar->[0];
	    } elsif ($operator eq 'MATCH IN BOOLEAN MODE') {
		$self->_throw_if_innodb_table($table, "Table '$table' does not support fulltext queries.");
		push @where, "MATCH($alias.$att) AGAINST(? IN BOOLEAN MODE)";
		push @values, $value_ar->[0];
	    } else {
		push @where, "$alias.$att $operator ?";
		push @values, $value_ar->[0];
	    }
	}
	$c++;
    }
    unless ($from{$class}++) {
	push @where, "$class.$DB_ID_NAME=$root_class.$DB_ID_NAME";
    }
    # Ensure that the root class name is the last one in the FROM bit as left join would
    # not otherwise not work in MySQL 5.
    # Sorting is to ensure that the order of tables in FROM is the same from query to
    # query. Not strictly necessary but good for own sanity.
    delete $from{$root_class};
    return [(sort {$a cmp $b} keys %from), $root_class],\@where,\@join,\@values;
}

sub find_1_directed_path_between_instances {
    my ($self,@args) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    my ($from,$to,$instructions,$max_depth,$kill_list,$filter_function) =
	$self->_rearrange([qw(
			      FROM
			      TO
			      INSTRUCTIONS
			      DEPTH
			      KILL_LIST
			      FILTER_FUNCTION
			     )],@args);
    ($from && ref($from) && $from->isa("GKB::Instance")) ||
	$self->throw("Need GKB::Instance, got '$from'.");
    $filter_function ||= sub {1;};
    $instructions =
	[
	 ['PhysicalEntity',  \&GKB::Instance::reverse_attribute_value, 'input', 'Reaction'],
	 ['Reaction', \&GKB::Instance::attribute_value, 'output', 'PhysicalEntity'],
	 ['PhysicalEntity',  \&GKB::Instance::reverse_attribute_value, 'physicalEntity', 'CatalystActivity'],
	 ['CatalystActivity', \&GKB::Instance::reverse_attribute_value, 'catalystActivity', 'Reaction'],
	 ['CatalystActivity',  \&GKB::Instance::reverse_attribute_value, 'regulator', 'PositiveRegulation'],
	 ['PhysicalEntity',  \&GKB::Instance::reverse_attribute_value, 'regulator', 'PositiveRegulation'],
	 ['Reaction',  \&GKB::Instance::reverse_attribute_value, 'regulator', 'PositiveRegulation'],
	 ['ReactionlikeEvent', \&GKB::Instance::reverse_attribute_value, 'regulatedBy','PositiveRegulation'],
	 ];
    my %seen;
    my %kill_h;
    map {$kill_h{$_->db_id}++} @{$kill_list};
    my %to;
    map {$to{$_->db_id}++} @{$to};
    my @t = ($from);
    my $cur_depth = 0;
    my $successful;
  OUTER_WHILE:
    while ((@t > 0) && ((! defined $max_depth) || ($cur_depth < $max_depth))) {
	my @instances = @t;
	@t = ();
	$cur_depth++;
	$self->debug && print "Finding path at depth $cur_depth\n";
#	print "Finding path at depth $cur_depth\n";
#	foreach my $instance (grep {! $seen{$_->db_id}++} @instances) {
	foreach my $instance (@instances) {
#	    print "$cur_depth\t", $instance->extended_displayName, "\n";
	    foreach my $ar (@{$instructions}) {
		my ($class,$f_ref,$att,$out_class) = @{$ar};
		next unless $instance->is_a($class);
		my @tmp =
		grep {&{$filter_function}($_)}
		grep {! $seen{$_->db_id}++}
		grep {! $kill_h{$_->db_id}}
		grep {$_->is_a($out_class)}
		@{&{$f_ref}($instance,$att)};
#		# Throw out generic events which have instances in the same set
#		if (@tmp and $tmp[0]->is_a('Event')) {
#		    my %h;
#		    foreach my $e (@tmp) {
#			map {$h{$_->db_id}++} @{$e->reverse_attribute_value('hasInstance')}
#		    }
#		    @tmp = grep {! $h{$_->db_id}} @tmp;
#		}
		map {$_->{'_previous_instance'} = $instance} @tmp;
		map {$_->{'_previous_instance_instruction'} = $ar} @tmp;
#		map {$_ == $to and $successful = 1 and last OUTER_WHILE} @tmp;
		foreach (@tmp) {
		    if ($to{$_->db_id}) {
			$successful = $_;
			last OUTER_WHILE;
		    }
		}
		push @t, @tmp;
	    }
	}
    }
    my @order;
    my @instruction_order;
    if ($successful) {
	my $current = $successful;
	while ($current->{'_previous_instance'}) {
	    unshift @order, $current;
	    unshift @instruction_order, $current->{'_previous_instance_instruction'};
	    my $new_current = $current->{'_previous_instance'};
	    delete $current->{'_previous_instance'};
	    delete $current->{'_previous_instance_instruction'};
	    $current = $new_current;
	    last if ($current == $from);
	}
	unshift @order, $current;
    }
    return \@order, \@instruction_order;
}

sub fetch_table_type {
    my ($self,$tbl_name) = @_;
    my ($sth,$res) = $self->execute("SHOW TABLE STATUS LIKE '$tbl_name'");
    my $ar = $sth->fetchrow_arrayref;
    $sth->finish;
    return $ar->[1];
}

sub fetch_col_type {
    my ($self,$tbl_name,$col_name) = @_;
    my ($sth,$res) = $self->execute("DESCRIBE $tbl_name");
    while(my $hash_ref = $sth->fetchrow_hashref) {
	if ($hash_ref->{'Field'} eq $col_name) {
	    $sth->finish;
	    return $hash_ref->{'Type'};
	}
    }
}

sub _throw_if_innodb_table {
    my ($self,$tbl,$msg) = @_;
    if (lc($self->fetch_table_type($tbl)) eq 'innodb') {
	$self->throw($msg);
    }
}

sub fetch_root_class_table_type {
    my $self = shift;
    return lc($self->fetch_table_type($self->ontology->root_class));
}

sub back_up_db {
    my ($dba,$user,$pass) = @_;
    my $host = $dba->host;
    my $port = $dba->port;
    my $db = $dba->db_name;
    # Untaint db name
    if ($db =~ /^(\w+)$/) {
	$db = $1;
    } else {
	die "Invalid dodgy database name '$db'.";
    }
    require GKB::Config;
    $user ||= $GKB::Config::GK_DB_USER;
    $pass ||= $GKB::Config::GK_DB_PASS;
#    my $outdir = '/home/vastrik/database_backups';
#    my $outdir = '/scratch/vastrik/backups';
    my $outdir = $GKB::Config::DB_BACKUP_DIR;
    unless (-d $outdir) {
	$dba->throw("Backup directory '$outdir' does not exist.");
    }
    unless (-w $outdir) {
	$dba->throw("Not permitted to write to backup dir '$outdir'.")
    }
    my ($sec, $min, $hour, $day, $month, $year) = localtime();
    $year  += 1900;
    $month += 1;
    my $outpath = $outdir . '/' . sprintf( "$ {db}.%04d-%02d-%02d_%02d:%02d:%02d.sql", $year, $month, $day, $hour, $min, $sec );
    dump_db($user,$pass,$host,$port,$db,$outpath);
}

sub dump_db {
    my ($user,$pass,$host,$port,$db,$outpath) = @_;
    $db || confess("Need db name.\n");
    $outpath || confess("Need path to output file.\n");
    my $cmd = "/usr/bin/mysqldump --opt --lock-tables $db";
    if ($user) {
	$cmd .= " -u $user";
    }
    if (defined $pass) {
	$cmd .= " -p$pass";
    }
    if ($host) {
	$cmd .= " -h $host";
    }
    if ($port) {
	$cmd .= " -P $port";
    }
    if ($outpath =~ /\.gz$/) {
	$cmd .= " | /bin/gzip -c > $outpath";
    } else {
	$cmd .= " > $outpath";
    }
    my $retval = system("$cmd");
    if ($retval) {
	throw("Database backup failed with return value $retval.\n");
    }
}

sub create_InstanceEdit_for_existing_Person {
    my $self = shift;
    my $person_db_id = shift || $self->throw("Need DB_ID of a Person instance.");
     my $person = $self->fetch_instance_by_attribute('Person',[[$DB_ID_NAME, [$person_db_id]]])->[0] ||
#    my $person = $self->fetch_instance_by_db_id($person_db_id)->[0] ||
	$self->throw("Person with DB_ID $person_db_id not found in the database.");
    my ($sth,$res) = $self->execute('SELECT NOW()');
    my $tmp = $sth->fetchrow_arrayref->[0];
    $tmp =~ s/\D//g;
    my $ie = GKB::Instance->new
	(
	 -CLASS => 'InstanceEdit',
	 -ONTOLOGY => $self->ontology,
	 -DBA => $self,
	 'author' => [$person],
	 'dateTime' => $tmp,
	);
    $ie->inflated(1);
    return $ie;
}

sub create_InstanceEdit_for_effective_user {
    my $self = shift;
    my @a = getpwnam($ENV{USER});
    if (my ($firstname,$surname) = $a[6] =~ /^(\w+)\s+(\w+)/) {
	my $person = $self->fetch_instance_by_attribute('Person',[['firstname', [$firstname]],['surname',[$surname]]])->[0] ||
	    $self->throw("Person $firstname $surname not found in the database.");
	my ($sth,$res) = $self->execute('SELECT NOW()');
	my $tmp = $sth->fetchrow_arrayref->[0];
	$tmp =~ s/\D//g;
	my $ie = GKB::Instance->new
	    (
	     -CLASS => 'InstanceEdit',
	     -ONTOLOGY => $self->ontology,
	     -DBA => $self,
	     'author' => [$person],
	     'dateTime' => $tmp,
	    );
	$ie->inflated(1);
	return $ie;
    } else {
	$self->throw("Unable to parse first name and surname form '$a[6]'");
    }
}

sub create_config_table {
    my $self = shift;
    $self->execute(qq/CREATE TABLE IF NOT EXISTS $CONFIG_TABLE (parameter VARCHAR(64) NOT NULL PRIMARY KEY, value LONGBLOB) ENGINE=/ . $self->table_type);
}

sub exists_table {
    my ($self,$tbl_name) = @_;
    my ($sth,$res) = $self->execute("SHOW TABLES LIKE '$tbl_name'");
    if (@{$sth->fetchall_arrayref}) {
	return 1;
    }
    return;
}

sub fetch_parameters {
    my $self = shift;
    return unless($self->exists_table($CONFIG_TABLE));
    $self->remove_cached_db_parameters;
    my ($sth,$res) = $self->execute(qq/SELECT parameter,value FROM $CONFIG_TABLE/);
    while (my $ar = $sth->fetchrow_arrayref) {
	$self->db_parameter($ar->[0],$ar->[1]);
    }
}

sub db_parameter {
    my $self = shift;
    my $param = shift;
    if (@_) {
	$self->{'paramater'}->{$param} = shift;
    }
    return $self->{'paramater'}->{$param};
}

sub remove_cached_db_parameters {
    my $self = shift;
    $self->{'paramater'} = {};
}

sub store_db_parameters {
    my $self = shift;
    if ($self->{'paramater'}) {
	while (my ($param,$value) = each %{$self->{'paramater'}}) {
	    $self->store_db_parameter_value($param,$value);
	}
    }
}

sub store_db_parameter_value {
    my ($self,$param,$value) = @_;
    my ($sth,$res) = $self->execute("SELECT * FROM $CONFIG_TABLE WHERE parameter = ?", $param);
    if (@{$sth->fetchall_arrayref}) {
	$self->execute("UPDATE $CONFIG_TABLE SET value = ? WHERE parameter = ?", $value,$param);
    } else {
	$self->execute("INSERT INTO $CONFIG_TABLE SET parameter = ?, value = ?", $param,$value);
    }
}

sub get_max_db_id {
    my $self = shift;
    my ($sth,$res) = $self->execute("SELECT MAX($DB_ID_NAME) FROM " . $self->ontology->root_class);
    return $sth->fetchall_arrayref->[0]->[0];
}

sub store_dummy_db_id {
    my ($self,$db_id) = @_;
    $self->execute('INSERT INTO ' . $self->ontology->root_class . " SET $DB_ID_NAME=$db_id");
}

# Dummy method to prevent problems which sometimes occur because
# objects of type DBAdaptor and ReactomeDBAdaptor sometimes get
# used in inappropriate places.
sub fetch_frontpage_species {
    my $self = shift;
    return [];
}


# Given a list reference database name, find all species who have
# reference entities associated with those databases
sub species_for_ref_dbs {
    my $self  = shift;
    my @rdbs = @_;
    my $query = 'SELECT DB_ID FROM DatabaseObject WHERE _displayName LIKE ?';

    my @rids;
    for my $rdb (@rdbs) {
	my ($sth) = $self->execute($query,$rdb);
        while (my $id = $sth->fetchrow_arrayref) {
            push @rids, $id->[0];
	}
    }

    $query = <<"END";
    SELECT DISTINCT(do._displayName)
    FROM ReferenceEntity re, ReferenceSequence rs, DatabaseObject do
    WHERE re.referenceDatabase = ?
    AND do.DB_ID = rs.species
    AND rs.DB_ID = re.DB_ID
END
;

    my @out;
    for my $rdb (@rids) {
	my ($sth) = $self->execute($query,$rdb);
        while (my $id = $sth->fetchrow_arrayref) {
            push @out, $id->[0];
        }
    }

    my %out = map {$_ => 1} @out;

    return sort keys %out;
}


1;
