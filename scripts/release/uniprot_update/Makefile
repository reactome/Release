#has '+mail' => ( default => sub { 
#					my $self = shift;
#					return {
#						'to' => 'curation',
#						'subject' => $self->name,
#						'body' => "",
#						'attachment' => $self->directory . "/uniprot.wiki"
#					};
#				}
#);

user = curator
pass = Ixact1y
gkcentral = gk_clean
gkcentral_host = localhost

all: update

permissions:
	echo $(sudo) | sudo -S chgrp gkb *

gk_central.dump:
	mysqldump -u$(user) -p$(pass) -h$(gkcentral_host) --lock_tables=FALSE $(gkcentral) > $(gkcentral).dump

uniprot_sprot.xml.gz:
	wget --no-passive-ftp -a uniprot.err ftp://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/complete/uniprot_sprot.xml.gz

uniprot-reviewed^:no.list.gz:
	wget -a uniprot.err -O uniprot-reviewed:no.list.gz 'http://www.uniprot.org/uniprot/?query=reviewed%3Ano&compress=yes&format=list'

args = -db $(gkcentral) -host $(gkcentral_host) -user $(user) -pass $(pass)
update: permissions gk_central.dump uniprot_sprot.xml.gz uniprot-reviewed^:no.list.gz
	perl uniprot_xml2sql_isoform.pl $(args) > uniprot.out 2> uniprot.err

output = 
clean:
	rm -f $(output)
