package org.monarchinitiative.lr2pg.hpo;

import com.github.phenomics.ontolib.formats.hpo.HpoTerm;
import com.github.phenomics.ontolib.formats.hpo.HpoTermRelation;
import com.github.phenomics.ontolib.ontology.data.ImmutableTermPrefix;
import com.github.phenomics.ontolib.ontology.data.Ontology;
import com.github.phenomics.ontolib.ontology.data.TermId;
import com.github.phenomics.ontolib.ontology.data.TermPrefix;
import com.google.common.collect.ImmutableMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.lr2pg.io.HpoAnnotation2DiseaseParser;
import org.monarchinitiative.lr2pg.io.HpoOntologyParser;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Creates a map from the {@code phenotype_annotation.tab} file that relates the
 * an HPO feature to the frerquency of the HPO in a disease
 */
public class Disease2TermFrequency {
    private static final Logger logger = LogManager.getLogger();

    final private String hpoOboFilePath;

    final private String hpoPhenotypeAnnotationPath;

    /** The subontology of the HPO with all the phenotypic abnormality terms. */
    private  Ontology<HpoTerm, HpoTermRelation> phenotypeSubOntology =null;
    /** The subontology of the HPO with all the inheritance terms. */
    private  Ontology<HpoTerm, HpoTermRelation> inheritanceSubontology=null;

    private  Map<String,HpoDiseaseWithMetadata> diseaseMap;

    private static TermPrefix HP_PREFIX=new ImmutableTermPrefix("HP");

    private ImmutableMap<TermId, Double> hpoTerm2OverallFrequency = null;


    public Disease2TermFrequency(String hpoPath, String hpoAnnotationPath){
        hpoOboFilePath=hpoPath;
        hpoPhenotypeAnnotationPath=hpoAnnotationPath;
        HP_PREFIX = new ImmutableTermPrefix("HP");
        try {
            inputHpoData();
        } catch (IOException e) {
            logger.fatal(String.format("could not input data: %s",e.toString()));
            System.exit(1);
        }
        initializeFrequencyMap();
    }


    private void inputHpoData() throws IOException {
        HpoOntologyParser parser = new HpoOntologyParser(this.hpoOboFilePath);
        parser.parseOntology();
        phenotypeSubOntology = parser.getPhenotypeSubontology();
        inheritanceSubontology = parser.getInheritanceSubontology();

        HpoAnnotation2DiseaseParser annParser =
                new HpoAnnotation2DiseaseParser(this.hpoPhenotypeAnnotationPath,
                        phenotypeSubOntology,
                        inheritanceSubontology);
        diseaseMap=annParser.getDiseaseMap();
    }

    private void initializeFrequencyMap() {
       // ImmutableMap.Builder<TermId, Double> imb = new ImmutableMap.Builder<>();
        Map<TermId, Double> mp = new HashMap<>();
        for (HpoDiseaseWithMetadata dis: this.diseaseMap.values()) {
            for (TermIdWithMetadata tidm : dis.getPhenotypicAbnormalities()) {
                TermId tid=tidm.getTermId();
                if (! mp.containsKey(tid)) {
                    mp.put(tid,0.0);
                }
                Double cumulativeFreq = mp.get(tid) + tidm.getFrequency().upperBound();
                mp.put(tid,cumulativeFreq);
            }
        }
    }


}
