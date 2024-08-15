package uk.gov.hmcts.reform.managecase;

import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.servlet.WebMvcEndpointHandlerMapping;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(SpringExtension.class)
@ImportAutoConfiguration(TestIdamConfiguration.class)
public class BaseTest {
    
    @Autowired
    protected MockMvc mockMvc;

    @MockBean
    protected ModelMapper mapper;

    @MockBean
    protected WebEndpointsSupplier webEndpointsSupplier;

    @MockBean
    protected WebMvcEndpointHandlerMapping webMvcEndpointHandlerMapping;

    @Autowired
    protected ObjectMapper objectMapper;

}
