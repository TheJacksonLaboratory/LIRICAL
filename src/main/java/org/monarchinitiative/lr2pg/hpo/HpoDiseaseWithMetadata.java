package org.monarchinitiative.lr2pg.hpo;

import com.github.phenomics.ontolib.formats.hpo.HpoFrequency;
import com.github.phenomics.ontolib.ontology.data.TermId;
import com.google.common.collect.ImmutableList;

import java.util.List;



/**
 * Model of a disease from the HPO annotations. This is an extension of HpoDisease and will be replaced in ontolib
 *
 * <p>
 * The main purpose here is to separate phenotypic abnormalities from mode of inheritance and other
 * annotations.
 * </p>
 *
 * @author <a href="mailto:manuel.holtgrewe@bihealth.de">Manuel Holtgrewe</a>
 * @author <a href="mailto:sebastian.koehler@charite.de">Sebastian Koehler</a>
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 * @version 0.2.1 (2017-11-16)
 */
public final class HpoDiseaseWithMetadata {

    /** Name of the disease from annotation. */
    private final String name;

    private final String diseaseDatabaseId;

    /** {@link TermId}s with phenotypic abnormalities and their frequencies. */
    private final List<TermIdWithMetadata> phenotypicAbnormalities;

    /** {@link TermId}s with mode of inheritance and their frequencies. */
    private final List<TermId> modesOfInheritance;


    private final List<TermId> negativeAnnotations;

    public String getDiseaseDatabaseId() {
        return diseaseDatabaseId;
    }

    /**
     * Constructor.
     *
     * @param name Name of the disease.
     * @param phenotypicAbnormalities {@link List} of phenotypic abnormalities with their frequencies.
     * @param modesOfInheritance {@link List} of modes of inheritance with their frequencies.
     */
    public HpoDiseaseWithMetadata(String name,
                      String databaseId,
                      List<TermIdWithMetadata> phenotypicAbnormalities,
                      List<TermId> modesOfInheritance,
                      List<TermId> notTerms) {
        this.name = name;
        this.diseaseDatabaseId=databaseId;
        this.phenotypicAbnormalities = ImmutableList.copyOf(phenotypicAbnormalities);
        this.modesOfInheritance = ImmutableList.copyOf(modesOfInheritance);
        this.negativeAnnotations = ImmutableList.copyOf(notTerms);
    }

    /**
     * @return The name of the disease.
     */
    public String getName() {
        return name;
    }

    /**
     * @return The list of frequency-annotated phenotypic abnormalities.
     */
    public List<TermIdWithMetadata> getPhenotypicAbnormalities() {
        return phenotypicAbnormalities;
    }

    /**
     * @return The list of frequency-annotated modes of inheritance.
     */
    public List<TermId> getModesOfInheritance() {
        return modesOfInheritance;
    }


    public List<TermId> getNegativeAnnotations() { return this.negativeAnnotations;}

    @Override
    public String toString() {
        return "HpoDisease [name=" + name + ", phenotypicAbnormalities=" + phenotypicAbnormalities
                + ", modesOfInheritance=" + modesOfInheritance + "]";
    }

}

