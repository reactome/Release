## workflow for counting hits to AnalysisService and converting count to a file formated for joyofdata.de

First run this:

    grep '\/AnalysisService\/identifiers' /usr/local/gkb/website/logs/extended_log \
    perl massage_IPs.pl | cut -f1 -d' ' | sort | uniq -c | perl -pe 's/^\s+//'     \
    perl -pe 's/\s+/,/' | python geo_ip.py > ~/analysis_counts.txt

Then upload the text to http://heatmap.joyofdata.de/
