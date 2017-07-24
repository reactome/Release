#2015 pre-SAB Log analysis

1) get log info for Aug 2014 - present 

    grep REST /usr/local/gkb/website/logs/extended_log | grep Diagram | grep PathwayBrowser   

* <b>NOTE: some ad hoc filtering (grep, head, tail) to use only dates from Aug-April</b>

2) get older (pre Aug, 2014) log info from archive

    grep REST /nfs/reactome/reactome/archive/old_reactome_backup/Reactome/website_3_0/GKB/website/logs/extended_log \
    | grep Diagram | grep PathwayBrowser    

* <b>NOTE: some ad hoc filtering (grep, head, tail) to get only April 2014 - July 2014</b>

3) getting the hits by IP

    zcat last_year.txt.gz | perl -pe 's/^(\S+).+pathwayDiagram\/(\d+).+$/$1\t$2/' > IP_hits.txt

4) hit counts

    cut -f2 IP_hits.txt | sort | uniq -c | sort -nr | perl -pe 's/^\s+//' | perl -pe 's/\s+/\t/' > hit_counts.txt    

5) unique event IDs

    cut -f2 hit_counts.txt | sort -u > unique_hits.txt    

6) pathway information (also filtering out non-pathway hits)

    perl pathway_info.pl username password unique_hits.txt > pathway_info.txt

7) flattened pathway hierarchy for all species seen in log

    cut -f2 pathway_info.txt | sort -u | ./flatten_pathway_hierarchy.pl > pathway_hierarchy.txt

8) generate reports
 
    perl analyze_pathway_hits.pl
    


