package com.nike.wingtips.tags;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * TODO: Class Description
 *
 * @author Nic Munroe
 */
public class NoOpHttpTagAdapter<REQ,RES> extends HttpTagAndSpanNamingAdapter<REQ,RES> {

    @Override
    public @Nullable String getErrorResponseTagValue(@Nullable RES response) {
        return null;
    }

    @Override
    public @Nullable String getRequestUrl(@Nullable REQ request) {
        return null;
    }

    @Override
    public @Nullable String getRequestPath(@Nullable REQ request) {
        return null;
    }

    @Override
    public @Nullable Integer getResponseHttpStatus(@Nullable RES response) {
        return null;
    }

    @Override
    public @Nullable String getRequestHttpMethod(@Nullable REQ request) {
        return null;
    }

    @Override
    public @Nullable String getHeaderSingleValue(@Nullable REQ request, @NotNull String headerKey) {
        return null;
    }

    @Override
    public @Nullable List<String> getHeaderMultipleValue(@Nullable REQ request, @NotNull String headerKey) {
        return null;
    }

    @Override
    public @Nullable String getSpanNamePrefix(@Nullable REQ request) {
        return null;
    }

    @Override
    public @Nullable String getInitialSpanName(@Nullable REQ request) {
        return null;
    }

    @Override
    public @Nullable String getFinalSpanName(@Nullable REQ request, @Nullable RES response) {
        return null;
    }

    @Override
    public @Nullable String getRequestUriPathTemplate(@Nullable REQ request, @Nullable RES response) {
        return null;
    }

    @Override
    public @Nullable String getSpanHandlerTagValue(@Nullable REQ request, @Nullable RES response) {
        return null;
    }
}
