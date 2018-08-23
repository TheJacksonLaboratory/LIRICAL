package org.monarchinitiative.lr2pg.configuration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import org.monarchinitiative.lr2pg.hpo.HpoPhenoGenoCaseSimulator;
import org.monarchinitiative.lr2pg.hpo.PhenotypeOnlyHpoCaseSimulator;
import org.monarchinitiative.lr2pg.hpo.VcfSimulator;
import org.monarchinitiative.lr2pg.io.GenotypeDataIngestor;
import org.monarchinitiative.phenol.base.PhenolException;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.formats.hpo.HpoOntology;
import org.monarchinitiative.phenol.io.assoc.HpoAssociationParser;
import org.monarchinitiative.phenol.io.obo.hpo.HpOboParser;
import org.monarchinitiative.phenol.io.obo.hpo.HpoDiseaseAnnotationParser;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.ontology.data.TermPrefix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;


import java.io.File;
import java.util.List;
import java.util.Map;

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

    @Bean(name = "hpoOboFile")
    public File hpoOboFile() {
        return new File("data/hp.obo");
    }

    @Bean(name = "phenotype.hpoa")
    public File annotationFile() {
        return new File("data/phenotype.hpoa");
    }


    public Lr2pgConfiguration() {

    }

    /**
     * We expect to get an argument such as
     * --term.list=HP:0002751,HP:0001166,HP:0004933,HP:0001083,HP:0003179
     *
     * @return list of termid for this patient
     */
    @Bean
    List<TermId> termIdList() {
        ImmutableList.Builder<TermId> builder = new ImmutableList.Builder<>();
        for (String id : this.termlist.split(",")) {
            TermId tid = TermId.constructWithPrefix(id);
        }
        return builder.build();
    }

    @Bean
    @Primary
    public HpoOntology hpoOntology() {
        HpOboParser parser = new HpOboParser(hpoOboFile());
        HpoOntology ontology;
        try {
            ontology = parser.parse();
            return ontology;
        } catch (PhenolException ioe) {
            System.err.println("Could not parse hp.obo file: " + ioe.getMessage());
            throw new RuntimeException("Could not parse hp.obo file: " + ioe.getMessage());
        }
    }


    @Bean
    @Primary
    public Map<TermId, HpoDisease> diseaseMap(HpoOntology ontology) {
        HpoDiseaseAnnotationParser annotationParser = new HpoDiseaseAnnotationParser(annotationFile(), ontology);
        try {
            Map<TermId, HpoDisease> diseaseMap = annotationParser.parse();
            logger.info("disease map size=" + diseaseMap.size());
            if (!annotationParser.validParse()) {
                logger.debug("Parse problems encountered with the annotation file at {}.",
                        annotationFile().getAbsolutePath());
                int n = annotationParser.getErrors().size();
                int i = 0;
                for (String error : annotationParser.getErrors()) {
                    i++;
                    logger.debug(i + "/" + n + ") " + error);
                }
                logger.debug("Done showing errors");
            }
            return diseaseMap;
        } catch (PhenolException pe) {
            throw new RuntimeException("Could not parse annotation file: " + pe.getMessage());
        }
    }


    @Bean
    @Primary
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

    @Bean(name = "gene2backgroundFrequency")
    Map<TermId, Double> gene2backgroundFrequency() {
        String path = String.format("%s%s%s",datapath,File.separator,"background-freq.txt");
        GenotypeDataIngestor gdingestor = new GenotypeDataIngestor(path);
        Map<TermId, Double> gene2backgroundFrequency = gdingestor.parse();
        return gene2backgroundFrequency;
    }

    @Bean
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




    @Bean
    HpoAssociationParser hpoAssociationParser(HpoOntology ontology) {
        String geneInfoPath = String.format("%s%s%s", datapath, File.separator, "Homo_sapiens_gene_info.gz");
        File geneInfoFile = new File(geneInfoPath);
        if (!geneInfoFile.exists()) {
            System.err.println("Could not find gene info file at " + geneInfoPath + ". Run download command");
            System.exit(1);
        }
        String mim2genemedgen = String.format("%s%s%s", datapath, File.separator, "mim2gene_medgen");
        File mim2genemedgenFile = new File(mim2genemedgen);
        if (!mim2genemedgenFile.exists()) {
            System.err.println("Could not find medgen file at " + mim2genemedgen + ". Run download command");
            System.exit(1);
        }
        File orphafilePlaceholder = null;
        HpoAssociationParser assocParser = new HpoAssociationParser(geneInfoFile,
                mim2genemedgenFile,
                orphafilePlaceholder,
                ontology);
        assocParser.parse();
        return assocParser;
    }


    /**  key: a gene CURIE such as NCBIGene:123; value: a collection of disease CURIEs such as OMIM:600123. */
    @Bean(name="gene2diseaseMultimap")
    Multimap<TermId,TermId> gene2diseaseMultimap(HpoAssociationParser parser) {
        return parser.getGeneToDiseaseIdMap();
    }
    /* key: disease CURIEs such as OMIM:600123; value: a collection of gene CURIEs such as NCBIGene:123.  */

    @Bean(name="disease2geneMultimap")
    Multimap<TermId,TermId> disease2geneMultimap(HpoAssociationParser parser) {
        return parser.getDiseaseToGeneIdMap();
    }
    /** key: a gene id, e.g., NCBIGene:2020; value: the corresponding symbol. */
    @Bean(name="geneId2symbolMap")
    Map<TermId,String> geneId2symbolMap(HpoAssociationParser parser) {
        return parser.getGeneIdToSymbolMap();
    }

    @Bean
    VcfSimulator vcfSimulator(@Autowired @Qualifier("disease2geneMultimap") Multimap<TermId, TermId> disease2geneMultimap) {
        TermPrefix ENTREZ=new TermPrefix("NCBIGene");
        TermId entrezId = new TermId(ENTREZ,entrezgeneid);
        Integer vcount = Integer.parseInt(varcount);
        Double vpath=Double.parseDouble(varpath);
        return new  VcfSimulator(disease2geneMultimap.keySet(),entrezId,vcount,vpath);
    }





}
