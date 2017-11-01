package org.monarchinitiative.lr2pg;
import org.monarchinitiative.lr2pg.old.MapUtil;
import java.util.*;
import org.junit.*;
/**
 * Created by ravanv on 10/5/17.
 */
public class MapUtilTest {

        @Test
        public void testSortByValue() {
            Random random = new Random(System.currentTimeMillis());
            Map<String, Integer> testMap = new HashMap<String, Integer>();
            //for(int i = 0; i < 10; ++i) {
             //   testMap.put( "SomeString" + random.nextInt(), random.nextInt());
           // }
            testMap.put("Ali", 3);
            testMap.put("Vida",20);
            testMap.put("Venus",18);
            System.out.println(testMap);
            testMap = MapUtil.sortByValue(testMap);
            System.out.println(testMap);
            Assert.assertEquals(3, testMap.size());
            Integer previous = null;
            for(Map.Entry<String, Integer> entry : testMap.entrySet()) {
                Assert.assertNotNull(entry.getValue());
                if (previous != null) {
                    Assert.assertTrue(entry.getValue() <= previous);
                }
                previous = entry.getValue();
            }
        }
    }

