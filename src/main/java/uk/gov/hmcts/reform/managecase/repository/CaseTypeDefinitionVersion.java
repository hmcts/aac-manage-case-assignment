package uk.gov.hmcts.reform.managecase.repository;

import java.io.Serializable;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class CaseTypeDefinitionVersion implements Serializable {

    private static final long serialVersionUID = 3792842101045258030L;
    private Integer version;
}
