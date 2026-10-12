package uk.gov.hmcts.reform.managecase.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SuppressWarnings("unchecked")
class JacksonObjectMapperConfigTest {

    private final JacksonObjectMapperConfig config = new JacksonObjectMapperConfig();

    @Test
    void createsSimpleObjectMapperWithJavaTimeSupport() throws Exception {
        ObjectMapper mapper = config.simpleObjectMapper();

        assertThat(mapper.writeValueAsString(java.time.LocalDate.of(2025, 1, 2)))
            .isEqualTo("[2025,1,2]");
    }

    @Test
    void replacesJackson3ConverterWithJackson2Converter() {
        HttpMessageConverters.ServerBuilder builder = mock(HttpMessageConverters.ServerBuilder.class);
        config.configureMessageConverters(builder);

        var consumerCaptor = forClass(Consumer.class);
        verify(builder).configureMessageConvertersList(consumerCaptor.capture());

        List<HttpMessageConverter<?>> converters = new ArrayList<>(List.of(
            new JacksonJsonHttpMessageConverter()
        ));
        consumerCaptor.getValue().accept(converters);

        assertThat(converters).hasSize(1);
        assertThat(config.isJackson2Converter(converters.get(0))).isTrue();
    }

    @Test
    void doesNotAddDuplicateJackson2Converter() {
        HttpMessageConverters.ServerBuilder builder = mock(HttpMessageConverters.ServerBuilder.class);
        config.configureMessageConverters(builder);

        var consumerCaptor = forClass(Consumer.class);
        verify(builder).configureMessageConvertersList(consumerCaptor.capture());

        HttpMessageConverter<?> jackson2Converter = config.createJackson2Converter();
        List<HttpMessageConverter<?>> converters = new ArrayList<>(List.of(jackson2Converter));
        consumerCaptor.getValue().accept(converters);

        assertThat(converters).containsExactly(jackson2Converter);
    }

    @Test
    void reportsUnavailableJackson2ConverterClass() {
        assertThatThrownBy(() -> config.jackson2ConverterClass("not.a.RealConverter"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Jackson 2 HTTP converter is unavailable");
    }

    @Test
    void reportsUnconstructableJackson2Converter() {
        assertThatThrownBy(() -> config.createJackson2Converter(String.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Jackson 2 HTTP converter is unavailable");
    }
}
