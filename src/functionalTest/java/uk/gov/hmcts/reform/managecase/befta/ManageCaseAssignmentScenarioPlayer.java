package uk.gov.hmcts.reform.managecase.befta;

import io.cucumber.java.en.Given;

public class ManageCaseAssignmentScenarioPlayer {

    @Given("logstash has finished indexing case data")
    public void waitForLogstashToIndexCaseData() throws InterruptedException {
        Thread.sleep(5000);
    }
}
