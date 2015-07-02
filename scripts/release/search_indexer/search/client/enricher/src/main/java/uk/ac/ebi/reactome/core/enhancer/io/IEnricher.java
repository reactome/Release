package uk.ac.ebi.reactome.core.enhancer.io;

import uk.ac.ebi.reactome.core.enhancer.exception.EnricherException;
import uk.ac.ebi.reactome.core.model.result.EnrichedEntry;

/**
 * Interface for accessing Enricher
 *
 * @author Florian Korninger (fkorn@ebi.ac.uk)
 * @version 1.0
 */
public interface IEnricher {

    public EnrichedEntry enrichEntry(String dbId) throws EnricherException;

}
