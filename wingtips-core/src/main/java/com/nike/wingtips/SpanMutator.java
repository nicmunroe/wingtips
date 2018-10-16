package com.nike.wingtips;

import com.nike.internal.util.StringUtils;

import org.jetbrains.annotations.Nullable;

/**
 * A helper class that has methods that can mutate some fields in {@link Span}s that are otherwise immutable.
 *
 * @author Nic Munroe
 */
public class SpanMutator {

    // Intentionally private to force all access to go through the static methods.
    private SpanMutator() {
        // Do nothing
    }

    /**
     * Changes the given span's {@link Span#getSpanName()} to the given {@code newName}. Does nothing if the given
     * span is null, or if the given {@code newName} is null or blank.
     *
     * @param span The span to mutate. You can pass null, although nothing will happen if you do.
     * @param newName The new span name to set on the given span. You can pass null or blank string, although nothing
     * will happen if you do (the span's name will not be changed in these cases).
     */
    public static void changeSpanName(@Nullable Span span, @Nullable String newName) {
        if (span == null) {
            return;
        }

        if (StringUtils.isBlank(newName)) {
            return;
        }

        span.setSpanName(newName);
    }

}
