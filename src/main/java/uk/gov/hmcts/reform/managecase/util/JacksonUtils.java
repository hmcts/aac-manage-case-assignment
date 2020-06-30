package uk.gov.hmcts.reform.managecase.util;

import com.fasterxml.jackson.databind.ObjectMapper;
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
}
