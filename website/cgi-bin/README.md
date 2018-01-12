# CGI Scripts

This is based on scripts currently (as of 2018-01-10) in Production

## GKB and Perl modules
Perl modules are structured as directories. The module `GKB::DocumentGeneration::GenerateTextPDF` refers to a file named `GenerateTextPDF.pm` in the path `GKB/DocumentationGeneration`.

`GKB` is a Reactome module. The code can be found [here](../../modules/GKB). Most of the scripts below have a `lib` directive referencing `../cgi-modules`, so this is where you should place `GKB`. Other dependencies can be installed using distribution-specific packages (example: search the web for _"ubuntu package for Perl HTTP::Tiny"_ and you'll see that you can install this module using `apt-get install libhttp-tiny-perl` ), `cpanminus`, or `cpanm`.

## The Scripts

### author_contributions.
This script is used to generate content for the Author Contributions page. Example: https://reactome.org/cgi-bin/author_contributions?ID=2993965&
This page is accessed by clicking an author name from the Table Of Contents (https://reactome.org/cgi-bin/toc?DB=gk_current)

Perl Depdencies (GKB is a Reactome module, found in /usr/local/reactomes/Reactome/production/Website/static/cgi-modules/GKB):
<pre>
lib '../cgi-modules';
GKB::Config;
GKB::DBAdaptor;
GKB::WebUtils;
GKB::FrontPage3;
CGI qw(:standard);
CGI::Carp 'fatalsToBrowser';
HTTP::Tiny;
JSON;
Data::Dumper;
Email::Valid;
URI::Encode 'uri_encode';
</pre>

### bibtex
This script is used to generate bibtex output. Accessible from author contribution pages.

This page: https://reactome.org/cgi-bin/author_contributions?DB=gk_current&ID=77241&
has a link to
https://reactome.org/cgi-bin/bibtex?DB_ID=109703;personId=77241

Perl dependencies:
<pre>
CGI qw(:standard);
CGI::Carp 'fatalsToBrowser';
HTTP::Tiny;
Data::Dumper;
JSON;
</pre>

### classbrowser
Still in use on reactomecurator: https://reactomecurator.oicr.on.ca/cgi-bin/classbrowser?DB=gk_central&ID=9025969

Perl dependencies:
<pre>
CGI qw(:standard);
GKB::DBAdaptor;
GKB::Config;
Data::Dumper;
GKB::WebUtils;
strict;
GKB::FrontPage3;
</pre>

### control_panel_st_id
Renders “External identifier history” page for pathway browser, for example:
https://reactome.org/cgi-bin/control_panel_st_id?ST_ID=R-HSA-392841 -

you can see it if you click the Stable Identifier in the  lower panel.

Perl dependencies:
<pre>
CGI qw(:standard);
CGI::Carp qw/fatalsToBrowser/;
common::sense;
lib "../cgi-modules";
GKB::DBAdaptor;
GKB::WebUtils;
GKB::Utils;
GKB::Config;
GKB::StableIdentifierDatabase;
HTTP::Tiny;
JSON;
GKB::FrontPage3;
Data::Dumper;
</pre>

### doi_toc

Renders DOI page: https://reactome.org/cgi-bin/doi_toc?DB=gk_current

Outputs to $FRONTPAGE_IMG_DIR, the value of which is set in GKB::Config.pm - currently points to:
/usr/local/reactomes/Reactome/production/Website/static/cgi-tmp/img-fp

Perl dependencies:
<pre>
lib "../cgi-modules";
strict;
CGI qw(:standard);
GKB::DBAdaptor;
GKB::PrettyInstance;
GKB::WebUtils;
GKB::Config;
Data::Dumper;
Carp;
GKB::FrontPage3;
</pre>

### eventbrowser
Renders pages like this: https://reactomedev.oicr.on.ca/cgi-bin/eventbrowser?DB=gk_current&ID=2993965&

Although this script now redirects the user to https://reactomedev.oicr.on.ca/content/schema/instance/browser/2993965

This script could probably be removed if the “cgi-bin/eventbrowswer?...” URL could be routed by Joomla.

Perl dependencies:
<pre>
lib "../cgi-modules";
feature qw/state/;
CGI qw(:standard);
CGI::Carp 'fatalsToBrowser';
GKB::Config;
GKB::WebUtils;
</pre>

### extendedsearch
Still in use on reactomecurator: https://reactomecurator.oicr.on.ca/cgi-bin/extendedsearch

Perl dependencies (Also has an odd BEGIN block to find modules):
<pre>
GKB::DBAdaptor;
GKB::PrettyInstance;
GKB::WebUtils;
GKB::Config;
Data::Dumper;
GKB::FrontPage3;
</pre>

### footer
Still in use on reactomecurator

### link
Still available on reactomecurator: https://reactomecurator.oicr.on.ca/cgi-bin/link?ID=1234&SOURCE=gk_current

Perl dependencies:
<pre>
lib '/usr/local/gkb/modules';
CGI qw(:standard);
GKB::DBAdaptor;
GKB::PrettyInstance;
GKB::WebUtils;
GKB::Config;
Data::Dumper;
GKB::FrontPage3;
GKB::StableIdentifierDatabase;
</pre>

### instancebrowser
Still in use on reactome curator: https://reactomecurator.oicr.on.ca/cgi-bin/instancebrowser?ID=96168;DB=gk_current

Perl dependencies:
<pre>
lib "/usr/local/gkb/modules"; #adjust this path if necessary
CGI qw(:standard);
GKB::DBAdaptor;
GKB::PrettyInstance;
GKB::WebUtils;
GKB::Config;
Data::Dumper;
GKB::FrontPage3;
</pre>

### pdfexporter
Used to generate PDF documents from the PathwayBrowser download tab.

Outputs to /usr/local/reactomes/Reactome/production/Website/static/cgi-tmp/pdf/
(uses $GK_TMP_IMG_DIR from GKB::Config)

Perl Dependencies:
<pre>
lib "../cgi-modules";
CGI qw(:standard);
GKB::DBAdaptor;
GKB::PrettyInstance;
GKB::WebUtils;
GKB::Config;
Data::Dumper;
GKB::DocumentGeneration::ReactomeDatabaseReader;
GKB::DocumentGeneration::GenerateTextPDF;
</pre>

### protegeexporter
Used to generate protege exports from the PathwayBrowser download tab.

Outputs to /usr/local/reactomes/Reactome/production/Website/static/cgi-tmp/protege/
(uses $GK_TMP_IMG_DIR from GKB::Config)

Perl dependencies:
<pre>
lib "../cgi-modules";
GKB::WebUtils;
GKB::Config;
</pre>

### remoteattsearch

Runs an advanced search: https://reactome.org/cgi-bin/remoteattsearch

Links are not provided but curators apparantly might use this.

Perl dependencies:
<pre>
lib "../cgi-modules";
GKB::WebUtils;
GKB::Config;
GKB::FrontPage3;
Data::Dumper;
</pre>

### remoteattsearch2
Runs an advanced search: https://reactome.org/cgi-bin/remoteattsearch2

Links are not provided but curators apparantly might use this. Seems to not work, but this might be missing dependencies or incompatibilities with Joomla.

Perl dependencies:
<pre>
lib "../cgi-modules";
CGI qw(:standard);
GKB::Config;
GKB::WebUtils;
GKB::FrontPage3;
</pre>

### rtfexporter
Generates RTF exports (for MS Word) from the PathwayBrowser.

Outputs to /usr/local/reactomes/Reactome/production/Website/static/cgi-tmp/rtf/
(uses $GK_TMP_IMG_DIR from GKB::Config)

Perl dependencies:
<pre>
lib "../cgi-modules";
lib "/tmp/libs/bioperl-1.0";
lib "/tmp/libs/my_perl_stuff";
CGI qw(:standard);
GKB::DBAdaptor;
GKB::PrettyInstance;
GKB::WebUtils;
GKB::Config;
Data::Dumper;
GKB::DocumentGeneration::ReactomeDatabaseReader;
GKB::DocumentGeneration::GenerateTextRTF;
</pre>

### search2

Still in use in reactomecurator:

https://reactomecurator.oicr.on.ca/cgi-bin/search2?QUERY=ntn1&species=Homo+sapiens&OPERATOR=ANY&cluster=true

### taboutputter
Generates tab output. Was linked to from old CGI-based instance browser. Can still be accessed at https://reactome.org/cgi-bin/taboutputter?DB=gk_current&INSTRUCTIONID=4&ID=168898

I think the old CGI instance browser was accessible from the remoteattsearch2 script results page so this script might still be needed. Might also still be needed for reactomecurator.

### toc
Generates content for the Table Of Contents page: https://reactome.org/cgi-bin/toc?DB=gk_current

Outputs to $FRONTPAGE_IMG_DIR from GKB::Config.pm - currently points to:
/usr/local/reactomes/Reactome/production/Website/static/cgi-tmp/img-fp

Perl dependencies:
<pre>
lib '../cgi-modules';
CGI qw(:standard);
GKB::DBAdaptor;
GKB::PrettyInstance;
GKB::WebUtils;
GKB::Config;
Data::Dumper;
Carp;
GKB::FrontPage3;
</pre>
