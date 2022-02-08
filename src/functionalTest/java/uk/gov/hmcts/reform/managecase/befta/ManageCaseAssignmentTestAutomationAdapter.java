package uk.gov.hmcts.reform.managecase.befta;

import uk.gov.hmcts.befta.BeftaTestDataLoader;
import uk.gov.hmcts.befta.DefaultBeftaTestDataLoader;
import uk.gov.hmcts.befta.DefaultTestAutomationAdapter;
import uk.gov.hmcts.befta.dse.ccd.CcdEnvironment;

public class ManageCaseAssignmentTestAutomationAdapter extends DefaultTestAutomationAdapter {

    @Override
    protected BeftaTestDataLoader buildTestDataLoader() {
        return new DefaultBeftaTestDataLoader(CcdEnvironment.AAT);
    }

}
