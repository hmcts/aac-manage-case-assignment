package uk.gov.hmcts.reform.managecase.data.casedetails;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetailsExtended;
import uk.gov.hmcts.reform.managecase.data.casedetails.search.MetaData;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.valueOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class CachedCaseDetailsRepositoryTest {

    private static final long CASE_ID = 100000L;
    private static final long CASE_REFERENCE = 999999L;
    private static final String CASE_REFERENCE_STR = "1234123412341236";
    private static final String JURISDICTION_ID = "JeyOne";
    private static final String CASE_TYPE_ID = "CaseTypeOne";

    @Mock
    private CaseDetailsRepository caseDetailsRepository;

    private CaseDetailsExtended caseDetails;
    private List<CaseDetailsExtended> caseDetailsList;

    private MetaData metaData;
    private Map<String, String> dataSearchParams;

    @InjectMocks
    private CachedCaseDetailsRepository cachedRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        caseDetails = new CaseDetailsExtended();
        caseDetails.setId(valueOf(CASE_ID));
        caseDetails.setReference(CASE_REFERENCE);
        caseDetails.setJurisdiction(JURISDICTION_ID);
        caseDetails.setCaseTypeId(CASE_TYPE_ID);

        CaseDetailsExtended anotherCaseDetails = new CaseDetailsExtended();

        caseDetailsList = Arrays.asList(caseDetails, anotherCaseDetails);

        metaData = new MetaData(CASE_TYPE_ID, JURISDICTION_ID);
        metaData.setCaseReference(Optional.of(valueOf(CASE_REFERENCE)));

        dataSearchParams = new HashMap<>();

    }

    @Nested
    @DisplayName("findByReference(String, String)")
    class FindByReferenceAsString {
        @Test
        @DisplayName("should initially retrieve case details from decorated repository")
        void findByReference() {
            doReturn(Optional.of(caseDetails)).when(caseDetailsRepository).findByReference(JURISDICTION_ID,
                                                                                           CASE_REFERENCE_STR);

            final CaseDetailsExtended returned = cachedRepository.findByReference(JURISDICTION_ID, CASE_REFERENCE_STR)
                .orElseThrow(() -> new AssertionError("Not found"));

            assertAll(
                () -> assertThat(returned, is(caseDetails)),
                () -> verify(caseDetailsRepository, times(1)).findByReference(JURISDICTION_ID,
                                                                              CASE_REFERENCE_STR)
            );
        }

        @Test
        @DisplayName("should cache case details for subsequent calls")
        void findByReferenceAgain() {
            doReturn(Optional.of(caseDetails)).when(caseDetailsRepository).findByReference(JURISDICTION_ID,
                                                                                           CASE_REFERENCE_STR);

            cachedRepository.findByReference(JURISDICTION_ID, CASE_REFERENCE_STR);

            verify(caseDetailsRepository, times(1)).findByReference(JURISDICTION_ID,
                                                                    CASE_REFERENCE_STR);

            doReturn(Optional.of(new CaseDetailsExtended())).when(caseDetailsRepository).findByReference(
                JURISDICTION_ID, CASE_REFERENCE_STR);

            final CaseDetailsExtended returned = cachedRepository.findByReference(JURISDICTION_ID, CASE_REFERENCE_STR)
                .orElseThrow(() -> new AssertionError("Not found"));

            assertAll(
                () -> assertThat(returned, is(caseDetails)),
                () -> verifyNoMoreInteractions(caseDetailsRepository)
            );
        }
    }
}
