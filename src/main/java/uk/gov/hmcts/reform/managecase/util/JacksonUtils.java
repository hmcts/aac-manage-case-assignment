package uk.gov.hmcts.reform.managecase.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.managecase.domain.DynamicList;
import uk.gov.hmcts.reform.managecase.domain.DynamicListElement;

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
