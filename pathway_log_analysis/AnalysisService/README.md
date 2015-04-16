## Web site hits heat map

This is the workflow for counting hits to AnalysisService and converting count to a file formatted for joyofdata.de

First run this:

    grep '\/AnalysisService\/identifiers' /usr/local/gkb/website/logs/extended_log \
    | cut -f1 -d' ' | sort | uniq -c | perl -pe 's/^\s+//' \
    | perl -pe 's/\s+/,/' | python geo_ip.py > ~/counts.txt
    
The file looks like so:

    "weight";"lat";"lon"
    23;-33.9333;18.85
    5;37.2296;-80.4139
    3;47.6145;-122.348
    2;57.6472;-3.3215
    ...
    
Then upload the text to http://heatmap.joyofdata.de/
