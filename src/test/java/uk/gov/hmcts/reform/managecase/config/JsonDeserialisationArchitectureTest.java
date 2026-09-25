package uk.gov.hmcts.reform.managecase.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonDeserialisationArchitectureTest {

    private static final JavaClasses MAIN_CLASSES = new ClassFileImporter()
        .withImportOption(new ImportOption.DoNotIncludeTests())
        .importPackages("uk.gov.hmcts.reform.managecase", "uk.gov.hmcts.ccd");

    private static final Map<String, String> PERMITTED_IGNORE_UNKNOWN_TYPES = Map.of();

    @Test
    void shouldNotAllowJsonIgnorePropertiesIgnoreUnknownTrue() {
        classes()
            .should(notBeAnnotatedWithJsonIgnorePropertiesIgnoreUnknownTrue())
            .because("unknown JSON properties must fail deserialisation unless an exception is explicitly documented")
            .check(MAIN_CLASSES);
    }

    @Test
    void guardShouldFailForJsonIgnorePropertiesIgnoreUnknownTrue() {
        JavaClasses testClasses = new ClassFileImporter().importClasses(DisallowedIgnoreUnknownDto.class);

        assertThatThrownBy(() -> classes()
            .should(notBeAnnotatedWithJsonIgnorePropertiesIgnoreUnknownTrue())
            .check(testClasses))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining(DisallowedIgnoreUnknownDto.class.getName());
    }

    @Test
    void permittedIgnoreUnknownExceptionsMustHaveRationale() {
        assertThat(PERMITTED_IGNORE_UNKNOWN_TYPES)
            .allSatisfy((className, rationale) -> assertThat(rationale)
                .as("Exception %s must document why unknown JSON properties are safe", className)
                .isNotBlank());
    }

    private static ArchCondition<JavaClass> notBeAnnotatedWithJsonIgnorePropertiesIgnoreUnknownTrue() {
        return new ArchCondition<>("not be annotated with @JsonIgnoreProperties(ignoreUnknown = true)") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                JsonIgnoreProperties annotation = item.reflect().getAnnotation(JsonIgnoreProperties.class);
                if (annotation != null && annotation.ignoreUnknown()
                    && !PERMITTED_IGNORE_UNKNOWN_TYPES.containsKey(item.getName())) {
                    events.add(SimpleConditionEvent.violated(
                        item,
                        item.getName() + " is annotated with @JsonIgnoreProperties(ignoreUnknown = true)"
                    ));
                }
            }
        };
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class DisallowedIgnoreUnknownDto {
    }
}
