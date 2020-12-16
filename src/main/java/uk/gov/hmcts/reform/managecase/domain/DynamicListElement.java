package uk.gov.hmcts.reform.managecase.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class DynamicListElement {
    public static final String CODE = "code";
    public static final String LABEL = "label";

    private String code;
    private String label;
}
