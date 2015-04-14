# workflow for counting hits to Analysis service and converting count to a CSV file

   ```grep '\/AnalysisService\/identifiers' /usr/local/gkb/website/logs/extended_log | \<br>
   perl massage_IPs.pl | cut -f1 -d' ' | sort | uniq -c | perl -pe 's/^\s+//' | \<br>
   perl -pe 's/\s+/,/' | python geo_ip.py > ~/analysis_counts.csv```

The upload the csv to http://heatmap.joyofdata.de/
