package uk.gov.hmcts.reform.managecase.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
     */
    public void nullifyObjectNode(ObjectNode node) {
        node.fieldNames().forEachRemaining(fieldName -> {
            if (node.get(fieldName).isObject()) {
                nullifyObjectNode((ObjectNode) node.get(fieldName));
            } else {
                node.set(fieldName, objectMapper.nullNode());
            }
        });
    }
}
