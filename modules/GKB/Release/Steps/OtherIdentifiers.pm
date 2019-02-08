package GKB::Release::Steps::OtherIdentifiers;

use GKB::Config_Species;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

has '+gkb' => ( default => "gkbdev" );
has '+passwords' => ( default => sub { ['mysql'] } );
has '+directory' => ( default => "$release/other_identifiers" );
has '+mail' => ( default => sub {
                    my $self = shift;
                    return {
                        'to' => '',
                        'subject' => $self->name,
                        'body' => "",
                        'attachment' => ""
                    };
                }
);

override 'run_commands' => sub {
    my ($self, $gkbdir) = @_;
    
    $self->cmd("Backing up database",[["mysqldump -u$user -p$pass $db > $db.before_other_identifiers.dump"]]);
    $self->cmd("Adding other identifiers",[["perl add_identifiers_from_mart.pl -user $user -pass $pass -db $db > other_identifiers.$version.out 2> other_identifiers.$version.err"]]);
    $self->cmd("Backing up database",[["mysqldump -u$user -p$pass $db > $db.after_other_identifiers.dump"]]);
};

override 'post_step_tests' => sub {
    my $self = shift;
    
    my @errors = super();
    push @errors, _check_other_identifier_count_for_all_species();
    return @errors;
};

sub _check_other_identifier_count_for_all_species {
    my @errors;
    foreach my $species (@species) {
        my $species_name = $species_info{$species}->{'name'}->[0];
        my @current_RGPs = _get_RGPs_with_other_identifiers($db, $species_name);
        my $current_RGP_count = scalar @current_RGPs;
        my @previous_RGPs = _get_RGPs_with_other_identifiers("test_reactome_$prevver", $species_name);
        my $previous_RGP_count = scalar @previous_RGPs;
 
        if ($current_RGP_count < $previous_RGP_count) {
            my $error = "$species_name has fewer reference gene products with other identifiers compared to the previous release: $version - $current_RGP_count; $prevver - $previous_RGP_count";
            push @errors, $error;
            releaselog($error);
            next;
        }
               
        my $current_other_identifier_count = _get_total_other_identifier_count(@current_RGPs);
        my $previous_other_identifier_count = _get_total_other_identifier_count(@previous_RGPs);
        
        if ($current_other_identifier_count < $previous_other_identifier_count) {
            my $error = "$species_name has fewer total other identifiers compared to the previous release: $version - $current_other_identifier_count; $prevver - $previous_other_identifier_count";
            push @errors, $error;
            releaselog($error);
            next;
        }
        
        releaselog("$species_name other identifier addition successful (RGPs with other identifiers/Total other identifiers): " .
                   "$version - $current_RGP_count/$current_other_identifier_count; $prevver - $previous_RGP_count/$previous_other_identifier_count");
    }
    return @errors;
}

# RGP is the database class ReferenceGeneProduct
sub _get_RGPs_with_other_identifiers {
    my $db = shift;
    my $species_name = shift;
    
    my $reference_gene_products = get_dba($db)->fetch_instance_by_remote_attribute('ReferenceGeneProduct', [['species._displayName', '=', [$species_name]]]);
    return grep {$_->otherIdentifier->[0]} @{$reference_gene_products};
}

sub _get_total_other_identifier_count {
    my @reference_gene_products = @_;
    
    my $total_other_identifier_count = 0;
    foreach my $reference_gene_product (@reference_gene_products) {
        $total_other_identifier_count += scalar @{$reference_gene_product->otherIdentifier};
    }
    return $total_other_identifier_count;
}

1;
