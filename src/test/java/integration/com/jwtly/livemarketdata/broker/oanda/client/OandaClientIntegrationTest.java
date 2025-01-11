package integration.com.jwtly.livemarketdata.broker.oanda.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jwtly.livemarketdata.broker.common.Stream;
import com.jwtly.livemarketdata.broker.oanda.client.OandaClient;
import com.jwtly.livemarketdata.core.Price;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariables;

import java.net.http.HttpClient;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@EnabledIfEnvironmentVariables({
        @EnabledIfEnvironmentVariable(named = "OANDA_API_KEY", matches = ".*"),
        @EnabledIfEnvironmentVariable(named = "OANDA_ACCOUNT_ID", matches = ".*")
})
@Slf4j
public class OandaClientIntegrationTest {
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private OandaClient client;
    private static final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        String apiKey = System.getenv("OANDA_API_KEY");
        String accountId = System.getenv("OANDA_ACCOUNT_ID");

        Assertions.assertNotNull(apiKey, "OANDA_API_KEY environment variable must be set");
        Assertions.assertNotNull(accountId, "OANDA_ACCOUNT_ID environment variable must be set");

        client = new OandaClient(
                httpClient,
                apiKey,
                "https://stream-fxpractice.oanda.com",
                accountId,
                objectMapper);
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testCanStreamPriceForInstrument() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger count = new AtomicInteger(0);

        var stream = client.createPriceStream(List.of("BTC_USD"));

        stream.start(new Stream.StreamCallback<>() {
            @Override
            public void onData(Price data) {
                log.info("Received price: {}", data);
                count.incrementAndGet();
                if (count.get() > 5) latch.countDown();
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
                latch.countDown();
            }

            @Override
            public void onComplete() {
                latch.countDown();
            }
        });

        var done = latch.await(10, TimeUnit.SECONDS);

        log.info("Received {} prices", count.get());

        if (!done) {
            Assertions.fail("Timed out waiting for price stream to complete");
        }

        Assertions.assertTrue(count.get() > 0, "Expected to receive at least one price");
    }
}
