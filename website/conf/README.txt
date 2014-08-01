reactome.conf is under revision control.  httpd.conf is not undver revision control and can be used for server-specific configuration.
To install:

1) cp reactome.conf httpd.conf
steps 2-6 only need to be done once
2) edit httpd.conf as required for local architecture
3) cd /etc/apache/sites-available
4) ln -s /usr/local/gkb/website/conf/httpd.conf reactome
5) cd /etc/apache/sites-enabled
6) ln -s ../sites-available/reactome
