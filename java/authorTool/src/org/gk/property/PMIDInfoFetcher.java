/*
 * Created on Jul 28, 2004
 *
 * A refactored class from RefernecePane to fetch detailed information for 
 * PMIDs.
 * Note: The URL used in this class to fetch a plain text summary from PubMed
 * is not supported any more. This class is deprecated. Please used another class
 * based on XML, PMIDXMLInfoFetch.
 */
package org.gk.property;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.gk.model.Reference;

/**
 * @deprecated
 * @author guanming
 *
 */
public class PMIDInfoFetcher {
	// These tags for parse medline record
	public static final String PMID = "PMID";
	public static final String DATE = "DP";
	public static final String TITLE = "TI";
	public static final String PAGE = "PG";
	public static final String AUTHOR = "AU";
	public static final String JOURNAL = "TA";
	public static final String VOLUME = "VI";

    public PMIDInfoFetcher() {
    }
    
    public Reference fetchInfo(Long pmid) throws Exception {
        List pmids = new ArrayList(1);
        pmids.add(pmid);
        List references = fetchInfo(pmids);
        if (references != null && references.size() > 0)
            return (Reference) references.get(0);
        return null;
    }
    
    public java.util.List fetchInfo(java.util.List pmids) throws Exception {
		String url1 = "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Text&db=PubMed&";
		String url2 = "&dopt=MEDLINE";
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < pmids.size(); i++) {
			Long pmid = (Long) pmids.get(i);
			buffer.append("uid=" + pmid);
			if (i < pmids.size() - 1)
				buffer.append("&");
		}
		String url = url1 + buffer.toString() + url2;
        // Query the pubmed
		java.util.List references = null;
        URL pubmed = new URL(url);
        BufferedReader reader = new BufferedReader(new InputStreamReader(pubmed.openStream()));
        String line = null;
        java.util.List lines = new ArrayList();
        while ((line = reader.readLine()) != null) {
            //System.out.println(line);
        	    //should not trim line. The empty space is used in the parsing method, parseDocSum(List).
            //line = line.trim();
            if (line.length() == 0)
                continue; // Don't need empty string
            if (line.startsWith("<pre>")) {
                // Format changed as of June 18, 2007
                if (line.length() == 5)
                    continue;
                line = line.substring(5).trim();
                if (line.length() == 0)
                    continue;
            }
            else if (line.startsWith("</pre>"))
                break;
            lines.add(line);
        }
        references = parseDocSum(lines);
        return references;
    }
    
	private java.util.List parseDocSum(java.util.List lines) {
        java.util.List references = new ArrayList();
		String line = null;
		Reference ref = new Reference();
		int index = 0;
		boolean isInTitle = false;
		StringBuffer title = new StringBuffer();
		StringBuffer author = new StringBuffer();
		for (int i = 0; i < lines.size(); i++) {
			line = (String) lines.get(i);
			if (isInTitle) {
				if (line.substring(0, 2).trim().length() == 2)
					isInTitle = false;
			}
			//System.out.println(line);
			if (line.startsWith(PMID)) {
				index = line.indexOf("-") + 1;
				ref.setPmid(Long.parseLong(line.substring(index).trim()));
			}
			else if (line.startsWith(VOLUME)) {
				index = line.indexOf("-") + 1;
				ref.setVolume(line.substring(index).trim());
			}
			else if (line.startsWith(DATE)) {
				index = line.indexOf("-") + 1;
				String tmp = line.substring(index).trim();
				index = tmp.indexOf(" ");
				if (index == -1)
					ref.setYear(Integer.parseInt(tmp.trim()));
				else
					ref.setYear(Integer.parseInt(tmp.substring(0, index)));
			}
			else if (line.startsWith(TITLE)) {
				isInTitle = true;
				title.setLength(0);
				index = line.indexOf("-") + 1;
				String tmp = line.substring(index).trim();
				title.append(tmp);
			}
			else if (line.startsWith(PAGE)) {
				index = line.indexOf("-") + 1;
				ref.setPage(line.substring(index + 1).toString());
			}
			else if (line.startsWith(AUTHOR)) {
				index = line.indexOf("-") + 1;
				if (author.length() > 0)
					author.append(", ");
				author.append(line.substring(index).trim());
			}
			else if (line.startsWith(JOURNAL)) {
				index = line.indexOf("-") + 1;
				ref.setJournal(line.substring(index).trim());
			}
			else if (isInTitle) {
				title.append(" ");
				title.append(line.trim());
			}
			else if (line.length() == 0 || i == lines.size() - 1) {
				ref.setAuthor(author.toString());
                index = title.indexOf(".");
				if (index == title.length() - 1)
					ref.setTitle(title.substring(0, index));
				else
					ref.setTitle(title.toString());
				author.setLength(0);
				title.setLength(0);
				references.add(ref);
				ref = new Reference();
			}
		}
       	return references;
	}
}
