package org.monarchinitiative.lr2pg.prototype;
import java.util.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * Created by ravanv on 9/28/17. (obtained from stackoverflow.com)
 */
public class MapUtil {
  /*  public static <K, V extends Comparable<? super V>> Map<K, V>
    sortByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>(map.entrySet());
        list.sort(new Comparator<Map.Entry<K, V>>() {
            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                return (o1.getValue()).compareTo(o2.getValue());
            }
        });

        Map<K, V> result = new LinkedHashMap<K, V>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }*/
//}


   public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        /*Collections.reverseOrder()*/
       List<Map.Entry<K, V>> toSort = new ArrayList<>();
       for (Map.Entry<K, V> kvEntry : map.entrySet()) {
           toSort.add(kvEntry);
       }
       toSort.sort(Map.Entry.comparingByValue(Collections.reverseOrder()));
       LinkedHashMap<K, V> result = new LinkedHashMap<>();
       for (Map.Entry<K, V> kvEntry : toSort) {
           result.putIfAbsent(kvEntry.getKey(), kvEntry.getValue());
       }
       return result;
    }
}

   /* public static Map<String, Double> sortByValue(Map<String, Double> unsortMap) {

        // 1. Convert Map to List of Map
        List<Map.Entry<String, Double>> list =
                new LinkedList<Map.Entry<String, Double>>(unsortMap.entrySet());

        // 2. Sort list with Collections.sort(), provide a custom Comparator
        //    Try switch the o1 o2 position for a different order
        Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
            public int compare(Map.Entry<String, Double> o1,
                               Map.Entry<String, Double> o2) {
                return (o1.getValue()).compareTo(o2.getValue());
            }
        });

        // 3. Loop the sorted list and put it into a new insertion order Map LinkedHashMap
        Map<String, Double> sortedMap = new LinkedHashMap<String, Double>();
        for (Map.Entry<String, Double> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        /*
        //classic iterator example
        for (Iterator<Map.Entry<String, Integer>> it = list.iterator(); it.hasNext(); ) {
            Map.Entry<String, Integer> entry = it.next();
            sortedMap.put(entry.getKey(), entry.getValue());
        }*/


       // return sortedMap;
   // }

//}