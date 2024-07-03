package org.monarchinitiative.lirical.exomiser_db_adapter;

import org.monarchinitiative.exomiser.core.proto.AlleleProto;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.GenomicVariant;
import org.monarchinitiative.svart.Strand;

class AlleleUtil {
    private AlleleUtil() {}

    static AlleleProto.AlleleKey createAlleleKey(GenomicVariant variant) {
        return AlleleProto.AlleleKey.newBuilder()
                .setChr(variant.contigId())
                .setPosition(variant.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.oneBased()))
                .setRef(variant.ref())
                .setAlt(variant.alt())
                .build();
    }
}
