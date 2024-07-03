package org.monarchinitiative.lirical.exomiser_db_adapter;

import de.charite.compbio.jannovar.annotation.VariantEffect;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.monarchinitiative.exomiser.core.proto.AlleleProto;
import org.monarchinitiative.lirical.core.model.ClinVarAlleleData;
import org.monarchinitiative.lirical.core.model.ClinvarClnSig;
import org.monarchinitiative.lirical.core.model.VariantMetadata;
import org.monarchinitiative.lirical.core.service.VariantMetadataService;
import org.monarchinitiative.lirical.exomiser_db_adapter.model.AlleleProtoAdaptor;
import org.monarchinitiative.lirical.exomiser_db_adapter.model.frequency.FrequencyData;
import org.monarchinitiative.lirical.exomiser_db_adapter.model.pathogenicity.PathogenicityData;
import org.monarchinitiative.lirical.exomiser_db_adapter.model.pathogenicity.VariantEffectPathogenicityScore;
import org.monarchinitiative.svart.GenomicVariant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

class ExomiserMvStoreMetadataService implements VariantMetadataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExomiserMvStoreMetadataService.class);

    /** A map with pathogenicity scores and population frequencies for alleles sourced from the Exomiser database. */
    private final Map<AlleleProto.AlleleKey, AlleleProto.AlleleProperties> alleleMap;
    /** A map with Clinvar data for alleles sourced from the Exomiser database. */
    private final Map<AlleleProto.AlleleKey, AlleleProto.ClinVar> clinvarMap;

    /**
     * @deprecated {@link MVStore} should be closed, but we cannot reasonably close it
     * if we construct the metadata service by this method.
     * Use either {@link #of(Map, Map)} or {@link ExomiserMvStoreMetadataServiceFactory} instead.
     */
    @Deprecated(forRemoval = true, since = "2.0.3")
    public static ExomiserMvStoreMetadataService of(Path mvStore) {
        MVStore alleleMvStore = new MVStore.Builder()
            .fileName(mvStore.toAbsolutePath().toString())
            .readOnly()
            .open();
        alleleMvStore.setCacheSize(ExomiserMvStoreMetadataServiceFactory.CACHE_SIZE);

        MVMap<AlleleProto.AlleleKey, AlleleProto.AlleleProperties> alleleMap = MvStoreUtil.openAlleleMVMap(alleleMvStore);
        Map<AlleleProto.AlleleKey, AlleleProto.ClinVar> clinvarMap = Map.of();
        return new ExomiserMvStoreMetadataService(alleleMap, clinvarMap);
    }

    static ExomiserMvStoreMetadataService of(
            Map<AlleleProto.AlleleKey, AlleleProto.AlleleProperties> alleleMap,
            Map<AlleleProto.AlleleKey, AlleleProto.ClinVar> clinvarMap
    ) {
        return new ExomiserMvStoreMetadataService(alleleMap, clinvarMap);
    }

    private ExomiserMvStoreMetadataService(
            Map<AlleleProto.AlleleKey, AlleleProto.AlleleProperties> alleleMap,
            Map<AlleleProto.AlleleKey, AlleleProto.ClinVar> clinvarMap
    ) {
        this.alleleMap = Objects.requireNonNull(alleleMap);
        this.clinvarMap = Objects.requireNonNull(clinvarMap);
    }

    @Override
    public VariantMetadata metadata(GenomicVariant variant, List<VariantEffect> effects) {
        AlleleProto.AlleleKey alleleKey = AlleleUtil.createAlleleKey(variant);
        AlleleProto.AlleleProperties alleleProp = alleleMap.get(alleleKey);

        // Pathogenicity and frequency
        float frequency;
        float pathogenicity;
        if (alleleProp == null) {
            frequency = DEFAULT_FREQUENCY;
            pathogenicity = effects.stream()
                    .map(VariantEffectPathogenicityScore::pathogenicityScoreOf)
                    .max(Float::compareTo)
                    .orElse(0f);
        } else {
            FrequencyData frequencyData = AlleleProtoAdaptor.toFrequencyData(alleleProp);
            frequency = frequencyData.maxFreq();

            PathogenicityData pathogenicityData = AlleleProtoAdaptor.toPathogenicityData(alleleProp);
            pathogenicity = calculatePathogenicity(effects, pathogenicityData);
        }

        // Clinvar data
        AlleleProto.ClinVar clinVar = clinvarMap.get(alleleKey);
        ClinVarAlleleData clinVarAlleleData;
        if (clinVar == null) {
            clinVarAlleleData = null;
        } else {
            clinVarAlleleData = processClinicalSignificance(clinVar);
        }

        return VariantMetadata.of(frequency, pathogenicity, clinVarAlleleData);
    }

    private static ClinVarAlleleData processClinicalSignificance(AlleleProto.ClinVar cVarData) {
        String alleleId = cVarData.getVariationId();

        ClinvarClnSig clinvarClnSig;
        if (cVarData.getReviewStatus().equals(AlleleProto.ClinVar.ReviewStatus.NO_ASSERTION_PROVIDED)) {
            clinvarClnSig = ClinvarClnSig.NOT_PROVIDED;
        } else {
            clinvarClnSig = mapToClinvarClnSig(cVarData.getPrimaryInterpretation());
        }

        if (clinvarClnSig.equals(ClinvarClnSig.NOT_PROVIDED) && alleleId.isBlank())
            return null; // we have no useful data

        try {
            long alleleIdd = Long.parseLong(alleleId);
            return ClinVarAlleleData.of(clinvarClnSig, alleleIdd);
        } catch (NumberFormatException nfe) {
            LOGGER.warn("Non-parsable ClinVar allele ID {}, please report to developers.", alleleId);
            return ClinVarAlleleData.of(clinvarClnSig, null);
        }
    }

    private static ClinvarClnSig mapToClinvarClnSig(AlleleProto.ClinVar.ClinSig primaryInterpretation) {
        return switch (primaryInterpretation) {
            case LIKELY_PATHOGENIC -> ClinvarClnSig.LIKELY_PATHOGENIC;
            case PATHOGENIC_OR_LIKELY_PATHOGENIC -> ClinvarClnSig.PATHOGENIC_OR_LIKELY_PATHOGENIC;
            case PATHOGENIC -> ClinvarClnSig.PATHOGENIC;

            case UNCERTAIN_SIGNIFICANCE -> ClinvarClnSig.UNCERTAIN_SIGNIFICANCE;
            case CONFLICTING_PATHOGENICITY_INTERPRETATIONS -> ClinvarClnSig.CONFLICTING_PATHOGENICITY_INTERPRETATIONS;

            case BENIGN -> ClinvarClnSig.BENIGN;
            case BENIGN_OR_LIKELY_BENIGN -> ClinvarClnSig.BENIGN_OR_LIKELY_BENIGN;
            case LIKELY_BENIGN -> ClinvarClnSig.LIKELY_BENIGN;

            case AFFECTS -> ClinvarClnSig.AFFECTS;
            case ASSOCIATION -> ClinvarClnSig.ASSOCIATION;
            case DRUG_RESPONSE -> ClinvarClnSig.DRUG_RESPONSE;
            case PROTECTIVE -> ClinvarClnSig.PROTECTIVE;
            case RISK_FACTOR -> ClinvarClnSig.RISK_FACTOR;

            case OTHER, UNRECOGNIZED -> ClinvarClnSig.OTHER;
            case NOT_PROVIDED -> ClinvarClnSig.NOT_PROVIDED;
        };
    }

    /**
     * Calculate a pathogenicity score for the current variant in the same way that the Exomiser does.
     *
     * @param effects           classes of variant such as Missense, Nonsense, Synonymous, etc.
     * @param pathogenicityData Object representing the predicted pathogenicity of the data.
     * @return the predicted pathogenicity score.
     */
    private static float calculatePathogenicity(List<VariantEffect> effects,
                                                PathogenicityData pathogenicityData) {
        float finalScore = 0f;

        for (VariantEffect effect : effects) {
            float variantEffectPathogenicityScore = VariantEffectPathogenicityScore.pathogenicityScoreOf(effect);
            float current;
            if (pathogenicityData.isEmpty())
                current = variantEffectPathogenicityScore;
            else {
                float predictedScore = pathogenicityData.pathogenicityScore();
                current = switch (effect) {
                    case MISSENSE_VARIANT -> pathogenicityData.hasPredictedScore()
                            ? predictedScore
                            : variantEffectPathogenicityScore;
                    // there are cases where synonymous variants have been assigned a high MutationTaster score.
                    // These looked to have been wrongly mapped and are therefore probably wrong. So we'll use the default score for these.
                    case SYNONYMOUS_VARIANT -> variantEffectPathogenicityScore;
                    default -> Math.max(predictedScore, variantEffectPathogenicityScore);
                };
            }

            finalScore = Math.max(current, finalScore);
        }

        return finalScore;
    }

}
