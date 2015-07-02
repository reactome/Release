
**************
  IMPORTANT
**************

You probably have seen the templates in this folder are referring to "header.jsp" and "footer.jsp" and
these files don not exist when you load the project.

That is not an error. These files are created once you execute the project and are not gonna be included
in the version control because they are likely to change depending on the Wordpress templates in the
server.

Please select the correct MAVEN profile and execute the project and you will see them appear.

NOTE:
In your profile you should have a line like the following in order to ensure you access to the server for these files:

<template.server>http://dev2.reactome.org/</template.server>