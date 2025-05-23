/*
 * The Exomiser - A tool to annotate and prioritize genomic variants
 *
 * Copyright (c) 2016-2018 Queen Mary University of London.
 * Copyright (c) 2012-2016 Charité Universitätsmedizin Berlin and Genome Research Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.monarchinitiative.lirical.exomiser_db_adapter;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.monarchinitiative.exomiser.core.proto.AlleleProto;
import org.monarchinitiative.lirical.exomiser_db_adapter.serializers.AlleleKeyDataType;
import org.monarchinitiative.lirical.exomiser_db_adapter.serializers.AllelePropertiesDataType;
import org.monarchinitiative.lirical.exomiser_db_adapter.serializers.ClinVarDataType;

import java.util.Objects;

/**
 * Utility class for helping read and write Alleles to the {@link org.h2.mvstore.MVStore}
 *
 * @author Jules Jacobsen
 * @since 9.0.0
 */
public class MvStoreUtil {

    private static final String ALLELE_MAP_NAME = "alleles";
    private static final String CLINVAR_MAP_NAME = "clinvar";

    private MvStoreUtil() {
    }

    /**
     * Opens the 'alleles' map from the {@link MVStore}. If the store does not already contain this map, a new one will
     * be created and returned.
     *
     * @param mvStore The {@code MVStore} to be used for the 'alleles' {@link MVMap}
     * @return an instance of the {@link MVMap}. This map may be empty.
     * @since 10.1.0
     */
    public static MVMap<AlleleProto.AlleleKey, AlleleProto.AlleleProperties> openAlleleMVMap(MVStore mvStore) {
        return openMap(mvStore, ALLELE_MAP_NAME, alleleMapBuilder());
    }

    private static MVMap.Builder<AlleleProto.AlleleKey, AlleleProto.AlleleProperties> alleleMapBuilder() {
        return new MVMap.Builder<AlleleProto.AlleleKey, AlleleProto.AlleleProperties>()
                .keyType(AlleleKeyDataType.INSTANCE)
                .valueType(AllelePropertiesDataType.INSTANCE);
    }

    /**
     * Opens the 'clinvar' map from the {@link MVStore}. If the store does not already contain this map, a new one will
     * be created and returned.
     *
     * @param mvStore The {@code MVStore} to be used for the 'clinvar' {@link MVMap}
     * @return an instance of the {@link MVMap}. This map may be empty.
     * @since 14.0.0
     */
    public static MVMap<AlleleProto.AlleleKey, AlleleProto.ClinVar> openClinVarMVMap(MVStore mvStore) {
        return openMap(mvStore, CLINVAR_MAP_NAME, clinVarMapBuilder());
    }

    private static MVMap.Builder<AlleleProto.AlleleKey, AlleleProto.ClinVar> clinVarMapBuilder() {
        return new MVMap.Builder<AlleleProto.AlleleKey, AlleleProto.ClinVar>()
                .keyType(AlleleKeyDataType.INSTANCE)
                .valueType(ClinVarDataType.INSTANCE);
    }

    private static <K, V> MVMap<K, V> openMap(MVStore mvStore, String mapName, MVMap.Builder<K, V> mapBuilder) {
        return Objects.requireNonNull(mvStore)
                .openMap(mapName, mapBuilder);
    }
}
