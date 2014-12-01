package uk.ac.ebi.reactome.solr.indexer.impl;

import org.apache.commons.lang.StringEscapeUtils;
import uk.ac.ebi.reactome.solr.indexer.exception.IndexerException;
import uk.ac.ebi.reactome.solr.indexer.model.CrossReference;
import uk.ac.ebi.reactome.solr.indexer.model.IndexDocument;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by flo on 5/28/14.
 */
public class Marshaller {

    public static final String INDENT = "  ";
    public static final String NEW_LINE = "\n";

    private final String name;
    private final String description;
    private final String release;

    private Writer writer;


    public Marshaller( File output, String name, String description, String release ) {
        this.name = name;
        this.description = description;
        this.release = release;

        if ( output == null ) {
            throw new IllegalArgumentException( "output file must not be null." );
        }

        if ( !output.exists() ) {
            try {
                output.createNewFile();
            } catch ( IOException e ) {
                throw new IllegalArgumentException( "Could not create a new output file.", e );
            }
        }

        if ( !output.canWrite() ) {
            throw new IllegalArgumentException( "Cannot write on " + output.getAbsolutePath() );
        }

        try {
            this.writer = new BufferedWriter(new FileWriter(output));
        } catch (IOException e) {
            throw new IllegalArgumentException( "Cannot write on " + output.getAbsolutePath(), e );
        }

    }


    public void writeHeader() throws IndexerException {
        try {
            writer.write( "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>" + NEW_LINE );
            writer.write("<database>" + NEW_LINE);
            writer.write( INDENT + "<name>" + name + "</name>" + NEW_LINE );
            writer.write( INDENT + "<description>" + description + "</description>" + NEW_LINE );
            writer.write( INDENT + "<release>" + release + "</release>" + NEW_LINE );
            writer.write( INDENT + "<release_date>" + getCurrentDate() + "</release_date>" + NEW_LINE );
            writer.write( INDENT + "<entries>" + NEW_LINE );
        } catch ( IOException e ) {
            throw new IndexerException( e );
        }
    }
    public void writeEntry(IndexDocument document) throws IndexerException {
        final String i = INDENT + INDENT;
        final String ii = INDENT + INDENT+ INDENT;
        final String iii = INDENT + INDENT + INDENT + INDENT;

        try {
            writer.write( i + "<entry id=\"" + document.getDbId() + "\">" + NEW_LINE );

            String escaped = StringEscapeUtils.escapeXml(document.getName());
            writer.write( ii + "<name>" + escaped + "</name>" + NEW_LINE );
            if (document.getSummation()!= null) {
                String noHTMLString = document.getSummation().replaceAll("\\<.*?\\>", "");
                String noHtmlAndEscaped = StringEscapeUtils.escapeXml(noHTMLString);
                writer.write( ii +"<description>" + noHtmlAndEscaped + "</description>" + NEW_LINE );
            }

            writer.write( ii + "<cross_references>" + NEW_LINE );

            if (document.getReferenceIdentifiers()!= null) {
                writeRef(document.getDatabaseName(), document.getReferenceIdentifiers().get(0),iii);
            }
            if (document.getTaxId()!= null) {
                writeRef("TAXONOMY", document.getTaxId(),iii);
            }
            if (document.getCompartmentAccession()!= null && !document.getCompartmentAccession().isEmpty()) {
                for (String accession: document.getCompartmentAccession()) {
                    if (accession.contains("go:")){
                        writeRef("GO", accession, iii);
                    }
                }
            }
            if (document.getGoMolecularFunctionAccession()!= null && !document.getGoMolecularFunctionAccession().isEmpty()) {
                for (String accession: document.getGoMolecularFunctionAccession()) {
                    if (accession.contains("go:")){
                        writeRef("GO", accession, iii);
                    }
                }
            }
            if (document.getGoCellularComponentAccession()!= null) {
                for (String accession: document.getGoCellularComponentAccession()) {
                    if (accession.contains("go:")){
                        writeRef("GO", accession, iii);
                    }
                }
            }
            if (document.getGoBiologicalProcessAccession()!= null) {
                for (String accession: document.getGoBiologicalProcessAccession()) {
                    if (accession.contains("go:")){
                        writeRef("GO", accession, iii);
                    }
                }
            }
            if (document.getDiseaseId()!= null && !document.getDiseaseId().isEmpty()) {
                for (String diseaseId: document.getDiseaseId()) {
                    if (diseaseId.contains("doid:")){
                        writeRef("DOID", diseaseId, iii);
                    }
                }
            }

            if (document.getLiteratureReferencePubMedId()!=null && !document.getLiteratureReferencePubMedId().isEmpty()) {
                for (String literatureReferencePubMedId : document.getLiteratureReferencePubMedId()) {
                    writeRef("PubMed", literatureReferencePubMedId, iii);
                }
            }

            if (document.getAllCrossReferences()!=null && !document.getAllCrossReferences().isEmpty()) {
                for (CrossReference crossReference : document.getAllCrossReferences()) {
                    writeRef(crossReference.getDbName(), crossReference.getId(), iii);
                }
            }

            writer.write( ii + "</cross_references>" + NEW_LINE );



            writer.write( ii + "<additional_fields>" + NEW_LINE );
            writeField("species" ,document.getSpecies(),iii);
            if(document.getRelatedSpecies()!=null) {
                for (String relatedSpecies : document.getRelatedSpecies()) {
                    writeField("related_species", relatedSpecies, iii);
                }
            }
            if(document.getDiseaseName()!=null) {
                for (String diseaseName : document.getDiseaseName()) {
                    writeField("disease_name", diseaseName, iii);
                }
            }
            if(document.getDiseaseSynonyms()!=null) {
                for (String synonym : document.getDiseaseSynonyms()) {
                    writeField("disease_synonym", synonym, iii);
                }
            }
            writeField("type", document.getType(), iii);
            if (document.getStId()!= null) {
                writeField("stId", document.getStId(), iii);
            }
            if (document.getSynonyms()!= null && !document.getSynonyms().isEmpty()) {
                for (String synonym : document.getSynonyms()) {
                    writeField("synonym", synonym, iii);
                }
            }
            if (document.getKeywords()!= null && !document.getKeywords().isEmpty()) {
                for (String keyword : document.getKeywords()) {
                    writeField("keyword", keyword, iii);
                }
            }
            if (document.getRegulatedEntity()!= null) {
                writeField("regulated_entity", document.getRegulatedEntity(),iii);
            }
            if (document.getRegulator()!= null) {
                writeField("regulator", document.getRegulator(),iii);
            }
            if (document.getCompartmentName()!= null && !document.getCompartmentName().isEmpty()) {
                for (String compartment: document.getCompartmentName()) {
                    writeField("compartment_name", compartment, iii);
                }
            }
            if (document.getGoBiologicalProcessName()!= null) {
                writeField("go_biological_process_name", document.getGoBiologicalProcessName(), iii);
            }
            if (document.getGoMolecularFunctionName()!= null && !document.getGoMolecularFunctionName().isEmpty()) {
                for (String goMolecularFunctionName: document.getGoMolecularFunctionName()) {
                    writeField("go_molecular_function_name", goMolecularFunctionName, iii);
                }
            }

            if (document.getLiteratureReferenceTitle()!= null && !document.getLiteratureReferenceTitle().isEmpty()) {
                for (String literatureReferenceTitle: document.getLiteratureReferenceTitle()) {
                    writeField("literature_reference_title", literatureReferenceTitle, iii);
                }
            }
            if (document.getReferenceName()!= null) {
                writeField("external_reference_name", document.getReferenceName(),iii);
            }
            if (document.getReferenceSynonyms()!= null && !document.getReferenceSynonyms().isEmpty()) {
                for (String referenceSynonyms: document.getReferenceSynonyms()) {
                    writeField("external_reference_synonym", referenceSynonyms, iii);
                }
            }
            if (document.getReferenceGeneNames()!= null && !document.getReferenceGeneNames().isEmpty()) {
                for (String referenceGeneNames: document.getReferenceGeneNames()) {
                    writeField("gene_names", referenceGeneNames, iii);
                }
            }
            if (document.getReferenceOtherIdentifier()!= null && !document.getReferenceOtherIdentifier().isEmpty()) {
                for (String referenceOtherIdentifier: document.getReferenceOtherIdentifier()) {
                    writeField("external_reference_other_identifier", referenceOtherIdentifier, iii);
                }
            }
            if (document.getInferredSummation() != null) {
                String noHTMLString = document.getInferredSummation().replaceAll("\\<.*?\\>", "");
                writeField("inferred_summation", noHTMLString, iii);
                }

            writer.write( ii + "</additional_fields>" + NEW_LINE );

            writer.write( i + "</entry>" + NEW_LINE );
        } catch ( IOException e ) {
            throw new IndexerException( e );
        }
    }

    public void writeFooter(int entriesCount) throws IndexerException {
        try {
            writer.write(INDENT + "</entries>" + NEW_LINE);
            writer.write(INDENT + "<entry_count>" + entriesCount + "</entry_count>" + NEW_LINE);
            writer.write("</database>" + NEW_LINE);
            closeIndex();
        } catch (IOException e) {
            throw new IndexerException(e);
        }
    }
    public void flush() throws IOException {
        writer.flush();
    }

    private void closeIndex() throws IOException {
        writer.flush();
        writer.close();
    }

    private void writeRef( String db, String id, String indent ) throws IOException {
        db = db.replaceAll( "/", "_" );
        writer.write(indent + "<ref dbname=\"" + db + "\" dbkey=\"" + StringEscapeUtils.escapeXml(id) + "\" />" + NEW_LINE);
    }

    private void writeField( String name, String text, String indent ) throws IOException {
        if (text!= null) {
            String escapedXml = StringEscapeUtils.escapeXml(text);
            writer.write( indent + "<field name=\"" + name + "\">" + escapedXml + "</field>" + NEW_LINE );
        }
    }

    public String getCurrentDate() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        Date date = new Date();
        return(dateFormat.format(date));
    }
}


