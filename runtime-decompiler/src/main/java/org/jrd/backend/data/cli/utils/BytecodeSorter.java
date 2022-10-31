package org.jrd.backend.data.cli.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

//todo, somehow reuse also for both compilations in patch and compile
public interface BytecodeSorter {

    Map<Integer, Map<String, String>> sort();

    Integer decide();

    class HexDummySorter implements BytecodeSorter {

        private final Set<Map.Entry<String, String>> entrySet;

        public HexDummySorter(Set<Map.Entry<String, String>> entrySet) {
            this.entrySet = entrySet;
        }

        @Override
        public Map<Integer, Map<String, String>> sort() {
            Map<Integer, Map<String, String>> binariesToUpload = new HashMap();
            Map<String, String> defaultByteCodeMap = new HashMap<>();
            for (Map.Entry<String, String> singlePatched : entrySet) {
                defaultByteCodeMap.put(singlePatched.getKey(), singlePatched.getValue());
            }
            binariesToUpload.put(null, defaultByteCodeMap);
            return binariesToUpload;
        }

        @Override
        public Integer decide() {
            return null;
        }
    }
}
