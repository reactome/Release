package org.reactome.server.analysis.core.model.identifier;

import org.reactome.server.analysis.core.model.AnalysisIdentifier;
import org.reactome.server.analysis.core.model.resource.Resource;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class OtherIdentifier extends Identifier<Resource> {

    public OtherIdentifier(Resource resource, AnalysisIdentifier identifier) {
        super(resource, identifier);
    }

}
