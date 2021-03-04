package uk.gov.hmcts.reform.managecase.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.managecase.domain.DynamicList;
import uk.gov.hmcts.reform.managecase.domain.DynamicListElement;

import java.util.List;

import java.util.Iterator;
import java.util.Map;

import java.util.Arrays;
import java.util.List;

@Component
public class JacksonUtils {

    private final ObjectMapper objectMapper;

    @Autowired
    public JacksonUtils(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public <T> T convertValue(Object fromValue, Class<T> toValueType) {
        return objectMapper.convertValue(fromValue, toValueType);
    }

    /**
     * Nullify all fields within an object node - all fields are set to null nodes.
     * NOTE: Arrays are not supported
     * @param node object node to be nullified
     * @param ignoredNestedFields names of fields that will be set to NullNode, even if they are nested fields
     */
    public void nullifyObjectNode(ObjectNode node, String... ignoreNestedFields) {
        List<String> ignoredNestedFieldsList = Arrays.asList(ignoreNestedFields);
        node.fieldNames().forEachRemaining(fieldName -> {
            if (node.get(fieldName).isObject() && !ignoredNestedFieldsList.contains(fieldName)) {
                nullifyObjectNode((ObjectNode) node.get(fieldName));
            } else {
                node.set(fieldName, objectMapper.nullNode());
            }
        });
    }

    public void merge(Map<String, JsonNode> mergeFrom, Map<String, JsonNode> mergeInto) {

        for (String key : mergeFrom.keySet()) {
            JsonNode value = mergeFrom.get(key);
            if (!mergeInto.containsKey(key)) {
                mergeInto.put(key, value);
            } else {
                mergeInto.put(key, merge(mergeInto.get(key), value));
            }
        }
    }

    private static JsonNode merge(JsonNode mainNode, JsonNode updateNode) {
        // If the top level node is an @ArrayNode we do not update
        if (mainNode.isArray()) {
            return mainNode;
        }

        Iterator<String> fieldNames = updateNode.fieldNames();
        while (fieldNames.hasNext()) {
            String updatedFieldName = fieldNames.next();
            JsonNode valueToBeUpdated = mainNode.get(updatedFieldName);
            JsonNode updatedValue = updateNode.get(updatedFieldName);

            // If the node is an @ArrayNode we do not update
            if (valueToBeUpdated != null && valueToBeUpdated.isArray()) {
                return mainNode;
            } else if (valueToBeUpdated != null && valueToBeUpdated.isObject()) {
                merge(valueToBeUpdated, updatedValue);
            } else {
                if (mainNode instanceof ObjectNode) {
                    ((ObjectNode) mainNode).replace(updatedFieldName, updatedValue);
                }
            }
        }
        return mainNode;
    }

    /**
     * Ex.:
     * {
     *   "value": {
     *     "code": "[Claimant]",
     *     "label": "Claimant"
     *   },
     *   "list_items": [
     *     {
     *       "code": "[Claimant]",
     *       "label": "Claimant"
     *     },
     *     {
     *       "code": "[Defendant]",
     *       "label": "Defendant"
     *     }
     *   ]
     * }
     */
    public ObjectNode createDynamicList(DynamicListElement value, List<DynamicListElement> listItems) {
        DynamicList dynamicList = DynamicList.builder()
            .value(value)
            .listItems(listItems)
            .build();

        return objectMapper.valueToTree(dynamicList);
    }
}
