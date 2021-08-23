package uk.gov.hmcts.reform.managecase.data.casedetails.supplementarydata;

import org.hibernate.query.NativeQuery;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.Properties;
import java.util.regex.Pattern;
import com.vladmihalcea.hibernate.type.json.JsonNodeBinaryType;
import pl.jalokim.propertiestojson.util.PropertiesToJsonConverter;


public interface SupplementaryDataQueryBuilder {

    Query build(EntityManager entityManager,
                String caseReference,
                String fieldPath,
                Object fieldValue);

    SupplementaryDataOperation operationType();

    default void setCommonProperties(Query query,
                                     String caseReference,
                                     String fieldPath,
                                     Object fieldValue) {
        String key = fieldPath.replaceAll(Pattern.quote("."), ",");
        query.setParameter("leaf_node_key", "{" + key + "}");
        query.setParameter("value", fieldValue);
        query.setParameter("reference", caseReference);
        query.unwrap(NativeQuery.class)
            .addScalar("supplementary_data", JsonNodeBinaryType.INSTANCE);
    }

    default String requestedDataToJson(String fieldPath, Object fieldValue) {
        PropertiesToJsonConverter propertiesMapper = new PropertiesToJsonConverter();
        Properties properties = new Properties();
        properties.put(fieldPath, fieldValue);
        return propertiesMapper.convertToJson(properties);
    }
}
