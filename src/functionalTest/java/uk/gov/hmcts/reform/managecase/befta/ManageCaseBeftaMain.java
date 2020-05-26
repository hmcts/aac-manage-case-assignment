package uk.gov.hmcts.reform.managecase.befta;

import uk.gov.hmcts.befta.BeftaMain;

public class ManageCaseBeftaMain extends BeftaMain {

    public static void main(String[] args) {
        BeftaMain.main(args, new ManageCaseTestAutomationAdapter());
    }

}
