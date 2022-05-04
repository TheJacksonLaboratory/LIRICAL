package org.monarchinitiative.lirical.exomiser_db_adapter;

import de.charite.compbio.jannovar.annotation.VariantEffect;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.monarchinitiative.exomiser.core.proto.AlleleProto;
import org.monarchinitiative.lirical.core.model.ClinvarClnSig;
import org.monarchinitiative.lirical.core.model.VariantMetadata;
import org.monarchinitiative.lirical.core.service.VariantMetadataService;
import org.monarchinitiative.lirical.exomiser_db_adapter.serializers.AlleleKeyDataType;
import org.monarchinitiative.lirical.exomiser_db_adapter.serializers.AllelePropertiesDataType;
import org.monarchinitiative.lirical.exomiser_db_adapter.model.AlleleProtoAdaptor;
import org.monarchinitiative.lirical.exomiser_db_adapter.model.frequency.FrequencyData;
import org.monarchinitiative.lirical.exomiser_db_adapter.model.pathogenicity.ClinVarData;
import org.monarchinitiative.lirical.exomiser_db_adapter.model.pathogenicity.PathogenicityData;
import org.monarchinitiative.lirical.exomiser_db_adapter.model.pathogenicity.VariantEffectPathogenicityScore;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.GenomicVariant;
import org.monarchinitiative.svart.Strand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class ExomiserMvStoreMetadataService implements VariantMetadataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExomiserMvStoreMetadataService.class);


    // Note: Repeated retrieval of AlleleProperties from MVMap will hopefully not pose a huge performance issue
    // since MVMap uses caching (16MB, 16 segments) by default.
    /**
     * Cache size in MB.
     */
    private static final int CACHE_SIZE = 16;

    /**
     * A map with data from the Exomiser database.
     */
    private final MVMap<AlleleProto.AlleleKey, AlleleProto.AlleleProperties> alleleMap;
    private final Options options;

    public static ExomiserMvStoreMetadataService of(Path mvStore,
                                                    Options options) {
        MVStore store = new MVStore.Builder()
                .fileName(mvStore.toAbsolutePath().toString())
                .readOnly()
                .open();
        store.setCacheSize(CACHE_SIZE);

        return new ExomiserMvStoreMetadataService(store, options);
    }

    private ExomiserMvStoreMetadataService(MVStore mvStore, Options options) {
        this.alleleMap = MvStoreUtil.openAlleleMVMap(mvStore);
        this.options = options;
    }

    @Override
    public VariantMetadata metadata(GenomicVariant variant, List<VariantEffect> effects) {
        AlleleProto.AlleleProperties alleleProp = getAlleleProperties(variant);

        float frequency;
        float pathogenicity;
        ClinvarClnSig clinvarClnSig;

        if (alleleProp == null) {
            frequency = options.defaultFrequency();
            pathogenicity = effects.stream()
                    .map(VariantEffectPathogenicityScore::getPathogenicityScoreOf)
                    .max(Float::compareTo)
                    .orElse(0f);
            clinvarClnSig = ClinvarClnSig.NOT_PROVIDED;
        } else {
            FrequencyData frequencyData = AlleleProtoAdaptor.toFrequencyData(alleleProp);
            frequency = frequencyData.getMaxFreq();

            PathogenicityData pathogenicityData = AlleleProtoAdaptor.toPathogenicityData(alleleProp);
            pathogenicity = calculatePathogenicity(effects, pathogenicityData);

            clinvarClnSig = processClinicalSignificance(pathogenicityData);
        }

        return VariantMetadata.of(frequency, pathogenicity, clinvarClnSig);

    }

    private static ClinvarClnSig processClinicalSignificance(PathogenicityData pathogenicityData) {
        ClinvarClnSig clinvarClnSig;
        ClinVarData cVarData = pathogenicityData.getClinVarData();
        // Only use ClinVar data if it is backed up by assertions.
        if (cVarData.getReviewStatus().startsWith("no_assertion")) {
            clinvarClnSig = ClinvarClnSig.NOT_PROVIDED;
        } else {
            ClinVarData.ClinSig primaryInterpretation = cVarData.getPrimaryInterpretation();
            clinvarClnSig = mapToClinvarClnSig(primaryInterpretation);
        }
        return clinvarClnSig;
    }

    private static ClinvarClnSig mapToClinvarClnSig(ClinVarData.ClinSig primaryInterpretation) {
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

            case OTHER -> ClinvarClnSig.OTHER;
            case NOT_PROVIDED -> ClinvarClnSig.NOT_PROVIDED;
        };
    }

    private AlleleProto.AlleleProperties getAlleleProperties(GenomicVariant variant) {
        AlleleProto.AlleleKey alleleKey = createAlleleKey(variant);
        return alleleMap.get(alleleKey);
    }

    private static AlleleProto.AlleleKey createAlleleKey(GenomicVariant variant) {
        return AlleleProto.AlleleKey.newBuilder()
                .setChr(variant.contigId())
                .setPosition(variant.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.oneBased()))
                .setRef(variant.ref())
                .setAlt(variant.alt())
                .build();
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
            float variantEffectPathogenicityScore = VariantEffectPathogenicityScore.getPathogenicityScoreOf(effect);
            float current;
            if (pathogenicityData.isEmpty())
                current = variantEffectPathogenicityScore;
            else {
                float predictedScore = pathogenicityData.getScore();
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
