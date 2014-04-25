/*
 * Created on Dec 15, 2005
 */
package org.gk.IDGeneration;

import java.util.ArrayList;
import java.util.List;

import org.gk.database.AttributeEditEvent;
import org.gk.database.AttributePane;
import org.gk.model.GKInstance;
import org.gk.model.StoichiometryInstance;
import org.gk.schema.GKSchemaAttribute;
import org.gk.schema.GKSchemaClass;
import org.gk.schema.InvalidAttributeException;

/**
 * Dumbs down on AttributePane somewhat, because we don't need all
 * of its functionality.
 * 
 * @author croft
 *
 */
public class SimpleAttributePane extends org.gk.database.AttributePane {
	public SimpleAttributePane(GKInstance instance) {
		super(instance);
	}
}
