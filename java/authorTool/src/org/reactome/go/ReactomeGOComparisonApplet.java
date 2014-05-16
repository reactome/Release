/*
 * Created on Jun 16, 2004
 */
package org.reactome.go;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.gk.persistence.MySQLAdaptor;
import org.gk.util.GKApplicationUtilities;

/**
 * 
 * @author wugm
 */
public class ReactomeGOComparisonApplet extends JApplet {
	
	private ReactomeGOComparisonPane centerPane;
	private MessagePane messagePane;
	private boolean needDBHost = false;

	public ReactomeGOComparisonApplet() {
		super();
		AppletHelper helper = new AppletHelper(this);
		AppletHelper.setHelper(helper);
	}
	
	public ReactomeGOComparisonApplet(boolean isStandAlone) {
	    super();
	    init(true);
	}
	
	public void init() {
		// Ask the user to input user name and pwd for the database
		final ConnectionPane connPane = new ConnectionPane(needDBHost);
		getContentPane().add(connPane, BorderLayout.CENTER);
		connPane.dbNameTF.requestFocus();
		connPane.addOKActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String userName = connPane.getUserName();
				String pwd = connPane.getPassword();
				String dbName = connPane.getDBName();
				String dbHost = connPane.getDBHost();
				if (userName.length() > 0 && pwd.length() > 0 && dbName.length() > 0) {
					getContentPane().remove(connPane);
					getContentPane().validate();
					getContentPane().repaint();
					fetchData(connPane.getDBHost(), dbName, userName, pwd);
				}
			}
		});
	}
	
	private void init(boolean isStandalone) {
	    this.needDBHost = isStandalone;
	    init();
	}
	
	public void destroy() {
		centerPane.cleanUp();
	}
	
	private void fetchData(final String dbHost, final String dbName, final String userName, final String pwd) {
		messagePane = new MessagePane();
		messagePane.setMessage("<html>Please wait. <br>Data is being loaded from the database...</html>");
		getContentPane().add(messagePane, BorderLayout.CENTER);
		getContentPane().validate();
		getContentPane().repaint();
		Thread t = new Thread() {
			public void run() {
				try {
					String tmpDBHost = dbHost;
				    if (tmpDBHost == null) // It might be run in an Applet
					    tmpDBHost = getParameter("dbHost");
					if (tmpDBHost == null)
					    throw new IllegalStateException("No database host is defined!");
					MySQLAdaptor dba = new MySQLAdaptor(tmpDBHost, dbName, userName, pwd, 3306);
					centerPane = new ReactomeGOComparisonPane();
					centerPane.setMySQLAdaptor(dba);
					getContentPane().remove(messagePane);
					getContentPane().add(centerPane, BorderLayout.CENTER);
					getContentPane().validate();
					getContentPane().repaint();
				}
				catch (Exception e) {
					messagePane.setMessage("<html>Exception during connecting to the database:<br>" +						                    e.getMessage() + "</html>");
					System.err.println("ReactomeGOComparisonApplet.init(): " + e);
					e.printStackTrace();
				}
			}
		};
		t.start();
	}
	
	public static void main(String[] args) {
	    JFrame frame = new JFrame("Reactome GO Comparison");
	    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    ReactomeGOComparisonApplet applet = new ReactomeGOComparisonApplet(true);
	    frame.getContentPane().add(applet, BorderLayout.CENTER);
	    frame.setSize(800, 600);
	    GKApplicationUtilities.center(frame);
	    frame.setVisible(true);
	}
	
	class ConnectionPane extends JPanel {
	    private JTextField dbHostTF;
		private JPasswordField pwdTF;
		private JTextField userTF;
		private JTextField dbNameTF;
		private JButton okBtn;
		private boolean needHostName = false;
		private boolean isOKClicked = false;
	
		public ConnectionPane(boolean needDBHost) {
			this.needHostName = needDBHost;
		    init();
		}
		
		private void init() {
			setLayout(new GridBagLayout());
			GridBagConstraints constraints = new GridBagConstraints();
			constraints.insets = new Insets(4, 4, 4, 4);
			JLabel caption = new JLabel("Please input database connection information:");
			constraints.gridwidth = 2;
			add(caption, constraints);
			if (needHostName) {
			    JLabel hostLbl = new JLabel("Database Host:");
			    constraints.gridx = 0;
			    constraints.gridy = 1;
			    constraints.fill = GridBagConstraints.HORIZONTAL;
			    constraints.anchor = GridBagConstraints.EAST;
			    add(hostLbl, constraints);
			    dbHostTF = new JTextField();
			    constraints.gridx = 1;
			    add(dbHostTF, constraints);
			}
			JLabel dbNameLbl = new JLabel("Database Name:");
			constraints.gridx = 0;
			constraints.gridy = 2;
			constraints.gridwidth = 1;
			constraints.fill = GridBagConstraints.HORIZONTAL;
			constraints.anchor = GridBagConstraints.EAST;
			add(dbNameLbl, constraints);
			Dimension tfSize = new Dimension(120, 25);
			dbNameTF = new JTextField();
			dbNameTF.setPreferredSize(tfSize);
			constraints.gridx = 1;
			add(dbNameTF, constraints);
			JLabel userLbl = new JLabel("User Name:");
			constraints.gridy = 3;
			constraints.gridx = 0;
			add(userLbl, constraints);
			userTF = new JTextField();
			constraints.gridx = 1;
			userTF.setPreferredSize(tfSize);
			add(userTF, constraints);
			JLabel pwdLbl = new JLabel("Password:");
			constraints.gridx = 0;
			constraints.gridy = 4;
			add(pwdLbl, constraints);
			pwdTF = new JPasswordField();
			pwdTF.setPreferredSize(tfSize);
			constraints.gridx = 1;
			add(pwdTF, constraints);
			
			// OKBtn
			okBtn = new JButton("OK");
			okBtn.setMnemonic('O');
			okBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					isOKClicked = true;
				}
			});
			constraints.gridx = 0;
			constraints.gridy = 5;
			constraints.anchor = GridBagConstraints.CENTER;
			constraints.fill = GridBagConstraints.NONE;
			constraints.insets = new Insets(8, 4, 4, 4);
			constraints.anchor = GridBagConstraints.CENTER;
			constraints.gridwidth = 2;
			add(okBtn, constraints);
		}
		
		public void addOKActionListener(ActionListener l) {
			okBtn.addActionListener(l);
			pwdTF.addActionListener(l);
			userTF.addActionListener(l);
			dbNameTF.addActionListener(l);
		}
		
		public boolean isOKClicked() {
			return isOKClicked;
		}
		
		public String getDBName() {
			return dbNameTF.getText().trim();
		}
		
		public String getUserName() {
			return userTF.getText().trim();
		}
		
		public String getPassword() {
			char[] pwd = pwdTF.getPassword();
			return new String(pwd);
		}
		
		public String getDBHost() {
		    if (dbHostTF == null)
		        return null;
		    return dbHostTF.getText().trim();
		}
	}
	
	class MessagePane extends JPanel {
		private JLabel msgLabel;
		
		public MessagePane() {
			init();
		}
		
		private void init() {
			setLayout(new GridBagLayout());
			GridBagConstraints constraints = new GridBagConstraints();
			msgLabel = new JLabel();
			add(msgLabel, constraints);
		}
		
		public void setMessage(String msg) {
			msgLabel.setText(msg);
		}
	}

}
