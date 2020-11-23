package uk.gov.hmcts.reform.managecase.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.managecase.domain.DynamicListElement;

import java.util.List;

import static java.util.stream.Collectors.toList;

@Component
public class JacksonUtils {
    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    protected static final String DL_VALUE = "value";
    protected static final String DL_LIST_ITEMS = "list_items";
    protected static final String DL_ELEMENT_CODE = "code";
    protected static final String DL_ELEMENT_LABEL = "label";

    private final ObjectMapper objectMapper;

    @Autowired
    public JacksonUtils(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public <T> T convertValue(Object fromValue, Class<T> toValueType) {
        return objectMapper.convertValue(fromValue, toValueType);
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
        ObjectNode root = JSON_NODE_FACTORY.objectNode();
        root.putObject(DL_VALUE).setAll(createObjectNode(value.getCode(), value.getLabel()));
        root.putArray(DL_LIST_ITEMS)
            .addAll(listItems.stream()
                        .map(listItem -> createObjectNode(listItem.getCode(), listItem.getLabel()))
                        .collect(toList()));
        return root;
    }

    /**
     * Ex.:
     * {
     *   "code": "[Claimant]",
     *   "label": "Claimant"
     * }
     */
    private ObjectNode createObjectNode(String code, String label) {
        return JSON_NODE_FACTORY.objectNode()
            .put(DL_ELEMENT_CODE, code)
            .put(DL_ELEMENT_LABEL, label);
    }
}
