# Live Market Data Service

Core live market data streaming service for my trading infrastructure. Handles connectivity to multiple brokers and
publishes normalized price data to Kafka for downstream consumption.

## Core Features

- Multi-broker support via pluggable adapters (currently supporting Oanda)
- Reactive streams using Project Reactor
- Protobuf serialization
- Robust error handling & retry functionality

## Architecture

![System Diagram](/docs/system-arch.png)

New brokers can be added by implementing the simple `MarketDataPort` interface:

```java
public interface MarketDataPort {
    Flux<Price> createPriceStream(String streamId, List<String> instruments);
}
```

## Tech

- Spring Boot 3.x
- Project Reactor
- Kafka
- Protocol Buffers