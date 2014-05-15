=head1 NAME

GKB::AddLinks::BioModelsEventToDatabaseIdentifier

=head1 SYNOPSIS

Insert links from Pathways and ReactionlikeEvents to
corresponding BioModels models.

=head1 DESCRIPTION

Right now, it is not possible to create links to BioModels
reactions, so links from ReactionlikeEvents can only be
linked to models.

Original code lifted from the script add_biomodels_links.sh, from David.

=head1 SEE ALSO

GKB::DBAdaptor

=head1 AUTHOR

David Croft E<lt>croft@ebi.ac.ukE<gt>

Copyright (c) 2008 European Bioinformatics Institute and Cold Spring
Harbor Laboratory.

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

package GKB::AddLinks::BioModelsEventToDatabaseIdentifier;

use GKB::Config;
use GKB::AddLinks::Builder;
use strict;
use vars qw(@ISA $AUTOLOAD %ok_field);

@ISA = qw(GKB::AddLinks::Builder);

sub AUTOLOAD {
    my $self = shift;
    my $attr = $AUTOLOAD;
    $attr =~ s/.*:://;
    return unless $attr =~ /[^A-Z]/;  # skip DESTROY and all-cap methods
    $self->throw("invalid attribute method: ->$attr()") unless $ok_field{$attr};
    $self->{$attr} = shift if @_;
    return $self->{$attr};
}  

sub new {
    my($pkg) = @_;

   	# Get class variables from superclass and define any new ones
   	# specific to this class.
	$pkg->get_ok_field();

   	my $self = $pkg->SUPER::new();
   	
    return $self;
}

# Needed by subclasses to gain access to object variables defined in
# this class.
sub get_ok_field {
	my ($pkg) = @_;
	
	%ok_field = $pkg->SUPER::get_ok_field();

	return %ok_field;
}

sub buildPart {
	my ($self) = @_;
	
	print STDERR "\n\nBioModelsEventToDatabaseIdentifier.buildPart: entered\n";
	
	$self->timer->start($self->timer_message);

	# Set up some handy varaibles
	my $cabigr3 = "$GK_ROOT_DIR/../caBIG/caBIGR3";
	if (!(-e $cabigr3)) {
		print STDERR "BioModelsEventToDatabaseIdentifier.buildPart: WARNING - root directory for caBIGR3, $cabigr3, not available!\n";
		$cabigr3 = "/usr/local/caBIG/caBIGR3";
		if (!(-e $cabigr3)) {
			print STDERR "BioModelsEventToDatabaseIdentifier.buildPart: WARNING - root directory for caBIGR3, $cabigr3, not available, aborting!!\n";
			return;
		}
	}
	my $lib = "$cabigr3/lib";
	my $src = "$cabigr3/src";
	
	print STDERR "BioModelsEventToDatabaseIdentifier.buildPart: lib=$lib, src=$src\n";

	# Collect stuff for classpath
	my $axis_cp = "$lib/axis/axis.jar:$lib/axis/jaxrpc.jar:$lib/axis/saaj.jar:$lib/axis/wsdl4j-1.5.1.jar";
	my $batik_cp = "$lib/batik/xml-apis.jar";
	my $biomodels_cp = "$lib/biomodels/biomodelswslib-commons-discovery-1.8.jar:$lib/biomodels/biomodelswslib-single-1.8.jar:$lib/biomodels/junit.jar";
	my $commons_math_cp = "$lib/commons-math/commons-math-1.1.jar";
	my $ensj_cp = "$lib/ensj/ensj-39.2.jar";
	my $freemarker_cp = "$lib/freemarker/freemarker.jar";
	my $hibernate_cp = "$lib/hibernate/hibernate3.jar:$lib/hibernate/commons-logging-1.0.4.jar";
	my $jdom_cp = "$lib/jdom/jaxen-core.jar:$lib/jdom/jaxen-jdom.jar:$lib/jdom/jdom.jar:$lib/jdom/saxpath.jar";
	my $jung_cp = "$lib/jung/colt.jar:$lib/jung/commons-collections-3.2.jar:$lib/jung/concurrent.jar:$lib/jung/jung-1.7.6.jar";
	my $junit_cp = "$lib/junit/junit.jar";
	my $kegg_cp = "$lib/kegg/keggapi.jar";
	my $log4j_cp = "$lib/log4j/log4j-1.2.12.jar";
	my $mysql_cp = "$lib/mysql/mysql-connector-java-3.1.12-bin.jar";
	my $owl_cp = "$lib/owl/commons-logging.jar";
	my $reactome_cp = "$lib/reactome/reactome.jar:$lib/reactome/caBigR3.jar";
#	my $r3_cp = "$lib/r3.jar";
	my $sjsxp_cp = "$lib/sjsxp/jsr173_api.jar:$lib/sjsxp/sjsxp.jar";
	my $smile_cp = "$lib/smile/jsmile.jar";
	my $weka_cp = "$lib/weka/weka.jar";
	my $xfire_cp = "$lib/xfire/xfire-all-1.2.2.jar";
	my $cp = "$axis_cp:$batik_cp:$biomodels_cp:$commons_math_cp:$ensj_cp:$freemarker_cp:$hibernate_cp:$jdom_cp:$jung_cp:$junit_cp:$kegg_cp:$log4j_cp:$mysql_cp:$owl_cp:$reactome_cp:$sjsxp_cp:$smile_cp:$weka_cp:$xfire_cp:$src";
	
	chdir $cabigr3;

	print STDERR "BioModelsEventToDatabaseIdentifier.buildPart: cabigr3=$cabigr3\n";
	print STDERR "BioModelsEventToDatabaseIdentifier.buildPart: CWD=" . &Cwd::cwd() . "\n";
	
	# Compile the java source
	print "Compiling....\n";
	my $compile_success = system("./compile.sh");
	if ($compile_success!=0) {
		print STDERR "BioModelsEventToDatabaseIdentifier.buildPart: WARNING - could not compile java code, aborting!!\n";
		return;
	}

	print STDERR "BioModelsEventToDatabaseIdentifier.buildPart: compilation complete\n";
	
	my $args = "";
	my $db_params = $self->builder_params->get_db_params();
	if (defined $db_params->[0]) {
		$args .= " -db " . $db_params->[0];
	}
	if (defined $db_params->[1]) {
		$args .= " -host " . $db_params->[1];
	}
	if (defined $db_params->[2]) {
		$args .= " -port " . $db_params->[2];
	}
	if (defined $db_params->[3]) {
		$args .= " -user " . $db_params->[3];
	}
	if (defined $db_params->[4]) {
		$args .= " -pass " . $db_params->[4];
	}
	chomp($args);
	
	print STDERR "BioModelsEventToDatabaseIdentifier.buildPart: args=$args\n";

	# Run command
	my $command = "java -Xmx500m -classpath $cp org.reactome.biomodels.InsertBioModelsXrefsIntoRelease $args";
	print "Running command=$command\n";
	system($command);
	
	$self->timer->stop($self->timer_message);
	$self->timer->print();
}

1;

