package com.nike.wingtips.jersey2;

import com.nike.wingtips.tags.KnownZipkinTags;

import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ExtendedUriInfo;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;
import org.glassfish.jersey.uri.UriTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Tests the functionality of {@link SpanCustomizingApplicationEventListener}.
 *
 * @author Nic Munroe
 */
public class SpanCustomizingApplicationEventListenerTest {

    private SpanCustomizingApplicationEventListener implSpy;
    private RequestEvent requestEventMock;
    private ContainerRequest requestMock;
    private ExtendedUriInfo extendedUriInfoMock;

    @BeforeEach
    public void beforeMethod() {
        setupMocks();
    }

    private void setupMocks() {
        implSpy = spy(SpanCustomizingApplicationEventListener.create());
        requestEventMock = mock(RequestEvent.class);
        requestMock = mock(ContainerRequest.class);
        extendedUriInfoMock = mock(ExtendedUriInfo.class);

        doReturn(RequestEvent.Type.REQUEST_MATCHED).when(requestEventMock).getType();
        doReturn(requestMock).when(requestEventMock).getContainerRequest();
        doReturn(extendedUriInfoMock).when(requestMock).getUriInfo();
    }

    @Test
    public void create_creates_a_new_instance() {
        // when
        SpanCustomizingApplicationEventListener result = SpanCustomizingApplicationEventListener.create();

        // then
        assertThat(result).isNotNull();

        // and expect
        assertThat(SpanCustomizingApplicationEventListener.create()).isNotSameAs(result);
    }

    @Test
    public void onEvent_for_ApplicationEvent_does_nothing() {
        // given
        ApplicationEvent eventMock = mock(ApplicationEvent.class);

        // when
        implSpy.onEvent(eventMock);

        // then
        verify(implSpy).onEvent(eventMock);
        verifyNoMoreInteractions(implSpy);
        verifyNoInteractions(eventMock);
    }

    @Test
    public void onRequest_retunrs_self_for_START_otherwise_null() {
        for (RequestEvent.Type type : RequestEvent.Type.values()) {
            // given
            doReturn(type).when(requestEventMock).getType();
            boolean expectNonNullResult = (type == RequestEvent.Type.START);

            // when
            RequestEventListener result = implSpy.onRequest(requestEventMock);

            // then
            if (expectNonNullResult) {
                assertThat(result).isSameAs(implSpy);
            }
            else {
                assertThat(result).isNull();
            }
        }
    }

    @Test
    public void onEvent_for_RequestEvent_handles_REQUEST_MATCHED_only_and_sets_HTTP_ROUTE_to_result_of_route_method() {
        for (RequestEvent.Type type : RequestEvent.Type.values()) {
            // given
            setupMocks();
            doReturn(type).when(requestEventMock).getType();

            boolean expectHandled = (type == RequestEvent.Type.REQUEST_MATCHED);

            String routeMethodResult = "route-" + UUID.randomUUID().toString();
            doReturn(routeMethodResult).when(implSpy).route(any(ContainerRequest.class));

            // when
            implSpy.onEvent(requestEventMock);

            // then
            if (expectHandled) {
                verify(requestMock).setProperty(KnownZipkinTags.HTTP_ROUTE, routeMethodResult);
            }
            else {
                verifyNoInteractions(requestMock);
            }
        }
    }

    private void setupUriInfoWithBasePathAndUriTemplates(
        ExtendedUriInfo uriInfoMock,
        URI basePath,
        String ... templateStrings
    ) {
        doReturn(basePath).when(uriInfoMock).getBaseUri();

        List<UriTemplate> templates = new ArrayList<>();
        if (templateStrings != null) {
            for (String templateString : templateStrings) {
                templates.add(new UriTemplate(templateString));
            }
        }

        doReturn(templates).when(uriInfoMock).getMatchedTemplates();
    }

    public static Stream<Arguments> route_method_works_as_expected_DataProvider() {
        return Stream.of(
            Arguments.of("/", "/foo/bar/{id}", "/foo/bar/{id}"),
            Arguments.of("/", "/foo/bar/{id},/", "/foo/bar/{id}"),
            Arguments.of("/", "/foo/{restOfPath:.+}", "/foo/{restOfPath:.+}"),
            Arguments.of("/", "/foo/{restOfPath:.+},/", "/foo/{restOfPath:.+}"),
            Arguments.of("/", "/last/path/{id},/start/path", "/start/path/last/path/{id}"),
            Arguments.of("/", "/last/path/{id},/start/path,/", "/start/path/last/path/{id}"),
            Arguments.of("/", "/", ""),
            Arguments.of("/", "/,/", ""),
            Arguments.of("/", null, ""),
            Arguments.of("notSlashBasePath", "/foo/bar/{id},/", "notSlashBasePath/foo/bar/{id}"),
            Arguments.of("notSlashBasePath", "/foo/{restOfPath:.+},/", "notSlashBasePath/foo/{restOfPath:.+}"),
            Arguments.of("notSlashBasePath", "/last/path/{id},/start/path,/", "notSlashBasePath/start/path/last/path/{id}"),
            Arguments.of("notSlashBasePath", "/", "notSlashBasePath"),
            Arguments.of("notSlashBasePath", null, "")
        );
    }

    @ParameterizedTest
    @MethodSource("route_method_works_as_expected_DataProvider")
    public void route_method_works_as_expected(
        String basePath, String commaDelmitedTemplates, String expectedResult
    ) {
        // given
        String[] templates = (commaDelmitedTemplates == null)
                             ? null
                             : commaDelmitedTemplates.split(",");
        setupUriInfoWithBasePathAndUriTemplates(
            extendedUriInfoMock,
            URI.create(basePath),
            templates
        );

        // when
        String result = implSpy.route(requestMock);

        // then
        assertThat(result).isEqualTo(expectedResult);
    }
}