/*
 * Created on Apr 20, 2004
 */
package org.gk.pathView;

import java.io.File;
import java.io.IOException;

/**
 * @author wugm
 */
public interface GeneExpressionDataImporter {

	public void setColorableEdge(java.util.List edges);
	
	public void setDataSource(File file) throws IOException;
	
	public void setDataIndex(int index);
	
	public java.util.List getDataPoints();
	
	public void resetEdges();
	
	public int getMajorTickSpacing();
	
	public boolean isDataPointsUsedForLabels();

}
