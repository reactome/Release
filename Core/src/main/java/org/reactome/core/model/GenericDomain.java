package org.reactome.core.model;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class GenericDomain extends Domain {
	private List<Domain> hasInstance;
	
	public GenericDomain() {
	}

	public List<Domain> getHasInstance() {
		return hasInstance;
	}

	public void setHasInstance(List<Domain> hasInstance) {
		this.hasInstance = hasInstance;
	}
	
}
