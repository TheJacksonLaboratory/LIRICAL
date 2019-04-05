package org.monarchinitiative.lr2pg.io;



import com.google.protobuf.util.JsonFormat;
import org.junit.Ignore;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.lr2pg.likelihoodratio.PhenotypeLikelihoodRatioTest;
import org.monarchinitiative.phenol.ontology.data.TermId;


import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.List;


import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.phenopackets.schema.v1.Phenopacket;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TODO REFACTOR TEST PHENOPACKETS -- THEY DO NOT MACTH V 0.4.0 ANYMORE
 */
@Ignore
class PhenopacketImporterTest {


    private static Phenopacket phenopacketKabuki2NoVcf;

    private static Phenopacket spherocytosisPhenopacket;
    private static Phenopacket phenopacketPfeifferNoVcf;

   /*
    @BeforeAll

    static void setup() throws ParseException, IOException,NullPointerException {
        ClassLoader classLoader = PhenotypeLikelihoodRatioTest.class.getClassLoader();
        URL resource = classLoader.getResource("phenopacket/spherocytosis.json");
        if (resource==null){
            throw new FileNotFoundException("Could not find phenopacket file");
        }
        String phenopacketPath = resource.getFile();
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(new FileReader(phenopacketPath));
        JSONObject jsonObject = (JSONObject) obj;
        String phenopacketJsonString = jsonObject.toJSONString();
        Phenopacket.Builder phenoPacketBuilder = Phenopacket.newBuilder();
        JsonFormat.parser().merge(phenopacketJsonString, phenoPacketBuilder);
        spherocytosisPhenopacket = phenoPacketBuilder.build();

        resource = classLoader.getResource("phenopacket/pfeifferNoVcf.json");
        if (resource==null){
            throw new FileNotFoundException("Could not find pfeifferNoVcf phenopacket file");

        }
        phenopacketPath = resource.getFile();
        parser = new JSONParser();
        obj = parser.parse(new FileReader(phenopacketPath));
        jsonObject = (JSONObject) obj;
        String pfeifferNoVcf = jsonObject.toJSONString();
        phenoPacketBuilder = Phenopacket.newBuilder();
        JsonFormat.parser().merge(pfeifferNoVcf, phenoPacketBuilder);
        phenopacketPfeifferNoVcf = phenoPacketBuilder.build();
        resource = classLoader.getResource("phenopacket/Kabuki2.phenopacket");
        if (resource==null){
            throw new FileNotFoundException("Could not find Kabuki2 phenopacket file");
        }
        phenopacketPath = resource.getFile();
        parser = new JSONParser();
        obj = parser.parse(new FileReader(phenopacketPath));
        jsonObject = (JSONObject) obj;
        String kabuki2  = jsonObject.toJSONString();
        phenoPacketBuilder = Phenopacket.newBuilder();
        JsonFormat.parser().merge(kabuki2, phenoPacketBuilder);
        phenopacketKabuki2NoVcf = phenoPacketBuilder.build();
    }


    @Test
    void testInputPhenopacketNotNull() throws IOException  {
       assertNotNull(spherocytosisPhenopacket);
    }

    @Test
    void testNumberOfHpoTermsImported() throws IOException {
        PhenopacketImporter importer = new PhenopacketImporter(spherocytosisPhenopacket);
        List<TermId> nonnegatedTerms = importer.getHpoTerms();
        TermId reticulocytosis = TermId.of("HP:0001923");
        assertTrue(nonnegatedTerms.contains(reticulocytosis));
        // the following term is negated in the phenopacket
        TermId Hepatomegaly = TermId.of("HP:0002240");
        assertFalse(nonnegatedTerms.contains(Hepatomegaly));
        TermId Splenomegaly = TermId.of("HP:0001744");
        assertTrue(nonnegatedTerms.contains(Splenomegaly));
        TermId Jaundice= TermId.of("HP:0000952");
        assertTrue(nonnegatedTerms.contains(Jaundice));
        TermId Spherocytosis= TermId.of("HP:0004444");
        assertTrue(nonnegatedTerms.contains(Spherocytosis));
        // There are a total of four non-negated terms in the phenopacket
        assertEquals(4,nonnegatedTerms.size());
    }

    // The phenopacket contains a single negated term.
    @Test
    void testGetOneNegatedTerm() throws IOException {
        PhenopacketImporter importer = new PhenopacketImporter(spherocytosisPhenopacket);
        List<TermId> negatedTerms = importer.getNegatedHpoTerms();
        TermId Hepatomegaly = TermId.of("HP:0002240");
        assertTrue(negatedTerms.contains(Hepatomegaly));
        assertEquals(1,negatedTerms.size());
    }


    @Test
    void testGetPathToVcfFile() throws IOException {
        PhenopacketImporter importer = new PhenopacketImporter(spherocytosisPhenopacket);
        String expectedVcfPath="/home/user/example.vcf"; // as in the Json phenopacket file
        assertTrue(importer.hasVcf());
        assertEquals(expectedVcfPath,importer.getVcfPath());
        String expectedGenomeBuild="GRCH_38"; // as in the Json phenopacket file
        assertEquals(expectedGenomeBuild,importer.getGenomeAssembly());
    }


    @Test
    void testGetPathToVcfFileWhenNotPresent() throws IOException {
        PhenopacketImporter importer = new PhenopacketImporter(phenopacketPfeifferNoVcf);
        assertFalse(importer.hasVcf());
    }


    @Test
    void testNegatedFeatures() throws IOException {
        PhenopacketImporter importer = new PhenopacketImporter(phenopacketKabuki2NoVcf);
        List<TermId> negatedTerms = importer.getNegatedHpoTerms();
        // Not Abnormality of the spleen
        TermId abnSpleen = TermId.of("HP:0001743");
        assertTrue(negatedTerms.contains(abnSpleen));

    }
    */


}
