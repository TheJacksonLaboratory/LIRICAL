package org.monarchinitiative.lr2pg.cmd;


import org.monarchinitiative.lr2pg.exception.Lr2pgException;
import org.monarchinitiative.lr2pg.hpo.HpoCase;
import org.monarchinitiative.lr2pg.hpo.PhenotypeOnlyHpoCaseSimulator;
import org.monarchinitiative.lr2pg.svg.Lr2Svg;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.formats.hpo.HpoOntology;
import org.monarchinitiative.phenol.ontology.data.TermId;

/**
 * Simulate a single case and produce an SVG output file.
 * TODO -- make output file name flexible.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class SimulateSvgPhenoOnlyCommand extends SimulatePhenotypesCommand {

    private TermId diseaseCurie;

    public SimulateSvgPhenoOnlyCommand(String dataDirPath, String diseaseId){
        super(dataDirPath);
        this.diseaseCurie=TermId.of(diseaseId);
    }

    public void run() throws Lr2pgException {
        PhenotypeOnlyHpoCaseSimulator phenotypeOnlyHpoCaseSimulator = getPhenotypeOnlySimulator();
        HpoDisease disease = phenotypeOnlyHpoCaseSimulator.name2disease(diseaseCurie);
        if (disease==null) {
            throw new Lr2pgException("Could not find disease for " + diseaseCurie.getValue());
        }
        phenotypeOnlyHpoCaseSimulator.simulateCase(disease);
        HpoCase hpocase = phenotypeOnlyHpoCaseSimulator.getCurrentCase();
        HpoOntology ontology = phenotypeOnlyHpoCaseSimulator.getOntology();
        Lr2Svg l2svg = new Lr2Svg(hpocase, diseaseCurie, disease.getName(), ontology, null);
        l2svg.writeSvg("test.svg");
    }
}
