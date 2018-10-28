package org.monarchinitiative.lr2pg.configuration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import org.monarchinitiative.lr2pg.hpo.HpoPhenoGenoCaseSimulator;
import org.monarchinitiative.lr2pg.hpo.VcfSimulator;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.formats.hpo.HpoOntology;

import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.ontology.data.TermPrefix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * This is the Spring configuration file for Lr2Pg.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
@Deprecated
public class Lr2pgConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(Lr2pgConfiguration.class);

    private String datapath;

    private String cases_to_simulate;

    private String terms_per_case;

    private String noise_terms;

    private String imprecise;

    private String varcount;

    private String varpath;

    private String entrezgeneid;

    private String diseaseId;

    private String termlist;



    File annotationFile() {
        return new File("data/phenotype.hpoa");
    }


    File jannovarHg19File() {
        return new File("data/hg19_refseq.ser");
    }



    public String diseaseId(){return diseaseId;}


    public Lr2pgConfiguration() {
    }

    /**
     * We expect to get an argument such as
     * --term.list=HP:0002751,HP:0001166,HP:0004933,HP:0001083,HP:0003179
     *
     * @return list of termid for this patient
     */

    List<TermId> termIdList() {
        ImmutableList.Builder<TermId> builder = new ImmutableList.Builder<>();
        for (String id : this.termlist.split(",")) {
            TermId tid = TermId.constructWithPrefix(id);
            builder.add(tid);
        }
        return builder.build();
    }















    HpoPhenoGenoCaseSimulator hpoPhenoGenoCaseSimulator(HpoOntology ontology,
                                                        Map<TermId, HpoDisease> diseaseMap,
                                                        Multimap<TermId, TermId> disease2geneMultimap,
                                                        List<TermId> termIdList,
                                                         Map<TermId, Double> backgroundfreq
                                                        ){
        String geneSymbol="fake"; // todo
        Integer variantCount=Integer.parseInt(varcount);
        Double meanVarPathogenicity=Double.parseDouble(varpath);

       return new HpoPhenoGenoCaseSimulator(ontology,
                diseaseMap,
                disease2geneMultimap,
                geneSymbol,
                variantCount,
                meanVarPathogenicity,
                termIdList,
                backgroundfreq);
    }










    VcfSimulator vcfSimulator(Multimap<TermId, TermId> disease2geneMultimap) {
        TermPrefix ENTREZ=new TermPrefix("NCBIGene");
        TermId entrezId = new TermId(ENTREZ,entrezgeneid);
        Integer vcount = Integer.parseInt(varcount);
        Double vpath=Double.parseDouble(varpath);
        return new  VcfSimulator(disease2geneMultimap.keySet(),entrezId,vcount,vpath);
    }



}
