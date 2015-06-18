package org.reactome.web.fireworks.util;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.InlineLabel;
import org.reactome.web.fireworks.model.Node;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
class PathwayInfoPanel extends Composite {

    PathwayInfoPanel(Node pathway) {
        initWidget(new InlineLabel(pathway.getName()));
    }
}
