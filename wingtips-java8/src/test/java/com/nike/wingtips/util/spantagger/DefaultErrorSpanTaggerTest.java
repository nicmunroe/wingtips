package com.nike.wingtips.util.spantagger;

import com.nike.wingtips.Span;
import com.nike.wingtips.tags.KnownZipkinTags;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.UUID;
import java.util.stream.Stream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Tests the functionality of {@link DefaultErrorSpanTagger}.
 */
public class DefaultErrorSpanTaggerTest {

    public static Stream<Arguments> tagSpanForError_works_as_expected_DataProvider() {
        return Stream.of(
            Arguments.of(false, false, true),
            Arguments.of(true, false, false),
            Arguments.of(false, true, false),
            Arguments.of(true, true, false)
        );
    }

    @ParameterizedTest
    @MethodSource("tagSpanForError_works_as_expected_DataProvider")
    @SuppressWarnings("ConstantConditions")
    public void tagSpanForError_works_as_expected(boolean spanIsNull, boolean errorIsNull, boolean expectErrorTag) {
        // given
        DefaultErrorSpanTagger tagger = new DefaultErrorSpanTagger();
        Span span = (spanIsNull) ? null : mock(Span.class);
        Throwable error = (errorIsNull)
                          ? null
                          : new Exception("Intentional test exception: " + UUID.randomUUID().toString());

        // when
        tagger.tagSpanForError(span, error);

        // then
        if (expectErrorTag) {
            verify(span).putTag(KnownZipkinTags.ERROR, error.toString());
        }
        else if (span != null) {
            verifyNoInteractions(span);
        }
    }
}