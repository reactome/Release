package org.reactome.server.analysis.core.model;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class UserData {
    List<String> columnNames;
    Set<AnalysisIdentifier> identifiers;
    String inputMD5;
    ExpressionBoundaries expressionBoundaries;

    public UserData(List<String> columnNames, Set<AnalysisIdentifier> identifiers, String inputMD5) {
        this.columnNames = columnNames;
        this.identifiers = identifiers;
        this.inputMD5 = inputMD5;
        this.expressionBoundaries = new ExpressionBoundaries(identifiers);
    }

    public String getSampleName(){
        return this.columnNames!=null && !this.columnNames.isEmpty() ?
                this.columnNames.get(0) :
                null;
    }

    public List<String> getExpressionColumnNames() {
        List<String> rtn = new LinkedList<String>();
        if(this.columnNames!=null && this.columnNames.size() > 1){
            for (int i = 1 ; i < this.columnNames.size(); i++) {
                 rtn.add(this.columnNames.get(i));
            }
        }
        return rtn;
    }

    public ExpressionBoundaries getExpressionBoundaries() {
        return expressionBoundaries;
    }

    public Set<AnalysisIdentifier> getIdentifiers() {
        return identifiers;
    }

    public String getInputMD5() {
        return inputMD5;
    }
}
