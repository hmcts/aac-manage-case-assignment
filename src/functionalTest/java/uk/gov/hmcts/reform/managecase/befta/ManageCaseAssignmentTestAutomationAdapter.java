package uk.gov.hmcts.reform.managecase.befta;

import uk.gov.hmcts.befta.BeftaTestDataLoader;
import uk.gov.hmcts.befta.DefaultBeftaTestDataLoader;
import uk.gov.hmcts.befta.DefaultTestAutomationAdapter;
import uk.gov.hmcts.befta.dse.ccd.TestDataLoaderToDefinitionStore;
import uk.gov.hmcts.befta.BeftaMain;

public class ManageCaseAssignmentTestAutomationAdapter extends DefaultTestAutomationAdapter {

    private TestDataLoaderToDefinitionStore loader = new TestDataLoaderToDefinitionStore(this,
            "uk/gov/hmcts/ccd/test_definitions/valid", BeftaMain.getConfig().getDefinitionStoreUrl());

    @Override
    public BeftaTestDataLoader getDataLoader() {
        return new DefaultBeftaTestDataLoader() {
            @Override
            protected void doLoadTestData() {
                loader.addCcdRoles();
                loader.importDefinitions();
            }
        };
    }
}
