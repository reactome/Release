/*
 * Created on Jan 3, 2008
 *
 */
package org.gk.property;

import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.gk.model.Reference;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

/**
 * This class is used to process PMID information in XML. This class should replace
 * the old one PMIDInfoFetcher that is not supported by PubMed any more (Jan, 2008).
 * This class has beeb deprecated. Use PMIDXMLInfoFetcher2 instead to get the full
 * author information.
 * @author guanming
 */
@Deprecated
public class PMIDXMLInfoFetcher {
    // The following two URLs are parsed from this example (http://eutils.ncbi.nlm.nih.gov/entrez/query/static/esummary_help.html):
    // http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=pubmed&id=11850928,11482001&retmode=xml
    private final String URL1 = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=pubmed&id=";
    private final String URL2 = "&retmode=xml";
    // These tags for parse medline record
    public static final String PMID = "PMID";
    public static final String DATE = "DP";
    public static final String TITLE = "TI";
    public static final String PAGE = "PG";
    public static final String AUTHOR = "AU";
    public static final String JOURNAL = "TA";
    public static final String VOLUME = "VI";

    public PMIDXMLInfoFetcher() {
    }
    
    public Reference fetchInfo(Long pmid) throws Exception {
        List pmids = new ArrayList(1);
        pmids.add(pmid);
        List references = fetchInfo(pmids);
        if (references != null && references.size() > 0)
            return (Reference) references.get(0);
        return null;
    }
    
    public List fetchInfo(List pmids) throws Exception {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < pmids.size(); i++) {
            Long pmid = (Long) pmids.get(i);
            buffer.append(pmid);
            if (i < pmids.size() - 1)
                buffer.append(",");
        }
        String url = URL1 + buffer.toString() + URL2;
        // Query the pubmed
        URL pubmed = new URL(url);
        SAXBuilder builder = new SAXBuilder();
        Document document = builder.build(pubmed);
        // The following is a XML snippet: http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=pubmed&id=11850928,11482001&retmode=xml
//        <eSummaryResult>
//        <DocSum>
//            <Id>11850928</Id>
//            <Item Name="PubDate" Type="Date">1965 Aug</Item>
//            <Item Name="Source" Type="String">Arch Dermatol</Item>
//            <Item Name="AuthorList" Type="List">
//                <Item Name="Author" Type="String">LoPresti PJ</Item>
//                <Item Name="Author" Type="String">Hambrick GW Jr</Item>
//            </Item>
//            <Item Name="Title" Type="String">Zirconium granuloma following treatment of rhus dermatitis.</Item>
//            <Item Name="Volume" Type="String">92</Item>
//
//            <Item Name="Issue" Type="String">2</Item>
//            <Item Name="Pages" Type="String">188-91</Item>
//            <Item Name="FullJournalName" Type="String">Archives of dermatology</Item>
//            <Item Name="ELocationID" Type="String"></Item>
//            <Item Name="SO" Type="String">1965 Aug;92(2):188-91</Item>
        // ......
//        </DocSum>
        Element root = document.getRootElement();
        List docSumElms = root.getChildren("DocSum");
        return parseDocSum(docSumElms);
    }
    
    private List parseDocSum(List docSumElms) {
        List references = new ArrayList(docSumElms.size());
        for (Iterator it = docSumElms.iterator(); it.hasNext();) {
            Element docSumElm = (Element) it.next();
            Reference reference = new Reference();
            List children = docSumElm.getChildren();
            for (Iterator it1 = children.iterator(); it1.hasNext();) {
                Element child = (Element) it1.next();
                String elmName = child.getName();
                if (elmName.equals("Id")) {
                    String id = child.getText();
                    reference.setPmid(Long.parseLong(id));
                }
                else if (elmName.equals("Item")) {
                    String name = child.getAttributeValue("Name");
                    String text = child.getText();
                    if (name.equals("AuthorList"))
                        extractAuthors(reference, child);
                    else if (name.equals("Source")) {
                        reference.setJournal(text);
                    }
                    else if (name.equals("PubDate")) {
                        int index = text.indexOf(" ");
                        if (index < 0) {
                            // The whole text should be year
                            try {
                                reference.setYear(Integer.parseInt(text));
                            }
                            catch(NumberFormatException e) {
                                System.err.println("PubDate: " + e);
                                e.printStackTrace();
                            }
                        }
                        else 
                            reference.setYear(Integer.parseInt(text.substring(0, index)));
                    }
                    else if (name.equals("Volume")) {
                        reference.setVolume(text);
                    }
                    else if (name.equals("Pages")) {
                        reference.setPage(text);
                    }
                    else if (name.equals("Title")) {
                        // Want to get rid of the last period
                        int index = text.lastIndexOf(".");
                        if (index == text.length() - 1)
                            text = text.substring(0, index);
                        reference.setTitle(text);
                    }
                }
            }
            references.add(reference);
        }
        return references;
    }
    
    private void extractAuthors(Reference reference,
                                Element authorListElm) {
//      <Item Name="AuthorList" Type="List">
//          <Item Name="Author" Type="String">LoPresti PJ</Item>
//          <Item Name="Author" Type="String">Hambrick GW Jr</Item>
//      </Item>
        List children = authorListElm.getChildren("Item");
        StringBuffer buffer = new StringBuffer();
        for (Iterator it = children.iterator(); it.hasNext();) {
            Element elm = (Element) it.next();
            String author = elm.getText();
            buffer.append(author);
            if (it.hasNext())
                buffer.append(", ");
        }
        reference.setAuthor(buffer.toString());
    }
}
