/*
 * Created on Sep 24, 2006
 *
 */
package org.gk.gkCurator.authorTool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.gk.database.SynchronizationManager;
import org.gk.model.GKInstance;
import org.gk.model.InstanceDisplayNameGenerator;
import org.gk.model.ReactomeJavaConstants;
import org.gk.model.Reference;
import org.gk.model.Summation;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.render.Renderable;
import org.gk.render.RenderablePropertyNames;

/**
 * A helper class for RenderabelConverter.
 * @author guanming
 *
 */
public class RenderableHandlerHelper {
    private XMLFileAdaptor fileAdaptor;
    private MySQLAdaptor dbAdaptor;

    public RenderableHandlerHelper() {    
    }
    
    public void setDBAdaptor(MySQLAdaptor dbAdaptor) {
        this.dbAdaptor = dbAdaptor;
    }
    
    public void setFileAdaptor(XMLFileAdaptor fileAdaptor) {
        this.fileAdaptor = fileAdaptor;
    }
    
    private java.util.List extractReferences(java.util.List references) throws Exception {
        if (references == null || references.size() == 0)
            return null;
        Reference reference = null;
        java.util.List refInstances = new ArrayList(references.size());
        for (Iterator it = references.iterator(); it.hasNext();) {
            reference = (Reference)it.next();
            // PMID is long in Reference
            Integer pmid = new Integer(reference.getPmid() + "");
            GKInstance refInstance = getInstanceByAttribute(ReactomeJavaConstants.LiteratureReference, 
                                                            ReactomeJavaConstants.pubMedIdentifier, 
                                                            pmid);
            if (refInstance.getDBID().longValue() < 0) { // This is new: extracting all properties
                if (reference.getJournal() != null)
                    refInstance.setAttributeValue("journal", reference.getJournal());
                if (reference.getPage() != null)
                    refInstance.setAttributeValue("pages", reference.getPage());
                if (reference.getTitle() != null)
                    refInstance.setAttributeValue("title", reference.getTitle());
                String volume = reference.getVolume();
                if (volume != null) {
                    int vol = -1;
                    try {
                        vol = Integer.parseInt(volume);
                    }
                    catch (NumberFormatException e) {
                        System.err.println("ImportAuthoringFileHelper.extractReference(): " + e);
                        e.printStackTrace();
                    }
                    if (vol != -1)
                        refInstance.setAttributeValue("volume", new Integer(vol));
                }
                if (reference.getYear() > 0)
                    refInstance.setAttributeValue("year", new Integer(reference.getYear()));
                String author = reference.getAuthor();
                if (author != null) {
                    // Parse the author
                    java.util.List persons = parseAuthors(author);
                    if (persons.size() > 0)
                        refInstance.setAttributeValue("author", persons);
                }
                refInstance.setDisplayName(InstanceDisplayNameGenerator.generateDisplayName(refInstance));
            }
            refInstances.add(refInstance);
        }
        return refInstances;
    }
    
    protected void extractReference(Renderable r, GKInstance instance) throws Exception {
        java.util.List references = (java.util.List) r.getAttributeValue(RenderablePropertyNames.REFERENCE);
        java.util.List refInstances = extractReferences(references);
        instance.setAttributeValue(ReactomeJavaConstants.literatureReference, 
                                   refInstances);
    }
    
    private java.util.List parseAuthors(String authorString) throws Exception {
        StringTokenizer tokenizer = new StringTokenizer(authorString, ",");
        java.util.List persons = new ArrayList();
        while (tokenizer.hasMoreTokens()) {
            String author = tokenizer.nextToken().trim();
            int index = author.indexOf(" ");
            String surName = null;
            String initial = null;
            if (index >= 0) {
                surName = author.substring(0, index);
                initial = author.substring(index + 1);
            }
            else 
                surName = author;
            String displayName = surName;
            if (initial != null)
                displayName += (", " + initial);
            GKInstance person = getInstanceByAttribute(ReactomeJavaConstants.Person, 
                                                       ReactomeJavaConstants._displayName, 
                                                       displayName);
            if (person.getDBID().longValue() < 0) {
                person.setAttributeValue(ReactomeJavaConstants.surname, 
                                         surName);
                if (initial != null)
                    person.setAttributeValue(ReactomeJavaConstants.initial, 
                                             initial);
                InstanceDisplayNameGenerator.setDisplayName(person);
            }
            persons.add(person);
        }
        return persons;
    }
    
    protected void extractSummation(Renderable r, GKInstance instance) throws Exception {
        Summation summation = (Summation) r.getAttributeValue("summation");
        if (summation == null) {
            instance.setAttributeValue(ReactomeJavaConstants.summation, 
                                       null);
            return;
        }
        // Something differnt for summation: there is a isChanged mark
        GKInstance sumInst = null;
        Long dbId = summation.getDB_ID();
        if (dbId != null) {
            sumInst = fileAdaptor.fetchInstance(dbId);
            if (sumInst == null || sumInst.isShell()) {
                GKInstance dbInst = dbAdaptor.fetchInstance(dbId);
                if (dbInst != null) {
                    if (sumInst == null)
                        sumInst = SynchronizationManager.getManager().checkOutShallowly(dbInst);
                    else
                        SynchronizationManager.getManager().updateFromDB(sumInst, dbInst);
                }
            }
            if (sumInst != null && !summation.isChanged()) {
                instance.setAttributeValue(ReactomeJavaConstants.summation, 
                                           sumInst);
                return;
            }
        }
        if (sumInst == null) {
            sumInst = fileAdaptor.createNewInstance(ReactomeJavaConstants.Summation);
            if (dbId != null) {
                Long old = sumInst.getDBID();
                sumInst.setDBID(dbId);
                fileAdaptor.dbIDUpdated(old, sumInst);
            }
        }
        if (summation.getText() != null && summation.getText().length() > 0) {
            sumInst.setAttributeValue(ReactomeJavaConstants.text, summation.getText());
        }
        java.util.List literatures = summation.getReferences();
        java.util.List refInstances = extractReferences(literatures);
        sumInst.setAttributeValue(ReactomeJavaConstants.literatureReference,
                                  refInstances);
        sumInst.setIsDirty(true);
        InstanceDisplayNameGenerator.setDisplayName(sumInst);
        instance.setAttributeValue(ReactomeJavaConstants.summation,
                                   sumInst);
    }
    
    protected void extractPropertyFromName(Renderable r,
                                         String rPropName,
                                         GKInstance instance,
                                         String iAttName,
                                         String iClsName) throws Exception {
        String rValue = (String) r.getAttributeValue(rPropName);
        if (rValue == null || rValue.length() == 0) {
            instance.setAttributeValue(iAttName, null);
            return;
        }
        GKInstance iAttValue = getInstanceByAttribute(iClsName, 
                                                      ReactomeJavaConstants._displayName,
                                                      rValue);
        instance.setAttributeValue(iAttName, iAttValue);
    }
    
    protected void extractLocalization(Renderable r,
                                       GKInstance instance,
                                       String iAttName,
                                       String iClsName) throws Exception {
        String rValue = r.getLocalization();
        if (rValue == null || rValue.length() == 0) {
            instance.setAttributeValue(iAttName, null);
            return;
        }
        GKInstance iAttValue = getInstanceByAttribute(iClsName, 
                                                      ReactomeJavaConstants._displayName,
                                                      rValue);
        instance.setAttributeValue(iAttName, iAttValue);
    }
    
    protected void extractAttachments(Renderable r, 
                                      GKInstance instance) throws Exception {
        java.util.List attachments = (java.util.List) r.getAttributeValue(RenderablePropertyNames.ATTACHMENT);
        if (attachments == null || attachments.size() == 0)
            return;
        java.util.List figures = new ArrayList(attachments.size());
        int c = 0;
        for (Iterator it = attachments.iterator(); it.hasNext();) {
            String attachment = (String) it.next();
            int index = attachment.lastIndexOf("\\");
            if (index == -1)
                index = attachment.lastIndexOf("/");
            // No file seperator, it should be OK since index = -1.
            String url = "/figures/" + attachment.substring(index + 1);
            GKInstance figureInstance = getInstanceByAttribute(ReactomeJavaConstants.Figure,
                                                               ReactomeJavaConstants.url,
                                                               url);
            figures.add(figureInstance);
        }
        instance.setAttributeValue(ReactomeJavaConstants.figure, 
                                   figures);
    }
    
    protected GKInstance getInstanceByAttribute(String clsName,
                                              String attName,
                                              Object value) throws Exception {
        // Search from local first
        Collection c = fileAdaptor.fetchInstanceByAttribute(clsName, attName, "=", value);
        if (c != null && c.size() > 0)
            return (GKInstance) c.iterator().next();
        // Check database
        c = dbAdaptor.fetchInstanceByAttribute(clsName, attName, "=", value);
        if (c != null && c.size() > 0) {
            GKInstance dbInstance = (GKInstance) c.iterator().next();
            return SynchronizationManager.getManager().checkOutShallowly(dbInstance);
        }
        // Have to create a new one
        GKInstance newInstance = fileAdaptor.createNewInstance(clsName);
        newInstance.setAttributeValue(attName, value);
        if (attName.equals(ReactomeJavaConstants._displayName) &&
            newInstance.getSchemClass().isValidAttribute(ReactomeJavaConstants.name)) 
            newInstance.setAttributeValue(ReactomeJavaConstants.name, value);
        else
            InstanceDisplayNameGenerator.setDisplayName(newInstance);
        return newInstance;
    }
    
    protected GKInstance getInstanceByDBID(Long dbId) throws Exception {
        GKInstance local = fileAdaptor.fetchInstance(dbId);
        if (local != null)
            return local;
        GKInstance db = dbAdaptor.fetchInstance(dbId);
        if (db != null)
            return SynchronizationManager.getManager().checkOutShallowly(db);
        return null;
    }
    
    protected void extractNames(Renderable r, 
                                GKInstance instance) throws Exception {
        // Name
        String displayName = r.getDisplayName();
        java.util.List aliases = (java.util.List) r.getAttributeValue(RenderablePropertyNames.ALIAS);
        java.util.List names = new ArrayList();
        names.add(displayName); // Display Name should be at the first.
        if (aliases != null && aliases.size() > 0)
            names.addAll(aliases);
        instance.setAttributeValue(ReactomeJavaConstants.name, names);  
    }
}
