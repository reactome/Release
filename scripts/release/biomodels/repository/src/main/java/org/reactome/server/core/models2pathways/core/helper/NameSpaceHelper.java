package org.reactome.server.core.models2pathways.core.helper;

import org.reactome.server.core.models2pathways.core.model.Namespace;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class NameSpaceHelper {

    private static NameSpaceHelper nameSpaceHelper = new NameSpaceHelper();
    private Set<Namespace> namespaces;

    private NameSpaceHelper() {
    }

    public static NameSpaceHelper getInstance() {
        return nameSpaceHelper;
    }

    public Set<Namespace> getNamespaces() {
        return namespaces;
    }

    public void setNamespaces(Set<Namespace> namespaces) {
        this.namespaces = namespaces;
    }

    public Namespace getNamespace(String namespaceName) {
        for (Namespace namespace : namespaces) {
            if (namespace.getName().equals(namespaceName)) {
                return namespace;
            }
        }
        return null;
    }

    public Set<Namespace> getNamespacesWithTrivialChemicals() {
        Set<Namespace> namespaceSet = new HashSet<>();
        for (Namespace namespace : namespaces) {
            if (namespace.hasTrivialMolecules()) {
                namespaceSet.add(namespace);
            }
        }
        return namespaceSet;
    }

    @Override
    public String toString() {
        return "NameSpaceHelper{" +
                "namespaces=" + namespaces +
                '}';
    }
}