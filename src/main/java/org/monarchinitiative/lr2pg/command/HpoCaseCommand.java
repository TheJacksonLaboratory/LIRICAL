package org.monarchinitiative.lr2pg.command;

import com.github.phenomics.ontolib.ontology.data.ImmutableTermPrefix;
import com.github.phenomics.ontolib.ontology.data.TermPrefix;
import org.monarchinitiative.lr2pg.hpo.Disease2TermFrequency;
import org.monarchinitiative.lr2pg.hpo.HpoCase;

/**
 * Analyze the likelihood ratios for a case represented by a list of HPO terms.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 * @version 0.0.2 (2017-11-24)
 */
public class HpoCaseCommand extends Command {

    HpoCase hpocase =null;

    public HpoCaseCommand(String hpoPath, String annotationPath, String caseData) {
       // hpocase = new HpoCase(hpoPath,annotationPath,caseData);
    }


    public void execute() {
        //pocase.outputResults();
        System.err.println("TODO REFACTOR ME");
    }

}
