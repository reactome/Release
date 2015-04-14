package org.reactome.server.models2pathways.biomodels.model;

import java.util.ArrayList;
import java.util.List;


/**
 * Stores all the annotations related to one given model element.
 *
 * @author Camille Laibe
 * @version 20140703
 */
public class ModelElement {
    private List<Bag> bags;

    /**
     * Default constructor: builds an empty object.
     */
    public ModelElement() {
    }

    public void addBag(Bag bag) {
        if (null == this.bags) {
            this.bags = new ArrayList<>();
        }
        this.bags.add(bag);
    }

    public List<Bag> getBags() {
        return bags;
    }
}