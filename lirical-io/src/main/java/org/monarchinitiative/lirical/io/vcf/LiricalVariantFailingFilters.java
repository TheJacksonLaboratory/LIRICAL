package org.monarchinitiative.lirical.io.vcf;

import org.monarchinitiative.lirical.core.model.*;
import org.monarchinitiative.svart.GenomicVariant;

import java.util.List;
import java.util.Optional;
import java.util.Set;

record LiricalVariantFailingFilters(GenotypedVariant gv, List<TranscriptAnnotation> annotations) implements LiricalVariant {

    @Override
    public GenomeBuild genomeBuild() {
        return gv.genomeBuild();
    }

    @Override
    public GenomicVariant variant() {
        return gv.variant();
    }

    @Override
    public Set<String> sampleNames() {
        return gv.sampleNames();
    }

    @Override
    public Optional<AlleleCount> alleleCount(String sampleId) {
        return gv.alleleCount(sampleId);
    }

    @Override
    public boolean passedFilters() {
        return false;
    }

    @Override
    public boolean failedFilters() {
        return true;
    }

    @Override
    public Optional<Float> frequency() {
        return Optional.empty();
    }

    @Override
    public float pathogenicity() {
        return 0;
    }

    @Override
    public Optional<ClinVarAlleleData> clinVarAlleleData() {
        return Optional.empty();
    }
}
