#FIREWORKS SERVER

##Why a server side?
Fireworks is meant to be a genome-wide, hierarchical visualisation of Reactome pathways in a space-filling graph. To do
so the nodes and their layout are set up in a off-line process.

Fireworks server access Reactome database and Analysis intermediate files to retrieve pathways information in order to
create the Fireworks content json files. It assigns a size and a position in the plane for each node in Fireworks
(which represents a Reactome pathway).

The generated json files remain immutable for a given Reactome data release, what means they can be generated once and
stored for later widget consumption. This strategy ensures the layout generation is not a handicap for the future
visualisation.

##How to use it?
It is possible that some tweaking is needed to the top level pathways configuration after a certain release. If that is
the case in the [Fireworks bursts configuration folder](config) there are different files where the top level pathways
initial point in the map are defined. Edit them in order to change the position, start angle or burst direction.

The configuration file for [Homo sapiens](config/Homo_sapiens_bursts.json) contains the data related to the Reactome main
species so it is the one with more top level pathways.

To run the layout algorithm, please follow the following steps:

1. First package this project to generate the fireworks.jar file:

        $mvn clean package

2. Using the Analysis Service intermediate file, the following statement generates the graph reduced binary file:

        $java -jar fireworks.jar GRAPH -s /path/to/analysis.bin -o /path/to/ReactomeGraphs.bin --verbose

3. Generate the json files for Homo Sapiens and then decide whether some tweaking is needed

    3.1 Only Homo Sapiens:

        $java -jar fireworks.jar LAYOUT -d database -u user -p password -s homo_sapiens -g /path/to/ReactomeGraphs.bin -f /path/to/config -o /path/to/output --verbose

    3.2 Only "Other species" (being species_name its name):

        $java -jar fireworks.jar LAYOUT -d database -u user -p password -s species_name -g /path/to/ReactomeGraphs.bin -f /path/to/config -o /path/to/output --verbose

4. Generate the json files for all species in Reactome:

        $java -jar fireworks.jar LAYOUT -d database -u user -p password -g /path/to/ReactomeGraphs.bin -f /path/to/config -o /path/to/output --verbose

###To take into account
The generated Fireworks json files are meant to be consumed by the Widget, so they will need to be available from the
web clients, so please specify a reachable "/path/to/output".

In Reactome the files for the current release are available under the
[http://www.reactome.org/download/current/fireworks](http://www.reactome.org/download/current/fireworks/)
folder. So for example, the file for "Homo Sapiens" is
[Homo_sapiens.json](http://www.reactome.org/download/current/fireworks/Homo_sapiens.json).

####Recommendation
It is recommended to specify the initial and maximum memory allocation pool for the Java Virtual Machine

    -Xms2048M -Xmx5120M
