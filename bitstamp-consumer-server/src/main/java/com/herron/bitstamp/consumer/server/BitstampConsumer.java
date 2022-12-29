package com.herron.bitstamp.consumer.server;

import com.herron.bitstamp.consumer.server.api.EventHandler;
import com.herron.bitstamp.consumer.server.client.BitstampSubscription;
import com.herron.bitstamp.consumer.server.config.BitstampConsumerConfig;
import com.herron.bitstamp.consumer.server.model.BitstampOrderbookData;
import com.herron.bitstamp.consumer.server.model.BitstampStateChange;
import com.herron.bitstamp.consumer.server.model.BitstampStockInstrument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.DeploymentException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.herron.bitstamp.consumer.server.utils.BitstampUtils.createInstrumentId;
import static com.herron.bitstamp.consumer.server.utils.BitstampUtils.createOrderbookId;

public class BitstampConsumer {

    private final EventHandler eventHandler;
    private static final Logger LOGGER = LoggerFactory.getLogger(BitstampConsumer.class);
    private final BitstampConsumerConfig.SubscriptionDetailConfig subscriptionDetailConfig;
    private final Map<BitstampConsumerConfig.SubscriptionDetailConfig.SubscriptionDetail, BitstampSubscription> keyToSubscription = new ConcurrentHashMap<>();

    public BitstampConsumer(BitstampConsumerConfig.SubscriptionDetailConfig subscriptionDetailConfig, EventHandler eventHandler) {
        this.subscriptionDetailConfig = subscriptionDetailConfig;
        this.eventHandler = eventHandler;
    }

    public void init() {
        for (var details : subscriptionDetailConfig.getSubscriptionDetails()) {
            if (keyToSubscription.containsKey(details)) {
                throw new IllegalArgumentException(String.format("Duplicate subscription detail: %s", details));
            }
            try {
                initOrderbook(details);
                var subscription = new BitstampSubscription(
                        eventHandler,
                        details.fxCurrency(),
                        details.cryptoCurrency(),
                        details.channel(),
                        subscriptionDetailConfig.getUri()
                );
                subscription.subscribe();
                keyToSubscription.computeIfAbsent(details, k -> subscription);

            } catch (DeploymentException | IOException | URISyntaxException e) {
                LOGGER.error("Unable to subscribe to channel {}: {}", details, e);
            }
        }
    }

    private void initOrderbook(BitstampConsumerConfig.SubscriptionDetailConfig.SubscriptionDetail detail) {
        LOGGER.info("Init Orderbook for detail: {}", detail);
        var instrument = new BitstampStockInstrument(createInstrumentId(detail.channel()), detail.fxCurrency(), Instant.now().toEpochMilli());
        var orderbook = new BitstampOrderbookData(createOrderbookId(detail.channel()), instrument.instrumentId(), Instant.now().toEpochMilli());
        var stateChange = new BitstampStateChange(orderbook.orderbookId(), "continuous trading", Instant.now().toEpochMilli());
        eventHandler.handleEvents(List.of(instrument, orderbook, stateChange));
    }

}