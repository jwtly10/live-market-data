package com.jwtly.livemarketdata.adapter.out.messaging.kafka;

import com.jwtly.livemarketdata.domain.model.Price;
import com.jwtly.livemarketdata.proto.PriceMessage;

public class PriceProtoHandler {
    public static PriceMessage toProto(Price price) {
        return PriceMessage.newBuilder()
                .setInstrument(price.instrument())
                .setBid(String.valueOf(price.bid()))
                .setAsk(String.valueOf(price.ask()))
                .setVolume(String.valueOf(price.volume()))
                .setTime(com.google.protobuf.Timestamp.newBuilder()
                        .setSeconds(price.time().getEpochSecond())
                        .setNanos(price.time().getNano())
                        .build())
                .build();
    }


    public static Price fromProto(PriceMessage priceMessage) {
        return new Price(
                priceMessage.getInstrument(),
                Double.parseDouble(priceMessage.getBid()),
                Double.parseDouble(priceMessage.getAsk()),
                Double.parseDouble(priceMessage.getVolume()),
                java.time.Instant.ofEpochSecond(
                        priceMessage.getTime().getSeconds(),
                        priceMessage.getTime().getNanos()
                )
        );
    }
}
