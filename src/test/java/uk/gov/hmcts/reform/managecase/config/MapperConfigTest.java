package uk.gov.hmcts.reform.managecase.config;

import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

import static org.assertj.core.api.Assertions.assertThat;

class MapperConfigTest {

    @Test
    void shouldConfigurePrivateFieldMatching() {
        ModelMapper modelMapper = new MapperConfig().modelMapper();

        assertThat(modelMapper.getConfiguration().isFieldMatchingEnabled()).isTrue();
        assertThat(modelMapper.getConfiguration().getFieldAccessLevel())
            .isEqualTo(org.modelmapper.config.Configuration.AccessLevel.PRIVATE);
    }
}
