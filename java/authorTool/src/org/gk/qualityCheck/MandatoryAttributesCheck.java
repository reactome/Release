/*
 * Created on Jul 14, 2006
 *
 */
package org.gk.qualityCheck;

import org.gk.schema.SchemaAttribute;

/**
 * This class is used to check mandatory attributes. All work should be done in
 * RequiredAttributesCheck. Since the current dynamic loading cannot take a JavaBean
 * property, a class is used.
 * @author guanming
 *
 */
public class MandatoryAttributesCheck extends RequiredAttributesCheck {

    public MandatoryAttributesCheck() {
        super();
        // Reset the value used in the super class.
        setCheckedType(SchemaAttribute.MANDATORY); 
        setCheckedTypeLabel("mandatory");
    }
    
}
