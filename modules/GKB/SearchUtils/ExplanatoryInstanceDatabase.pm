package GKB::SearchUtils::ExplanatoryInstanceDatabase;

=head1 NAME

GKB::SearchUtils::ExplanatoryInstanceDatabase

=head1 SYNOPSIS

A bunch of methods for maintaining and mining a database of explanatory
instances

=head1 DESCRIPTION

Finding explanatory instances is a time-consuming business, it uses a
lot of DBAdaptor->fetch* methods.  This class contains methods for
creating a database containing a single table that contains pre-generated
mappings from all instances and their explanatory instances.

In theory, you should be able to retrieve the information at a later date
more rapidly than it was generated, but in reality that isn't so.
TODO: could this be optimised, e.g. by using primary keys?

Also, the code bombs unpredictably while trying to do a SELECT statement,
so I have abandoned it for the time being.

=head1 SEE ALSO

GKB::SearchUtils::ExplanatoryInstances

=head1 AUTHOR

David Croft E<lt>croft@ebi.ac.ukE<gt>

Copyright (c) 2008 European Bioinformatics Institute and Cold Spring
Harbor Laboratory.

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

use strict;
use vars qw(@ISA $AUTOLOAD %ok_field);
use Bio::Root::Root;
use Carp;
use DBI;
use GKB::Config;
use GKB::DBAdaptor;

@ISA = qw(Bio::Root::Root);

for my $attr
    (qw(
    	db_name
    	host
    	port
    	user
    	pass
    	driver
    	dbh_host
    	dbh_database
	) ) { $ok_field{$attr}++; }

sub AUTOLOAD {
    my $self = shift;
    my $attr = $AUTOLOAD;
    $attr =~ s/.*:://;
    return unless $attr =~ /[^A-Z]/;  # skip DESTROY and all-cap methods
    $self->throw("invalid attribute method: ->$attr()") unless $ok_field{$attr};
    $self->{$attr} = shift if @_;
    return $self->{$attr};
}  

# User can specify as arguments either:
# * a DBAdaptor object OR
# * db_name, host, port, user, password
# In the latter case, host, port, user, password are optional.
sub new {
    my($pkg, $db_name_or_dba, $host, $port, $user, $pass) = @_;
    
    my $self = bless {}, $pkg;
    
    if (!(defined $db_name_or_dba)) {
    	$self->throw("Need db_name or a DBAdaptor object");
    }
    
    my $db_name = $db_name_or_dba;
    if (scalar($db_name_or_dba) =~ /DBAdaptor/) {
    	$db_name = $db_name_or_dba->db_name . "_ei";
    	$host = $db_name_or_dba->host;
    	$port = $db_name_or_dba->port;
    	$user = $db_name_or_dba->user;
    	$pass = $GK_DB_PASS; # from Config.pm
    }
    
    $self->db_name($db_name);
    if (!(defined $host)) {
    	$host = 'localhost';
    }
    $self->host($host);
    if (!(defined $port)) {
    	$port = '3306';
    }
    $self->port($port);
    $self->user($user);
    $self->pass($pass);
    $self->driver('mysql');
    
    print STDERR "db_name=$db_name, host=$host, port=$port, user=$user, pass=$pass\n";
    
    return $self;
}

# Needed by subclasses
sub get_ok_field {
	return %ok_field;
}

# Returns 1 if the explanatory instance database already exists,
# 0 otherwise.
sub exists_database {
	my ($self) = @_;
	
	# Works by trying to establish a connection to the database
	
	# Run in an own thread, so that if the connection fails, it doesn't
	# break the program.
	my $dbh = $self->connect_database();
	
	if (defined $dbh) {
		return 1;
	} else {
		return 0;
	}
}

# Connect to explanatory instance host server.  Returns a database
# handler with limited capabilities, e.g. creating and dropping
# databases.
sub connect_host {
	my ($self) = @_;
	
	my $dbh = $self->dbh_host;
	if (!(defined $dbh)) {
		# Run in an own shell, so that if the connection fails, it doesn't
		# break the program.
		eval {
			my $dsn = "DBI:" . $self->driver . ":host=" . $self->host . ";port=" . $self->port;
			$dbh = DBI->connect($dsn, $self->user, $self->pass, { RaiseError => 1});
		};
		$self->dbh_host($dbh);
	}
	
	return $dbh;
}

sub disconnect_host {
	my ($self) = @_;
	
	my $dbh = $self->dbh_host();
	if (defined $dbh) {
		$dbh->disconnect();
		$self->dbh_host(undef);
	}
}

# Connect to explanatory instance database.  Returns a database handler
# where USE has already been applied to the explanatory instance database,
# and commands like SELECT will operate on this database.
sub connect_database {
	my ($self) = @_;
	
	print STDERR "connect_database: entered\n";
	
	my $dbh = $self->dbh_database;

	print STDERR "connect_database: dbh=$dbh\n";
	
	if (!(defined $dbh)) {
		# Run in an own shell, so that if the connection fails, it doesn't
		# break the program.
		eval {
			my $dsn = "DBI:" . $self->driver . ":host=" . $self->host . ";port=" . $self->port . ";database=" . $self->db_name;
			
			print STDERR "connect_database: dsn=$dsn\n";
			
			$dbh = DBI->connect($dsn, $self->user, $self->pass, { RaiseError => 1});
			
			print STDERR "connect_database: connected!!!\n";
		};
		$self->dbh_database($dbh);
	}
	
	return $dbh;
}

sub disconnect_database {
	my ($self) = @_;
	
	my $dbh = $self->dbh_database();
	if (defined $dbh) {
		print STDERR "disconnect_database: do a dbh disconnect\n";
		
		$dbh->disconnect();
		
		print STDERR "disconnect_database: empty dbh\n";
		
		$self->dbh_database(undef);
	}
}

# Creates an explanatory instance database and connects to it.  Also
# initiates the table structure.
#
# N.B. If a database with this name already exists, all its contents
# will be erased, so think carefully before you use this!!!!
#
# Returns a database handler
# where USE has already been applied to the explanatory instance database,
# and commands like SELECT will operate on this database.
sub create_database {
	my ($self) = @_;
	
	my $dbh = $self->connect_host();
	
	my $statement = "CREATE DATABASE IF NOT EXISTS " . $self->db_name;
	my $sth = $dbh->prepare($statement);
	$sth->execute();
	$statement = "DROP TABLE IF EXISTS EXPLANATORY_INSTANCES ";
	$sth = $dbh->prepare($statement);
	$sth->execute();
	$statement = "CREATE TABLE EXPLANATORY_INSTANCES (DB_ID INTEGER(10), SPECIES_DB_ID INTEGER(10), EXPLANATORY_DB_ID INTEGER(10))";
	$sth = $dbh->prepare($statement);
	$sth->execute();
	
	$self->disconnect_host();
	
	return $self->connect_database();
}

# Inserts one row into the EXPLANATORY_INSTANCES table.
# You must run
# connect_database before you use this method.
sub insert {
	my ($self, $db_id, $species_db_id, $explanatory_db_id) = @_;
	
	my $dbh = $self->dbh_database;
	my $statement = "INSERT INTO EXPLANATORY_INSTANCES VALUES ($db_id, $species_db_id, $explanatory_db_id)";
	my $sth = $dbh->prepare($statement);
	$sth->execute();
}

# Uses the supplied instance DB_ID and optional species DB_ID
# to select explanatory instance DB_IDs.  Returns an array of
# explanatory instance DB_IDs.
# You must run
# connect_database before you use this method.
sub select {
	my ($self, $db_id, $species_db_id) = @_;
	
	my $dbh = $self->dbh_database;
	my $where = "DB_ID='$db_id'";
	if (defined $species_db_id) {
		$where .= " AND SPECIES_DB_ID='$species_db_id'";
	}
	my $statement = "SELECT EXPLANATORY_DB_ID FROM EXPLANATORY_INSTANCES WHERE $where";
	
	print STDERR "select: statement=$statement\n";
	
	# TODO: the code randomly bombs at this point, needs fixing.
	
	my $sth = $dbh->prepare($statement);
	
	print STDERR "select: execute SQL\n";
	
	$sth->execute();
	
	print STDERR "select: extract data\n";
	
	my @explanatory_db_ids = ();
	my @row;
	while (@row = $sth->fetchrow_array) {
		push(@explanatory_db_ids, $row[0]);
	}
	
	print STDERR "select: db_id=$db_id, species_db_id=$species_db_id, explanatory_db_ids=@explanatory_db_ids\n";
	
	return @explanatory_db_ids;
}

1;

