package uk.gov.hmcts.reform.managecase;

import org.springframework.beans.factory.annotation.Value;

import javax.inject.Named;
import javax.inject.Singleton;

@Named
@Singleton
public class ApplicationParams {

    @Value("${ccd.data-store.host}")
    private String dataStoreHost;

    @Value("${prd.host}")
    private String referenceDataHost;

    public String getDataStoreHost() {
        return dataStoreHost;
    }

    public String getReferenceDataHost() {
        return referenceDataHost;
    }
}
