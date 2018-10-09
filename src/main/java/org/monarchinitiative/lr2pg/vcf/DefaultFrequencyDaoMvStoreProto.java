package org.monarchinitiative.lr2pg.vcf;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.monarchinitiative.exomiser.core.genome.dao.FrequencyDao;
import org.monarchinitiative.exomiser.core.genome.dao.serialisers.MvStoreUtil;
import org.monarchinitiative.exomiser.core.model.AlleleProtoAdaptor;
import org.monarchinitiative.exomiser.core.model.frequency.FrequencyData;
import org.monarchinitiative.exomiser.core.proto.AlleleProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultFrequencyDaoMvStoreProto {//implements FrequencyDao {

    private static final Logger logger = LoggerFactory.getLogger(DefaultFrequencyDaoMvStoreProto.class);


//    private final MVMap<AlleleProto.AlleleKey, AlleleProto.AlleleProperties> map;
//
//
//
//    public DefaultFrequencyDaoMvStoreProto(MVStore mvStore) {
//
//        map = MvStoreUtil.openAlleleMVMap(mvStore);
//
//    }
//
//
//
//    @Override
//    public FrequencyData getFrequencyData(org.monarchinitiative.exomiser.core.model.Variant variant) {
//
//        AlleleProto.AlleleKey key = AlleleProtoAdaptor.toAlleleKey(variant);
//
//
//
//        // AlleleProperties has all the data in it, including the ClinVar data
//
//        AlleleProto.AlleleProperties alleleProperties = map.getOrDefault(key, AlleleProto.AlleleProperties.getDefaultInstance());
//
//        logger.debug("{} {}", key, alleleProperties);
//
//        return AlleleProtoAdaptor.toFrequencyData(alleleProperties);
//
//    }
}
