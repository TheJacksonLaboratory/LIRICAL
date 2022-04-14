package org.monarchinitiative.lirical.io.vcf;

import htsjdk.samtools.util.CloseableIterator;
import htsjdk.variant.variantcontext.*;
import org.monarchinitiative.lirical.core.model.AlleleCount;
import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.monarchinitiative.lirical.core.model.GenotypedVariant;
import org.monarchinitiative.svart.Contig;
import org.monarchinitiative.svart.GenomicVariant;
import org.monarchinitiative.svart.assembly.GenomicAssembly;
import org.monarchinitiative.svart.util.VariantTrimmer;
import org.monarchinitiative.svart.util.VcfConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

class GenotypedVariantIterator implements Iterator<GenotypedVariant> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenotypedVariantIterator.class);

    private final GenomicAssembly assembly;
    private final CloseableIterator<VariantContext> iterator;
    private final VcfConverter converter;

    private final Queue<GenotypedVariant> queue;
    private final GenomeBuild genomeBuild;

    GenotypedVariantIterator(GenomicAssembly assembly,
                             GenomeBuild genomeBuild,
                             CloseableIterator<VariantContext> iterator) {
        this.assembly = Objects.requireNonNull(assembly);
        this.iterator = Objects.requireNonNull(iterator);
        // TODO - pull out trimmer config?
        this.converter = new VcfConverter(assembly, VariantTrimmer.leftShiftingTrimmer(VariantTrimmer.retainingCommonBase()));
        this.genomeBuild = genomeBuild;
        this.queue = new LinkedList<>();

        readNextVariant();
    }

    @Override
    public boolean hasNext() {
        return !queue.isEmpty();
    }

    @Override
    public GenotypedVariant next() {
        GenotypedVariant next = queue.poll();
        if (queue.isEmpty())
            readNextVariant();
        return next;
    }

    private void readNextVariant() {
        while (true) {
            if (iterator.hasNext()) {
                VariantContext vc = iterator.next();

                Contig contig = assembly.contigByName(vc.getContig());
                if (contig.isUnknown()) {
                    LOGGER.warn("Unknown contig {}", vc.getContig());
                    continue;
                }

                int start = vc.getStart();
                List<Allele> alts = vc.getAlternateAlleles();

                Allele ref = vc.getReference();
                for (Allele alt : alts) {
                    GenomicVariant variant = converter.convert(contig, vc.getID(), start, ref.getBaseString(), alt.getBaseString());
                    Map<String, AlleleCount> countMap = countGenotypes(ref, alt, vc.getGenotypes());
                    queue.add(GenotypedVariant.of(genomeBuild, variant, countMap, vc.isNotFiltered()));
                }
            }
            break;
        }
    }

    private static Map<String, AlleleCount> countGenotypes(Allele ref,
                                                           Allele alt,
                                                           GenotypesContext genotypes) {
        Map<String, AlleleCount> countMap = new HashMap<>(genotypes.size());
        for (Genotype gt : genotypes) {
            if (gt.isNoCall())
                continue;

            int refCount = gt.countAllele(ref);
            int altCount = gt.countAllele(alt);

            AlleleCount ac;
            if (refCount == 0) {
                ac = switch (altCount) {
                    case 0 -> AlleleCount.zeroZero();
                    case 1 -> AlleleCount.zeroOne();
                    case 2 -> AlleleCount.zeroTwo();
                    default -> AlleleCount.of(((byte) refCount), (byte) altCount);
                };
            } else if (refCount == 1) {
                if (altCount == 1) {
                    ac = AlleleCount.oneOne();
                } else {
                    ac = AlleleCount.of(((byte) refCount), (byte) altCount);
                }
            } else {
                ac = AlleleCount.of(((byte) refCount), (byte) altCount);
            }
            countMap.put(gt.getSampleName(), ac);
        }
        return Collections.unmodifiableMap(countMap);
    }

}
