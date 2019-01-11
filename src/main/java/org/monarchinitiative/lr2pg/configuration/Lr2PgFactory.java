package org.monarchinitiative.lr2pg.configuration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import de.charite.compbio.jannovar.data.JannovarData;
import de.charite.compbio.jannovar.data.JannovarDataSerializer;
import de.charite.compbio.jannovar.data.SerializationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.h2.mvstore.MVStore;
import org.monarchinitiative.exomiser.core.genome.GenomeAssembly;
import org.monarchinitiative.lr2pg.exception.Lr2pgException;
import org.monarchinitiative.lr2pg.io.GenotypeDataIngestor;
import org.monarchinitiative.phenol.base.PhenolException;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.io.assoc.HpoAssociationParser;
import org.monarchinitiative.phenol.io.obo.hpo.HpoDiseaseAnnotationParser;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Not a full implementation of the factory pattern but rather a convenience class to create objects of various
 * classes that we need as singletons with the various commands.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class Lr2PgFactory {
    private static final Logger logger = LogManager.getLogger();
    /** Path to the {@code hp.obo} file. */
    private final String hpoOboFilePath;
    /** Path to the {@code phenotype.hpoa} file. */
    private final String phenotypeAnnotationPath;
    /** Path to the Exomiser data file. */
    private final String mvStoreAbsolutePath;
    /** Path to the {@code Homo_sapiens_gene_info.gz} file. */
    private final String geneInfoPath;
    /** Path to the mimgene/medgen file with MIM to gene associations. */
    private final String mim2genemedgenPath;
    /** Path to the file that we create with background frequencies for predicted pathogenic variants in genes. */
    private final String backgroundFrequencyPath;
    /** Path to the VCF file that is be evaluated. */
    private final String vcfPath;
    /** Path to the Jannovar file with transcript definitions to be used for the VCF analysis. */
    private final String jannovarTranscriptFile;
    /** List of HPO terms (phenotypic abnormalities) observed in the person being evaluated. */
    private final List<TermId> hpoIdList;

    private final GenomeAssembly assembly;

    private final Ontology ontology;
    private MVStore mvstore = null;
    private Multimap<TermId,TermId> gene2diseaseMultiMap=null;
    private Multimap<TermId,TermId> disease2geneIdMultiMap=null;
    private Map<TermId,String> geneId2SymbolMap=null;
    private JannovarData jannovarData=null;

    public Lr2PgFactory(Builder builder) {
        this.hpoOboFilePath = builder.hpOboPath;
        this.mvStoreAbsolutePath = builder.mvStorePath;
        this.geneInfoPath=builder.geneInfoPath;
        this.mim2genemedgenPath=builder.mim2genemedgenPath;
        this.backgroundFrequencyPath=builder.backgroundFrequencyPath;
        this.phenotypeAnnotationPath=builder.phenotypeAnnotationPath;
        this.vcfPath=builder.vcfPath;
        this.jannovarTranscriptFile=builder.jannovarTranscriptFile;
        String ga = builder.genomeAssembly;
        switch(ga.toLowerCase()) {
            case "hg19":
            case "hg37":
            case  "grch37":
                this.assembly=GenomeAssembly.HG19;
                break;
            case "hg38":
            case "grc38":
                this.assembly=GenomeAssembly.HG38;
                break;
            default:
                this.assembly=null;
        }
        ImmutableList.Builder<TermId> listbuilder = new ImmutableList.Builder<>();
        for (String id : builder.observedHpoTerms) {
            TermId hpoId = TermId.of(id);
            listbuilder.add(hpoId);
        }
        this.hpoIdList=listbuilder.build();
        if (builder.hpOboPath!=null) {
            this.ontology=hpoOntology();
        } else {
            this.ontology=null;
        }
    }

    /**
     * @return a list of observed HPO terms (from the YAML file)
     * @throws Lr2pgException if one of the terms is not in the HPO Ontology
     */
    public List<TermId> observedHpoTerms() throws Lr2pgException{
        for (TermId hpoId : hpoIdList) {
            if (! this.ontology.getTermMap().containsKey(hpoId)) {
                throw new Lr2pgException("Could not find HPO term " + hpoId.getValue() + " in ontology");
            }
        }
        return hpoIdList;
    }

    /** @return the genome assembly corresponding to the VCF file. Can be null. */
    public GenomeAssembly getAssembly() {
        return assembly;
    }

    /** @return HpoOntology object. */
    public Ontology hpoOntology() {
        if (ontology != null) return ontology;
        // The HPO is in the default  curie map and only contains known relationships / HP terms
        Ontology ontology =  OntologyLoader.loadOntology(new File(this.hpoOboFilePath));
        if (ontology==null) {
            throw new PhenolRuntimeException("Could not load ontology from \"" + this.hpoOboFilePath +"\"");
        } else {
            return ontology;
        }
    }

    /** @return MVStore object with Exomiser data on variant pathogenicity and frequency. */
    public MVStore mvStore() {
        if (mvstore==null) {
            mvstore = new MVStore.Builder()
                    .fileName(mvStoreAbsolutePath)
                    .readOnly()
                    .open();
        }
        return mvstore;
    }

    public String vcfPath(){ return vcfPath;}



    private void parseHpoAnnotations() throws Lr2pgException {
        if (this.ontology==null) {
            hpoOntology();
        }
        if (this.geneInfoPath==null) {
            throw new Lr2pgException("Path to Homo_sapiens_gene_info.gz file not found");
        }
        if (this.mim2genemedgenPath==null) {
            throw new Lr2pgException("Path to mim2genemedgen file not found");
        }

        File geneInfoFile = new File(geneInfoPath);
        if (!geneInfoFile.exists()) {
            throw new Lr2pgException("Could not find gene info file at " + geneInfoPath + ". Run download!");
        }
        File mim2genemedgenFile = new File(this.mim2genemedgenPath);
        if (!mim2genemedgenFile.exists()) {
            System.err.println("Could not find medgen file at " + this.mim2genemedgenPath + ". Run download!");
            System.exit(1);
        }
        File orphafilePlaceholder = null;//we do not need this for now
        HpoAssociationParser assocParser = new HpoAssociationParser(geneInfoFile,
                mim2genemedgenFile,
                orphafilePlaceholder,
                ontology);
        assocParser.parse();
        assocParser.getDiseaseToGeneIdMap();

        this.gene2diseaseMultiMap=assocParser.getGeneToDiseaseIdMap();
        this.disease2geneIdMultiMap=assocParser.getDiseaseToGeneIdMap();
        this.geneId2SymbolMap=assocParser.getGeneIdToSymbolMap();
    }


    /** @return a multimap with key: a gene CURIE such as NCBIGene:123; value: a collection of disease CURIEs such as OMIM:600123. */
    public Multimap<TermId,TermId> gene2diseaseMultimap() throws Lr2pgException {
        if (this.gene2diseaseMultiMap==null) {
            parseHpoAnnotations();
        }
        return this.gene2diseaseMultiMap;
    }

    /** @return multimap with key:disease CURIEs such as OMIM:600123; value: a collection of gene CURIEs such as NCBIGene:123.  */
    public Multimap<TermId,TermId> disease2geneMultimap() throws Lr2pgException {
        if (this.disease2geneIdMultiMap==null) {
            parseHpoAnnotations();
        }
        return this.disease2geneIdMultiMap;
    }
    /** @return a map with key:a gene id, e.g., NCBIGene:2020; value: the corresponding gene symbol. */
    public Map<TermId,String> geneId2symbolMap() throws Lr2pgException{
        if (this.geneId2SymbolMap==null) {
            parseHpoAnnotations();
        }
        return this.geneId2SymbolMap;
    }

    /** @return Map with key: geneId, value: corresponding background frequency of bin P variants. */
    public Map<TermId, Double> gene2backgroundFrequency() throws Lr2pgException{
        if (this.backgroundFrequencyPath==null) {
            throw new Lr2pgException("Path to background-freq.txt file not found");
        }
        GenotypeDataIngestor gdingestor = new GenotypeDataIngestor(this.backgroundFrequencyPath);
        return gdingestor.parse();
    }

    /** @return the object created by deserilizing a Jannovar file. */
    public JannovarData jannovarData() throws Lr2pgException {
        if (jannovarData != null) return jannovarData;
        if (this.jannovarTranscriptFile == null) {
            throw new Lr2pgException("Path to jannovar transcript file not found");
        }
        try {
            this.jannovarData = new JannovarDataSerializer(jannovarTranscriptFile).load();

        } catch (SerializationException e) {
            throw new Lr2pgException(String.format("Could not load Jannovar data from %s (%s)",
                    jannovarTranscriptFile, e.getMessage()));
        }
        return jannovarData;
    }

    /** @return a map with key: a disease id (e.g., OMIM:654321) and key the corresponding {@link HpoDisease} object.*/
    public Map<TermId, HpoDisease> diseaseMap(Ontology ontology) throws Lr2pgException {
        if (this.phenotypeAnnotationPath==null) {
            throw new Lr2pgException("Path to phenotype.hpoa file not found");
        }
        // phenol 1.3.2
        List<String> desiredDatabasePrefixes=ImmutableList.of("OMIM","DECIPHER");
        HpoDiseaseAnnotationParser annotationParser=new HpoDiseaseAnnotationParser(phenotypeAnnotationPath,ontology,desiredDatabasePrefixes);
       // HpoDiseaseAnnotationParser annotationParser = new HpoDiseaseAnnotationParser(this.phenotypeAnnotationPath, this.ontology);
        try {
            Map<TermId, HpoDisease> diseaseMap = annotationParser.parse();
            if (!annotationParser.validParse()) {
                int n = annotationParser.getErrors().size();
                logger.warn("Parse problems encountered with the annotation file at {}. Got {} errors",
                        this.phenotypeAnnotationPath,n);
            }
            return diseaseMap;
        } catch (PhenolException pe) {
            throw new Lr2pgException("Could not parse annotation file: " + pe.getMessage());
        }
    }



    public static class Builder {

        private String hpOboPath=null;
        private String phenotypeAnnotationPath=null;
        private String mvStorePath=null;
        private String geneInfoPath=null;
        private String mim2genemedgenPath=null;
        private String backgroundFrequencyPath=null;
        private String vcfPath=null;
        private String jannovarTranscriptFile=null;
        private String genomeAssembly=null;
        private List<String> observedHpoTerms=ImmutableList.of();

        public Builder(){
        }



        public Builder hp_obo(String hpPath) {
            hpOboPath=hpPath;
            return this;
        }

        public Builder genomeAssembly(String ga) {
            this.genomeAssembly=ga;
            return this;
        }

        public Builder mvStore(String mvsPath) {
            this.mvStorePath=mvsPath;
            return this;
        }

        public Builder geneInfo(String gi) {
            this.geneInfoPath=gi;
            return this;
        }

        public Builder mim2genemedgen(String m2gm) {
            this.mim2genemedgenPath=m2gm;
            return this;
        }

        public Builder backgroundFrequency(String bf) {
            this.backgroundFrequencyPath=bf;
            return this;
        }

        public Builder phenotypeAnnotation(String pa) {
            this.phenotypeAnnotationPath=pa;
            return this;
        }

        public Builder vcf(String vcf) {
            this.vcfPath=vcf;
            return this;
        }

        public Builder jannovarFile(String jf) {
            this.jannovarTranscriptFile=jf;
            return this;
        }

        public Builder observedHpoTerms(String [] terms) {
            this.observedHpoTerms=new ArrayList<>();
            Collections.addAll(observedHpoTerms, terms);
            return this;
        }


        public Lr2PgFactory build() {
            return new Lr2PgFactory(this);
        }


    }
}
