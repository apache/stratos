package org.apache.stratos.messaging.message.filter;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.message.filter.topology.TopologyServiceFilter;
import org.apache.stratos.messaging.util.Constants;

import java.util.*;

/**
 * Message filter for filtering incoming messages in message processors.
 */
public class MessageFilter {

    private static final Log log = LogFactory.getLog(TopologyServiceFilter.class);

    private String filterName;
    private Map<String, Map<String, Boolean>> filterMap;

    public MessageFilter(String filterName) {
        this.filterName = filterName;
        this.filterMap = new HashMap<String, Map<String, Boolean>>();
        init();
    }

    private Map<String, String> splitToMap(String filter) {
        HashMap<String, String> keyValuePairMap = new HashMap<String, String>();
        List<String> keyValuePairList = splitUsingTokenizer(filter, Constants.FILTER_KEY_VALUE_PAIR_SEPARATOR);
        for (String keyValuePair : keyValuePairList) {
            List<String> keyValueList = splitUsingTokenizer(keyValuePair, Constants.FILTER_VALUE_ASSIGN_OPERATOR);
            if (keyValueList.size() == 2) {
                keyValuePairMap.put(keyValueList.get(0).trim(), keyValueList.get(1).trim());
            } else {
                throw new RuntimeException(String.format("Invalid key-value pair: %s", keyValuePair));
            }
        }
        return keyValuePairMap;
    }

    public static List<String> splitUsingTokenizer(String string, String delimiter) {
        StringTokenizer tokenizer = new StringTokenizer(string, delimiter);
        List<String> list = new ArrayList<String>(string.length());
        while (tokenizer.hasMoreTokens()) {
            list.add(tokenizer.nextToken());
        }
        return list;
    }

    /**
     * Initialize message filter using system property.
     */
    public void init() {
        String filter = System.getProperty(filterName);
        if (StringUtils.isNotBlank(filter)) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Initializing filter: %s", filterName));
            }

            String propertyValue;
            Map<String, Boolean> propertyValueMap;
            String[] propertyValueArray;
            Map<String, String> keyValuePairMap = splitToMap(filter);

            for (String propertyName : keyValuePairMap.keySet()) {
                propertyValue = keyValuePairMap.get(propertyName);
                propertyValueMap = new HashMap<String, Boolean>();
                propertyValueArray = propertyValue.split(Constants.FILTER_VALUE_SEPARATOR);
                for (String value : propertyValueArray) {
                    propertyValueMap.put(value, true);
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Filter property value found: [property] %s [value] %s", propertyName, value));
                    }
                }
                filterMap.put(propertyName, propertyValueMap);
            }
        }
    }

    public boolean isActive() {
        return filterMap.size() > 0;
    }

    public boolean included(String propertyName, String propertyValue) {
        if (filterMap.containsKey(propertyName)) {
            Map<String, Boolean> propertyValueMap = filterMap.get(propertyName);
            return propertyValueMap.containsKey(propertyValue);
        }
        return false;
    }

    public boolean excluded(String propertyName, String propertyValue) {
        return !included(propertyName, propertyValue);
    }

    public Collection<String> getIncludedPropertyValues(String propertyName) {
        if (filterMap.containsKey(propertyName)) {
            return filterMap.get(propertyName).keySet();
        }
        return CollectionUtils.EMPTY_COLLECTION;
    }
}
