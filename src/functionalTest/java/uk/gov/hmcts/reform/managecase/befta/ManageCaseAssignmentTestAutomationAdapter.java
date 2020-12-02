package uk.gov.hmcts.reform.managecase.befta;

import uk.gov.hmcts.befta.BeftaTestDataLoader;
import uk.gov.hmcts.befta.DefaultBeftaTestDataLoader;
import uk.gov.hmcts.befta.DefaultTestAutomationAdapter;
import uk.gov.hmcts.befta.dse.ccd.TestDataLoaderToDefinitionStore;

public class ManageCaseAssignmentTestAutomationAdapter extends DefaultTestAutomationAdapter {

    private final TestDataLoaderToDefinitionStore loader = new TestDataLoaderToDefinitionStore(this);

    @Override
    public BeftaTestDataLoader getDataLoader() {
        return new ManageCaseAssignmentTestDataLoader();
    }

    private class ManageCaseAssignmentTestDataLoader extends DefaultBeftaTestDataLoader {
        @Override
        protected void doLoadTestData() {
            loader.addCcdRoles();
            loader.importDefinitions();
        }
    }
}
