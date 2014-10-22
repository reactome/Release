package org.reactome.core.model;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class Domain extends DatabaseObject {
	private List<String> name;
	
	public Domain() {
	}

	public List<String> getName() {
		return name;
	}

	public void setName(List<String> name) {
		this.name = name;
	}
	
}
