package org.monarchinitiative.lr2pg.configuration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import org.monarchinitiative.lr2pg.hpo.HpoPhenoGenoCaseSimulator;
import org.monarchinitiative.lr2pg.hpo.PhenotypeOnlyHpoCaseSimulator;
import org.monarchinitiative.lr2pg.hpo.VcfSimulator;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.formats.hpo.HpoOntology;

import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.ontology.data.TermPrefix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;


import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * This is the Spring configuration file for Lr2Pg.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
@Configuration
@PropertySource("classpath:application.properties")
public class Lr2pgConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(Lr2pgConfiguration.class);
    @Value("${data.path}")
    private String datapath;
    @Value("${cases_to_simulate}")
    private String cases_to_simulate;
    @Value("${terms_per_case}")
    private String terms_per_case;
    @Value("${noise_terms}")
    private String noise_terms;
    @Value("${imprecise}")
    private String imprecise;
    @Value("${var.count}")
    private String varcount;
    @Value("${var.path}")
    private String varpath;
    @Value("${entrezgene.id}")
    private String entrezgeneid;
    @Value("${disease.id}")
    private String diseaseId;
    @Value("${term.list}")
    private String termlist;
    @Value("${hp.obo.path}")
    private String hpOboPath;

    @Bean(name = "hpoOboFile")
    File hpoOboFile() {
        return new File("data/hp.obo");
    }

    @Bean(name = "phenotype.hpoa")
    File annotationFile() {
        return new File("data/phenotype.hpoa");
    }

    @Bean(name = "jannovarTranscriptFile")
    File jannovarHg19File() {
        return new File("data/hg19_refseq.ser");
    }
/*
    @Bean
    JannovarData jannovarData( File jannovarTranscriptFile ) throws Lr2pgException{
        System.err.println("jannovar = "+jannovarTranscriptFile.getAbsolutePath());
        try {
            return new JannovarDataSerializer(jannovarTranscriptFile.getAbsolutePath()).load();
        } catch (SerializationException e) {
            throw new Lr2pgException(String.format("Could not load Jannovar data from %s (%s)",
                    jannovarTranscriptFile, e.getMessage()));
        }
    }
*/

    @Bean(name ="diseaseId")
    public String diseaseId(){return diseaseId;}

    @SuppressWarnings("FieldCanBeLocal")
    private final Environment env;
    public Lr2pgConfiguration(Environment environment) {
        this.env=environment;
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










    PhenotypeOnlyHpoCaseSimulator phenotypeOnlyHpoCaseSimulator(HpoOntology ontology, Map<TermId, HpoDisease> diseaseMap) {
        int n_cases_to_simulate = Integer.parseInt(cases_to_simulate);
        int n_terms_per_case = Integer.parseInt(terms_per_case);
        int n_noise_terms = Integer.parseInt(noise_terms);
        boolean imprecise_phenotype = false;
        if (imprecise != null && imprecise.equals("true"))
            imprecise_phenotype = true;
        return new PhenotypeOnlyHpoCaseSimulator(ontology,
                diseaseMap,
                n_cases_to_simulate,
                n_terms_per_case,
                n_noise_terms,
                imprecise_phenotype);
    }




    HpoPhenoGenoCaseSimulator hpoPhenoGenoCaseSimulator(HpoOntology ontology,
                                                        Map<TermId, HpoDisease> diseaseMap,
                                                        @Autowired @Qualifier("disease2geneMultimap") Multimap<TermId, TermId> disease2geneMultimap,
                                                        List<TermId> termIdList,
                                                        @Autowired @Qualifier("gene2backgroundFrequency") Map<TermId, Double> backgroundfreq
                                                        ){
        String geneSymbol="fake"; // todo
        Integer variantCount=Integer.parseInt(varcount);
        Double meanVarPathogenicity=Double.parseDouble(varpath);

        HpoPhenoGenoCaseSimulator simulator = new HpoPhenoGenoCaseSimulator(ontology,
                diseaseMap,
                disease2geneMultimap,
                geneSymbol,
                variantCount,
                meanVarPathogenicity,
                termIdList,
                backgroundfreq);

        return simulator;
    }










    VcfSimulator vcfSimulator(@Autowired @Qualifier("disease2geneMultimap") Multimap<TermId, TermId> disease2geneMultimap) {
        TermPrefix ENTREZ=new TermPrefix("NCBIGene");
        TermId entrezId = new TermId(ENTREZ,entrezgeneid);
        Integer vcount = Integer.parseInt(varcount);
        Double vpath=Double.parseDouble(varpath);
        return new  VcfSimulator(disease2geneMultimap.keySet(),entrezId,vcount,vpath);
    }

//    @Bean
//    VcfParser vcfParser( File jannovarTranscriptFile ) throws Lr2pgException{
//        System.err.println("jannovar = "+jannovarTranscriptFile.getAbsolutePath());
//        try {
//            String transcriptFilePath=jannovarTranscriptFile.getAbsolutePath();
//            String vcf="/Users/peterrobinson/Desktop/Pfeifer.vcf";
//            JannovarData jdata = new JannovarDataSerializer(transcriptFilePath).load();
//            return new VcfParser(vcf,jdata);
//        } catch (SerializationException e) {
//            throw new Lr2pgException(String.format("Could not load Jannovar data from %s (%s)",
//                    jannovarTranscriptFile, e.getMessage()));
//        }
//    }





//    @Bean
//    GenomeAssembly hg38genomeAssembly() {
//        return GenomeAssembly.HG38;
//    }

/*
    @Value("${exomiser.mv.store}")
    private String mvPath;
    @Bean @Singleton
    MVStore hg19MvStore() {
            return new MVStore.Builder()
                    .fileName(mvPath)
                    .readOnly()
                    .open();
    }
*/

//    @Bean
//    Lr2pgVariantAnnotator lr2pgVariantAnnotator(GenomeAssembly hg38genomeAssembly,JannovarData jannovarData){
//    return new Lr2pgVariantAnnotator(hg38genomeAssembly,jannovarData);
//    }


//    @Bean
//    PredPathCalculator predPathCalculator(Lr2pgVariantAnnotator variantAnnotator,
//                                          MVStore mvStore,
//                                          VariantDataService variantDataService) {
//        PredPathCalculator ppc = new PredPathCalculator(variantAnnotator,mvStore,variantDataService);
//        return ppc;
//    }




//    @Bean
//    GridSearch gridSearch() {
//        Integer casesToSimulate = Integer.parseInt(cases_to_simulate);
//        Integer termsPerCase = Integer.parseInt(terms_per_case);
//        Integer noiseTerms = Integer.parseInt( noise_terms );
//       GridSearch gs = new GridSearch(hpoOntology(),diseaseMap(hpoOntology()),casesToSimulate,termsPerCase,noiseTerms);
//       return gs;
//    }


}
