# build as: `docker build -t reactome-release .`
FROM ubuntu:18.04
RUN apt-get update && apt-get install -y apt-utils software-properties-common
RUN add-apt-repository ppa:webupd8team/java && apt-get update
RUN echo "oracle-java8-installer shared/accepted-oracle-license-v1-1 select true" | debconf-set-selections
RUN apt-get install -y \
    netcat cpanminus wget make tree libterm-readline-perl-perl \
    liblog-log4perl-perl libdbi-perl libwww-search-perl \
    libtemplate-plugin-gd-perl libxml-simple-perl libcgi-pm-perl \
    libemail-valid-perl libpdf-api2-perl librtf-writer-perl \
    liburi-encode-perl libdbd-mysql-perl libcommon-sense-perl \
    libipc-system-simple-perl libjson-perl libreadonly-perl \
    libdatetime-format-strptime-perl libsoap-lite-perl libnet-openssh-perl \
    libarray-compare-perl libtime-out-perl libterm-readkey-perl \
    libmime-lite-perl libarray-utils-perl libtie-ixhash-perl \
    liblist-compare-perl libberkeleydb-perl libany-moose-perl \
    libcgi-session-perl libole-storage-lite-perl libtemplate-plugin-number-format-perl \
    libspreadsheet-writeexcel-perl libnumber-format-perl libspreadsheet-writeexcel-perl \
    libcgi-session-perl libmoose-perl git libgd-svg-perl libgtk2-perl libregexp-common-perl\
    libpod-strip-perl libgraphviz-perl libtemplate-plugin-yaml-perl libcache-cache-perl \
    libset-scalar-perl libapache-dbi-perl libdata-dumper-simple-perl \
    libdata-dump-perl libperl6-form-perl libenum-perl libconvert-binary-c-perl libxml-dom-xpath-perl \
    libgraph-perl libconfig-tiny-perl libconfig-inifiles-perl libhash-merge-perl \
    t-coffee clustalw libunicode-casefold-perl librtf-document-perl librtf-writer-perl \
    libmail-sendmail-perl libswitch-perl libnet-sftp-foreign-perl curl maven ftp \
    vim mysql-client iputils-ping sendmail oracle-java8-installer oracle-java8-set-default mlocate \
    libdata-table-perl  libfile-homedir-perl libspreadsheet-writeexcel-perl libcgi-session-perl \
    libbio-perl-perl libmail-sendmail-perl libemail-address-perl

RUN cpanm --notest Data::CTable CPANPLUS::Backend Ace Test::Base Bio::Root::TestObject \
    RTF::Tokenizer Parallel::Loops

# Some perl scripts expect GKB to be in /root/GKB/modules so we need to symlink there.
RUN mkdir -p /root/GKB && ln -s /usr/local/gkb/modules /root/GKB/modules
RUN mkdir -p /usr/local/gkb
RUN mkdir /release
RUN mkdir /tmp/gitroot
# And other perl scripts expect them to be in ../modules.
RUN ln -s /usr/local/gkb/modules ../modules
RUN ln -s /release/scripts /usr/local/gkb/scripts
# Ugh... so many differnt lib paths. whatever, will just link GKB directly into /etc/perl
RUN ln -s /usr/local/gkb/modules/GKB /etc/perl/GKB
RUN mkdir -p /usr/local/gkbdev/ && ln -s /usr/local/gkb/modules /usr/local/gkbdev/modules
RUN ln -s /release/scripts /usr/local/gkbdev/scripts


# Install Biomart
WORKDIR /release/modules
RUN git clone https://github.com/biomart/biomart-perl.git && cd biomart-perl && git checkout cvs/release-0_7
RUN ln -s /release/modules/biomart-perl/lib/BioMart /etc/perl/BioMart

WORKDIR /tmp/gitroot
ARG RELEASE_TAG=release-container
RUN git clone https://github.com/reactome/Release.git && cd Release && git checkout $RELEASE_TAG
RUN mv /tmp/gitroot/Release/java /release/java && \
    mv /tmp/gitroot/Release/modules /usr/local/gkb/modules && \
    mv /tmp/gitroot/Release/scripts /release/scripts && \
    mv /tmp/gitroot/Release/slicingTool /release/slicingTool
# Remove whatever's left - it's not needed.
RUN rm -rf /tmp/gitroot
# YOU must provide your own Secrets.pm file!
COPY ./Secrets.pm /usr/local/gkb/modules/GKB/Secrets.pm
WORKDIR /release/scripts

ARG ENSEMBL_API_VERSION=96
WORKDIR /usr/local/gkb/modules
RUN perl install_ensembl_api.pl $ENSEMBL_API_VERSION

RUN ln -s /release/modules/ensembl_api/ensembl/modules/Bio /etc/perl/Bio
RUN ln -s /release/modules/ensembl_api/ensembl/modules/EnsEMBL /etc/perl/EnsEMBL

# Check code (*.pl and *.pm) in modules
WORKDIR /usr/local/gkb/modules
# RUN tree -Pf *.pl --prune
RUN echo "Perl files to check: $(tree -Pf '*.pl' --prune | grep \.pl | grep -v BioMart.pm | grep -v biomart-perl | grep -v ensembl_api | sed -e 's/^.* \.\(.*\)/.\1/g')"
RUN for f in $(tree -Pf '*.pl' --prune | grep \.pl | grep -v BioMart.pm | grep -v biomart-perl |grep -v ensembl_api | sed -e 's/^.* \.\(.*\)/.\1/g') ; do echo "checking $f..." ; perl -c $f ; done;
# RUN tree -Pf *.pm --prune
RUN echo "Perl files to check: $(tree -Pf '*.pm' --prune | grep \.pm | grep -v BioMart.pm |grep -v biomart-perl | grep -v ensembl_api | sed -e 's/^.* \.\(.*\)/.\1/g')"
RUN for f in $(tree -Pf '*.pm' --prune | grep \.pm | grep -v BioMart.pm | grep -v biomart-perl | grep -v ensembl_api | sed -e 's/^.* \.\(.*\)/.\1/g') ; do echo "checking $f..." ; perl -c $f ; done;

# Check code (*.pl only) in scripts
WORKDIR /release/scripts
# RUN tree -Pf *.pl --prune
RUN echo "Perl files to check: $(tree -Pf '*.pl' --prune | grep \.pl | grep -v obsolete_steps | grep -v biomart_update | grep -v reactome_mart | sed -e 's/^.* \.\(.*\)/.\1/g')"
RUN for f in $(tree -Pf '*.pl' --prune | grep \.pl | grep -v obsolete_steps | grep -v biomart_update | grep -v reactome_mart | sed -e 's/^.* \.\(.*\)/.\1/g') ; do echo "checking $f..." ; perl -c $f ; done;
# Build the authorTool
WORKDIR /release/java/authorTool
RUN ./compile.sh

# Clean up.
RUN apt autoremove && apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/* /var/cache/oracle-jdk9-installer

# Index files to make it easier to find stuff
RUN apt-get install -y mlocate && updatedb

WORKDIR /release/scripts/release
# NOTE: can probably uninstall git after all code is pulled. Nope. commit_stats pushes to github.
# So if you want to run this step, you will need to set your github credentials before running it.

# Other things that could be parameterized and mounted into the container:
# /release/modules/GKB/Release/Config.pm
# Possible entrypoint "/release/scripts/release/release.pl $STEP_NUMBER"
# Directories that need to persist from one execution to another:
# - /release/scripts/release/orthopairs
# - report and log files from /release/scripts/release/uniprot_update/:
#    uniprot.wiki
#    uniprot.err
#    uniprot.out
#    gk_central.dump.gz
#    reference_DNA_sequence_report.txt
#    sequence_uniprot_report.txt
#    duplicated_db_id.txt
#    trembl_to_update.acc
# - report and log files from /usr/local/gkb/scripts/release/go_update:
#    ec_number.err
#    ec_number.out
#    go.err
#    go.out
#    go.wiki
#    gk_central_after_uniprot_update.dump.gz
#    ec2go
# General rule: anything ending in *.out, *.err, *.dump.gz, *.log, *.wiki
# - report and log files from /usr/local/gkb/scripts/release/chebi_update:
#    improve_chebi_ids.err  improve_chebi_ids.out
#    chebi.wiki
# - For orthopairs: The *whole* directory named by the release number (eg. "62")
#    also: compara.err, compara.log,
# - For update_stable_ids:
#    - $RELEASE_NUM.dump.gz
#    - gk_central_$RELEASE_NUM_before_st_id.dump.gz and gk_central_$RELEASE_NUM_after_st_id.dump.gz
#    - gk_central.dump.gz (maybe redundant??)
#    - test_slice_$RELEASE_NUM_after_st_id.dump.gz and test_slice_$RELEASE_NUM.dump.gz
#    - generate_stable_ids_$RELEASE_NUM.(err|out)
#    - update_stable_ids.(err|log)
#    - pre_step_test_errors.log
#    - correct_stable_ids.log (soon this will not be needed)
# - for myisam, capture myisam.out and myisam.err
# update_source_code will not be needed in docker!
#
# NOTE: step "6	ClearData" is no longer used.
# NOTE: step "7	CVSUpdate" will need to ssh to other servers. Also there is ssh-keygen in the setup_keypairs.pl script...
# For orthoinference:
#   - you will be prompted for skiplist, should be mounted from outside (normal_event_skip_list.txt)
#   - logs to capture:
#       - deleted_unused_pe_test_reactome_$RELEASE_NUMBER.txt
#       - infer_events.(err|log)
#       - remove_unused_PE.(err|log)
#       - tweak_datamodel.(log|err)
#       - wrapper_ortho_inference.(err|log)
#       - test_reactome_$RELEASE_NUM_after_ortho.dump.gz
