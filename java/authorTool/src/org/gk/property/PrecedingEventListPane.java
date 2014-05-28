/*
 * Created on Mar 9, 2004
 */
package org.gk.property;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.gk.render.RenderUtility;
import org.gk.render.Renderable;
import org.gk.render.RenderablePropertyNames;
import org.gk.util.AuthorToolAppletUtilities;

/**
 * A customized JPanel for precedingEvent properties for RenderablePathway
 * and RenderableReaction objects.
 * @author wugm
 */
public class PrecedingEventListPane extends RenderablePropertyPane {
	private JList precedingList;
	private JButton addBtn;
	private JButton removeBtn;
	// All events that the user can select from.
	private java.util.List allEvents;

	public PrecedingEventListPane() {
		init();
	}
	
	private void init() {
		setBorder(BorderFactory.createEtchedBorder());
		setLayout(new BorderLayout());
		JPanel northPane = new JPanel();
		northPane.setLayout(new BorderLayout());
		// Add name label
		JLabel label = new JLabel("Preceding events not in the containing pathway:");
		label.setHorizontalAlignment(JLabel.LEFT);
		label.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		northPane.add(label, BorderLayout.WEST);
		// Add control buttons
		JToolBar toolbar = new JToolBar();
		toolbar.setRollover(true);
		toolbar.setBorderPainted(false); // No border for toolbar
		toolbar.setFloatable(false);
		Dimension btnSize = new Dimension(18, 18);
		addBtn = new JButton(AuthorToolAppletUtilities.createImageIcon("Add16.gif"));
		addBtn.setPreferredSize(btnSize);
		addBtn.setActionCommand("add");
		addBtn.setToolTipText("Add new preceding instance");
		toolbar.add(addBtn);
		removeBtn = new JButton(AuthorToolAppletUtilities.createImageIcon("Remove16.gif"));
		removeBtn.setPreferredSize(btnSize);
		removeBtn.setActionCommand("remove");
		removeBtn.setToolTipText("Remove selected instances");
		toolbar.add(removeBtn);
		removeBtn.setEnabled(false);
		northPane.add(toolbar, BorderLayout.EAST);
		add(northPane, BorderLayout.NORTH);
		// Add value list
		precedingList = new JList();
		precedingList.setToolTipText("Use the buttons at the top-right corner for editing");
		precedingList.setModel(new DefaultListModel());
		add(new JScrollPane(precedingList), BorderLayout.CENTER);
		installListeners();
	}
	
	private void installListeners() {
		ActionListener actionListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String cmd = e.getActionCommand();
				if (cmd.equals("add")) {
					addPrecedingEvents();
				}
				else if (cmd.equals("remove")) {
					removePrecedingEvents();
				}
			}
		};
		addBtn.addActionListener(actionListener);
		removeBtn.addActionListener(actionListener);
		precedingList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (precedingList.getSelectedValue() != null)
					removeBtn.setEnabled(true);
				else
					removeBtn.setEnabled(false);
			}
		});
        precedingList.getModel().addListDataListener(new ListDataListener() {

            public void contentsChanged(ListDataEvent arg0) {
                doValueChanged();
            }

            public void intervalAdded(ListDataEvent arg0) {
                doValueChanged();
            }

            public void intervalRemoved(ListDataEvent arg0) {
                doValueChanged();
            }
        });
	}
    
    private void doValueChanged() {
        if (duringSetting)
            return;
        List old = (List) r.getAttributeValue(RenderablePropertyNames.PRECEDING_EVENT);
        List newList = getPrecedingEvents();
        r.setAttributeValue(RenderablePropertyNames.PRECEDING_EVENT, newList);
        fireRenderablePropertyChange(r,
                                     RenderablePropertyNames.PRECEDING_EVENT,
                                     old,
                                     newList);
    }
	
	private void addPrecedingEvents() {
		Component parentComp = SwingUtilities.getRoot(this);
		AddPrecedingEventDialog addDialog = null;
		if (parentComp instanceof Dialog)
			addDialog = new AddPrecedingEventDialog((Dialog)parentComp);
		else if (parentComp instanceof Frame)
			addDialog = new AddPrecedingEventDialog((Frame)parentComp);
		else {
			addDialog = new AddPrecedingEventDialog();
            //throw new IllegalStateException("PrecedingEventListPane.addPrecedingEvents(): cannot make this call.");
		}
		addDialog.setLocationRelativeTo(this);
		addDialog.setSize(400, 300);
		addDialog.setModal(true);
		addDialog.setVisible(true);
		if (addDialog.isOKClicked) {
			java.util.List selectedEvents = addDialog.getSelectedEvents();
			if (selectedEvents.size() > 0) {
				DefaultListModel model = (DefaultListModel) precedingList.getModel();
				for (Iterator it = selectedEvents.iterator(); it.hasNext();) {
					model.addElement(it.next());
				}
			}
		}
	}
	
	private void removePrecedingEvents() {
		// Remove the selected events
		if (precedingList.getSelectedValues() != null) {
			Object[] selectedValues = precedingList.getSelectedValues();
			DefaultListModel model = (DefaultListModel) precedingList.getModel();
			for (int i = 0; i < selectedValues.length; i++)
				model.removeElement(selectedValues[i]);
		}
    }
	
	/**
	 * Get a list of events that are selected by the user.
	 * @return a list of event.
	 */
	public java.util.List getPrecedingEvents() {
		DefaultListModel model = (DefaultListModel) precedingList.getModel();
		java.util.List list = new ArrayList(model.size());
		for (int i = 0; i < model.size(); i++) {
			list.add(model.get(i));
		}
		return list;
	}

	private void setPrecedingEvents(java.util.List list) {
		DefaultListModel model = (DefaultListModel) precedingList.getModel();
		model.clear();
		if (list != null) {
		    for (Iterator it = list.iterator(); it.hasNext();) {
		        model.addElement(it.next());
		    }
		}
	}
	
	public JList getPrecedingList() {
		return this.precedingList;
	}
    
	public void setRenderable(Renderable r) {
        super.setRenderable(r);
        duringSetting = true;
        extractPrecedingEventInfo(r);
        duringSetting = false;
    }

    private void extractPrecedingEventInfo(Renderable r) {
        // Get the top-level container
        Renderable topContainer = r;
        while (topContainer.getContainer() != null)
            topContainer = topContainer.getContainer();
        allEvents = RenderUtility.getAllEvents(r);
        if (r.getContainer() != null && 
            r.getContainer().getComponents() != null) {
            allEvents.removeAll(r.getContainer().getComponents());
        }
        // Make sure r self is not in the list
        for (Iterator it = allEvents.iterator(); it.hasNext();) {
            Renderable event = (Renderable) it.next();
            if (event.getDisplayName().equals(r.getDisplayName()))
                it.remove();
        }
        // Sorting them
        Comparator comparator = new Comparator() {
            public int compare(Object obj1, Object obj2) {
                Renderable r1 = (Renderable) obj1;
                Renderable r2 = (Renderable) obj2;
                return r1.getDisplayName().compareTo(r2.getDisplayName());
            }
        };
        Collections.sort(allEvents, comparator);
        java.util.List unconnectInputs = (java.util.List) r.getAttributeValue("precedingEvent");
        setPrecedingEvents(unconnectInputs);
    }    

    class AddPrecedingEventDialog extends JDialog {
		private boolean isOKClicked = false;
		private JList resultList;
		private JList eventList;
		
        public AddPrecedingEventDialog() {
            setTitle("Add Preceding Reactions or Pathways");
            init();
            initEvents();
        }
        
		public AddPrecedingEventDialog(Dialog parentDialog) {
			super(parentDialog, "Add Preceding Reactions or Pathways");
			init();
			initEvents();
		}
		
		public AddPrecedingEventDialog(Frame parentFrame) {
			super(parentFrame, "Add Preceding Reactions or Pathways");
			init();
			initEvents();
		}
		
		private void init() {
			JPanel contentPane = new JPanel();
			contentPane.setBorder(BorderFactory.createRaisedBevelBorder());
			contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
			JPanel workPane = new JPanel();
			workPane.setLayout(new BorderLayout());
			JLabel label1 = new JLabel("Please select reactions or pathways:");
			Border labelBorder = BorderFactory.createEmptyBorder(2, 2, 2, 2);
			label1.setBorder(labelBorder);
			workPane.add(label1, BorderLayout.NORTH);
			eventList = new JList();
			eventList.setModel(new DefaultListModel());
			workPane.add(new JScrollPane(eventList), BorderLayout.CENTER);
			JPanel resultPane = new JPanel();
			resultPane.setLayout(new BorderLayout());
			JLabel label2 = new JLabel("Selected reactions or pathways:");
			label2.setBorder(labelBorder);
			resultPane.add(label2, BorderLayout.NORTH);
			resultList = new JList();
			resultList.setModel(new DefaultListModel());
			resultPane.add(new JScrollPane(resultList), BorderLayout.CENTER);
			workPane.setPreferredSize(new Dimension(400, 200));
			resultPane.setPreferredSize(new Dimension(400, 75));
			contentPane.add(workPane); 
			contentPane.add(Box.createVerticalStrut(6));
			contentPane.add(resultPane);
			// Add two control buttons
			JPanel controlPane = new JPanel();
			controlPane.setLayout(new FlowLayout(FlowLayout.RIGHT, 8, 8));
			JButton okBtn = new JButton("OK");
			okBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					isOKClicked = true;
					dispose();
				}
			});
			okBtn.setMnemonic('O');
			okBtn.setDefaultCapable(true);
			getRootPane().setDefaultButton(okBtn);
			JButton cancelBtn = new JButton("Cancel");
			cancelBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					isOKClicked = false;
					dispose();
				}
			});
			cancelBtn.setMnemonic('C');
			okBtn.setPreferredSize(cancelBtn.getPreferredSize());
			controlPane.add(okBtn);
			controlPane.add(cancelBtn);
			
			getContentPane().add(contentPane, BorderLayout.CENTER);
			getContentPane().add(controlPane, BorderLayout.SOUTH);
			
			eventList.addListSelectionListener(new ListSelectionListener() {
				public void valueChanged(ListSelectionEvent e) {
					validateResultList();
				}
			});
		}
		
		private void validateResultList() {
			DefaultListModel model = (DefaultListModel) resultList.getModel();
			model.removeAllElements();
			Object[] selectedItems = eventList.getSelectedValues();
			for (int i = 0; i < selectedItems.length; i++)
				model.addElement(selectedItems[i]);
		}
		
		private void initEvents() {
			DefaultListModel model = (DefaultListModel) eventList.getModel();
			for (Iterator it = allEvents.iterator(); it.hasNext();) {
				model.addElement(it.next());
			}
		}
		
		public java.util.List getSelectedEvents() {
			DefaultListModel model = (DefaultListModel) resultList.getModel();
			int size = model.size();
			java.util.List events = new ArrayList(size); 
			for (int i = 0; i < size; i++) {
				events.add(model.get(i));
			}
			return events;
		}
		
	}
}
