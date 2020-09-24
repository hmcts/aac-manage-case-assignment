package uk.gov.hmcts.reform.managecase.client.datastore.model.elasticsearch;

import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableList;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;

public class MetaData {
    public static final String STATE_FIELD_COL = "state";
    public static final String JURISDICTION_FIELD_COL = "jurisdiction";
    public static final String CASE_TYPE_ID_FIELD_COL = "case_type_id";
    public static final String REFERENCE_FIELD_COL = "reference";
    public static final String CREATED_DATE_FIELD_COL = "created_date";
    public static final String LAST_MODIFIED_FIELD_COL = "last_modified";
    public static final String LAST_STATE_MODIFIED_DATE_FIELD_COL = "last_state_modified_date";
    public static final String SECURITY_CLASSIFICATION_FIELD_COL = "security_classification";

    public static final String PAGE_PARAM = "page";
    public static final String SORT_PARAM = "sortDirection";
    public static final ImmutableList<CaseField> DATE_FIELDS = ImmutableList.of(
        CaseField.CREATED_DATE,
        CaseField.LAST_MODIFIED_DATE,
        CaseField.LAST_STATE_MODIFIED_DATE
    );
    private static final List<String> METADATA_QUERY_PARAMETERS = newArrayList(CaseField.STATE.getParameterName(),
                                                                               CaseField.CASE_REFERENCE
                                                                                   .getParameterName(),
                                                                               CaseField.CREATED_DATE
                                                                                   .getParameterName(),
                                                                               CaseField.LAST_MODIFIED_DATE
                                                                                   .getParameterName(),
                                                                               CaseField.LAST_STATE_MODIFIED_DATE
                                                                                   .getParameterName(),
                                                                               CaseField.SECURITY_CLASSIFICATION
                                                                                   .getParameterName(),
                                                                               PAGE_PARAM, SORT_PARAM);

    // Metadata case fields
    public enum CaseField {
        JURISDICTION("jurisdiction", JURISDICTION_FIELD_COL),
        CASE_TYPE("case_type", CASE_TYPE_ID_FIELD_COL),
        STATE("state", STATE_FIELD_COL),
        CASE_REFERENCE("case_reference", REFERENCE_FIELD_COL),
        CREATED_DATE("created_date", CREATED_DATE_FIELD_COL),
        LAST_MODIFIED_DATE("last_modified_date", LAST_MODIFIED_FIELD_COL),
        LAST_STATE_MODIFIED_DATE("last_state_modified_date", LAST_STATE_MODIFIED_DATE_FIELD_COL),
        SECURITY_CLASSIFICATION("security_classification", SECURITY_CLASSIFICATION_FIELD_COL);

        private final String parameterName;
        private final String dbColumnName;

        CaseField(String parameterName, String dbColumnName) {
            this.parameterName = parameterName;
            this.dbColumnName = dbColumnName;
        }

        public String getParameterName() {
            return parameterName;
        }

        public String getDbColumnName() {
            return dbColumnName;
        }

        public String getReference() {
            return String.join(getParameterName().toUpperCase(), "[", "]");
        }

        public static CaseField valueOfReference(String reference) {
            return valueOf(reference.replace("[", "").replace("]", ""));
        }

        public static CaseField valueOfColumnName(String dbColumnName) {
            return Arrays.stream(values())
                .filter(v -> v.getDbColumnName().equals(dbColumnName))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("No MetaData field exists with column name '%s'", dbColumnName)));
        }
    }

    private final String caseTypeId;
    private final String jurisdiction;
    private Optional<String> state = Optional.empty();
    private Optional<String> caseReference = Optional.empty();
    private Optional<String> createdDate = Optional.empty();
    private Optional<String> lastModifiedDate = Optional.empty();
    private Optional<String> lastStateModifiedDate = Optional.empty();
    private Optional<String> securityClassification = Optional.empty();
    private Optional<String> page = Optional.empty();
    private Optional<String> sortDirection = Optional.empty();
    private List<SortOrderField> sortOrderFields = newArrayList();

    public MetaData(String caseTypeId, String jurisdiction) {
        this.caseTypeId = caseTypeId;
        this.jurisdiction = jurisdiction;
    }

    public String getCaseTypeId() {
        return caseTypeId;
    }

    public String getJurisdiction() {
        return jurisdiction;
    }

    public Optional<String> getState() {
        return state;
    }

    public void setCaseReference(Optional<String> caseReference) {
        this.caseReference = caseReference;
    }

    public Optional<String> getCaseReference() {
        return caseReference;
    }

    public void setState(Optional<String> state) {
        this.state = state;
    }

    public Optional<String> getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Optional<String> createdDate) {
        this.createdDate = createdDate;
    }

    public void setLastModifiedDate(Optional<String> lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public Optional<String> getLastStateModifiedDate() {
        return lastStateModifiedDate;
    }

    public void setLastStateModifiedDate(Optional<String> lastStateModifiedDate) {
        this.lastStateModifiedDate = lastStateModifiedDate;
    }

    public Optional<String> getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setSecurityClassification(Optional<String> securityClassification) {
        this.securityClassification = securityClassification.map(this::toTrimmedLowerCase);
    }

    public Optional<String> getSecurityClassification() {
        return securityClassification;
    }

    public Optional<String> getPage() {
        return page;
    }

    public void setPage(Optional<String> page) {
        this.page = page;
    }

    public Optional<String> getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(Optional<String> sortDirection) {
        this.sortDirection = sortDirection;
    }

    public List<SortOrderField> getSortOrderFields() {
        return sortOrderFields;
    }

    public void setSortOrderFields(List<SortOrderField> sortOrderFields) {
        this.sortOrderFields = sortOrderFields;
    }

    public void addSortOrderField(SortOrderField sortOrderField) {
        this.sortOrderFields.add(sortOrderField);
    }

    public static List<String> unknownMetadata(List<String> parameters) {
        return parameters.stream().filter(p -> !METADATA_QUERY_PARAMETERS.contains(p)).collect(toList());
    }

    private String toTrimmedLowerCase(String s) {
        return s.trim().toLowerCase();
    }

    @SuppressWarnings("unchecked")
    public Optional<String> getOptionalMetadata(CaseField metadataField) {
        final String methodName = getMethodName(metadataField, "get");
        try {
            final Method method = getClass().getMethod(methodName);
            return (Optional<String>) method.invoke(this);
        } catch (NoSuchMethodException | ClassCastException e) {
            throw new IllegalArgumentException(
                String.format("No getter method with Optional<String> return value found for '%s'; looking for '%s()'",
                    metadataField.getParameterName(), methodName));
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException(
                String.format("Failed to invoke method '%s'",
                    methodName));
        }
    }

    public void setOptionalMetadata(CaseField metadataField, String value) {
        final String methodName = getMethodName(metadataField, "set");
        try {
            final Method method = getClass().getMethod(methodName, Optional.class);
            method.invoke(this, Optional.ofNullable(value));
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                String.format("No setter method with Optional argument found for '%s'; looking for '%s()'",
                    metadataField.getParameterName(), methodName));
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException(
                String.format("Failed to invoke method '%s' with Optional String value of '%s'",
                    methodName, value));
        }
    }

    private String getMethodName(CaseField metadataField, String prefix) {
        return prefix + CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, metadataField.getParameterName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MetaData metaData = (MetaData) o;
        return Objects.equals(caseTypeId, metaData.caseTypeId)
            && Objects.equals(jurisdiction, metaData.jurisdiction)
            && Objects.equals(state, metaData.state)
            && Objects.equals(caseReference, metaData.caseReference)
            && Objects.equals(createdDate, metaData.createdDate)
            && Objects.equals(lastModifiedDate, metaData.lastModifiedDate)
            && Objects.equals(lastStateModifiedDate, metaData.lastStateModifiedDate)
            && Objects.equals(securityClassification, metaData.securityClassification)
            && Objects.equals(page, metaData.page)
            && Objects.equals(sortDirection, metaData.sortDirection);
    }

    @Override
    public int hashCode() {
        return Objects.hash(caseTypeId,
                            jurisdiction,
                            state,
                            caseReference,
                            createdDate,
                            lastModifiedDate,
                            lastStateModifiedDate,
                            securityClassification,
                            page,
                            sortDirection);
    }
}
