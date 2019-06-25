#!/usr/bin/perl
use strict;
use warnings;

use lib '/usr/local/gkb/modules';

use GKB::Config;
use GKB::DBAdaptor;

use autodie;
use Carp;
use Data::Dumper;
use feature qw/state/;
use Getopt::Long;
use List::MoreUtils qw/any/;

my($user, $host, $pass, $port, $db, $reactome_version, $num_output_files, $output_dir);
(@ARGV) || die "Usage: $0 -user db_user -host db_host -pass db_pass -port db_port -db db_name -version reactome_version -num_output_files number_of_output_files -output_dir dir_for_output_files\n";
&GetOptions(
   "user:s" => \$user,
   "host:s" => \$host,
   "pass:s" => \$pass,
   "port:i" => \$port,
   "db:s" => \$db,
   "version" => \$reactome_version,
   "num_output_files:i" => \$num_output_files,
   "output_dir:s" => \$output_dir
);
$db || die "Need database name (-db).\n";
$num_output_files ||= 1;
$output_dir ||= 'archive';

if (!$reactome_version) {
    ($reactome_version) = $db =~ /(\d+)$/;
    $reactome_version || die "Need Reactome version (-version).\n";
}

my $dba = GKB::DBAdaptor->new(
   -dbname => $db,
   -user   => $user || $GK_DB_USER,
   -host   => $host || $GK_DB_HOST, # host where mysqld is running
   -pass   => $pass || $GK_DB_PASS
);

print "Retreiving uniprot mapping " . `date`;
my %uniprot_id_mapping = get_uniprot_id_mapping($dba);
print "Uniprot mapping retrieved " . `date`;

print "Generating protein mapping file " . `date`;
generate_protein_mapping_file(
   "$output_dir/proteins_version$reactome_version",
   \%uniprot_id_mapping
);
print "Total number of proteins: ". (scalar keys %uniprot_id_mapping) ."\n";

print "Generating XML gene files " . `date`;
my @errors = generate_XML_gene_files(
   "$output_dir/gene_reactome$reactome_version.xml",
   \%uniprot_id_mapping,
   $num_output_files
);
print "XML gene files generated " . `date`;

write_error_report(\@errors, "$output_dir/geneentrez_$reactome_version.err");

sub get_uniprot_id_mapping {
   my $dba = shift || confess "DBAdaptor.pm instance is required\n";

   my %uniprot_id_mapping;
   foreach my $uniprot_instance (get_uniprot_reference_gene_products($dba)) {
      my $uniprot_id = $uniprot_instance->Identifier->[0];
      foreach my $ncbi_gene_id (get_ncbi_gene_ids_from_uniprot_instance($uniprot_instance)) {
         if (!(instance_in_list($uniprot_instance, $uniprot_id_mapping{$uniprot_id}{'database_instances'}))) {
            push @{$uniprot_id_mapping{$uniprot_id}{'database_instances'}}, $uniprot_instance;
         }
         push @{$uniprot_id_mapping{$uniprot_id}{'ncbi_gene_id'}}, $ncbi_gene_id;
      }
   }

   return %uniprot_id_mapping;
}

sub get_uniprot_reference_gene_products {
   my $dba = shift || confess "DBAdaptor.pm instance is required\n";

   my $uniprot_instances = $dba->fetch_instance_by_remote_attribute('ReferenceGeneProduct', [['referenceDatabase.name', '=', ['UniProt']]])
      || confess("No ReferenceDatabase with name 'UniProt'.\n");

   # For testing
   #my $uniprot_instances = [get_instances_by_db_id($dba, [164714, 93237, 164730, 164713, 93240])];

   return @{$uniprot_instances};
}

sub get_instances_by_db_id {
   my $dba = shift;
   my $db_ids = shift;

   return map { $dba->fetch_instance_by_db_id($_)->[0] } @{$db_ids};
}

sub get_ncbi_gene_ids_from_uniprot_instance {
   my $uniprot_instance = shift || confess "UniProt reference gene product instance is required\n";

   my @ncbi_gene_ids =
      map { $_->Identifier->[0] }
      grep { $_->referenceDatabase->[0]->displayName eq 'NCBI Gene' }
      @{$uniprot_instance->referenceGene};

   return @ncbi_gene_ids;
}

sub instance_in_list {
   my $instance = shift;
   my $instance_list = shift || [];

   return any { $_->db_id == $instance->db_id } @{$instance_list};
}

sub generate_protein_mapping_file {
   my $protein_file_name = shift;
   my $uniprot_id_mapping = shift;

   open(my $proteins_fh, '>', $protein_file_name);  # this is just to compare with prot_gene file from 1proteinentrez.pl
   print $proteins_fh "UniProt ID\tGene id\n\n";
   foreach my $uniprot_id (keys %{$uniprot_id_mapping}) {
      foreach my $ncbi_gene_id (@{$uniprot_id_mapping->{$uniprot_id}{'ncbi_gene_id'}}) {
         print $proteins_fh "$uniprot_id\t$ncbi_gene_id\n";
      }
   }
   close($proteins_fh);
}

sub generate_XML_gene_files {
   my $base_xml_file_name = shift;
   my $uniprot_id_mapping = shift;
   my $number_of_outputs = shift;

   my @generation_errors;
   if ($number_of_outputs == 1) {
      push @generation_errors, generate_XML_gene_file($uniprot_id_mapping, $base_xml_file_name);
   } else {
      my $hash_number = 0;
      foreach my $uniprot_id_mapping_sub_hash (@{split_hash($uniprot_id_mapping, $number_of_outputs)}) {
         $hash_number++;
         (my $xml_gene_file_name = $base_xml_file_name) =~ s/(.xml)$/\-$hash_number$1/;
         push @generation_errors, generate_XML_gene_file($uniprot_id_mapping_sub_hash, $xml_gene_file_name);
      }
   }

   # Filtering for defined values is done since undefined values
   # may have been pushed to the errors array
   return grep { defined } @generation_errors;
}

sub generate_XML_gene_file {
   my $uniprot_id_mapping = shift;
   my $gene_file_name = shift;

   my @errors;

   open(my $gene_xml_fh, '>', $gene_file_name);
   print $gene_xml_fh get_XML_header();
   print $gene_xml_fh "<LinkSet>\n";
   foreach my $uniprot_id (keys %{$uniprot_id_mapping}) {
      my @ncbi_gene_ids = @{$uniprot_id_mapping->{$uniprot_id}{'ncbi_gene_id'}};
      foreach my $uniprot_instance (@{$uniprot_id_mapping->{$uniprot_id}{'database_instances'}}) {
         my @events_with_uniprot_id = get_events($uniprot_instance);
         if (!@events_with_uniprot_id) {
            push @errors, "$uniprot_id \n".$uniprot_instance->extended_displayName . " participates in Event(s) but no top Pathway can be found, i.e. there seem to be a pathway which contains or is an instance of itself.\n";
            next;
         }

         my @pathways_with_uniprot_id = grep { is_top_level_pathway ($_) } @events_with_uniprot_id;
         foreach my $ncbi_gene_id (@ncbi_gene_ids) {
            print $gene_xml_fh get_link_XML($ncbi_gene_id, "&entity.base.url;", $uniprot_id, "Reactome Entity:$uniprot_id");

            foreach my $pathway (@pathways_with_uniprot_id) {
               print $gene_xml_fh get_link_XML($ncbi_gene_id, "&event.base.url;", $pathway->stableIdentifier->[0]->identifier->[0], "Reactome Event:".fix_name($pathway->Name->[0]));
            }
         }
      }
   }
   print $gene_xml_fh "</LinkSet>\n";
   close($gene_xml_fh);

   return @errors;
}

sub get_XML_header {
   return <<HEADER;
<?xml version="1.0"?>
<!DOCTYPE LinkSet PUBLIC "-//NLM//DTD LinkOut 1.0//EN"
"http://www.ncbi.nlm.nih.gov/entrez/linkout/doc/LinkOut.dtd"
[
<!ENTITY entity.base.url "http://www.reactome.org/content/query?q=UniProt:">
<!ENTITY event.base.url "http://www.reactome.org/PathwayBrowser/#">
]>
HEADER
}

sub get_events {
   my $reference_gene_product = shift;

   return @{$reference_gene_product->follow_class_attributes(
      -INSTRUCTIONS =>
      {
         $reference_gene_product->class => {'reverse_attributes' => [qw(referenceEntity referenceSequence)]},
         'PhysicalEntity' => {'reverse_attributes' => [qw(hasComponent hasMember hasCandidate input output physicalEntity inferredFrom)]},
         'CatalystActivity' => {'reverse_attributes' =>[qw(catalystActivity regulator)]},
         'Event' => {'reverse_attributes' =>[qw(hasComponent hasEvent hasMember)]}
      },
      -OUT_CLASSES => ['Event']
   )};
}

sub get_link_XML {
   my $entrez_gene = shift;
   my $base = shift;
   my $rule = shift;
   my $url_name = shift;

   my $link_id = get_next_link_id();

return <<XML;
   <Link>
      <LinkId>$link_id</LinkId>
      <ProviderId>4914</ProviderId>
      <ObjectSelector>
         <Database>Gene</Database>
         <ObjectList>
            <ObjId>$entrez_gene</ObjId>
         </ObjectList>
      </ObjectSelector>
      <ObjectUrl>
         <Base>$base</Base>
         <Rule>$rule</Rule>
         <UrlName>$url_name</UrlName>
      </ObjectUrl>
   </Link>
XML
}

sub get_next_link_id {
   state $link_id = 0;
   $link_id++;
   return $link_id;
}

sub is_top_level_pathway {
   my $instance = shift;

   return $instance->reverse_attribute_value('frontPageItem')->[0] ? 1 : 0;
}

# Adapted from https://stackoverflow.com/questions/27403978/split-a-hash-into-many-hashes
sub split_hash {
   my $hash_to_split = shift;
   my $number_of_desired_hashes = shift;

   my @hash_keys = keys %$hash_to_split;
   my $sub_hash_size = get_split_hash_size(scalar @hash_keys, $number_of_desired_hashes);

   my @split_hashes;
   # NOTE: @hash_keys is mutated here by splicing out chunks for each iteration.
   # Be aware it will be fully emptied after the while loop (in case a code change requires it later on)
   while (my @hash_keys_subset = splice(@hash_keys, 0, $sub_hash_size)) {
      push @split_hashes, { map { $_ => $hash_to_split->{$_} } @hash_keys_subset };
   }

   return \@split_hashes;
}

sub get_split_hash_size {
   my $original_hash_size = shift;
   my $number_of_desired_hashes = shift;

   my $hash_size = $original_hash_size / $number_of_desired_hashes;
   return ($hash_size == int($hash_size)) ? $hash_size : int($hash_size + 1);
}

sub fix_name {
   my ($str)= @_;

   if ($str =~ /amino acids/) {
      $str = "Metabolism of nitrogenous molecules";
   }

   if ($str =~ /Cycle, Mitotic/) {
      $str = "Cell Cycle (Mitotic)";
   }

   if ($str =~ /L13a-mediated translational/) {
      $str = "L13a-mediated translation";
   }

   if ($str =~ /Abortive initiation after/){
      $str = "Abortive initiation";
   }

   if ($str =~ /Formation of the Cleavage and Polyadenylation/){
      $str = "Cleavage and Polyadenylation";
   }

   if ($str =~ /energy metabolism/){
      $str = "Energy Metabolism";
   }

   if ($str =~ /sugars/){
      $str = "Metabolism of sugars";
   }

   return $str;
}

sub write_error_report {
   my $errors = shift;
   my $error_file = shift;

   open (my $error_fh, '>', $error_file);
   if (@{$errors}) {
      print $error_fh "$_\n" foreach @{$errors};
      print $error_fh "total number of errors: " . scalar @{$errors} . "\n";
   }
   close($error_fh);
}
