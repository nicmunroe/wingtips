package com.nike.wingtips;

import com.nike.internal.util.StringUtils;

/**
 * TODO: Class Description
 *
 * @author Nic Munroe
 */
public class SpanMutator {

    // Intentionally private to force all access to go through the static methods.
    private SpanMutator() {
        // Do nothing
    }

    public static void changeSpanName(Span span, String newName) {
        // TODO: Log warnings if span is null or newName is blank?
        if (span == null) {
            return;
        }

        if (StringUtils.isBlank(newName)) {
            return;
        }

        span.setSpanName(newName);
    }

}
