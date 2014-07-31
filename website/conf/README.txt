reactome.conf is under revision control.
To install:

1) cp reactome.conf httpd.conf
steps 2-6 only need to be done once
2) edit httpd.conf as required for local architecture
3) cd /etc/apache/sites-available
4) ln -s /usr/local/gkb/website/conf/httpd.conf reactome
5) cd /etc/apache/sites-enabled
6) ln -s ../sites-available/reactome
