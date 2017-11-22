package org.monarchinitiative.lr2pg.command;

import com.github.phenomics.ontolib.ontology.data.ImmutableTermPrefix;
import com.github.phenomics.ontolib.ontology.data.TermPrefix;
import org.monarchinitiative.lr2pg.hpo.Disease2TermFrequency;

/**
 * Analyze the likelihood ratios for a case represented by a list of HPO terms.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 * @version 0.0.2 (2017-11-24)
 */
public class CaseCommand extends Command {


    private static TermPrefix HP_PREFIX = new ImmutableTermPrefix("HP");
    private Disease2TermFrequency disease2TermFrequencyMap=null;

    public CaseCommand(String hpoPath, String annotationPath) {
        this.disease2TermFrequencyMap= new Disease2TermFrequency(hpoPath,annotationPath);
    }


    public void execute() {

    }

}
