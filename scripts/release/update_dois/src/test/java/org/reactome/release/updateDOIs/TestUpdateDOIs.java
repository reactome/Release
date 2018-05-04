package org.reactome.release.updateDOIs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
//import org.powermock.core.classloader.annotations.PrepareForTest;
import org.reactome.release.updateDOIs.findNewDOIsAndUpdate;

//@PrepareForTest({UpdateDOIs.class})
public class TestUpdateDOIs {

//@PrepareForTest({UpdateDOIs.class})
	private MySQLAdaptor mockAdaptor = PowerMockito.mock(MySQLAdaptor.class);
	
	@Before
	public void setup() throws Exception
	{
		PowerMockito.whenNew(MySQLAdaptor.class).withAnyArguments().thenReturn(mockAdaptor);
	}
	
//	@Test
//	public void testUpdateDOIs() throws Exception
//	{
//		UpdateDOIs check = new UpdateDOIs();
//	}
	
	@Test
	public void testfindNewDOIsAndUpdate() throws Exception 
	{
		findNewDOIsAndUpdate check = new findNewDOIsAndUpdate();
		check.setTestReactomeAdaptor(mockAdaptor);
		check.setGkCentralAdaptor(mockAdaptor);
		
		GKInstance inst1 = PowerMockito.mock(GKInstance.class);
		
		Collection<GKInstance> testResults = Arrays.asList(inst1);
		PowerMockito.when((GKInstance) inst1.getAttributeValue("stableIdentifier")).thenReturn(inst1);
		PowerMockito.when(inst1.getDisplayName()).thenReturn("HSA-12345");
		
		PowerMockito.when(inst1.getAttributeValue("name")).thenReturn("Name Value");
		PowerMockito.when(inst1.getAttributeValue("DB_ID")).thenReturn("67890");
		
		Mockito.when(mockAdaptor.fetchInstanceByAttribute("Pathway", "doi", "NOT REGEXP", "^10.3180")).thenReturn(testResults);
		Mockito.when(mockAdaptor.fetchInstanceByAttribute("Pathway", "DB_ID", "=", "67890")).thenReturn(testResults);
		
		check.findAndUpdateDOIs(12345L,"reportPath");
    }
	
	@Test
	public void testfindNewDOIsAndUpdateEmptyList() throws Exception 
	{
		findNewDOIsAndUpdate check = new findNewDOIsAndUpdate();
		check.setTestReactomeAdaptor(mockAdaptor);
		check.setGkCentralAdaptor(mockAdaptor);
		
		Mockito.when(mockAdaptor.fetchInstanceByAttribute("Pathway", "doi", "NOT REGEXP", "^10.3180")).thenReturn(new ArrayList<GKInstance>());
		Mockito.when(mockAdaptor.fetchInstanceByAttribute("Pathway", "DB_ID", "=", "67890")).thenReturn(new ArrayList<GKInstance>());
		
		check.findAndUpdateDOIs(12345L,"reportPath");
	}
	
	@Test
	public void testfindNewDOIsAndUpdateDeepEmptyList() throws Exception 
	{
		findNewDOIsAndUpdate check = new findNewDOIsAndUpdate();
		check.setTestReactomeAdaptor(mockAdaptor);
		check.setGkCentralAdaptor(mockAdaptor);
		
		GKInstance inst1 = PowerMockito.mock(GKInstance.class);
		Collection<GKInstance> testResults = Arrays.asList(inst1);
		
		PowerMockito.when((GKInstance) inst1.getAttributeValue("stableIdentifier")).thenReturn(inst1);
		PowerMockito.when(inst1.getAttributeValue("name")).thenReturn("Name Value");
		PowerMockito.when(inst1.getAttributeValue("DB_ID")).thenReturn("67890");
		
		Mockito.when(mockAdaptor.fetchInstanceByAttribute("Pathway", "doi", "NOT REGEXP", "^10.3180")).thenReturn(testResults);
		Mockito.when(mockAdaptor.fetchInstanceByAttribute("Pathway", "DB_ID", "=", "67890")).thenReturn(new ArrayList<GKInstance>());
		
		check.findAndUpdateDOIs(12345L,"reportPath");
	}
}