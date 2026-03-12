package com.nike.wingtips.spring.webflux.client;

import com.nike.wingtips.Span;
import com.nike.wingtips.Span.SpanPurpose;
import com.nike.wingtips.tags.HttpTagAndSpanNamingAdapter;
import com.nike.wingtips.tags.KnownZipkinTags;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.web.reactive.function.client.ClientRequest;

import java.util.UUID;
import java.util.stream.Stream;

import static com.nike.wingtips.spring.webflux.client.SpringWebfluxClientRequestZipkinTagStrategy.SPRING_LOG_ID_TAG_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Tests the functionality of {@link SpringWebfluxClientRequestZipkinTagStrategy}.
 *
 * @author Nic Munroe
 */
public class SpringWebfluxClientRequestZipkinTagStrategyTest {

    @Test
    public void getDefaultInstance_returns_DEFAULT_INSTANCE() {
        // expect
        assertThat(SpringWebfluxClientRequestZipkinTagStrategy.getDefaultInstance())
            .isSameAs(SpringWebfluxClientRequestZipkinTagStrategy.DEFAULT_INSTANCE);
    }

    @SuppressWarnings("unused")
    private enum LogPrefixScenario {
        EMPTY("", null),
        BLANK(" \t\n\r  ", null),
        NON_BLANK("foo-log-prefix-12345", "foo-log-prefix-12345"),
        NON_BLANK_BUT_NEEDS_TRIMMING("  needs trimming  \t\n\r  ", "needs trimming");

        public final String logPrefix;
        public final String expectedTagValue;

        LogPrefixScenario(String logPrefix, String expectedTagValue) {
            this.logPrefix = logPrefix;
            this.expectedTagValue = expectedTagValue;
        }
    }
    public static Stream<Arguments> logPrefixScenario_DataProvider() {
        return Stream.of(LogPrefixScenario.values()).map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("logPrefixScenario_DataProvider")
    public void doHandleRequestTagging_adds_expected_zipkin_tags_and_spring_log_prefix_tag(
        LogPrefixScenario scenario
    ) {
        // given
        SpringWebfluxClientRequestZipkinTagStrategy impl = new SpringWebfluxClientRequestZipkinTagStrategy();
        Span span = Span.newBuilder("fooSpan", SpanPurpose.CLIENT).build();
        ClientRequest requestMock = mock(ClientRequest.class);
        @SuppressWarnings("unchecked")
        HttpTagAndSpanNamingAdapter<ClientRequest, ?> adapterMock = mock(HttpTagAndSpanNamingAdapter.class);

        String httpMethod = UUID.randomUUID().toString();
        String path = UUID.randomUUID().toString();
        String url = UUID.randomUUID().toString();
        String pathTemplate = UUID.randomUUID().toString();

        doReturn(httpMethod).when(adapterMock).getRequestHttpMethod(requestMock);
        doReturn(path).when(adapterMock).getRequestPath(requestMock);
        doReturn(url).when(adapterMock).getRequestUrl(requestMock);
        doReturn(pathTemplate).when(adapterMock).getRequestUriPathTemplate(requestMock, null);
        doReturn(scenario.logPrefix).when(requestMock).logPrefix();

        // when
        impl.doHandleRequestTagging(span, requestMock, adapterMock);

        // then
        assertThat(span.getTags().get(KnownZipkinTags.HTTP_METHOD)).isEqualTo(httpMethod);
        assertThat(span.getTags().get(KnownZipkinTags.HTTP_PATH)).isEqualTo(path);
        assertThat(span.getTags().get(KnownZipkinTags.HTTP_URL)).isEqualTo(url);
        assertThat(span.getTags().get(KnownZipkinTags.HTTP_ROUTE)).isEqualTo(pathTemplate);
        assertThat(span.getTags().get(SPRING_LOG_ID_TAG_KEY)).isEqualTo(scenario.expectedTagValue);
    }

}