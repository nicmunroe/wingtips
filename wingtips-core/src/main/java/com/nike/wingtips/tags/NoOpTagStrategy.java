package com.nike.wingtips.tags;

import com.nike.wingtips.Span;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NoOpTagStrategy<REQ, RES> extends HttpTagAndSpanNamingStrategy<REQ, RES> {

    @Override
    protected @Nullable String doGetInitialSpanName(
        @NotNull REQ request,
        @NotNull HttpTagAndSpanNamingAdapter<REQ, ?> adapter
    ) {
        return null;
    }

    @Override
    protected void doDetermineAndSetFinalSpanName(
        @NotNull Span span,
        @Nullable REQ request,
        @Nullable RES response,
        @Nullable Throwable error,
        @NotNull HttpTagAndSpanNamingAdapter<REQ, RES> adapter
    ) {
        // intentionally do nothing
    }

    @Override
    protected void doHandleRequestTagging(
        @NotNull Span span,
        @NotNull REQ request,
        @NotNull HttpTagAndSpanNamingAdapter<REQ, ?> adapter
    ) {
        // intentionally do nothing
    }

    @Override
    protected void doHandleResponseAndErrorTagging(
        @NotNull Span span,
        @Nullable REQ request,
        @Nullable RES response,
        @Nullable Throwable error,
        @NotNull HttpTagAndSpanNamingAdapter<REQ, RES> adapter
    ) {
        // intentionally do nothing
    }
}
