package org.reactome.test;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;

/**
 * A Tool to check Reactome database consistence by checking Events
 * All events will be listed that either have no "hasEvent" or no input/output entries.
 * @author andreash
 * 
 */
public class FindEmptyEvents {
	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		try {
			if (args.length != 4 && args.length != 5) {
				System.out.println("FindEmptyEvents");
				System.out.println("Tool to check if Events are missing important"
						+ " attributes, like hasEvent in Pathways");
				System.out.println("Usage: FindEmptyEvents hostname[:port] databasename"
						+ " username password [species]");
				System.out.println("  species is optional. If used, it has to be the "
						+ "DBID of the species you want to be filter by");
				System.exit(0);
			}
			
			String hostname = args[0];
			String dbName = args[1];
			int port = 0;
			String username = args[2];
			String password = args[3];
			Long species = (long)0;
			try {
				species = (args.length == 5) ? Long.parseLong(args[4]) : 0;
			} catch (Exception ex) {
				System.out.println("Malformed species ID");
				System.exit(0);
			}
			
			if (hostname.contains(":")) {
				try {
					port = Integer.parseInt(hostname.substring(hostname.lastIndexOf(":") + 1));
					hostname = hostname.substring(0, hostname.lastIndexOf(":"));
				} catch (Exception ex) {
				}
			}
			
			MySQLAdaptor targetAdaptor;
			if (port == 0) {
				targetAdaptor = new MySQLAdaptor(hostname, dbName, username, password);
			} else {
				targetAdaptor = new MySQLAdaptor(hostname, dbName, username, password, port);
			}
			
			GKInstance speciesInstance = null;
			if (species != 0) {
				speciesInstance = targetAdaptor.fetchInstance(species);
				if (!speciesInstance.getSchemClass().isa(ReactomeJavaConstants.Species)) {
					speciesInstance = null;
				}
			}
			
			System.out.println("FindEmptyEvents - Checking database " + args[1] + " on " + args[0]);
			
			{
				Integer i = 0;
				Collection<GKInstance> pathways;
				if (speciesInstance == null) {
					pathways = (Collection<GKInstance>)targetAdaptor
							.fetchInstancesByClass(ReactomeJavaConstants.Pathway);
				} else {
					pathways = (Collection<GKInstance>)targetAdaptor.fetchInstanceByAttribute(
							ReactomeJavaConstants.Pathway, ReactomeJavaConstants.species, "=", speciesInstance);
				}
				
				System.out.println("Checking for Pathways"
						+ ((speciesInstance == null) ? "" : " [Restriction:" + speciesInstance + "]")
						+ ", total of " + pathways.size() + " pathways to check");
				for (Iterator<GKInstance> it = pathways.iterator(); it.hasNext();) {
					GKInstance pathway = (GKInstance)it.next();
					
					List hasEvents = pathway.getAttributeValuesList(ReactomeJavaConstants.hasEvent);
					if (hasEvents.size() == 0) {
						Boolean doRelease = (Boolean)pathway.getAttributeValue(ReactomeJavaConstants._doRelease);
						if (doRelease==null) doRelease = false;
						System.out.println("Pathway ["+(doRelease?"release":"no release")+"] has no events: " + pathway);
						i++;
					}
					pathway.deflate();
				}
				System.out.println("Finished checking pathways. " + i + " potentially problematic pathways were found.");
			}
			
			{
				Integer i = 0;
				
				Collection<GKInstance> reactionlikeEvent;
				
				if (speciesInstance == null) {
					reactionlikeEvent = (Collection<GKInstance>)targetAdaptor
							.fetchInstancesByClass(ReactomeJavaConstants.ReactionlikeEvent);
				} else {
					reactionlikeEvent = (Collection<GKInstance>)targetAdaptor.fetchInstanceByAttribute(
							ReactomeJavaConstants.Reaction, ReactomeJavaConstants.species, "=",
							speciesInstance);
				}
				
				System.out.println("Checking for Reactions"
						+ ((speciesInstance == null) ? "" : " [Restriction:" + speciesInstance + "]")
						+ ", total of " + reactionlikeEvent.size() + " events to check");
				
				for (Iterator<GKInstance> it = reactionlikeEvent.iterator(); it.hasNext();) {
					GKInstance reaction = (GKInstance)it.next();
					List inputs = reaction.getAttributeValuesList(ReactomeJavaConstants.input);
					List outputs = reaction.getAttributeValuesList(ReactomeJavaConstants.output);
					if (inputs.size() == 0 || outputs.size() == 0) {
						String what = "";
						if (inputs.size() == 0) {
							what = "inputs";
						}
						if (outputs.size() == 0) {
							what += (inputs.size()==0 ? " and " : "") + "outputs"; 
						}
						Boolean doRelease = (Boolean)reaction.getAttributeValue(ReactomeJavaConstants._doRelease);
						if (doRelease==null) doRelease = false;
						System.out.println("Reaction ["+(doRelease?"release":"no release")+"] has no " + what + ": " + reaction);
						i++;
					}
					reaction.deflate();
				}
				System.out.println("Finished checking ReactionlikeEvents. " + i + " potentially problematic events were found.");
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}