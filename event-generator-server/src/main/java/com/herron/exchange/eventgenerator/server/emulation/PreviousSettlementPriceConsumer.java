package com.herron.exchange.eventgenerator.server.emulation;

import com.herron.exchange.common.api.common.api.Message;
import com.herron.exchange.common.api.common.api.MessageFactory;
import com.herron.exchange.common.api.common.api.broadcasts.DataStreamState;
import com.herron.exchange.common.api.common.api.marketdata.MarketDataPrice;
import com.herron.exchange.common.api.common.enums.KafkaTopicEnum;
import com.herron.exchange.common.api.common.kafka.DataConsumer;
import com.herron.exchange.common.api.common.messages.common.PartitionKey;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.PartitionOffset;
import org.springframework.kafka.annotation.TopicPartition;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;


public class PreviousSettlementPriceConsumer extends DataConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(PreviousSettlementPriceConsumer.class);
    private static final PartitionKey PARTITION_ZERO_KEY = new PartitionKey(KafkaTopicEnum.PREVIOUS_SETTLEMENT_PRICE_DATA, 0);
    private final CountDownLatch countDownLatch;
    private final Map<String, MarketDataPrice> instrumentIdToPreviousSettlementPrices = new ConcurrentHashMap<>();

    public PreviousSettlementPriceConsumer(CountDownLatch countDownLatch, MessageFactory messageFactory) {
        super(messageFactory);
        this.countDownLatch = countDownLatch;
    }

    @KafkaListener(id = "previous-settlement-price-data-consumer-0",
            topicPartitions = {@TopicPartition(topic = "previous-settlement-price-data", partitionOffsets = @PartitionOffset(partition = "0", initialOffset = "0"))}
    )
    public void listenPreviousSettlementPriceDataPartitionZero(ConsumerRecord<String, String> consumerRecord) {
        var broadCastMessage = deserializeBroadcast(consumerRecord, PARTITION_ZERO_KEY);
        if (broadCastMessage != null) {
            handleMessage(broadCastMessage.message());
        }
    }

    private void handleMessage(Message message) {
        if (message instanceof DataStreamState state) {
            switch (state.state()) {
                case START -> LOGGER.info("Started consuming previous day settlement price data.");
                case DONE -> {
                    var count = countDownLatch.getCount();
                    countDownLatch.countDown();
                    LOGGER.info("Done consuming {} previous day settlement price data, countdown latch from {} to {}.", getTotalNumberOfEvents(), count, countDownLatch.getCount());
                }
            }
        } else if (message instanceof MarketDataPrice marketDataPrice) {
            instrumentIdToPreviousSettlementPrices.put(marketDataPrice.staticKey().instrumentId(), marketDataPrice);
        }
    }

    public Map<String, MarketDataPrice> getInstrumentIdToPreviousSettlementPrices() {
        return instrumentIdToPreviousSettlementPrices;
    }
}
