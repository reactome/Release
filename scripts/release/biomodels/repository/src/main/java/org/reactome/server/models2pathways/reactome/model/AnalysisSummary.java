package org.reactome.server.models2pathways.reactome.model;


import org.reactome.server.models2pathways.reactome.helper.AnalysisCoreHelper;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class AnalysisSummary {
    private String token;
    private String type;
    private String sampleName;
    private Long species;
    private boolean text = false;
    private String fileName;

    AnalysisSummary(String token, String sampleName, AnalysisCoreHelper.Type type) {
        this.token = token;
        this.type = type.toString();
        this.sampleName = sampleName;
    }

    public AnalysisSummary(String token, String sampleName, AnalysisCoreHelper.Type type, String fileName) {
        this(token, sampleName, type);
        this.fileName = fileName;
    }

    public AnalysisSummary(String token, String sampleName, AnalysisCoreHelper.Type type, Long species) {
        this(token, sampleName, type);
        this.species = species;
    }

    public AnalysisSummary(String token, String sampleName, AnalysisCoreHelper.Type type, boolean text) {
        this(token, sampleName, type);
        this.text = text;
    }

    public String getToken() {
        return token;
    }

    public String getType() {
        return type;
    }

    public String getSampleName() {
        return sampleName;
    }

    public Long getSpecies() {
        return species;
    }

    public String getFileName() {
        return fileName;
    }

    public boolean isText() {
        return text;
    }

    @Override
    public String toString() {
        return "AnalysisSummary{" +
                "token='" + token + '\'' +
                ", type='" + type + '\'' +
                ", sampleName='" + sampleName + '\'' +
                ", species=" + species +
                ", text=" + text +
                ", fileName='" + fileName + '\'' +
                '}';
    }
}