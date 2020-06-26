package uk.gov.hmcts.reform.managecase.befta;

import io.cucumber.java.en.Given;

public class ManageCaseAssignmentScenarioPlayer {

    @Given("logstash has finished indexing case data")
    public void waitForLogstashToIndexCaseData() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
