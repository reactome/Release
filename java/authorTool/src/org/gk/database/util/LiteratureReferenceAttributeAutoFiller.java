/*
 * Created on Aug 3, 2005
 *
 */
package org.gk.database.util;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gk.model.GKInstance;
import org.gk.model.InstanceDisplayNameGenerator;
import org.gk.model.Person;
import org.gk.model.ReactomeJavaConstants;
import org.gk.model.Reference;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.property.PMIDXMLInfoFetcher2;

public class LiteratureReferenceAttributeAutoFiller extends AbstractAttributeAutoFiller {
    
    public LiteratureReferenceAttributeAutoFiller() {
    }

    protected Object getRequiredAttribute(GKInstance instance) throws Exception {
        return instance.getAttributeValue("pubMedIdentifier");
    }
    
    protected String getConfirmationMessage() {
        return "Do you want to fetch other information for the specified PMID?";
    }

    /* (non-Javadoc)
     * @see org.gk.database.AttributeAutoFiller#process(org.gk.model.GKInstance)
     */
    public void process(GKInstance instance, Component parentComp) throws Exception {
        if (adaptor == null)
            throw new IllegalStateException("LiteratureReferenceAttributeAutoFiller.process(): " +
            "No PersistenceAdaptor assigned.");
        PMIDXMLInfoFetcher2 fetcher = new PMIDXMLInfoFetcher2();
        Integer pmid = (Integer) instance.getAttributeValue("pubMedIdentifier");
        if (pmid == null)
            return; // Cannot do anything
        Reference ref = fetcher.fetchInfo(new Long(pmid.intValue()));
        if (ref == null)
            return;
        if (autoCreatedInstances == null)
            autoCreatedInstances = new ArrayList();
        else
            autoCreatedInstances.clear();
        instance.setAttributeValue("title", ref.getTitle());
        // Get the digital from string
        String vol = ref.getVolume();
        if (vol != null) {
            Pattern pattern = Pattern.compile("\\d+");
            Matcher matcher = pattern.matcher(vol);
            if (matcher.find()) {
                String sub = matcher.group();
                instance.setAttributeValue("volume", new Integer(sub));
            }
        }
        instance.setAttributeValue("year", new Integer(ref.getYear()));
        instance.setAttributeValue("pages", ref.getPage());
        instance.setAttributeValue("journal", ref.getJournal());
        // Have to use Instances for authors
        java.util.List<?> authors = instance.getAttributeValuesList("author");
        if (authors != null)
            authors.clear();
        List<Person> persons = ref.getAuthors();
        if (persons != null && persons.size() > 0) {
            for (Person person : persons) {
                GKInstance authorInstance = queryPerson((XMLFileAdaptor)adaptor, 
                                                        person,
                                                        autoCreatedInstances);
                instance.addAttributeValue("author", authorInstance);
            }
        }
    }
    
    /**
     * Create a GKInstance for a Person object.
     * @param adaptor
     * @param person
     * @param autoCreatedInstances
     * @return
     * @throws Exception
     */
    private GKInstance queryPerson(XMLFileAdaptor adaptor,
                                   Person person,
                                   List<GKInstance> autoCreatedInstances) throws Exception {
        Collection<?> c = adaptor.fetchInstanceByAttribute("Person", 
                                                           "surname",
                                                           "=", 
                                                           person.getLastName());
        if (c != null && c.size() > 0) {
            for (Iterator<?> it = c.iterator(); it.hasNext();) {
                GKInstance personInst = (GKInstance) it.next();
                String initial = (String) personInst.getAttributeValue("initial");
                // For easy comparion
                if (initial == null)
                    initial = "";
                String firstName = (String) personInst.getAttributeValue(ReactomeJavaConstants.firstname);
                if (firstName == null)
                    firstName = "";
                if (initial.equals(person.getInitial()) && 
                    firstName.equals(person.getFirstName()))
                    return personInst;
            }
        }
        GKInstance authorInstance = adaptor.createNewInstance(ReactomeJavaConstants.Person);
        authorInstance.setAttributeValue(ReactomeJavaConstants.surname,
                                         person.getLastName());
        authorInstance.setAttributeValue(ReactomeJavaConstants.initial,
                                         person.getInitial());
        authorInstance.setAttributeValue(ReactomeJavaConstants.firstname,
                                         person.getFirstName());
        InstanceDisplayNameGenerator.setDisplayName(authorInstance);
        if (autoCreatedInstances != null)
            autoCreatedInstances.add(authorInstance);
        return authorInstance;
    }
    
    /**
     * Get a Person instance. If no matched is found, a new GKInstance will be created!
     * @param adaptor
     * @param lastName
     * @param initial
     * @param autoCreatedInstance
     * @return
     * @throws Exception
     */
    public GKInstance queryPerson(XMLFileAdaptor adaptor,
                                  String lastName,
                                  String initial,
                                  List<GKInstance> autoCreatedInstances) throws Exception {
        Collection c = adaptor.fetchInstanceByAttribute("Person", 
                                                        "surname",
                                                        "=", 
                                                        lastName);
        GKInstance authorInstance = null;
        if (c != null && c.size() > 0) {
            for (Iterator it = c.iterator(); it.hasNext();) {
                GKInstance person = (GKInstance) it.next();
                Object tmp = person.getAttributeValue("initial");
                if (initial != null && tmp != null && 
                        initial.toString().equals(tmp.toString())) {
                    authorInstance = person;
                    break;
                }
            }
        }
        if (authorInstance == null) {
            // It should be very safe to cast adaptor to XMLFileAdaptor.
            authorInstance = ((XMLFileAdaptor)adaptor).createNewInstance("Person");
            authorInstance.setAttributeValue("surname", lastName);
            authorInstance.setAttributeValue("initial", initial);
            InstanceDisplayNameGenerator.setDisplayName(authorInstance);
            if (autoCreatedInstances != null)
                autoCreatedInstances.add(authorInstance);
        }
        return authorInstance;
    }
}
