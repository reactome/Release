/*
 * Created on Jun 11, 2008
 *
 */
package org.gk.render;

/**
 * This class is used to describe sequence features for a Node (e.g. protein). This is implemented
 * as a NodeAttachment.
 * @author wgm
 *
 */
public class RenderableFeature extends NodeAttachment {
    // feature type
    private FeatureType type;
    // name of residue bearing the feature type
    private String residue;
    // description of this RenderableFeature
    private String description;
    // label
    private String label;
    
    public RenderableFeature() {
    }
    
    public void setFeatureType(FeatureType type) {
        this.type = type;
    }
    
    public FeatureType getFeatureType() {
        return this.type;
    }
    
    public String getLabel() {
        if (type != null)
            return type.getLabel();
        return label == null ? "" : label;
    }
    
    public void setResidue(String residue) {
        this.residue = residue;
    }
    
    public String getResidue() {
        return this.residue;
    }
    
    @Override
    public String getDescription() {
        if (description == null && residue != null)
            return residue;
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public void setLabel(String label) {
        if (type != null)
            type = FeatureType.valueOf(label);
        else
            this.label = label;
    }
    
    @Override
    public NodeAttachment duplicate() {
        RenderableFeature feature = new RenderableFeature();
        feature.setFeatureType(type);
        feature.setDescription(getDescription());
        feature.setResidue(residue);
        feature.setRelativePosition(relativeX, relativeY);
        feature.setTrackId(trackId);
        return feature;
    }

    /**
     * A list of feature types
     */
    public enum FeatureType {
        ACETYLATED {
            public String getLabel() {return "AC";}
            public String getDescription() {return "acetylated";}
        },
        AMIDATED {
            public String getLabel() {return "AM";}
            public String getDescription() {return "amidated";}
        },
        FORMYLATED {
            public String getLabel() {return "F";}
            public String getDescription() {return "formylated";}
        },
        HYDROXYLATED {
            public String getLabel() {return "HO";}
            public String getDescription() {return "hydroxylated";}
        },
        LIPID_MODIFIED {
            public String getLabel() {return "L";}
            public String getDescription() {return "lipid modified";}
        },
        METHYLATED {
            public String getLabel() {return "M";}
            public String getDescription() {return "methylated";}
        },
        PHOSPHORYLATED {
            public String getLabel() {return "P";}
            public String getDescription() {return "phosphorylated";}
        },
        ADP_RIBOSYLATED {
            public String getLabel() {return "R";}
            public String getDescription() {return "adp ribosylated";}
        },
        GLYCOSYLATED {
            public String getLabel() {return "G";}
            public String getDescription() {
                return "glycoslated";
            }
        },
        UBIQUITINATED {
            public String getLabel() {return "U";}
            public String getDescription() {return "uiquitinated";}
        },
        ALKYLATED {
            public  String getLabel() {return "AK";}
            public String getDescription() {return "alkylated";}
        },
        OTHER {
            public  String getLabel() {return " ";}
            public String getDescription() {return "other";}
        },
        UNKNOWN { 
            public String getLabel() {return "?";}
            public  String getDescription() {return "unknown";}
        };
        
        public abstract String getLabel();
        
        public abstract String getDescription();
        
        public String getOriginalName() {
            String desc = getDescription();
            String name = desc.toUpperCase();
            return name.replaceAll(" ", "_");
        }
        
        /**
         * Use description for display
         */
        public String toString() {return getDescription();}
    }
}
