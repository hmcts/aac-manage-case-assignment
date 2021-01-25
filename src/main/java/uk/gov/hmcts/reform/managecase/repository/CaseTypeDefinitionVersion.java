package uk.gov.hmcts.reform.managecase.repository;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;

@Getter
@Setter
public class CaseTypeDefinitionVersion implements Serializable {

    private static final long serialVersionUID = 3792842101045258030L;

    private Integer version;

    public CaseTypeDefinitionVersion() {
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("version", version)
            .toString();
    }
}
