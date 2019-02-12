#!/usr/local/bin/perl  -w
use strict;

use lib '/usr/local/gkb/modules';
use GKB::Config;

use autodie qw/:all/;
use Cwd;
use Getopt::Long;
use Data::Dumper;
use File::Basename;
use List::MoreUtils qw/uniq/;

use Log::Log4perl qw/get_logger/;
Log::Log4perl->init(\$LOG_CONF);
my $logger = get_logger(__PACKAGE__);


our($addlinks_dir, $configuration, $rebuild_jar, $user, $pass, $host, $port, $db, $person_id);

# Parse commandline
my $usage = "Usage: $0 -addlinks_dir repo_path -configuration download_only|insertion_only|full -rebuild_jar -user db_user -host db_host -pass db_pass -port db_port -db db_name -person_id person_db_id\n";
&GetOptions(
    "addlinks_dir:s" => \$addlinks_dir,
    "configuration:s" => \$configuration,
    "rebuild_jar" => \$rebuild_jar,
    "user:s" => \$user,
    "pass:s" => \$pass,
    "host:s" => \$host,
    "port:i" => \$port,
    "db:s" => \$db,
    "person_id:s" => \$person_id
);

($configuration && $configuration =~ /^download_only|insertion_only|full$/) || die "Need -configuration (download_only|insertion_only|full)\n";
$person_id || die "Need -person_id\n";

$user ||= $GK_DB_USER;
$pass ||= $GK_DB_PASS;
$host ||= $GK_DB_HOST;
$port ||= $GK_DB_PORT;
$db ||= $GK_DB_NAME;

my $cwd = getcwd;
$addlinks_dir ||= "$cwd/AddLinks";

(-e $addlinks_dir && -d $addlinks_dir) ? update_addlinks_repo($addlinks_dir) : clone_addlinks_repo($addlinks_dir);
build_jar($addlinks_dir); #if $rebuild_jar || !(-e "$addlinks_dir/target");
create_resources_symbolic_link("$addlinks_dir/scripts");
create_jar_file_symbolic_link("$addlinks_dir/scripts");
create_reports_directories("$addlinks_dir/scripts");
set_properties("$addlinks_dir/src/main/resources/db.properties",
               {"database.user" => $user, "database.password" => $pass, "database.host" => $host, "database.port" => $port, "database.name" => $db});
set_properties("$addlinks_dir/src/main/resources/auth.properties",
               {"orphanet.user" => $GK_ORPHAN_USER, "orphanet.password" => $GK_ORPHAN_PASS,
                "brenda.user" => $GK_BRENDA_USER, "brenda.password" => $GK_BRENDA_PASS,
                "dockblaster.user" => $GK_DOCKBLASTER_USER, "dockblaster.password" => $GK_DOCKBLASTER_PASS,
                "cosmic.user" => $GK_COSMIC_USER, "cosmic.password" => $GK_COSMIC_PASS
                }); # Orphanet, Brenda, Dock Blaster, and COSMIC credentials
set_properties("$addlinks_dir/src/main/resources/addlinks.properties", {"executeAsPersonID" => $person_id});
configure_application_context($configuration, "$addlinks_dir/src/main/resources/application-context.xml");
run_addlinks("$addlinks_dir/scripts");

sub update_addlinks_repo {
    my $addlinks_dir = shift;

    my $current_dir = getcwd;
    chdir $addlinks_dir;
    system("git pull");
    chdir $current_dir;
}

sub clone_addlinks_repo {
    my $addlinks_dir = shift;

    system("git clone https://github.com/reactome/add-links.git $addlinks_dir");
}

sub build_jar {
    my $addlinks_dir = shift;

    my $current_dir = getcwd;
    chdir $addlinks_dir;
    system("mvn clean package -DskipTests");
    chdir $current_dir;
}

sub create_resources_symbolic_link {
    my $addlinks_script_dir = shift;

    my $current_dir = getcwd;
    chdir $addlinks_script_dir;
    system("ln -sf ../src/main/resources resources") unless (-e 'resources');
    chdir $current_dir;
}

sub create_reports_directories {
    my $addlinks_script_dir = shift;

    my $current_dir = getcwd;
    chdir $addlinks_script_dir;
    system("mkdir -p reports/duplicateReports");
    system("mkdir -p reports/diffReports");
    chdir $current_dir;
}

sub create_jar_file_symbolic_link {
    my $addlinks_script_dir = shift;

    my $current_dir = getcwd;
    chdir $addlinks_script_dir;
    my $addlinks_jar_file = (split(/\n/, `ls ../target/AddLinks*SHADED.jar`))[0];

    system("ln -sf $addlinks_jar_file AddLinks.jar");
    chdir $current_dir;
}

sub set_properties {
    my $file_path = shift;
    my $properties = shift;

    my %initial_properties;
    open(my $read_fh, "<", "$file_path");
    my @contents = <$read_fh>;
    foreach my $line (@contents) {
        chomp $line;
        my ($key, $value) = $line =~ /^(.*)=(.*)$/;
        next unless $key;
        $initial_properties{$key} = $value;
    }
    close($read_fh);

    open(my $write_fh, ">", "$file_path.new");
    foreach my $property (uniq(keys %$properties, keys %initial_properties)) {
        my $value = $properties->{$property} || $initial_properties{$property};

        print $write_fh "$property=$value\n";
    }
    close($write_fh);

    system("mv -f $file_path.new $file_path");
}

sub configure_application_context {
    my $configuration = shift;
    my $application_context_file_path = shift;

    my $current_dir = getcwd;
    chdir dirname("$application_context_file_path");
    system("git checkout " . fileparse($application_context_file_path));
    chdir $current_dir;

    return if $configuration eq 'full';

    open(my $read_fh, '<', $application_context_file_path);
    local $/ = undef;
    my $file_contents = <$read_fh>;
    close($read_fh);

    open(my $write_fh, '>', "$application_context_file_path.new");
    $file_contents =~ s/<!--.*?-->//gms;
    if ($configuration eq 'download_only') {
        my $file_processor_filter = get_regex_for_util_list("fileProcessorFilter");
        $file_contents =~ s/$file_processor_filter/$1<!--$2-->$3/;

        my $reference_creator_filter = get_regex_for_util_list("referenceCreatorFilter");
        $file_contents =~ s/$reference_creator_filter/$1<!--$2-->$3/;

        my $reference_databases_to_link_check = get_regex_for_util_list("referenceDatabasesToLinkCheck");
        $file_contents =~ s/$reference_databases_to_link_check/$1<!--$2-->$3/;
    } elsif ($configuration eq 'insertion_only') {
        my $file_retriever_filter = get_regex_for_util_list("fileRetrieverFilter");
        $file_contents =~ s/$file_retriever_filter/$1<!--$2-->$3/;
    }
    print $write_fh $file_contents;
    close($write_fh);

    system("mv -f $application_context_file_path.new $application_context_file_path");
}

sub get_regex_for_util_list {
    my $id = shift;

    my $open_tag = qr/<util:list id="$id.*?>/;
    my $close_tag = qr/<\/util:list>/;

    return qr/($open_tag)(.*?)($close_tag)/s;
}

sub run_addlinks {
    my $addlinks_script_dir = shift;

    my $current_dir = getcwd;
    chdir $addlinks_script_dir;
    system("bash run_addlinks.sh");
    chdir $current_dir;
}
