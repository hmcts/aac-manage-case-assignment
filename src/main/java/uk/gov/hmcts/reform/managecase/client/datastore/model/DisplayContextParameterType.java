package uk.gov.hmcts.reform.managecase.client.datastore.model;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum DisplayContextParameterType {
    DATETIMEENTRY,
    DATETIMEDISPLAY,
    TABLE,
    LIST;

    private static final Pattern PATTERN = Pattern.compile("#(.+)\\((.+)\\)");
    private static final int TYPE_GROUP = 1;
    private static final int VALUE_GROUP = 2;

    public static Optional<DisplayContextParameterType> getParameterTypeFor(String displayContextParameter) {
        Matcher matcher = PATTERN.matcher(displayContextParameter);
        if (matcher.matches()) {
            try {
                return Optional.of(valueOf(matcher.group(TYPE_GROUP)));
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public static Optional<String> getParameterValueFor(String displayContextParameter) {
        Matcher matcher = PATTERN.matcher(displayContextParameter);
        if (matcher.matches()) {
            return Optional.of(matcher.group(VALUE_GROUP));
        }
        return Optional.empty();
    }
}
