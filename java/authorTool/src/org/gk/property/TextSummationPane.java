/*
 * Created on Jun 24, 2003
 */
package org.gk.property;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.gk.model.Summation;
import org.gk.render.Renderable;
import org.gk.render.RenderablePropertyNames;
import org.gk.render.Shortcut;
import org.gk.util.AuthorToolAppletUtilities;

/**
 * @author wgm
 * This customized JPanel is used to edit text summations for instances in GK.
 */
public class TextSummationPane extends RenderablePropertyPane {
	// Label for title
	private JLabel titleLabel;
	private JButton pasteBtn;
	// For text
	private JTextPane textPane;
	// For paste buttn
	private ComponentListener pasteBtnHandler;
	// For references attached to summation
	private ReferencePane referencePane;
	private JSplitPane jsp;
    // Track changes
    private boolean isDirty = false;
	
	public TextSummationPane() {
		init();		
	}

	private void init() {
		setLayout(new BorderLayout());
		JPanel summationPane = createSummationPane();
		referencePane = createReferencePane();
        referencePane.setBorder(BorderFactory.createEtchedBorder());
        jsp = new JSplitPane(JSplitPane.VERTICAL_SPLIT, summationPane, referencePane);
		jsp.setResizeWeight(0.80);
        // Set a larget size so that it can occupy more space
		jsp.setOneTouchExpandable(true);
        jsp.setDividerSize(4);
        add(jsp, BorderLayout.CENTER);
	}
    
    /**
     * Override this method to control JSplitPane's divider position.
     */
    public void doLayout() {
        super.doLayout();
        int height = getHeight();
        if (height == 0)
            return; 
        // To control divider location
        jsp.setDividerLocation(0.75);
    }
	
	private ReferencePane createReferencePane() {
		final ReferencePane referencePane = new ReferencePane();
		referencePane.setTitle("References for Description");
        //TODO: This might not be needed. Trigger an uncessary changes
        // when the data in list is reset.
		referencePane.addReferenceDataListener(new ListDataListener() {
            public void contentsChanged(ListDataEvent e) {
                doReferencesChanged();
            }
            public void intervalAdded(ListDataEvent e) {
                doReferencesChanged();
            }
            public void intervalRemoved(ListDataEvent e) {
                doReferencesChanged();
            }	
		});
		return referencePane;		
	}
    
    private void doReferencesChanged() {
        if (r == null || duringSetting)
            return;
        java.util.List references = referencePane.getReferences();
        Summation summation = (Summation) r.getAttributeValue(RenderablePropertyNames.SUMMATION);
        Summation newSummation = null;
        if (summation != null)
            newSummation = (Summation)summation.clone();
        if (references == null || references.size() == 0) {
            if (newSummation != null)
                newSummation.setReferences(null);
        }
        else {
            if (newSummation == null) {
                newSummation = new Summation();
            }
            newSummation.setReferences(references);
        }
        isDirty = true;
        fireRenderablePropertyChange(r, 
                                     RenderablePropertyNames.SUMMATION, 
                                     summation, 
                                     newSummation);        
    }
	
	private JPanel createSummationPane() {
		JPanel summationPane = new JPanel();
		summationPane.setLayout(new BorderLayout());
		titleLabel = new JLabel("Description");
		titleLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		JPanel northPane = new JPanel();
		northPane.setLayout(new BorderLayout());
		northPane.add(titleLabel, BorderLayout.WEST);
		// Add a toolbar
		JToolBar toolbar = new JToolBar();
        toolbar.setRollover(true);
        toolbar.setBorderPainted(false); // No border for toolbar
        toolbar.setFloatable(false);
		toolbar.setRollover(true);
		Dimension btnSize = new Dimension(18, 18);
		final JButton cutBtn = new JButton(AuthorToolAppletUtilities.createImageIcon("Cut16.gif"));
		cutBtn.setPreferredSize(btnSize);
		cutBtn.setToolTipText("Cut");
		cutBtn.setActionCommand("cut");
		toolbar.add(cutBtn);
		final JButton copyBtn = new JButton(AuthorToolAppletUtilities.createImageIcon("Copy16.gif"));
		copyBtn.setPreferredSize(btnSize);
		copyBtn.setToolTipText("Copy");
		copyBtn.setActionCommand("copy");
		toolbar.add(copyBtn);
		pasteBtn = new JButton(AuthorToolAppletUtilities.createImageIcon("Paste16.gif"));
		pasteBtn.setPreferredSize(btnSize);
		pasteBtn.setToolTipText("Paste");
		pasteBtn.setActionCommand("paste");
		toolbar.add(pasteBtn);
		// Set up ActionListners for buttons
		ActionListener l = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String command = e.getActionCommand();
				if (command.equals("cut")) {
					textPane.cut();
					pasteBtn.setEnabled(true);
				}
				else if (command.equals("paste")) 
					textPane.paste();
				else if (command.equals("copy")) { 
					textPane.copy();
					pasteBtn.setEnabled(true);
				}
			}
		};
		cutBtn.addActionListener(l);
		copyBtn.addActionListener(l);
		pasteBtn.addActionListener(l);
		// Disable these buttons first
		cutBtn.setEnabled(false);
		pasteBtn.setEnabled(false);
		copyBtn.setEnabled(false);
		northPane.add(toolbar, BorderLayout.EAST);
		summationPane.add(northPane, BorderLayout.NORTH);
		textPane = new JTextPane();
		summationPane.add(new JScrollPane(textPane), BorderLayout.CENTER);
		textPane.getDocument().addDocumentListener(new DocumentListener() {
			// To record the old Summation value
			public void changedUpdate(DocumentEvent e) {
				// Check selection
				String selectedText = textPane.getSelectedText();
				if (selectedText == null || selectedText.length() == 0) {
					cutBtn.setEnabled(false);
					copyBtn.setEnabled(false);
				}
				else {
					cutBtn.setEnabled(true);
					copyBtn.setEnabled(true);
				}
			}
			public void insertUpdate(DocumentEvent e) {
				if (r != null) {
					doTextChange();
				}
			}
			public void removeUpdate(DocumentEvent e) {
				if (r != null) {
					doTextChange();
				}
			}
		});
		textPane.addCaretListener(new CaretListener() {
			public void caretUpdate(CaretEvent e) {
				String selectedText = textPane.getSelectedText();
				if (selectedText == null || selectedText.length() == 0) {
					cutBtn.setEnabled(false);
					copyBtn.setEnabled(false);
				}
				else {
					cutBtn.setEnabled(true);
					copyBtn.setEnabled(true);
				}
			}
		});
		pasteBtnHandler = new ComponentAdapter() {
			public void componentShown(ComponentEvent e) {
				Clipboard clipBoard = Toolkit.getDefaultToolkit().getSystemClipboard();
				try {
					if (clipBoard.getContents(null) != null) {
						Transferable contents = clipBoard.getContents(null);
						Object obj = contents.getTransferData(DataFlavor.stringFlavor);
						if (obj == null)
							pasteBtn.setEnabled(false);
						else
							pasteBtn.setEnabled(true);
					}
				}
				catch (Exception e1) {
					//System.err.println("TextSummation.init(): " + e1);
				}
				//Do once only
				removeComponentListener(pasteBtnHandler);
				pasteBtnHandler = null;	
			}
			
		};
		addComponentListener(pasteBtnHandler);
		return summationPane;
	}
	
	/**
	 * A helper to catch the text change in the textpane.
	 */
	private void doTextChange() {
        if (duringSetting)
            return;
		Summation summation = (Summation) r.getAttributeValue(RenderablePropertyNames.SUMMATION);
		String newText = textPane.getText().trim();
		if (newText.length() == 0) {
			if (summation == null)
				return;
			if (summation != null && summation.getText() == null)
				return;
			newText = null;
		}
		Summation newSummation = new Summation();
		if (summation != null) { // Need to take DB_ID
			newSummation.setDB_ID(summation.getDB_ID());
			newSummation.setReferences(summation.getReferences());	
		}
		newSummation.setText(newText);
        isDirty = true;
        fireRenderablePropertyChange(r, RenderablePropertyNames.SUMMATION, summation, newSummation);
	}
	
	public void updateName() {
		if (r == null)
			titleLabel.setText("Description");
		else
			titleLabel.setText("Description for " + r.getDisplayName());
	}
	
	public void setRenderable(Renderable newRenderable) {
        super.setRenderable(newRenderable);
        if (r == null)
            return;
        duringSetting = true;
		if (newRenderable instanceof Shortcut)
			r = ((Shortcut)newRenderable).getTarget();
		else
			r = newRenderable;
		Summation summation = (Summation) r.getAttributeValue("summation");
		if (summation == null) {
			textPane.setText("");
			referencePane.setReferences(null);
		}
		else {
			textPane.setText(summation.getText());
			// For references
			referencePane.setReferences(summation.getReferences());
		}
        duringSetting = false;
	}
	
	public void refresh() {
		if (r == null)
			return;
		Summation summation = (Summation) r.getAttributeValue("summation");
		if (summation == null) {
			textPane.setText("");
			referencePane.setReferences(null);
		}
		else {
			textPane.setText(summation.getText());
			referencePane.setReferences(summation.getReferences());
		}
	}
	
	public void commit() {
		if (r == null || !isDirty)
			return;
		Summation summation = (Summation) r.getAttributeValue(RenderablePropertyNames.SUMMATION);
		String text = getText();
		java.util.List references = referencePane.getReferences();
		if (summation == null) {
			if (text != null || references != null) {
				summation = new Summation();
				r.setAttributeValue(RenderablePropertyNames.SUMMATION, summation);
				summation.setText(text);
				summation.setReferences(references);
			}
		}
		else {
			if (text == null && references == null && summation.getDB_ID() == null) {
				// Remove it from attibutes
				r.setAttributeValue(RenderablePropertyNames.SUMMATION, null);
			}
			else {
				summation.setText(text);
				summation.setReferences(references);
			}
		}
        summation.setIsChanged(true);
        r.setIsChanged(true);
	}
	
	public String getText() {
		String text = textPane.getText().trim();
		if (text.length() == 0)
			return null;
		return text;
	}
	
	public void setTitle(String title) {
		titleLabel.setText(title);
	}
}
