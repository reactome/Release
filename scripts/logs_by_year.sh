#!/bin/bash

echo "changing to the apache log directory..."
cd /usr/local/gkb/website/logs

# Get the logs for specified year
year=$1

if [[ ! -n $year ]]
then
    echo "You must tell which year you want: $0 year"
    exit 1
fi

logs=$year\_logs

mkdir -p $year\_logs

echo "Getting log data for year $year..."
grep "/$year" transfer_log | grep -vi 'js\|json\|css' > $logs/$year\_transfer_log.txt
grep "/$year" extended_log | grep -vi 'js\|json\|css'> $logs/$year\_extended_log.txt

cd $logs

# get the Analysis Service hits
echo "Processing Analysis Service..."
grep '/AnalysisService' $year\_transfer_log.txt > $year\_AnalysisService.txt
echo completed $year\_AnalysisService.txt

# get the counted, unique IPs
grep -v 'content/detail' $year\_AnalysisService.txt | cut -f1 -d' ' | sort | uniq -c | sort -nr > \
$year\_AnalysisService_hits_by_ip.txt
echo completed $year\_AnalysisService_hits_by_ip.txt


# Break down the Analysis Service hits
perl -pe 's/^.+AnalysisService\/([^\/ \%]+).+$/$1/' $year\_AnalysisService.txt | \
perl -pe 's!.+/AnalysisService/?.+$!/!' | grep -v  'content/detail' | sort  | \
uniq -c |sort -nr  > $year\_AnalysisService_breakdown.txt
echo completed $year\_AnalysisService_breakdown.txt


# Now RESTful API
echo Processing RESTful API...
grep RESTful $year\_extended_log.txt  | grep -v '/pages\|content\|wordpress' > $year\_REST.txt
echo completed $year\_REST.txt

# get number of hits by IP
cut -f1 -d' ' $year\_REST.txt | sort | uniq -c | sort -nr > $year\_REST_hits_by_ip.txt
echo completed $year\_REST_hits_by_ip.txt


# get breakdown
perl -pe 's!^.+(/ReactomeRESTfulAPI(/RESTfulWS/)?[\/A-Za-z]*)\b.+!$1!' $year\_REST.txt | \
sort |uniq -c | sort -nr > $year\_REST_breakdown.txt
echo completed $year\_REST_breakdown.txt

# get external accesses
grep -v PathwayBrowser $year\_REST.txt > $year\_REST_external.txt
echo completed $year\_REST_external.txt


# get external breakdown
perl -pe 's!^.+(/ReactomeRESTfulAPI(/RESTfulWS/)?[\/A-Za-z]*)\b.+!$1!' $year\_REST_external.txt | \
sort |uniq -c | sort -nr > $year\_REST_external_breakdown.txt
echo completed $year\_REST_external_breakdown.txt

# get external hits
cut -f1 -d' ' $year\_REST_external.txt | sort | uniq -c | sort -nr > $year\_REST_external_hits_by_ip.txt   
echo completed $year\_REST_external_hits_by_ip.txt


# Get searches
grep ' /content' $year\_transfer_log.txt > $year\_search.txt
echo completed $year\_search.txt

# get breakdown
perl -pe 's/^.+(\/content\/[A-Za-z]+)\b.+$/$1/' $year\_search.txt | 
perl -pe 's/^.+\/content\/?\s+.+$/\//' | sort | uniq -c | sort -nr > $year\_search_breakdown.txt
echo completed $year\_search_breakdown.txt

# Unique IPs for search
cut -f1 -d' ' $year\_search.txt | sort | uniq -c | sort -nr > $year\_search_hits_by_ip.txt
echo completed $year\_search_hits_by_ip.txt

# get the PathwayBrowser hits
echo "Processing PathwayBrowser..."
grep '/PathwayBrowser' $year\_transfer_log.txt |grep -v '\.png\|\.gif\|favicon\|javascript' > $year\_PathwayBrowser.txt
echo completed $year\_PathwayBrowser.txt

# get the counted, unique IPs
cut -f1 -d' ' $year\_PathwayBrowser.txt | sort | uniq -c | sort -nr > $year\_PathwayBrowser_hits_by_ip.txt
echo completed $year\_PathwayBrowser_hits_by_ip.txt

# Break down the PathwayBrowser hits
perl -pe 's/^.+(PathwayBrowser\/[^\/ \%]+).+$/$1/' $year\_PathwayBrowser.txt | \
perl -pe 's!.+/PathwayBrowser/?.+$!/!' | grep -v  'content/detail' | \
perl -pe 's!\#?R-[A-Z]\S+|\#?REACT_\S+!\#Stable_id!g' | \
perl -pe 's!PathwayBrowser/!/!' | sort  | \
uniq -c |sort -nr  > $year\_PathwayBrowser_breakdown.txt
echo completed $year\_PathwayBrowser_breakdown.txt

echo "Done!"
exit
