package org.monarchinitiative.lirical.exomiser_db_adapter;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.exomiser.core.proto.AlleleProto;

import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Functions for generating the test database files located at <code>src/test/resources/testdata</code>.
 * <p>
 * Note, the source databases must be from releases <code>2402</code> or later.
 */
@Disabled("Run manually to regenerate the test data")
public class CreateTestMVStoresTest {

    private static final Path TESTDATA_DIR = Path.of("src/test/resources/testdata");
    private static final Path CLINVAR_DB = TESTDATA_DIR.resolve("9999_hg38_clinvar.mv.db");
    private static final Path VARIANTS_DB = TESTDATA_DIR.resolve("9999_hg38_variants.mv.db");

    @Test
    public void createVariantDatabase() {
        // Add path to the real variants database
        Path source = Path.of("");

        try (MVStore sourceAlleleStore = new MVStore.Builder()
                .fileName(source.toAbsolutePath().toString())
                .readOnly()
                .open();
             MVStore destinationAlleleStore = new MVStore.Builder()
                .fileName(VARIANTS_DB.toAbsolutePath().toString())
                .open()) {

            MVMap<AlleleProto.AlleleKey, AlleleProto.AlleleProperties> sourceMap = MvStoreUtil.openAlleleMVMap(sourceAlleleStore);
            MVMap<AlleleProto.AlleleKey, AlleleProto.AlleleProperties> destinationMap = MvStoreUtil.openAlleleMVMap(destinationAlleleStore);

            Stream.of(TestVariants.lmnaVariant(), TestVariants.dmdVariant())
                    .forEach(v -> {
                        AlleleProto.AlleleKey alleleKey = AlleleUtil.createAlleleKey(v);
                        AlleleProto.AlleleProperties alleleProperties = sourceMap.get(alleleKey);
                        destinationMap.put(alleleKey, alleleProperties);
                    });
        }
    }

    @Test
    public void createClinvarDatabase() {
        // Add path to the real variants database
        Path source = Path.of("");

        try (MVStore sourceAlleleStore = new MVStore.Builder()
                .fileName(source.toAbsolutePath().toString())
                .readOnly()
                .open();
             MVStore destinationAlleleStore = new MVStore.Builder()
                     .fileName(CLINVAR_DB.toAbsolutePath().toString())
                     .open()) {

            MVMap<AlleleProto.AlleleKey, AlleleProto.ClinVar> sourceMap = MvStoreUtil.openClinVarMVMap(sourceAlleleStore);
            MVMap<AlleleProto.AlleleKey, AlleleProto.ClinVar> destinationMap = MvStoreUtil.openClinVarMVMap(destinationAlleleStore);

            Stream.of(TestVariants.lmnaVariant(), TestVariants.dmdVariant())
                    .forEach(v -> {
                        AlleleProto.AlleleKey alleleKey = AlleleUtil.createAlleleKey(v);
                        AlleleProto.ClinVar alleleProperties = sourceMap.get(alleleKey);
                        destinationMap.put(alleleKey, alleleProperties);
                    });
        }
    }
}
