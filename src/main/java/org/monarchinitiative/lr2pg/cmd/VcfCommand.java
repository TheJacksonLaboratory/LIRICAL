package org.monarchinitiative.lr2pg.cmd;

import de.charite.compbio.jannovar.data.JannovarData;
import org.h2.mvstore.MVStore;
import org.monarchinitiative.exomiser.core.genome.GenomeAssembly;
import org.monarchinitiative.lr2pg.analysis.Gene2Genotype;
import org.monarchinitiative.lr2pg.analysis.Vcf2GenotypeMap;
import org.monarchinitiative.lr2pg.configuration.Lr2PgFactory;
import org.monarchinitiative.lr2pg.exception.Lr2pgException;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Map;

public class VcfCommand extends Lr2PgCommand {

    private final Lr2PgFactory factory;

    public VcfCommand(Lr2PgFactory fact) {
        this.factory = fact;

    }

    /**
     * Identify the variants and genotypes from the VCF file.
     * @return a map with key: An NCBI Gene Id, and value: corresponding {@link Gene2Genotype} object.
     * @throws Lr2pgException upon error parsing the VCF file or creating the Jannovar object
     */
    private Map<TermId, Gene2Genotype> getVcf2GenotypeMap() throws Lr2pgException {
        String vcf = factory.vcfPath();
        MVStore mvstore = factory.mvStore();
        JannovarData jannovarData = factory.jannovarData();
        Vcf2GenotypeMap vcf2geno = new Vcf2GenotypeMap(vcf, jannovarData, mvstore, GenomeAssembly.HG19);
        Map<TermId, Gene2Genotype> genotypeMap = vcf2geno.vcf2genotypeMap();
        return genotypeMap;
    }


    public void run() throws Lr2pgException {
        Map<TermId, Gene2Genotype> genotypeMap = getVcf2GenotypeMap();
    }
}
