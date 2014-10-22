package org.reactome.server.analysis.service.report;

import org.reactome.server.analysis.service.helper.AnalysisHelper;
import org.reactome.server.analysis.service.model.AnalysisSummary;
import org.reactome.server.analysis.service.result.AnalysisStoredResult;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class ReportParameters {
    private AnalysisHelper.Type type;
    private String name;
    private Boolean toHuman;
    private int ids;
    private int found;
    private long milliseconds;

    public ReportParameters(AnalysisHelper.Type type) {
        this.type = type;
    }

    public ReportParameters(AnalysisHelper.Type type, Boolean toHuman){
        this.type = type;
        this.toHuman = toHuman;
    }

    public ReportParameters(AnalysisStoredResult analysisStoredResult) {
        this.setAnalysisStoredResult(analysisStoredResult);
    }

    public void setAnalysisStoredResult(AnalysisStoredResult result){
        AnalysisSummary aux = result.getSummary();
        this.type = AnalysisHelper.Type.getType(aux.getType());
        this.name = aux.getSampleName();
        if(this.name==null || this.name.isEmpty()) this.name = aux.getFileName();
        if( ( this.name==null || this.name.isEmpty() ) && aux.getSpecies()!=null) this.name = aux.getSpecies().toString();
        if(this.name==null || this.name.isEmpty()) this.name = "_NO_NAME_AVAILABLE_";

        this.found = result.getFoundEntities().size();
        int notFound = result.getNotFound().size();
        this.ids = this.found + notFound;
    }

    public void setMilliseconds(long milliseconds) {
        this.milliseconds = milliseconds;
    }

    @Override
    public String toString() {
        String name = this.name != null ? " " + this.name.replaceAll("\\s", "_") : "";
        String toHumanStr = toHuman != null ? ( toHuman ? "toHuman" : "toSpecies") : "N/A";
        String type = this.type != null ? "_" + this.type + "_" : "";
        @SuppressWarnings("StringBufferReplaceableByString")
        StringBuilder message = new StringBuilder(type)
                .append(name)
                .append(" ").append(toHumanStr)
                .append(" size:").append(ids)
                .append(" found:").append(found)
                .append(" notFound:").append(ids-found)
                .append(" time:").append(milliseconds).append("ms");
        return message.toString();
    }
}
