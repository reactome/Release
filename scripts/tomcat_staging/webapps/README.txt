Steps to deploy a war file
NOTE: This folder is backed up to reactomerelease daily
      keep webapps synchronized

1) copy war(s) from wherever to staging/webapps
2) ./unpack.pl [warfile name] to create a folder and unpack the war contents 
3) edit, if required (probably not required on live server)
4) ./pack.pl [warfile name] to re-package the war files
5) ./deploy.pl to copy to the apache-tomcat webapps folder
6) sudo /etc/init.d/tomcat7 restart

Each script accepts a war name as an argument, otherwise acts on all
