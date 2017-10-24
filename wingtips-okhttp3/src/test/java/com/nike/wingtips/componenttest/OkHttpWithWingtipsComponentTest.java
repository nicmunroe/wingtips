package com.nike.wingtips.componenttest;

import com.nike.wingtips.Span;
import com.nike.wingtips.Span.SpanPurpose;
import com.nike.wingtips.TraceHeaders;
import com.nike.wingtips.Tracer;
import com.nike.wingtips.lifecyclelistener.SpanLifecycleListener;
import com.nike.wingtips.okhttp.WingtipsOkHttpClientInterceptor;
import com.nike.wingtips.servlet.RequestTracingFilter;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.MDC;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.servlet.http.HttpServletRequest;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static com.nike.wingtips.componenttest.OkHttpWithWingtipsComponentTest.TestBackendServer.ENDPOINT_PATH;
import static com.nike.wingtips.componenttest.OkHttpWithWingtipsComponentTest.TestBackendServer.ENDPOINT_PAYLOAD;
import static com.nike.wingtips.componenttest.OkHttpWithWingtipsComponentTest.TestBackendServer.SLEEP_TIME_MILLIS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Component test validating Wingtips' integration with {@link OkHttpClient}. This launches a real running server
 * on a random port and sets up Wingtips-instrumented {@link OkHttpClient}s and fires requests through them at the
 * server to verify the integration.
 *
 * @author Nic Munroe
 */
@RunWith(DataProviderRunner.class)
public class OkHttpWithWingtipsComponentTest {
    private static final int SERVER_PORT = findFreePort();
    private static ConfigurableApplicationContext serverAppContext;

    private SpanRecorder spanRecorder;

    @BeforeClass
    public static void beforeClass() throws Exception {
        serverAppContext = SpringApplication.run(TestBackendServer.class, "--server.port=" + SERVER_PORT);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        SpringApplication.exit(serverAppContext);
    }

    private static int findFreePort() {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Before
    public void beforeMethod() {
        resetTracing();

        spanRecorder = new SpanRecorder();
        Tracer.getInstance().addSpanLifecycleListener(spanRecorder);
    }

    @After
    public void afterMethod() {
        resetTracing();
    }

    private void resetTracing() {
        MDC.clear();
        Tracer.getInstance().unregisterFromThread();
        removeSpanRecorderLifecycleListener();
    }

    private void removeSpanRecorderLifecycleListener() {
        List<SpanLifecycleListener> listeners = new ArrayList<>(Tracer.getInstance().getSpanLifecycleListeners());
        for (SpanLifecycleListener listener : listeners) {
            if (listener instanceof SpanRecorder) {
                Tracer.getInstance().removeSpanLifecycleListener(listener);
            }
        }
    }

    @DataProvider(value = {
        "true   |   true",
        "true   |   false",
        "false  |   true",
        "false  |   false"
    }, splitBy = "\\|")
    @Test
    public void verify_OkHttpClient_blocking_request_with_WingtipsOkHttpClientInterceptor_traced_correctly(
        boolean spanAlreadyExistsBeforeCall, boolean subspanOptionOn
    ) throws IOException {

        // given
        WingtipsOkHttpClientInterceptor interceptor = new WingtipsOkHttpClientInterceptor(subspanOptionOn);
        
        Span parent = null;
        if (spanAlreadyExistsBeforeCall) {
            parent = Tracer.getInstance().startRequestWithRootSpan("somePreexistingParentSpan");
        }

        OkHttpClient client = new OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build();
        
        Call request = client.newCall(
            new Request.Builder()
                .url("http://localhost:" + SERVER_PORT + ENDPOINT_PATH)
                .get()
                .build()
        );

        // We always expect at least one span to be completed as part of the call: the server span.
        //      We may or may not have a second span completed depending on the value of subspanOptionOn.
        int expectedNumSpansCompleted = (subspanOptionOn) ? 2 : 1;

        // when
        try (Response response = request.execute()) {
        // then
            assertThat(response.code()).isEqualTo(200);
            assertThat(response.body().string()).isEqualTo(ENDPOINT_PAYLOAD);
            verifySpansCompletedAndReturnedInResponse(
                response, SLEEP_TIME_MILLIS, expectedNumSpansCompleted, parent, subspanOptionOn
            );
        }

        if (parent != null) {
            parent.close();
        }
    }

//    @DataProvider(value = {
//        "true   |   true",
//        "true   |   false",
//        "false  |   true",
//        "false  |   false"
//    }, splitBy = "\\|")
//    @Test
//    public void verify_OkHttpClient_async_request_with_WingtipsOkHttpClientInterceptor_traced_correctly(
//        boolean spanAlreadyExistsBeforeCall, boolean subspanOptionOn
//    ) throws IOException, InterruptedException, ExecutionException, TimeoutException {
//
//        // given
//        WingtipsOkHttpClientInterceptor interceptor = new WingtipsOkHttpClientInterceptor(subspanOptionOn);
//
//        Span parent = null;
//        if (spanAlreadyExistsBeforeCall) {
//            parent = Tracer.getInstance().startRequestWithRootSpan("somePreexistingParentSpan");
//        }
//
//        OkHttpClient client = new OkHttpClient.Builder()
//            .addInterceptor(interceptor)
//            .eventListener()
////            .dispatcher(new Dispatcher() {
////
////            })
//            .build();
//
//        Call request = client.newCall(
//            new Request.Builder()
//                .url("http://localhost:" + SERVER_PORT + ENDPOINT_PATH)
//                .get()
//                .build()
//        );
//
//        // We always expect at least one span to be completed as part of the call: the server span.
//        //      We may or may not have a second span completed depending on the value of subspanOptionOn.
//        int expectedNumSpansCompleted = (subspanOptionOn) ? 2 : 1;
//
//        // when
//        CompletableFuture<Response> responseFuture = new CompletableFuture<>();
//        request.enqueue(new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//                responseFuture.completeExceptionally(e);
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//                responseFuture.complete(response);
//            }
//        });
//        Response response = responseFuture.get(1, TimeUnit.SECONDS);
//
//        // then
//        try (ResponseBody responseBody = response.body()) {
//            assertThat(response.code()).isEqualTo(200);
//            assertThat(responseBody).isEqualTo(ENDPOINT_PAYLOAD);
//            verifySpansCompletedAndReturnedInResponse(
//                response, SLEEP_TIME_MILLIS, expectedNumSpansCompleted, parent, subspanOptionOn
//            );
//        }
//
//        response.close();
//        if (parent != null) {
//            parent.close();
//        }
//    }

    private void verifySpansCompletedAndReturnedInResponse(Response response,
                                                           long expectedMinSpanDurationMillis,
                                                           int expectedNumSpansCompleted,
                                                           Span expectedUpstreamSpan,
                                                           boolean expectSubspanFromHttpClient) {
        // We can have a race condition where the response is sent and we try to verify here before the servlet filter
        //      has had a chance to complete the span. Wait a few milliseconds to give the servlet filter time to
        //      finish.
        try {
            Thread.sleep(10);
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        assertThat(spanRecorder.completedSpans).hasSize(expectedNumSpansCompleted);
        String traceIdFromResponse = response.header(TraceHeaders.TRACE_ID);
        assertThat(traceIdFromResponse).isNotNull();

        spanRecorder.completedSpans.forEach(
            completedSpan -> assertThat(completedSpan.getTraceId()).isEqualTo(traceIdFromResponse)
        );

        // Find the span with the longest duration - this is the outermost span (either from the server or from
        //      the OkHttpClient depending on whether the subspan option was on).
        Span outermostSpan = spanRecorder.completedSpans.stream()
                                                           .max(Comparator.comparing(Span::getDurationNanos))
                                                           .get();
        assertThat(TimeUnit.NANOSECONDS.toMillis(outermostSpan.getDurationNanos()))
            .isGreaterThanOrEqualTo(expectedMinSpanDurationMillis);

        SpanPurpose expectedOutermostSpanPurpose = (expectSubspanFromHttpClient)
                                                   ? SpanPurpose.CLIENT
                                                   : SpanPurpose.SERVER;
        assertThat(outermostSpan.getSpanPurpose()).isEqualTo(expectedOutermostSpanPurpose);

        if (expectedUpstreamSpan == null) {
            assertThat(outermostSpan.getParentSpanId()).isNull();
        }
        else {
            assertThat(outermostSpan.getTraceId()).isEqualTo(expectedUpstreamSpan.getTraceId());
            assertThat(outermostSpan.getParentSpanId()).isEqualTo(expectedUpstreamSpan.getSpanId());
        }
    }

    @SuppressWarnings("WeakerAccess")
    private static class SpanRecorder implements SpanLifecycleListener {

        public final List<Span> completedSpans = new ArrayList<>();

        @Override
        public void spanStarted(Span span) { }

        @Override
        public void spanSampled(Span span) { }

        @Override
        public void spanCompleted(Span span) {
            completedSpans.add(span);
        }
    }

    @SpringBootApplication
    public static class TestBackendServer {

        public static final String ENDPOINT_PATH = "/foo";
        public static final String ENDPOINT_PAYLOAD = "endpoint-payload-" + UUID.randomUUID().toString();
        public static final long SLEEP_TIME_MILLIS = 100;

        @Bean
        public RequestTracingFilter requestTracingFilter() {
            return new RequestTracingFilter();
        }

        @RestController
        @RequestMapping("/")
        public static class Controller {

            @GetMapping(path = ENDPOINT_PATH)
            @SuppressWarnings("unused")
            public String basicEndpoint(HttpServletRequest request) throws InterruptedException {
                String queryString = request.getQueryString();
                Thread.sleep(SLEEP_TIME_MILLIS);
                return ENDPOINT_PAYLOAD;
            }

        }

    }
}
