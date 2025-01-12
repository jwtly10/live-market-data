package com.jwtly.livemarketdata.adapter.out.broker.oanda.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariables;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@EnabledIfEnvironmentVariables({
        @EnabledIfEnvironmentVariable(named = "OANDA_API_KEY", matches = ".*"),
        @EnabledIfEnvironmentVariable(named = "OANDA_ACCOUNT_ID", matches = ".*")
})
@Slf4j
public class OandaPriceStreamIntegrationTest {
    private OandaPriceStream priceStream;
    private String apiKey;
    private String accountId;

    @BeforeEach
    void setUp() {
        apiKey = System.getenv("OANDA_API_KEY");
        accountId = System.getenv("OANDA_ACCOUNT_ID");

        Assertions.assertNotNull(apiKey, "OANDA_API_KEY environment variable must be set");
        Assertions.assertNotNull(accountId, "OANDA_ACCOUNT_ID environment variable must be set");

        WebClient webClient = WebClient.builder()
                .baseUrl("https://stream-fxpractice.oanda.com")
                .build();

        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        priceStream = new OandaPriceStream(webClient, objectMapper);
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testCanStreamPriceForInstrument() {
        List<String> instruments = List.of("BTC_USD");

        AtomicInteger priceCount = new AtomicInteger();

        var flux = priceStream.streamPrices(apiKey, accountId, instruments);

        StepVerifier.create(flux)
                .expectSubscription()
                .thenConsumeWhile(price -> {
                    log.info("Received price: {}", price);
                    priceCount.incrementAndGet();
                    return priceCount.get() <= 5;
                })
                .expectComplete()
                .verify(Duration.ofSeconds(15));

        Assertions.assertTrue(priceCount.get() > 0, "Expected to receive at least one price");
    }
}
