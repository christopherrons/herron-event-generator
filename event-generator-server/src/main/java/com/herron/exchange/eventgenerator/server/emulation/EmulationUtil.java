package com.herron.exchange.eventgenerator.server.emulation;

import com.herron.exchange.common.api.common.api.referencedata.orderbook.OrderbookData;
import com.herron.exchange.common.api.common.api.trading.Order;
import com.herron.exchange.common.api.common.enums.OrderSideEnum;
import com.herron.exchange.common.api.common.enums.TimeInForceEnum;
import com.herron.exchange.common.api.common.messages.common.Price;
import com.herron.exchange.common.api.common.messages.common.Timestamp;
import com.herron.exchange.common.api.common.messages.common.Volume;
import com.herron.exchange.common.api.common.messages.trading.ImmutableLimitOrder;
import com.herron.exchange.common.api.common.messages.trading.ImmutableMarketOrder;
import com.herron.exchange.common.api.common.messages.trading.LimitOrder;
import com.herron.exchange.common.api.common.messages.trading.MarketOrder;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import static com.herron.exchange.common.api.common.enums.EventType.USER;
import static com.herron.exchange.common.api.common.enums.OrderOperationCauseEnum.NEW_ORDER;
import static com.herron.exchange.common.api.common.enums.OrderOperationEnum.INSERT;
import static com.herron.exchange.common.api.common.enums.TimeInForceEnum.*;
import static com.herron.exchange.eventgenerator.server.utils.EventGeneratorUtils.generateParticipant;

public class EmulationUtil {
    private static final Random RANDOM_GENERATOR = new Random(17);
    private static final AtomicLong ORDER_ID_GENERATOR = new AtomicLong(0);

    public static LimitOrder mapLimitOrder(OrderbookData orderbookData, Price price, OrderSideEnum sideEnum) {
        return mapLimitOrder(orderbookData, price, sideEnum, SESSION);
    }

    public static LimitOrder mapLimitOrder(OrderbookData orderbookData, Price price, OrderSideEnum sideEnum, TimeInForceEnum timeInForceEnum) {
        double volume = Math.max(orderbookData.minTradeVolume(), RANDOM_GENERATOR.nextInt(100));
        return ImmutableLimitOrder.builder()
                .timeOfEvent(Timestamp.now())
                .orderId(String.valueOf(ORDER_ID_GENERATOR.getAndIncrement()))
                .currentVolume(Volume.create(volume).scale(5))
                .initialVolume(Volume.create(volume).scale(5))
                .instrumentId(orderbookData.instrument().instrumentId())
                .orderSide(sideEnum)
                .price(price)
                .participant(generateParticipant())
                .timeInForce(timeInForceEnum)
                .orderOperation(INSERT)
                .orderOperationCause(NEW_ORDER)
                .orderbookId(orderbookData.orderbookId())
                .eventType(USER)
                .build();
    }

    public static MarketOrder mapMarketOrder(OrderbookData orderbookData, OrderSideEnum sideEnum) {
        double volume = Math.max(orderbookData.minTradeVolume(), RANDOM_GENERATOR.nextInt(100));
        return ImmutableMarketOrder.builder()
                .timeOfEvent(Timestamp.now())
                .orderId(String.valueOf(ORDER_ID_GENERATOR.getAndIncrement()))
                .currentVolume(Volume.create(volume).scale(5))
                .initialVolume(Volume.create(volume).scale(5))
                .instrumentId(orderbookData.instrument().instrumentId())
                .orderSide(sideEnum)
                .participant(generateParticipant())
                .orderOperation(INSERT)
                .orderOperationCause(NEW_ORDER)
                .orderbookId(orderbookData.orderbookId())
                .eventType(USER)
                .build();
    }

    public static Order mapAddOrder(OrderbookData orderbookData, Price price, OrderSideEnum sideEnum) {

        TimeInForceEnum timeInForceEnum = SESSION;
        if (RANDOM_GENERATOR.nextDouble() <= 0.05) {
            timeInForceEnum = FAK;
        } else if (RANDOM_GENERATOR.nextDouble() >= 0.95) {
            timeInForceEnum = FOK;
        }

        return RANDOM_GENERATOR.nextDouble() < 0.01 ?
                mapMarketOrder(orderbookData, sideEnum) : mapLimitOrder(orderbookData, price, sideEnum, timeInForceEnum);
    }
}
