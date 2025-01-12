package com.jwtly.livemarketdata.adapter.out.broker.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jwtly.livemarketdata.domain.exception.stream.StreamStartupException;
import com.jwtly.livemarketdata.domain.port.out.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Timer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RetryableStreamTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpRequest httpRequest;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Timer timer;

    private TestRetryableStream stream;

    private CountDownLatch processLineLatch;
    private String processedLine;

    @BeforeEach
    void setUp() {
        processLineLatch = new CountDownLatch(1);
        stream = new TestRetryableStream(httpClient, httpRequest, objectMapper, timer);
    }

    @Test
    void testWhenStreamStartsSuccessfully_shouldProcessLines() throws Exception {
        String testLine = "test data";
        ByteArrayInputStream inputStream = new ByteArrayInputStream((testLine + "\n").getBytes());
        HttpResponse<java.io.InputStream> response = mock(HttpResponse.class);

        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(inputStream);
        when(httpClient.sendAsync(any(), any())).thenAnswer(inv -> CompletableFuture.completedFuture(response));

        Stream.StreamCallback<String> callback = mock(Stream.StreamCallback.class);

        stream.start(callback);

        verify(callback, times(1)).onHeartbeat();
        assertThat(processLineLatch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(processedLine).isEqualTo(testLine);
        verify(callback, never()).onError(any());
    }

    @Test
    void testWhenStreamFailsToStart_shouldNotRetryAndThrow() {
        HttpResponse<java.io.InputStream> response = mock(HttpResponse.class);
        String errorMsg = "something went wrong";
        ByteArrayInputStream inputStream = new ByteArrayInputStream((errorMsg + "\n").getBytes());

        when(response.statusCode()).thenReturn(500);
        when(response.body()).thenReturn(inputStream);
        when(httpClient.sendAsync(any(), any())).thenAnswer(inv -> CompletableFuture.completedFuture(response));

        Stream.StreamCallback<String> callback = mock(Stream.StreamCallback.class);
        ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);

        stream.start(callback);

        verify(callback).onError(exceptionCaptor.capture());

        verify(callback, never()).onHeartbeat();
        assertThat(exceptionCaptor.getValue())
                .isInstanceOf(StreamStartupException.class)
                .hasMessageContaining("500");
    }

    @Test
    void testWhenStreamIsClosedManually_shouldNotRetry_shouldNotThrowOrLogException() throws Exception {
        ByteArrayInputStream inputStream = new ByteArrayInputStream("test\n".getBytes());
        HttpResponse<java.io.InputStream> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(inputStream);
        when(httpClient.sendAsync(any(), any())).thenAnswer(inv -> CompletableFuture.completedFuture(response));

        Stream.StreamCallback<String> callback = mock(Stream.StreamCallback.class);
        stream.start(callback);
        stream.close();

        verify(timer, never()).schedule(any(), anyLong());
    }

    private class TestRetryableStream extends RetryableStream<String> {
        public TestRetryableStream(HttpClient client, HttpRequest request, ObjectMapper objectMapper, Timer timer) {
            super(client, request, objectMapper, timer);
        }

        @Override
        protected void processLine(String line, StreamCallback<String> callback) throws Exception {
            processedLine = line;
            processLineLatch.countDown();
        }
    }
}