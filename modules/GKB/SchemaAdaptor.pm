package GKB::SchemaAdaptor;

use strict;
use vars qw(@ISA);
use GKB::DBAdaptor;
use GKB::SchemaClassAttribute;
use GKB::Schema;

@ISA = qw(GKB::DBAdaptor);

my $CLASS_TABLE = 'SchemaClass';
my $ATTRIBUTE_TABLE = 'SchemaAttribute';
my $SCHEMA_TABLE = 'DataModel';

sub new {
    my($pkg, $dbh) = @_;
    my $self = bless {}, $pkg;
    $dbh || $self->throw("Need database handle.");
    $self->db_handle($dbh);
    $self->instance_cache(new GKB::InstanceCache);
    $self->db_internal_id_type('INTEGER(10) UNSIGNED');
    $self->primary_key_type('INTEGER(10) NOT NULL AUTO_INCREMENT PRIMARY KEY');
    return $self;
}

sub store_schema {
    my ($self,$schema) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    $self->execute("DROP TABLE IF EXISTS $SCHEMA_TABLE");
    my $table_type = $self->fetch_table_type('Ontology');
    my $create_statement = 
	"CREATE TABLE IF NOT EXISTS $SCHEMA_TABLE (\n" .
	"thing VARCHAR(255) NOT NULL,\n" .
	"thing_class ENUM('SchemaClass','SchemaClassAttribute','Schema'),\n" .
	"property_name VARCHAR(255) NOT NULL,\n" .
	"property_value TEXT,\n" .
	"property_value_type ENUM('INTEGER','SYMBOL','STRING','INSTANCE','SchemaClass','SchemaClassAttribute'),\n" .
	"property_value_rank INTEGER(10) UNSIGNED DEFAULT 0\n" .
	")\nENGINE = $table_type";
    #print "$create_statement\n";
    $self->execute($create_statement);
    my $statement = "Insert INTO $SCHEMA_TABLE SET thing=?,thing_class=?,property_name=?,property_value=?,property_value_type=?,property_value_rank=?";
    #print "$statement\n";
    my $sth = $self->prepare($statement);
    $self->_store_Schema($sth,$schema);
}

sub _store_Schema {
    my ($self,$sth,$schema) = @_;
    foreach my $property (qw(pont_file_content pprj_file_content pins_file_stub _timestamp)) {
	$sth->execute($schema->id,'Schema',$property,$schema->get_property_value($property)->[0],'STRING',0);
    }
    my $c = 0;
    foreach my $attribute (@{$schema->get_property_value('attributes')}) {
#	$sth->execute($schema->id,'Schema','attributes',$attribute->id,'SchemaClassAttribute',$c++);
	$self->_store_SchemaAttribute($sth,$attribute);
    }
    $c = 0;
    foreach my $class (@{$schema->get_property_value('classes')}) {
#	$sth->execute($schema->id,'Schema','classes',$class->id,'SchemaClass',$c++);
	$self->_store_SchemaClass($sth,$class);
    }
}

sub _store_SchemaClass {
    my ($self,$sth,$class) = @_;
    $sth->execute($class->id,'SchemaClass','name',$class->get_property_value('name')->[0],'STRING',0);
    $sth->execute($class->id,'SchemaClass','abstract',$class->get_property_value('abstract')->[0],'SYMBOL',0);
    my $c = 0;
    foreach my $superClass (@{$class->get_property_value('super_classes')}) {
	$sth->execute($class->id,'SchemaClass','super_classes',$superClass->id,'SchemaClass',$c++);
    }
 #   $c = 0;
    foreach my $attribute (@{$class->get_property_value('attributes')}) {
#	$sth->execute($class->id,
#		      'SchemaClass',
#		      'attributes',
#		      $attribute->id,
#		      'SchemaClassAttribute',
#		      $c++);
	$self->_store_SchemaAttribute($sth,$attribute);
    }
}

sub _store_SchemaAttribute {
    my ($self,$sth,$attribute) = @_;
    $sth->execute($attribute->id,'SchemaClassAttribute','max_cardinality',$attribute->get_property_value('max_cardinality')->[0],'INTEGER',0);
    $sth->execute($attribute->id,'SchemaClassAttribute','min_cardinality',$attribute->get_property_value('min_cardinality')->[0],'INTEGER',0);
    $sth->execute($attribute->id,'SchemaClassAttribute','multiple',$attribute->get_property_value('multiple')->[0],'SYMBOL',0);
#    if ($attribute->get_property_value('origin')->[0]) {
#	$sth->execute($attribute->id,'SchemaClassAttribute','origin',$attribute->get_property_value('origin')->[0]->id,'INSTANCE',0);
#    }
    $sth->execute($attribute->id,'SchemaClassAttribute','name',$attribute->get_property_value('name')->[0],'STRING',0);
    $sth->execute($attribute->id,'SchemaClassAttribute','type',$attribute->get_property_value('type')->[0],'STRING',0);
    $sth->execute($attribute->id,'SchemaClassAttribute','db_col_type',$attribute->get_property_value('db_col_type')->[0],'STRING',0);
    if($attribute->get_property_value('class')->[0]) {
	$sth->execute($attribute->id,'SchemaClassAttribute','class',$attribute->get_property_value('class')->[0]->id,'SchemaClass',0);
    }
    if (@{$attribute->get_property_value('value_defines_instance')}) {
	$sth->execute($attribute->id,'SchemaClassAttribute','value_defines_instance',$attribute->get_property_value('value_defines_instance')->[0],'SYMBOL',0);
    }
    if (@{$attribute->get_property_value('category')}) {
	$sth->execute($attribute->id,'SchemaClassAttribute','category',$attribute->get_property_value('category')->[0],'SYMBOL',0);
    }
    my $c = 0;
    foreach my $value (@{$attribute->get_property_value('inverse_slots')}) {
	$sth->execute($attribute->id,'SchemaClassAttribute','inverse_slots',$value->id,'SchemaClassAttribute',$c++);
    }
    $c = 0;
    foreach my $value (@{$attribute->get_property_value('allowed_classes')}) {
	$sth->execute($attribute->id,'SchemaClassAttribute','allowed_classes',$value->id,'SchemaClass',$c++);
    }
    my $default = $attribute->get_property_value('default')->[0];
    if (defined $default) {
	my $property_value_type = 'STRING';
	my $type = $attribute->get_property_value('type')->[0];
	if ($type eq 'db_integer_type') {
	    $property_value_type = 'INTEGER';
#	} elsif ($type eq 'db_string_type') {
#	    $property_value_type = 'STRING';
	} elsif ($type eq 'db_float_type') {
	    $property_value_type = 'FLOAT';
	} elsif ($type eq 'db_enum_type') {
	    $property_value_type = 'SYMBOL';
	}
	$sth->execute($attribute->id,'SchemaClassAttribute','default', $default, $property_value_type ,0);
    }
}

sub fetch_schema {
    my ($self) = @_;
    $self->debug && print "", (caller(0))[3], "\n";
    my ($sth,$res) = $self->execute("SELECT thing,thing_class,property_name,property_value,property_value_type,property_value_rank FROM $SCHEMA_TABLE");
    my $schema = GKB::Schema->new();
    $schema->cache->store('Schema',$schema);
    $self->schema($schema);
    while (my $ar = $sth->fetchrow_arrayref) {
	my $thing = $schema->schema_item_from_cache_or_new($ar->[0],$ar->[1]);
	my $property = $ar->[2];
	if ($ar->[4] =~ /^Sc/) {
	    $thing->add_property_value_by_rank($ar->[2],$schema->schema_item_from_cache_or_new($ar->[3],$ar->[4]),$ar->[5]);
	} else {
	    $thing->add_property_value_by_rank($ar->[2],$ar->[3],$ar->[5]);
	}
    }
    $schema->_initialize;
    return $schema;
}

sub schema {
    my $self = shift;
    if (@_) {
	$self->{'schema'} = shift;
    }
    return $self->{'schema'};
}

sub create_database {
    my ($self,$name) = @_;
    $name || $self->throw("Need name to create a database.");
    $self->db_name && $self->throw("This adaptor is already connected to database " . $self->db_name .
				   ". Use another instance of DBAdaptor to create another db.");
    $self->execute("CREATE DATABASE IF NOT EXISTS $name");
    $self->db_name($name);
    $self->execute("USE $name");
}

1;
