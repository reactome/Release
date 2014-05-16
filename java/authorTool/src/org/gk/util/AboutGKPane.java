/*
 * Created on Aug 18, 2003
 */
package org.gk.util;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.border.BevelBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;


/**
 * A customized JDialog to display about information.
 * @author wgm
 */
public class AboutGKPane extends JPanel {
	private JScrollPane jsp = null;
	private JEditorPane textPane = null;
	private JLabel statusLabel;
	// For scrollthread
	private Point p;
	private boolean isRunning = false;
	private Runnable scrollThread = null;
	// Application title
	private String applicationTitle = "Reactome Tool";
	private String version = "1.0";
	private int build = 1;
	
	public AboutGKPane() {
		init();
	}
	
	public AboutGKPane(boolean isForSplash, boolean needStatus) {
		if (isForSplash)
			initForSplash(needStatus);
		else
			init();
	}
	
	public void setApplicationTitle(String title) {
		this.applicationTitle = title;
	}
	
	public void setVersion(String version) {
		this.version = version;
	}
	
	public void setBuildNumber(int build) {
		this.build = build;
	}
	
	public void displayInDialog(Frame parentFrame) {
		final JDialog dialog = new JDialog(parentFrame);
		dialog.setTitle("About Reactome");
		dialog.getContentPane().add(this, BorderLayout.CENTER);
		
		JPanel southPane = new JPanel();
		JButton closeBtn = new JButton("Close");
		closeBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				isRunning = false;
				dialog.dispose();
			}
		});
		southPane.setLayout(new FlowLayout(FlowLayout.RIGHT, 8, 8));
		southPane.add(closeBtn);
		dialog.getContentPane().add(southPane, BorderLayout.SOUTH);
		
		jsp.setPreferredSize(new Dimension(386, 60));
		jsp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		jsp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
		jsp.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
		p = new Point(0, 0);
		jsp.getViewport().setViewPosition(p);
		textPane.setBackground(new Color(236, 231, 231));
		dialog.setSize(386, 300); // Should not change this width. The width is determined by icon width.		
		dialog.setModal(true);
		dialog.setLocationRelativeTo(parentFrame);
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				isRunning = false;
			}
		});
		textPane.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				isRunning = !isRunning;
				if (isRunning)
					new Thread(scrollThread).start();
			}
		});
		scrollThread = new Runnable() {
			public void run() {
				while (isRunning) {
					if (p.y > textPane.getHeight())
						p.y = 0;
					jsp.getViewport().setViewPosition(p);
					p.y += 1;
					try {
						Thread.sleep(100);
					}
					catch(InterruptedException e) {}
				}
			}
		};
		isRunning = true;
		new Thread(scrollThread).start();
		dialog.setVisible(true);
	}
	
	private void init() {
		setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
		setLayout(new BorderLayout());
		// For loading image.
		ImageIcon icon = GKApplicationUtilities.createImageIcon(getClass(), "GKB.gif");
		Image image = icon.getImage();
		ImagePanel imagePane = new ImagePanel(image);
		add(imagePane, BorderLayout.CENTER);
		// Add a rolling display pane
		textPane = createDisplayPane();
		jsp = new JScrollPane(textPane);
		add(jsp, BorderLayout.SOUTH);
	}
	
	private void initForSplash(boolean needStatus) {
		setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
		setLayout(new BorderLayout());
		// For loading image.
		ImageIcon icon = AuthorToolAppletUtilities.createImageIcon("GKBSplash.gif");
		Image image = icon.getImage();
		ImagePanel imagePane = new ImagePanel(image);
		add(imagePane, BorderLayout.CENTER);	
		//add(createDisplayPane(), BorderLayout.CENTER);
		// For status
		if (needStatus) {
			statusLabel = new JLabel("Status");
			statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
			add(statusLabel, BorderLayout.SOUTH);
		}
	}
	
	public void setStatus(String message) {
		if (statusLabel != null)
			statusLabel.setText(message);
	}
	
	private JEditorPane createDisplayPane() {
		JEditorPane textPane = new JEditorPane();
		textPane.setContentType("text/html");
//		textPane.setText(
//			"The <a href=\"http://www.reactome.org\">Reactome</a> project is a collaboration among "
//				+ "<a href=\"http://www.cshl.org\">Cold Spring Harbor Laboratory</a>, <a href=\"http://www.ebi.ac.uk\">The European Bioinformatics Institute</a>, "
//				+ "and <a href=\"http://www.geneontology.org\">The Gene Ontology Consortium</a> to develop a curated resource of core "
//				+ "pathways and reactions in human biology. The information in this database"
//				+ " is authored by biological researchers with expertise in their field, "
//				+ "maintained by the Reactome editorial staff, and cross-referenced with the "
//				+ "sequence databases Ensembl and SwissProt.");
        textPane.setText(
        "The <b>Reactome</b> project is a collaboration among <a href=\"http://www.cshl.org\">Cold Spring Harbor Laboratory</a>, <a href=\"http://www.ebi.ac.uk\">"
        + "The European Bioinformatics Institute</a>, "
        + "and <a href=\"http://www.geneontology.org\">The Gene Ontology Consortium</a> to develop a curated resource of core pathways and reactions in human biology. "
        + "The information in this database is authored by biological researchers with expertise in their fields, maintained by the Reactome editorial staff, and "
        + "cross-referenced with the sequence databases at <a href=\"http://www.ncbi.nlm.nih.gov\">NCBI</a>, <a href=\"http://www.ensembl.org\">Ensembl</a> and "
        + "<a href=\"http://www.ebi.uniprot.org\">UniProt</a>, the <a href=\"http://genome.ucsc.edu/cgi-bin/hgGateway\">UCSC Genome Browser</a>, " 
        + "<a href=\"http://www.hapmap.org\">HapMap</a>, KEGG (<a href=\"http://www.genome.jp/kegg/genes.html\">Gene</a> and " +
                "<a href=\"http://www.genome.jp/dbget-bin/www_bfind?compound\">Compound</a>), "
        + "<a href=\"http://www.ebi.ac.uk/chebi/init.do\">ChEBI</a>, <a href=\"http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?CMD=&amp;DB=PubMed\">PubMed</a> "
        + "and <a href=\"http://www.geneontology.org\">GO</a>. " 
        + "In addition to curated human events, <a href=\"../electronic_inference.html\">inferred orthologous events</a> "
        + "in 22 non-human species including mouse, rat, chicken, puffer fish, worm, fly, yeast, two plants and E.coli are also available. "
        + "A description of Reactome has been published in "
        + "<b><a href=\"http://genomebiology.com/2007/8/3/r39\">Genome Biology</a></b>."
        );
		textPane.setEditable(false);
		// Add html listener
		textPane.addHyperlinkListener(new HyperlinkListener() {
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
					String link = e.getURL().toExternalForm();
					try {
						BrowserLauncher.displayURL(link, AboutGKPane.this);
					}
					catch (IOException e1) {
						System.err.println("AboutGKDialog.createDisplayPane(): " + e1);
						e1.printStackTrace();
					}
				}
			}
		});
		return textPane;
	}
	
	class ImagePanel extends JPanel {
		Image image = null;
		
		public ImagePanel(Image image) {
			this.image = image;
		}
		
		public void paint(Graphics g) {
			Graphics2D g2 = (Graphics2D) g;
			g2.setPaint(Color.white);
			g2.fill(new Rectangle(0, 0, getWidth(), getHeight()));
			if (image != null) {
				g2.drawImage(image, 0, 0, this);
			}
			Font f = g2.getFont();
			f = f.deriveFont(Font.BOLD, 14.0f);
			g2.setFont(f);
			g2.setPaint(Color.BLACK);
			g2.drawString(applicationTitle, 126, 23);
			g2.drawString("Version: " + version, 126, 42);
			g2.drawString("Build: " + build, 126, 62);
		}
	}

}
