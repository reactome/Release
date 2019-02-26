package GKB::Config_Species;

use strict;

use vars qw(@ISA @EXPORT @species %species_info);

use Exporter();
@ISA=qw(Exporter);

#defines the order in which the species should be handled for orthology inference
@species = qw(hsap ddis pfal spom scer cele sscr btau cfam mmus rnor ggal xtro drer dmel);

%species_info = (
    # 'atha' => {
    #     'name' => ['Arabidopsis thaliana'],
    #     'refdb' => {'
    #         dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Arabidopsis_PROTEIN'],
    #         'url' => 'http://plants.ensembl.org/Arabidopsis_thaliana',
    #         'access' => 'http://plants.ensembl.org/Arabidopsis_thaliana/Transcript/ProteinSummary?peptide=###ID###',
    #         'ensg_access' => 'http://plants.ensembl.org/Arabidopsis_thaliana/geneview?gene=###ID###&db=core'
    #     },
    #     'alt_refdb' => {
    #         'dbname' => ['TAIR'],
    #         'url' => 'http://www.arabidopsis.org/index.jsp',
    #         'access' => 'http://www.arabidopsis.org/servlets/TairObject?type=locus&name=###ID###',
    #         'alt_id' => '-TAIR-G'
    #     }, #regex to remove suffix from ENSG identifier
    #     'group' => 'Fungi/Plants',
    #     'mart_url' => 'http://plants.ensembl.org/biomart/martservice',
    #     'mart_virtual_schema' => 'plants_mart',
    #     'mart_group' => 'athaliana_eg_gene'
    # },
    'btau' => {
        'name' => ['Bos taurus'],
        'refdb' => {
            'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Bos_taurus_PROTEIN'],
            'url' => 'http://www.ensembl.org/Bos_taurus/Info/Index/',
            'access' => 'http://www.ensembl.org/Bos_taurus/Transcript/ProteinSummary?peptide=###ID###',
            'ensg_access' => 'http://www.ensembl.org/Bos_taurus/geneview?gene=###ID###&db=core'
        },
        'group' => 'Vertebrate',
        'compara' => 'core',
        'mart_group' => 'btaurus_gene_ensembl'
    },
    'cele' => {
        'name' => ['Caenorhabditis elegans'],
        'refdb' => {
            'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_C_elegans_PROTEIN'],
            'url' => 'http://metazoa.ensembl.org/Caenorhabditis_elegans/Info/Index',
            'access' => 'http://metazoa.ensembl.org/Caenorhabditis_elegans/Transcript/ProteinSummary?peptide=###ID###',
            'ensg_access' => 'http://metazoa.ensembl.org/Caenorhabditis_elegans/geneview?gene=###ID###&db=core'
        },
        'alt_refdb' => {
            'dbname' => ['Wormbase'],
            'url' => 'http://www.wormbase.org',
            'access' => 'http://www.wormbase.org/db/gene/gene?name=###ID###'
        },
        'group' => 'Metazoan',
        'compara' => 'core',
        'mart_group' => 'celegans_gene_ensembl'
    },
    'cfam' => {
        'name' => ['Canis familiaris'],
        'refdb' => {
            'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Canis_PROTEIN'],
            'url' => 'http://www.ensembl.org/Canis_familiaris/Info/Index/',
            'access' => 'http://www.ensembl.org/Canis_familiaris/Transcript/ProteinSummary?peptide=###ID###',
            'ensg_access' => 'http://www.ensembl.org/Canis_familiaris/geneview?gene=###ID###&db=core'
        },
        'group' => 'Vertebrate',
        'compara' => 'core',
        'mart_group' => 'cfamiliaris_gene_ensembl'
    },
    'ddis' => {
        'name' => ['Dictyostelium discoideum'],
        'refdb' => {
            'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Dictyostelium discoideum_PROTEIN'],
            'url' => 'http://protists.ensembl.org/Dictyostelium_discoideum/Info/Index',
            'access' => 'http://protists.ensembl.org/Dictyostelium_discoideum/Transcript/ProteinSummary?peptide=###ID###',
            'ensg_access' => 'http://protists.ensembl.org/Dictyostelium_discoideum/geneview?gene=###ID###&db=core'
        },
        'alt_refdb' => {
            'dbname' => ['dictyBase'],
            'url' => 'http://www.dictybase.org/',
            'access' => 'http://dictybase.org/db/cgi-bin/search/search.pl?query=###ID###'
        },
        'group' => 'Eukaryotes',
        'mart_url' => 'http://protists.ensembl.org/biomart/martservice',
        'mart_virtual_schema' => 'protists_mart',
        'mart_group' => 'ddiscoideum_eg_gene'
    },
    'drer' => {
        'name' => ['Danio rerio'],
        'refdb' => {
            'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Danio_rerio_PROTEIN'],
            'url' => 'http://www.ensembl.org/Danio_rerio/Info/Index/',
            'access' => 'http://www.ensembl.org/Danio_rerio/Transcript/ProteinSummary?peptide=###ID###',
            'ensg_access' => 'http://www.ensembl.org/Danio_rerio/geneview?gene=###ID###&db=core'
        },
        'group' => 'Vertebrate',
        'compara' => 'core',
        'mart_group' => 'drerio_gene_ensembl'
    },
    'dmel' => {
        'name'  => ['Drosophila melanogaster'],
        'refdb' => {
            'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_D_melanogaster_PROTEIN'],
            'url' => 'http://metazoa.ensembl.org/Drosophila_melanogaster',
            'access' => 'http://metazoa.ensembl.org/Drosophila_melanogaster/Transcript/ProteinSummary?peptide=###ID###',
            'ensg_access' => 'http://metazoa.ensembl.org/Drosophila_melanogaster/geneview?gene=###ID###&db=core'
        },
        'alt_refdb' => {
            'dbname' => ['Flybase'],
            'url' => 'http://www.flybase.net',
            'access' => 'http://flybase.net/.bin/fbidq.html?###ID###'
        },
        'group' => 'Metazoan',
        'compara' => 'core',
        'mart_group' => 'dmelanogaster_gene_ensembl'
    },
    'ggal' => {
        'name' => ['Gallus gallus'],
        'refdb' => {
            'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Gallus_gallus_PROTEIN'],
            'url' => 'http://www.ensembl.org/Gallus_gallus/Info/Index/',
            'access' => 'http://www.ensembl.org/Gallus_gallus/Transcript/ProteinSummary?peptide=###ID###',
            'ensg_access' => 'http://www.ensembl.org/Gallus_gallus/geneview?gene=###ID###&db=core'
        },
        'group' => 'Vertebrate',
        'compara' => 'core',
        'mart_group' => 'ggallus_gene_ensembl'
    },
    'hsap' => {
        'name' => ['Homo sapiens'],
        'refdb' => {
            'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Homo_sapiens_PROTEIN'],
            'url' => 'http://www.ensembl.org/Homo_sapiens/Info/Index/',
            'access' => 'http://www.ensembl.org/Homo_sapiens/Transcript/ProteinSummary?peptide=###ID###',
            'ensg_access' => 'http://www.ensembl.org/Homo_sapiens/geneview?gene=###ID###&db=core'
        },
        'group' => 'Human',
        'mart_group' => 'hsapiens_gene_ensembl'
    },
    # 'mtub' => {
    #     'name' => ['Mycobacterium tuberculosis'],
    #     'pan_name' => 'mycobacterium_tuberculosis_h37rv_asm19595v2',
    #     'refdb' => {
    #         'dbname' => ['ENSEMBL', 'Ensembl', 'Ensembl', 'ENSEMBL_M_tuberculosis_PROTEIN'],
    #         'url' => 'http://bacteria.ensembl.org/Mycobacterium/M_tuberculosis_H37Rv/Info/Index',
    #         'access' => 'http://bacteria.ensembl.org/Mycobacterium/M_tuberculosis_H37Rv/Transcript/ProteinSummary?peptide=###ID###',
    #         'ensg_access' => 'http://bacteria.ensembl.org/Mycobacterium/M_tuberculosis_H37Rv/geneview?gene=###ID###&db=core'
    #     },
    #     'group' => 'Eubacteria',
    #     'mart_group' => 'myc_30_gene',
    #     'prokaryote' => 1
    # },
    'mmus' => {
        'name' => ['Mus musculus'],
        'refdb' => {
            'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Mus_musculus_PROTEIN'],
            'url' => 'http://www.ensembl.org/Mus_musculus/Info/Index/',
            'access' => 'http://www.ensembl.org/Mus_musculus/Transcript/ProteinSummary?peptide=###ID###',
            'ensg_access' => 'http://www.ensembl.org/Mus_musculus/geneview?gene=###ID###&db=core'
        },
        'group' => 'Vertebrate',
        'compara' => 'core',
        'mart_group' => 'mmusculus_gene_ensembl'
    },
    # 'osat' => {
    #     'name' => ['Oryza sativa'],
    #     'refdb' => {
    #         'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Oryza_sativa_PROTEIN'],
    #         'url' => 'http://plants.ensembl.org/Oryza_sativa/Info/Index',
    #         'access' => 'http://plants.ensembl.org/Oryza_sativa/Transcript/ProteinSummary?peptide=###ID###',
    #         'ensg_access' => 'http://plants.ensembl.org/Oryza_sativa/geneview?gene=###ID###&db=core'
    #     },
    #     'group' => 'Fungi/Plants',
    #     'mart_url' => 'http://plants.ensembl.org/biomart/martservice',
    #     'mart_virtual_schema' => 'plants_mart',
    #     'mart_group' => 'osativa_eg_gene'
    # },
    'pfal' => {
        'name' => ['Plasmodium falciparum'],
        'refdb' => {
            'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_P_falciparum_PROTEIN'],
            'url' => 'http://protists.ensembl.org/Plasmodium_falciparum/Info/Index',
            'access' => 'http://protists.ensembl.org/Plasmodium_falciparum/Transcript/ProteinSummary?peptide=###ID###',
            'ensg_access' => 'http://protists.ensembl.org/Plasmodium_falciparum/geneview?gene=###ID###&db=core'
        },
        'alt_refdb' => {
            'dbname' => ['PlasmoDB'],
            'url' => 'http://plasmodb.org',
            'access' => 'http://plasmodb.org/plasmodb/servlet/sv?page=gene&source_id=###ID###'
        },
        'group' => 'Eukaryotes',
        'mart_url' => 'http://protists.ensembl.org/biomart/martservice',
        'mart_virtual_schema' => 'protists_mart',
        'mart_group' => 'pfalciparum_eg_gene'
    },
    'rnor' => {
        'name' => ['Rattus norvegicus'],
        'refdb' => {
            'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Rattus_norvegicus_PROTEIN'],
            'url' => 'http://www.ensembl.org/Rattus_norvegicus/Info/Index/',
            'access' => 'http://www.ensembl.org/Rattus_norvegicus/Transcript/ProteinSummary?peptide=###ID###',
            'ensg_access' => 'http://www.ensembl.org/Rattus_norvegicus/geneview?gene=###ID###&db=core'
        },
        'group' => 'Vertebrate',
        'compara' => 'core',
        'mart_group' => 'rnorvegicus_gene_ensembl'
    },
    'scer' => {
        'name' => ['Saccharomyces cerevisiae'],
        'refdb' => {
            'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_S_cerevisiae_PROTEIN'],
            'url' => 'http://fungi.ensembl.org/Saccharomyces_cerevisiae/Info/Index',
            'access' => 'http://fungi.ensembl.org/Saccharomyces_cerevisiae/Transcript/ProteinSummary?peptide=###ID###',
            'ensg_access' => 'http://fungi.ensembl.org/Saccharomyces_cerevisiae/geneview?gene=###ID###&db=core'
        },
        'alt_refdb' => {
            'dbname' => ['SGD', 'Saccharomyces Genome Database'],
            'url' => 'http://www.yeastgenome.org',
            'access' => 'http://db.yeastgenome.org/cgi-bin/locus.pl?locus=###ID###'
        },
        'group' => 'Fungi/Plants',
        'compara' => 'core',
        'mart_group' => 'scerevisiae_gene_ensembl'
    },
    'spom' => {
        'name' => ['Schizosaccharomyces pombe'],
        'refdb' => {
            'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_S_pombe_PROTEIN'],
            'url' => 'http://fungi.ensembl.org/Schizosaccharomyces_pombe/Info/Index',
            'access' => 'http://fungi.ensembl.org/Schizosaccharomyces_pombe/Transcript/ProteinSummary?peptide=###ID###',
            'ensg_access' => 'http://fungi.ensembl.org/Schizosaccharomyces_pombe/geneview?gene=###ID###&db=core'
        },
        'alt_refdb' => {
            'dbname' => ['GeneDB'],
            'url' => 'http://www.genedb.org/genedb/pombe',
            'access' => 'http://www.genedb.org/genedb/Search?organism=pombe&name=###ID###'
        },
        'group' => 'Fungi/Plants',
        'mart_url' => 'http://fungi.ensembl.org/biomart/martservice',
        'mart_virtual_schema' => 'fungi_mart',
        'mart_group' => 'spombe_eg_gene'
    },
    'sscr' => {
        'name' => ['Sus scrofa'],
        'refdb' => {
            'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Sus_scrofa_PROTEIN'],
            'url' => 'http://www.ensembl.org/Sus_scrofa/Info/Index/',
            'access' => 'http://www.ensembl.org/Sus_scrofa/Transcript/ProteinSummary?peptide=###ID###',
            'ensg_access' => 'http://www.ensembl.org/Sus_scrofa/geneview?gene=###ID###&db=core'
        },
        'group' => 'Vertebrate',
        'mart_group' => 'sscrofa_gene_ensembl',
        'compara' => 'core'
    },
    # 'tgut' => {
    #     'name' => ['Taeniopygia guttata'],
    #     'refdb' => {
    #         'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Taeniopygia_guttata_PROTEIN'],
    #         'url' => 'http://www.ensembl.org/Taeniopygia_guttata/Info/Index/',
    #         'access' => 'http://www.ensembl.org/Taeniopygia_guttata/Transcript/ProteinSummary?peptide=###ID###',
    #         'ensg_access' => 'http://www.ensembl.org/Taeniopygia_guttata/geneview?gene=###ID###&db=core'
    #     },
    #     'group' => 'Vertebrate',
    #     'mart_group' => 'tguttata_gene_ensembl',
    #     'compara' => 'core'
    # },
    'xtro' => {
        'name' => ['Xenopus tropicalis'],
        'refdb' => {
            'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Xenopus_tropicalis_PROTEIN'],
            'url' => 'http://www.ensembl.org/Xenopus_tropicalis/Info/Index/',
            'access' => 'http://www.ensembl.org/Xenopus_tropicalis/Transcript/ProteinSummary?peptide=###ID###',
            'ensg_access' => 'http://www.ensembl.org/Xenopus_tropicalis/geneview?gene=###ID###&db=core'
        },
        'group' => 'Vertebrate',
        'compara' => 'core',
        'mart_group' => 'xtropicalis_gene_ensembl'
    },
);

@EXPORT = qw(@species %species_info);

