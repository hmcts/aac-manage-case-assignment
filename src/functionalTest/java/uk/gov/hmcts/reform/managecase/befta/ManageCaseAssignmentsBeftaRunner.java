package uk.gov.hmcts.reform.managecase.befta;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.runner.RunWith;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import uk.gov.hmcts.befta.BeftaMain;

@RunWith(Cucumber.class)
@CucumberOptions(
    plugin = "json:target/cucumber.json",
        glue = { "uk.gov.hmcts.befta.player" },
    features = { "classpath:features" },
    tags = { "not @Ignore" }
)
public final class ManageCaseAssignmentsBeftaRunner {

    private ManageCaseAssignmentsBeftaRunner() {
        // Hide Utility Class Constructor : Utility classes should not have a public or
        // default constructor (squid:S1118)
    }

    @BeforeAll
    public static void setUp() {
        BeftaMain.setUp(new ManageCaseAssignmentTestAutomationAdapter());
    }

    @AfterAll
    public static void tearDown() {
        BeftaMain.tearDown();
    }

}
