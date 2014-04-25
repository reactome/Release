/*
 * Created on Apr 15, 2004
 */
package org.gk.render;

/**
 * An interface to list all property names used in Renderable.
 * @author wugm
 */
public interface RenderablePropertyNames {

	public static final String TAXON = "taxon";
	public static final String LOCALIZATION = "localization";
	public static final String ALIAS = "names";
	public static final String DB_ID = "DB_ID";
	public static final String CREATED = "created";
	public static final String MODIFIED = "modified";
	/**
	 * External database identifier.
	 */
	public static final String DATABASE_IDENTIFIER = "databaseIdentifier";
	public static final String MODIFICATION = "modifications";
	public static final String SUMMATION = "summation";
	public static final String REFERENCE = "references";
	public static final String DISPLAY_NAME = "displayName";
	public static final String PRECEDING_EVENT = "precedingEvent";
	public static final String ATTACHMENT = "attachments";
	public static final String IS_REVERSIBLE = "isReversible";
	public static final String STOICHIOMETRY = "stoichiometry";
	// Corresponding schema class in the Reactome data model
	public static final String SCHEMA_CLASS = "schemaClass"; 
}
