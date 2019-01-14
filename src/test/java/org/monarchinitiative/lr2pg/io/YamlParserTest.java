package org.monarchinitiative.lr2pg.io;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.lr2pg.exception.Lr2pgException;
import org.monarchinitiative.lr2pg.likelihoodratio.PhenotypeLikelihoodRatioTest;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;

class YamlParserTest {

    private static String demo1path;

    @BeforeAll
    static void init() throws FileNotFoundException{
        ClassLoader classLoader = PhenotypeLikelihoodRatioTest.class.getClassLoader();
        URL resource = classLoader.getResource("yaml/demo1.yml");
        if (resource==null){
            throw new FileNotFoundException("Could not find demo1.yml file");
        }
        demo1path = resource.getFile();
    }

    @Test
    void testDemo1YamlFile() throws Lr2pgException {
        YamlParser parser = new YamlParser(demo1path);
        String expected = String.format("%s%s%s","data", File.separator,"hp.obo");
        assertEquals(expected,parser.getHpOboPath());
        expected = String.format("%s%s%s","data", File.separator,"mim2gene_medgen");
        assertEquals(expected,parser.getMedgen());
        expected = String.format("%s%s%s","data", File.separator,"Homo_sapiens_gene_info.gz");
        assertEquals(expected,parser.getGeneInfo());
        expected = String.format("%s%s%s","data", File.separator,"phenotype.hpoa");
        assertEquals(expected,parser.phenotypeAnnotation());
    }

    @Test
    void testMvStorePath() throws Lr2pgException {
        YamlParser parser = new YamlParser(demo1path);
        String expected="/home/robinp/data/exomiserdata/1802_hg19/1802_hg19_variants.mv.db";
        assertEquals(expected,parser.getMvStorePath());
    }

    @Test
    void testBadMvStorePath() {
        String badPath="/nonexistant/path/1802_hg19_variants.mv.db";
        Assertions.assertThrows(Lr2pgException.class, () -> {
            YamlParser parser = new YamlParser(badPath);
        });
    }


}
