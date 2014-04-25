/*
 * Created on Nov 7, 2008
 *
 */
package org.gk.elv;

import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.render.RenderableChemical;
import org.gk.render.RenderableComplex;
import org.gk.render.RenderableEntity;
import org.gk.render.RenderableFactory;
import org.gk.render.RenderableProtein;
import org.gk.schema.GKSchemaClass;
import org.gk.schema.SchemaClass;
import org.gk.util.AuthorToolAppletUtilities;

/**
 * This customized tree cell renderer is used to render an entity instance in a tree.
 * @author wgm
 *
 */
public class EntityInstanceCellRenderer extends DefaultTreeCellRenderer {
    // Cache this icon
    private Icon clsIcon = AuthorToolAppletUtilities.createImageIcon("Class.gif");
    
    public EntityInstanceCellRenderer() {
    }
    
    public Component getTreeCellRendererComponent(JTree tree,
                                                  Object value,
                                                  boolean sel,
                                                  boolean expanded,
                                                  boolean leaf,
                                                  int row,
                                                  boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, sel,
                                           expanded, leaf, row,
                                           hasFocus);
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        Object obj = node.getUserObject();
        if (obj instanceof GKInstance) {
            GKInstance instance = (GKInstance) obj;
            setIcon(getIconForValue(instance));
            if (instance.isDirty())
                setText(">" + instance.getDisplayName());
            else
                setText(instance.getDisplayName());
        }
        else if (obj instanceof GKSchemaClass) {
            GKSchemaClass cls = (GKSchemaClass) obj;
            setIcon(clsIcon);
            setText(cls.getName());
        }
        return this;
    }

    protected Icon getIconForValue(GKInstance instance) {
        Icon icon = null;
        SchemaClass cls = instance.getSchemClass();
        // To save some coding
        RenderableFactory factory = RenderableFactory.getFactory();
        if (cls.isa(ReactomeJavaConstants.Complex))
            icon = factory.getIcon(RenderableComplex.class);
        else if (cls.isa(ReactomeJavaConstants.GenomeEncodedEntity))
            icon = factory.getIcon(RenderableProtein.class);
        else if (cls.isa(ReactomeJavaConstants.SimpleEntity))
            icon = factory.getIcon(RenderableChemical.class);
        else
            icon = factory.getIcon(RenderableEntity.class);
        return icon;
    }
    
    
    
}
