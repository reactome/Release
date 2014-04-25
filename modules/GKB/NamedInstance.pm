package GKB::NamedInstance;

# Copyright 2002 Imre Vastrik <vastrik@ebi.ac.uk>

=head

GKB::NamedInstance

A Perl module for making and setting display names for GKB::Instance:s. Display name is
held as the value of attribute '_displayName' on the root (i.e. class DatabaseObject) level
and is stored in DatabaseObject._displayName.

The main benefit of having display names is ease and speed of their retrieval. This is
important when retrieving all instances of a certain class for, say, making a popup menu
or scrolling list.

For most of the classes display name is just the 1st value of some attribute (like
'name' or 'assertionText'), however there are some for which need a composite name made
up of values of 2 (or more) attributes. So this is a class (well, a set of classes)
handles the naming procedure.

The base class tries to handle most of the cases:
- if the instance has attribute 'name' the 1st value of this is used
- if the isntance has attribute 'assertionText' the 1st value of this is used
- 1st values of defining attributes are concatenated with space.

However, if none of them is good you just have to write a method 'set_displayName' for
the class of interest which makes the name, sets '_displayName' attribute and returns
the new name (the latter is not really necessary but all the other 'set_displayName'
functions do it).


=cut

use strict;
use vars qw(@ISA);
use GKB::Instance;
@ISA = qw(GKB::Instance);

sub new {
    my ($pkg,@args) = @_;
    my ($self) = $pkg->SUPER::_rearrange
	([qw(
	     INSTANCE
     )], @args);
    (ref($self) && $self->isa("GKB::Instance")) ||
	$pkg->throw("Need GKB::Instance, got '$self'.");
    if ($pkg eq 'GKB::NamedInstance' && _subclass_loaded($self->class)) {
	my $destination = "GKB::NamedInstance::" . $self->class;
	return $destination->new(@args);
    }
    bless $self, $pkg;
    $self->set_displayName;
#    print "<PRE>", $self->class_and_id, "\t", $self->attribute_value('_displayName')->[0], "</PRE>\n";
    return $self;
}

sub _subclass_loaded {
    return $main::GKB::NamedInstance::{$_[0] . '::'};
}

sub set_displayName {
    my ($self) = @_;
    $self->debug && print "<PRE>", join("\t", (caller(0))[3], $self,  $self->class, ($self->db_id || '')), "</PRE>\n";
    if ($self->is_valid_attribute('name') && $self->attribute_value('name')->[0]) {
	$self->attribute_value('_displayName', $self->attribute_value('name')->[0]);
    } else {
	my @tmp = $self->ontology->list_class_defining_attributes($self->class);
#	if ($self->ontology->class($self->class)->{'check'}->{'all'}) {
#	    push @tmp, keys %{$self->ontology->class($self->class)->{'check'}->{'all'}};
#	}
#	if ($self->ontology->class($self->class)->{'check'}->{'any'}) {
#	    push @tmp, keys %{$self->ontology->class($self->class)->{'check'}->{'any'}};
#	}
	my $name = join (" ",
			 map {($self->is_instance_type_attribute($_))
				  ? $self->attribute_value($_)->[0]->displayName
				  : $self->attribute_value($_)->[0]
			      }
			 grep {$self->attribute_value($_)->[0]} @tmp);
	if ($name) {
	    $self->attribute_value('_displayName', $name);
	} else {
		if (!$self->db_id && !$self->id) {
	    	$self->warn("Unable to create displayName for instance [".$self->class.":NO ID ASSIGNED].");
		} else {
	    	$self->warn("Unable to create displayName for instance [".$self->class.":".($self->db_id||$self->id)."].");
		}
	}
    }
    return $self->attribute_value('_displayName')->[0];
}


1;

package GKB::NamedInstance::ModifiedResidue;
use vars qw(@ISA);
use strict;
@ISA = qw(GKB::NamedInstance);

sub set_displayName {
    my ($self) = @_;
    $self->debug && print join("\t", (caller(0))[3], $self,  $self->class, ($self->db_id || '')), "\n";
    my $displayName;
    if ($self->PsiMod->[0]) {
	$displayName = $self->PsiMod->[0]->Name->[0] . ' at ';
	$displayName .= 'unknown position' unless ($self->Coordinate->[0]);
    } elsif ($self->is_valid_attribute('modification')) { #this attribute is now obsolete, keep for backward compatibility
	$displayName = (($self->Modification->[0]) ? $self->Modification->[0]->displayName : 'unknown modification') . ' on ';
	$displayName .= 'unknown ' unless ($self->Coordinate->[0]);
	$displayName .= (($self->Residue->[0]) ? $self->Residue->[0]->displayName : 'residue') . ' ';
    } else {
	$displayName = 'unknown modification' . ' at ';
	$displayName .= 'unknown position' unless ($self->Coordinate->[0]);
    }
    $displayName .= ($self->Coordinate->[0]) if ($self->Coordinate->[0]);
#    $displayName .= ' of ' . (($self->ReferenceSequence->[0]) ? $self->ReferenceSequence->[0]->displayName : 'unknown sequence');
    $self->attribute_value('_displayName',$displayName);
    return $displayName;
}

package GKB::NamedInstance::GroupModifiedResidue;
use vars qw(@ISA);
use strict;
@ISA = qw(GKB::NamedInstance);

sub set_displayName {
    my ($self) = @_;
    $self->debug && print join("\t", (caller(0))[3], $self,  $self->class, ($self->db_id || '')), "\n";
    my $displayName;
    if ($self->PsiMod->[0]) {
        $displayName = $self->PsiMod->[0]->Name->[0];
	$displayName .= ($self->Modification->[0]?(' ('.$self->Modification->[0]->displayName.')'):'') . ' at ';
        $displayName .= 'unknown position' unless ($self->Coordinate->[0]);
    } elsif ($self->is_valid_attribute('residue')) { #this attribute is now obsolete, keep for backward compatibility                                                                   
        $displayName = (($self->Modification->[0]) ? $self->Modification->[0]->displayName : 'unknown modification') . ' on ';
        $displayName .= 'unknown ' unless ($self->Coordinate->[0]);
        $displayName .= (($self->Residue->[0]) ? $self->Residue->[0]->displayName : 'residue') . ' ';
    } else {
        $displayName = 'unknown modification' . ' at ';
        $displayName .= 'unknown position' unless ($self->Coordinate->[0]);
    }
    $displayName .= ($self->Coordinate->[0]) if ($self->Coordinate->[0]);
#    $displayName .= ' of ' . (($self->ReferenceSequence->[0]) ? $self->ReferenceSequence->[0]->displayName : 'unknown sequence');
    $self->attribute_value('_displayName',$displayName);
    return $displayName;
}

package GKB::NamedInstance::ReplacedResidue;
use vars qw(@ISA);
use strict;
@ISA = qw(GKB::NamedInstance);

sub set_displayName {
    my ($self) = @_;
    $self->debug && print join("\t", (caller(0))[3], $self,  $self->class, ($self->db_id || '')), "\n";
    my $displayName;
    if ($self->PsiMod->[0]) {
        $displayName = $self->PsiMod->[0]->Name->[0]; #the first element indicates which residue has been removed
	$displayName =~ s/\s+removal//;
	$displayName .= ($self->Coordinate->[0]) ? ' '.$self->Coordinate->[0] : ' n?';
	if ($self->PsiMod->[1]) {
	    my @tmp = map {$_->Name->[0]} @{$self->PsiMod};
	    shift @tmp;
	    foreach (@tmp) {
		s/\s+residue//;
	    }
	    $displayName .= ' replaced with '. join(' - ', @tmp); #the remaining elements indicate the replacing residues
	} 
    } elsif ($self->is_valid_attribute('modification')) { #this attribute is now obsolete, keep for backward compatibility
	$displayName = (($self->Modification->[0]) ? $self->Modification->[0]->displayName : 'unknown modification') . ' at ';
	$displayName .= ($self->Coordinate->[0]) ? $self->Coordinate->[0] : 'unknown position';
    } else {
	$displayName = 'unknown replacement' .' at ';
	$displayName .= ($self->Coordinate->[0]) ? $self->Coordinate->[0] : 'unknown position';
    }
#    $displayName .= ' of ' . (($self->ReferenceSequence->[0]) ? $self->ReferenceSequence->[0]->displayName : 'unknown sequence');
    $self->attribute_value('_displayName',$displayName);
    return $displayName;
}

package GKB::NamedInstance::PsiMod;
use vars qw(@ISA);
use strict;
@ISA = qw(GKB::NamedInstance);

sub set_displayName {
    my ($self) = @_;
    $self->debug && print join("\t", (caller(0))[3], $self,  $self->class, ($self->db_id || $self->id || '')), "\n";
    my $dn;
    if (my $tmp = $self->Name->[0]) {
        $dn = $tmp;
    }
    if ($self->ReferenceDatabase->[0] && $self->Identifier->[0]) {
        my $tmp = '[' . $self->ReferenceDatabase->[0]->displayName . ':' . $self->Identifier->[0] . ']';
        $dn .= ($dn) ? " $tmp"  : $tmp;
    }
    $self->attribute_value('_displayName',$dn);
    return $dn;
}

package GKB::NamedInstance::DatabaseIdentifier;
use vars qw(@ISA);
use strict;
@ISA = qw(GKB::NamedInstance);

sub set_displayName {
    my ($self) = @_;
    $self->debug && print join("\t", (caller(0))[3], $self,  $self->class, ($self->db_id || $self->id || '')), "\n";
#    $self->attribute_value('_displayName',
#			   ($self->Identifier->[0] =~ /:/ ?
#			    $self->Identifier->[0] :
#			    $self->ReferenceDatabase->[0]->displayName .
#			    ":" .
#			    $self->Identifier->[0])
#			   );
    if ($self->Identifier->[0]) {
	if ($self->ReferenceDatabase->[0]) {
	    $self->attribute_value('_displayName',
				   $self->ReferenceDatabase->[0]->displayName .
				   ":" .
				   $self->Identifier->[0]);
	} else {
	    $self->attribute_value('_displayName', $self->Identifier->[0]);
	}
	return $self->attribute_value('_displayName')->[0];
    }
    $self->debug && $self->warn("Unable to set displayName for " . $self->id_string . ".");
    return;
}

package GKB::NamedInstance::SequenceDatabaseIdentifier;
use vars qw(@ISA);
use strict;
@ISA = qw(GKB::NamedInstance::DatabaseIdentifier);

package GKB::NamedInstance::Taxon;
use vars qw(@ISA);
use strict;
@ISA = qw(GKB::NamedInstance);

package GKB::NamedInstance::Species;
use vars qw(@ISA);
use strict;
@ISA = qw(GKB::NamedInstance::Taxon);

package GKB::NamedInstance::CatalystActivity;
use vars qw(@ISA);
use strict;
@ISA = qw(GKB::NamedInstance);

sub set_displayName {
    my ($self) = @_;
    $self->debug && print join("\t", (caller(0))[3], $self,  $self->class, ($self->db_id || '')), "\n";
    my $activityName = 'unknown';
    my $str = ' activity';
    if (my $a = $self->Activity->[0]) {
	$activityName = $a->displayName;
    }
    if ($activityName =~ /activity/) {
	$str = "";
    }
#    $self->attribute_value('_displayName',
#			   ($self->attribute_value('activity')->[0]
#			    ? $self->Activity->[0]->displayName
#			    : 'unknown activity' ).
#			   " of " .
#			   (($self->PhysicalEntity->[0])
#			    ? $self->PhysicalEntity->[0]->displayName
#			    : "unknown entity")
#			   );
    $self->attribute_value('_displayName',
			   $activityName . $str .
			   " of " .
			   (($self->PhysicalEntity->[0])
			    ? $self->PhysicalEntity->[0]->displayName
			    : "unknown entity")
			   );
    return $self->attribute_value('_displayName')->[0];
}

package GKB::NamedInstance::LiteratureReference;
use vars qw(@ISA);
use strict;
@ISA = qw(GKB::NamedInstance);

sub set_displayName {
    my ($self) = @_;
    $self->debug && print join("\t", (caller(0))[3], $self,  $self->class, ($self->db_id || '')), "\n";
#    $self->attribute_value('_displayName',
#			   "PMID:" .
#			   $self->PubMedIdentifier->[0]);
    $self->attribute_value('_displayName',
			   ($self->Title->[0] ?
			    $self->Title->[0] :
			    "PMID:" .
			    $self->PubMedIdentifier->[0])
			   );
    return $self->attribute_value('_displayName')->[0];
}

package GKB::NamedInstance::Regulation;
use vars qw(@ISA %mappings);
use strict;
@ISA = qw(GKB::NamedInstance);

sub set_displayName {
    my ($self) = @_;
    $self->debug && print join("\t", (caller(0))[3], $self,  $self->class, ($self->db_id || '')), "\n";
    $self->attribute_value('_displayName',
			   "'" . ($self->Regulator->[0] ? $self->Regulator->[0]->displayName : 'UNKNOWN ENTITY') .
			   "' regulates '" .
			   ($self->RegulatedEntity->[0] ? $self->RegulatedEntity->[0]->displayName : 'UNKNOWN ENTITY') . "'"
			   );
    return $self->attribute_value('_displayName')->[0];
}

package GKB::NamedInstance::PositiveRegulation;
use vars qw(@ISA);
use strict;
@ISA = qw(GKB::NamedInstance::Regulation);

sub set_displayName {
    my ($self) = @_;
    $self->debug && print join("\t", (caller(0))[3], $self,  $self->class, ($self->db_id || '')), "\n";
    $self->attribute_value('_displayName',
			   "'" . ($self->Regulator->[0] ? $self->Regulator->[0]->displayName : 'UNKNOWN ENTITY') .
			   "' positively regulates '" .
			   ($self->RegulatedEntity->[0] ? $self->RegulatedEntity->[0]->displayName : 'UNKNOWN ENTITY') . "'"
			   );
    return $self->attribute_value('_displayName')->[0];
}

package GKB::NamedInstance::Requirement;
use vars qw(@ISA);
use strict;
@ISA = qw(GKB::NamedInstance::PositiveRegulation);

sub set_displayName {
    my ($self) = @_;
    $self->debug && print join("\t", (caller(0))[3], $self,  $self->class, ($self->db_id || '')), "\n";
    $self->attribute_value('_displayName',
			   "'" . ($self->Regulator->[0] ? $self->Regulator->[0]->displayName : 'UNKNOWN ENTITY') .
			   "' is required for '" .
			   ($self->RegulatedEntity->[0] ? $self->RegulatedEntity->[0]->displayName : 'UNKNOWN ENTITY') . "'"
			   );
    return $self->attribute_value('_displayName')->[0];
}

package GKB::NamedInstance::NegativeRegulation;
use vars qw(@ISA);
use strict;
@ISA = qw(GKB::NamedInstance::Regulation);

sub set_displayName {
    my ($self) = @_;
    $self->debug && print join("\t", (caller(0))[3], $self,  $self->class, ($self->db_id || '')), "\n";
    $self->attribute_value('_displayName',
			   "'" . ($self->Regulator->[0] ? $self->Regulator->[0]->displayName : 'UNKNOWN ENTITY') .
			   "' negatively regulates '" .
			   ($self->RegulatedEntity->[0] ? $self->RegulatedEntity->[0]->displayName : 'UNKNOWN ENTITY') . "'"
			   );
    return $self->attribute_value('_displayName')->[0];
}

package GKB::NamedInstance::Figure;
use vars qw(@ISA);
use strict;
@ISA = qw(GKB::NamedInstance);

sub set_displayName {
    my ($self) = @_;
    $self->debug && print join("\t", (caller(0))[3], $self,  $self->class, ($self->db_id || '')), "\n";
    $self->attribute_value('_displayName',$self->Url->[0]);
    return $self->attribute_value('_displayName')->[0];
}

package GKB::NamedInstance::Summation;
use vars qw(@ISA);
use strict;
@ISA = qw(GKB::NamedInstance);

sub set_displayName {
    my ($self) = @_;
    $self->debug && print join("\t", (caller(0))[3], $self,  $self->class, ($self->db_id || '')), "\n";
    unless ($self->Text->[0]) {
	$self->warn("Summation w/o text: '" . $self->id_string . "'.");
	return;
    }
    (my $tmp = $self->Text->[0]) =~ s/<\/?\w+?>//g;
    $self->attribute_value('_displayName', map {"$_..."} ($tmp =~ /^(.{0,60})/));
#    $self->attribute_value('_displayName', map {"$_..."} ($self->Text->[0] =~ /^(.{0,60})/));
    return $self->attribute_value('_displayName')->[0];
}

package GKB::NamedInstance::Person;
use vars qw(@ISA);
use strict;
@ISA = qw(GKB::NamedInstance);

sub set_displayName {
    my ($self) = @_;
    $self->debug && print join("\t", (caller(0))[3], $self,  $self->class, ($self->db_id || $self->id)), "\n";
    $self->attribute_value('_displayName',
			   $self->Surname->[0] .
			   ', ' .
			   ($self->Initial->[0] || $self->Firstname->[0]));
    return $self->attribute_value('_displayName')->[0];
}


package GKB::NamedInstance::InstanceEdit;
use vars qw(@ISA);
use strict;
@ISA = qw(GKB::NamedInstance);

sub set_displayName {
    my $self = shift;
    if (my $date = $self->DateTime->[0]) {
	$date =~ s/^(\d{4})(\d{2})(\d{2}).*/$1\-$2\-$3/;
	$self->attribute_value('_displayName',
			       join(", ", (map {$_->displayName} @{$self->Author}),$date));
	return $self->attribute_value('_displayName')->[0];
    } else {
	$self->attribute_value('_displayName',
			       join(", ", map {$_->displayName} @{$self->Author}));
	return $self->attribute_value('_displayName')->[0];
    }
}

package GKB::NamedInstance::PhysicalEntity;
use vars qw(@ISA);
use strict;
@ISA = qw(GKB::NamedInstance);

sub set_displayName {
    my ($self) = @_;
    $self->debug && print join("\t", (caller(0))[3], $self,  $self->class, ($self->db_id || $self->id)), "\n";
    $self->attribute_value('_displayName',
			   $self->Name->[0] .
			   ($self->Compartment->[0] ? ' [' . join(', ', map{$_->displayName} @{$self->Compartment}) . ']' : '')
			   );
    return $self->attribute_value('_displayName')->[0];
}

package GKB::NamedInstance::SimpleEntity;
use vars qw(@ISA);
use strict;
@ISA = qw(GKB::NamedInstance::PhysicalEntity);

package GKB::NamedInstance::EntityWithAccessionedSequence;
use vars qw(@ISA);
use strict;
@ISA = qw(GKB::NamedInstance::PhysicalEntity);

package GKB::NamedInstance::OtherEntity;
use vars qw(@ISA);
use strict;
@ISA = qw(GKB::NamedInstance::PhysicalEntity);

package GKB::NamedInstance::Complex;
use vars qw(@ISA);
use strict;
@ISA = qw(GKB::NamedInstance::PhysicalEntity);

package GKB::NamedInstance::Polymer;
use vars qw(@ISA);
use strict;
@ISA = qw(GKB::NamedInstance::PhysicalEntity);

package GKB::NamedInstance::DefinedSet;
use vars qw(@ISA);
use strict;
@ISA = qw(GKB::NamedInstance::PhysicalEntity);

package GKB::NamedInstance::OpenSet;
use vars qw(@ISA);
use strict;
@ISA = qw(GKB::NamedInstance::PhysicalEntity);

package GKB::NamedInstance::CandidateSet;
use vars qw(@ISA);
use strict;
@ISA = qw(GKB::NamedInstance::PhysicalEntity);

package GKB::NamedInstance::GenomeEncodedEntity;
use vars qw(@ISA);
use strict;
@ISA = qw(GKB::NamedInstance::PhysicalEntity);

package GKB::NamedInstance::GO_CellularComponent;
use vars qw(@ISA);
use strict;
@ISA = qw(GKB::NamedInstance);

package GKB::NamedInstance::Compartment;
use vars qw(@ISA);
use strict;
@ISA = qw(GKB::NamedInstance::GO_CellularComponent);

package GKB::NamedInstance::EntityCompartment;
use vars qw(@ISA);
use strict;
@ISA = qw(GKB::NamedInstance::Compartment);

package GKB::NamedInstance::ReferenceGroupCount;
use vars qw(@ISA);
use strict;
@ISA = qw(GKB::NamedInstance);

sub set_displayName {
    my ($self) = @_;
    my $dn;
    if ($self->MaxCount->[0] == $self->MinCount->[0]) {
	$dn = $self->MaxCount->[0] . ' x ';
    } else {
	$dn = $self->MinCount->[0] . ' .. ' . $self->MaxCount->[0] . ' x ';
    }
    $dn .= $self->ReferenceGroup->[0]->displayName;
    $self->attribute_value('_displayName', $dn);
    return $dn;
}

package GKB::NamedInstance::ReferenceEntity;
use vars qw(@ISA);
use strict;
@ISA = qw(GKB::NamedInstance);

sub set_displayName {
    my ($self) = @_;
    $self->debug && print join("\t", (caller(0))[3], $self,  $self->class, ($self->db_id || $self->id || '')), "\n";
    my $dn = '';
    if ($self->ReferenceDatabase->[0]) {
	$dn =  $self->ReferenceDatabase->[0]->displayName . ':';
    }
    if ($self->Identifier->[0]) {
	$dn .= $self->Identifier->[0];
    }
    if (my $tmp = $self->Name->[0]) {
	$dn .= ' ' . $tmp;
    }
    if ($dn) {
	$self->attribute_value('_displayName',$dn);
	return $dn;
    }
    $self->debug && $self->warn("Unable to set displayName for " . $self->id_string . ".");
    return;
}

package GKB::NamedInstance::ReferenceMolecule;
use vars qw(@ISA);
use strict;
@ISA = qw(GKB::NamedInstance::ReferenceEntity);

sub set_displayName {
    my ($self) = @_;
    $self->debug && print join("\t", (caller(0))[3], $self,  $self->class, ($self->db_id || $self->id || '')), "\n";
    my $dn;
    if (my $tmp = $self->Name->[0]) {
	$dn = $tmp;
    }
    if ($self->ReferenceDatabase->[0] && $self->Identifier->[0]) {
	my $tmp = '[' . $self->ReferenceDatabase->[0]->displayName . ':' . $self->Identifier->[0] . ']';
	$dn .= ($dn) ? " $tmp"  : $tmp;
    }
    $self->attribute_value('_displayName',$dn);
    return $dn;
}

package GKB::NamedInstance::ReferenceMoleculeClass;
use vars qw(@ISA);
use strict;
@ISA = qw(GKB::NamedInstance::ReferenceEntity);

package GKB::NamedInstance::ReferenceGroup;
use vars qw(@ISA);
use strict;
@ISA = qw(GKB::NamedInstance::ReferenceEntity);

sub set_displayName {
    my ($self) = @_;
    $self->debug && print join("\t", (caller(0))[3], $self,  $self->class, ($self->db_id || $self->id || '')), "\n";
    my $dn = '';
    if ($self->Name->[0]) {
	$dn = $self->Name->[0];
	return $self->attribute_value('_displayName')->[0];
    } elsif ($self->ReferenceDatabase->[0] && $self->Identifier->[0]) {
	$dn = $self->ReferenceDatabase->[0]->displayName . ':' . $self->Identifier->[0];
    }
    $self->attribute_value('_displayName',$dn);
    return $dn;
}

package GKB::NamedInstance::ReferenceSequence;
use vars qw(@ISA);
use strict;
@ISA = qw(GKB::NamedInstance::ReferenceEntity);

sub set_displayName {
    my ($self) = @_;
    $self->debug && print join("\t", (caller(0))[3], $self,  $self->class, ($self->db_id || $self->id || '')), "\n";
#    unless ($self->ReferenceDatabase->[0] && $self->Identifier->[0]) {
#	$self->debug && $self->warn("Unable to set displayName for " . $self->id_string . ".");
#	return;
#    }
    my $dn = '';
    if ($self->ReferenceDatabase->[0]) {
	$dn =  $self->ReferenceDatabase->[0]->displayName . ':';
    }
    if ($self->Identifier->[0]) {
	$dn .= $self->Identifier->[0];
    }
    my $tmp;
    if ($tmp = $self->Name->[0]) {
	$dn .= ' ' . $tmp;
    } elsif ($tmp = $self->Description->[0]) {
	$dn .= ' ' . $tmp;
    }
    if ($dn) {
	$self->attribute_value('_displayName',$dn);
	return $dn;
    }
    $self->debug && $self->warn("Unable to set displayName for " . $self->id_string . ".");
    return;
}

package GKB::NamedInstance::ReferenceRNASequence;
use vars qw(@ISA);
use strict;
@ISA = qw(GKB::NamedInstance::ReferenceSequence);

package GKB::NamedInstance::ReferenceDNASequence;
use vars qw(@ISA);
use strict;
@ISA = qw(GKB::NamedInstance::ReferenceSequence);

# ReferencePeptideSequence is depracted, since it is no longer part of the Reactome
# data model, but it is being kept for backwards compatibility.
package GKB::NamedInstance::ReferencePeptideSequence;
use vars qw(@ISA);
use strict;
@ISA = qw(GKB::NamedInstance::ReferenceSequence);

sub set_displayName {
    my ($self) = @_;
    $self->debug && print join("\t", (caller(0))[3], $self,  $self->class, ($self->db_id || $self->id || '')), "\n";
    unless ($self->VariantIdentifier->[0]) {
	return $self->SUPER::set_displayName;
    }
    unless ($self->ReferenceDatabase->[0] && $self->VariantIdentifier->[0]) {
	$self->debug && $self->warn("Unable to set displayName for " . $self->id_string . ".");
	return;
    }
    my $dn = $self->ReferenceDatabase->[0]->displayName . ':' . $self->VariantIdentifier->[0];
    my $tmp;
    if ($tmp = $self->Name->[0]) {
	$dn .= ' ' . $tmp;
    } elsif ($tmp = $self->Description->[0]) {
	$dn .= ' ' . $tmp;
    }
    $self->attribute_value('_displayName',$dn);
    return $dn;
}

package GKB::NamedInstance::ReferenceGeneProduct;
use vars qw(@ISA);
use strict;
@ISA = qw(GKB::NamedInstance::ReferenceSequence);

package GKB::NamedInstance::ReferenceIsoform;
use vars qw(@ISA);
use strict;
@ISA = qw(GKB::NamedInstance::ReferenceGeneProduct);

sub set_displayName {
    my ($self) = @_;
    $self->debug && print join("\t", (caller(0))[3], $self,  $self->class, ($self->db_id || $self->id || '')), "\n";
    unless ($self->VariantIdentifier->[0]) {
	return $self->SUPER::set_displayName;
    }
    unless ($self->ReferenceDatabase->[0] && $self->VariantIdentifier->[0]) {
	$self->debug && $self->warn("Unable to set displayName for " . $self->id_string . ".");
	return;
    }
    my $dn = $self->ReferenceDatabase->[0]->displayName . ':' . $self->VariantIdentifier->[0];
    my $tmp;
    if ($tmp = $self->Name->[0]) {
	$dn .= ' ' . $tmp;
    } elsif ($tmp = $self->Description->[0]) {
	$dn .= ' ' . $tmp;
    }
    $self->attribute_value('_displayName',$dn);
    return $dn;
}

package GKB::NamedInstance::ReactionCoordinates;
use vars qw(@ISA);
use strict;
@ISA = qw(GKB::NamedInstance);

sub set_displayName {
    my ($self) = @_;
    $self->debug && print join("\t", (caller(0))[3], $self,  $self->class, ($self->db_id || $self->id || '')), "\n";
    my $dn;
    if (my $e = $self->LocatedEvent->[0]) {
	$dn =
	    join(',',
		 $self->SourceX->[0],
		 $self->SourceY->[0],
		 $self->TargetX->[0],
		 $self->TargetY->[0]) . '] ' .
		 $e->displayName;
	$self->attribute_value('_displayName',$dn);
    }
    return $dn;
}

package GKB::NamedInstance::StableIdentifier;
use vars qw(@ISA);
use strict;
@ISA = qw(GKB::NamedInstance);

sub set_displayName {
    my ($self) = @_;
    $self->debug && print join("\t", (caller(0))[3], $self,  $self->class, ($self->db_id || '')), "\n";
    unless ($self->Identifier && scalar($self->Identifier) =~ /ARRAY/ && $self->Identifier->[0]) {
	$self->warn("StableIdentifier w/o identifier: '" . $self->id_string . "'.");
	return '';
    }
    unless ($self->IdentifierVersion && scalar($self->IdentifierVersion) =~ /ARRAY/ && defined $self->IdentifierVersion->[0]) {
	$self->warn("StableIdentifier w/o identifierVersion: '" . $self->id_string . "'.");
	return '';
    }
    my $out = $self->Identifier->[0] . '.' . $self->IdentifierVersion->[0];
    $self->attribute_value('_displayName', $out);
    return $out;
}

package GKB::NamedInstance::Interaction;
use vars qw(@ISA);
use strict;
@ISA = qw(GKB::NamedInstance);

sub set_displayName {
    my ($self) = @_;
    $self->debug && print join("\t", (caller(0))[3], $self,  $self->class, ($self->db_id || $self->id || '')), "\n";
    my $dn = join(':', map {$_->displayName} @{$self->Interactor});
    $self->attribute_value('_displayName',$dn);
    return $dn;
}


1;

