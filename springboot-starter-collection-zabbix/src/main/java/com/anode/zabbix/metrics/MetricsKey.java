package com.anode.zabbix.metrics;

import lombok.Getter;
import lombok.ToString;

import java.util.Arrays;

@Getter
@ToString
public final class MetricsKey {

    private static final char KEY_SEPARATOR = '.';
    private static final char PARAMS_START = '[';
    private static final char PARAMS_END = ']';
    private static final char PARAMS_SEPARATOR = ',';

    private final String provider;
    private final String key;
    private final String[] parameters;

    /**
     * Constructs a new MetricsKey instance.
     *
     * @param keyData the textual key
     * @throws MetricsException when parsing fails
     */
    public MetricsKey(String keyData) throws MetricsException {
        if (keyData == null || keyData.isBlank()) {
            throw new MetricsException("Key string is null or blank");
        }

        try {
            int paramsStart = keyData.indexOf(PARAMS_START);
            int searchEnd = paramsStart == -1 ? keyData.length() : paramsStart;
            int sepIndex = keyData.lastIndexOf(KEY_SEPARATOR, searchEnd);

            if (sepIndex <= 0) {
                throw new MetricsException("Key string does not contain provider separator");
            }

            this.provider = keyData.substring(0, sepIndex);

            String remainder = keyData.substring(sepIndex + 1);

            int paramsOpen = remainder.indexOf(PARAMS_START);
            int paramsClose = remainder.lastIndexOf(PARAMS_END);

            if (paramsOpen >= 0 && paramsClose > paramsOpen) {
                this.key = remainder.substring(0, paramsOpen);
                this.parameters = parseParameters(
                        remainder.substring(paramsOpen + 1, paramsClose)
                );
            } else {
                this.key = remainder;
                this.parameters = new String[0];
            }

        } catch (MetricsException e) {
            throw e;
        } catch (Exception e) {
            throw new MetricsException("Parse error", e);
        }
    }

    public boolean hasParameters() {
        return parameters.length > 0;
    }

    public String[] getParameters() {
        return parameters.clone();
    }

    private static String[] parseParameters(String params) {
        return Arrays.stream(params.split(String.valueOf(PARAMS_SEPARATOR)))
                .map(MetricsKey::stripQuotes)
                .toArray(String[]::new);
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
