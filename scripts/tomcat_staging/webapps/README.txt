Steps to deploy a web file

0) ./backup.pl # back up the existings live webapps!
1) copy war(s) from wherever to /usr/local/reactomes/Reactome/development/staging/webapps
2) ./unpack.pl warfile_name.war to create a folder and unpack the war contents 
3) edit WEB-INF configuration file(s), if required
4) ./pack.pl warfile_name.war to re-package the war files
5) sudo ./deploy.pl warfile_name.war [deployed_warfile_name.war] to copy to the apache-tomcat webapps folder
6) sudo /etc/init.d/tomcat7 restart (optional)

Each script accepts a war name as a first argument.  
A second argument to deploy.pl renames the war file on deployment
