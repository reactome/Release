 package org.gk.examples;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.gk.model.ClassAttributeFollowingInstruction;
import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.InvalidAttributeException;

public class SkypainterDbExample {

	public static void main(String[] args) throws Exception {
		if (args.length != 5) {
			System.err.println("java InstanceFetchingExample host database user password port");
			System.exit(0);
		}
		
		// Connect to the "main" db
		MySQLAdaptor dba =
			new MySQLAdaptor(
				args[0],
				args[1],
				args[2],
				args[3],
				Integer.parseInt(args[4]));

		/* IMPORTANT!!!
		 * skypainter db should be named as <MAIN DB NAME>_dn, i.e. if your main db
		 * is test_reactome_14, the skypainter db should be called have test_reactome_14_dn.
		 */
		MySQLAdaptor dba_dn =
			new MySQLAdaptor(
				args[0],
				args[1] + "_dn",
				args[2],
				args[3],
				Integer.parseInt(args[4]));
		
		/*
		 * Fetch instances of class "Pathway" having attribute "name" value equal to "Apoptosis"
		 * and attribute "taxon" containing and instance of class "Species" with attribute "name"
		 * equal to "Homo sapiens".
		 */
		System.out.println("\nHuman pathway(s) with name 'Apoptosis':");
		// Construct the query
		List query = new ArrayList();
		query.add(dba.createAttributeQueryRequest("Pathway","name","=","Apoptosis"));
		MySQLAdaptor.QueryRequestList subquery = dba.new QueryRequestList();
		subquery.add(dba.createAttributeQueryRequest("Species","name","=","Homo sapiens"));
		query.add(dba.createAttributeQueryRequest("Pathway","taxon","=",subquery));
		// Execute the query
		Set pathways2 = dba.fetchInstance(query);
		// Loop over results
		for (Iterator i = pathways2.iterator(); i.hasNext();) {
			GKInstance pathway = (GKInstance) i.next();
			System.out.println(displayNameWithSpecies(pathway));
			
			/*
			 * Find the "terminal" events, i.e. thise not containing or generalising
			 * other Events.
			 * To find those we 1st specify intructions for getting from a given instance to all components
			 * of the pathway and then "grep" for the terminal Events
			 */
			List instructions1 = new ArrayList();
			instructions1.add(new ClassAttributeFollowingInstruction("Pathway", new String[]{"hasComponent"}, new String[]{}));
			instructions1.add(new ClassAttributeFollowingInstruction("GenericEvent", new String[]{"hasInstance"}, new String[]{}));
			Collection terminalEvents = grepTerminalEvents(InstanceUtilities.followInstanceAttributes(pathway,instructions1).values());
			// Stick their DB_IDs into a list
			List db_ids = new ArrayList();
			for (Iterator pi =  terminalEvents.iterator(); pi.hasNext();) {
				GKInstance r = (GKInstance) pi.next();
				db_ids.add(r.getDBID());
			}
			/* 
			 * Use the DB_ID list to fetch Reaction instances from the skypainter db (mind you, this contains just the
			 * indirectIdentifiers, displayName and sky coordinates)
			 */ 
			Collection reactions_w_indirectIdentifiers = dba_dn.fetchInstanceByAttribute("Reaction","DB_ID","=",db_ids);
			// Load the indirectIdentifier values in one go to save some time.
			dba_dn.loadInstanceAttributeValues(reactions_w_indirectIdentifiers, new String[]{"indirectIdentifier"});
			/*
			 * Loop over the "real" Reaction instances and get their skypainter db counterparts
			 * from dba_dn. Since the instances are cached by dba fetching them by DB_ID won't
			 * result in new sql being issued (unless teh instance with given DB_ID isn't found in cache).
			 */
			for (Iterator pi =  terminalEvents.iterator(); pi.hasNext();) {
				GKInstance r = (GKInstance) pi.next();
				GKInstance r2 = dba_dn.fetchInstance(r.getDBID());
				if ((r2 != null) && (r2.getAttributeValuesList("indirectIdentifier") != null)) {
					// Print out the Reaction name and everything in the indirectIdentifier slot
					System.out.println(displayNameWithSpecies(r) + "\t" + r2.getAttributeValuesList("indirectIdentifier"));
				}
			}
		}
	}
	
	public static String displayNameWithSpecies (GKInstance instance) throws InvalidAttributeException, Exception {
		if (instance.getSchemClass().isValidAttribute("taxon")) {
			return instance + " [" + ((GKInstance) instance.getAttributeValue("taxon")).getAttributeValue("name") + "]";
		} else if (instance.getSchemClass().isValidAttribute("species")) {
			return instance + " [" + ((GKInstance) instance.getAttributeValue("species")).getAttributeValue("name") + "]";
		} else {
			return instance.toString();
		}
	}
	
	public static Collection grepTerminalEvents (Collection events) throws InvalidAttributeException, Exception {
		Collection out = new ArrayList();
		for (Iterator i = events.iterator(); i.hasNext();) {
			GKInstance event = (GKInstance) i.next();
			if (event.getSchemClass().isa("ConcreteReaction")) {
				out.add(event);
				continue;
			} else if (event.getSchemClass().isa("GenericReaction") && (event.getAttributeValue("hasInstance") == null)) {
				out.add(event);
				continue;
			} else if (event.getSchemClass().isa("ConcretePathway") && (event.getAttributeValue("hasComponent") == null)) {
				out.add(event);
				continue;
			} else if (event.getSchemClass().isa("GenericPathway") 
					  && (event.getAttributeValue("hasInstance") == null)
					  && (event.getAttributeValue("hasComponent") == null)) {
				out.add(event);
				continue;
			} 
		}
		return out;
		
	}
}
