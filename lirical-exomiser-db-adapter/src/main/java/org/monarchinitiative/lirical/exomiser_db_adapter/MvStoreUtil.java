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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Utility class for helping read and write Alleles to the {@link org.h2.mvstore.MVStore}
 *
 * @author Jules Jacobsen
 * @since 9.0.0
 */
public class MvStoreUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(MvStoreUtil.class);
    private static final String ALLELE_MAP_NAME = "alleles";


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
        Objects.requireNonNull(mvStore);
        if (!mvStore.hasMap(ALLELE_MAP_NAME)) {
            LOGGER.warn("MVStore does not contain map '{}' - creating new map instance.", ALLELE_MAP_NAME);
        }

        MVMap<AlleleProto.AlleleKey, AlleleProto.AlleleProperties> map = mvStore.openMap(ALLELE_MAP_NAME, alleleMapBuilder());
        if (!map.isEmpty()) {
            LOGGER.debug("MVMap '{}' opened with {} entries", ALLELE_MAP_NAME, map.size());
        }

        return map;
    }

    private static MVMap.Builder<AlleleProto.AlleleKey, AlleleProto.AlleleProperties> alleleMapBuilder() {
        return new MVMap.Builder<AlleleProto.AlleleKey, AlleleProto.AlleleProperties>()
                .keyType(AlleleKeyDataType.INSTANCE)
                .valueType(AllelePropertiesDataType.INSTANCE);
    }
}
