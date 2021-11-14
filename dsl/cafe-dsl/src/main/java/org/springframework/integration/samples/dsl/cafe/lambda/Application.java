/*
 * Copyright 2014-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.samples.dsl.cafe.lambda;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.dsl.*;
import org.springframework.integration.handler.GenericHandler;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.integration.samples.cafe.Delivery;
import org.springframework.integration.samples.cafe.Drink;
import org.springframework.integration.samples.cafe.DrinkType;
import org.springframework.integration.samples.cafe.Order;
import org.springframework.integration.samples.cafe.OrderItem;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.stream.CharacterStreamWritingMessageHandler;
import org.springframework.messaging.*;

/**
 * @author Artem Bilan
 * @since 3.0
 */
@SpringBootApplication
public class Application {
    ExecutorService executorService = Executors.newFixedThreadPool(5);

    public static void main(String[] args) throws Exception {
        ConfigurableApplicationContext ctx = SpringApplication.run(Application.class, args);

        Cafe cafe = ctx.getBean(Cafe.class);
        for (int i = 1; i <= 15; i++) {
            Order order = new Order(i);
            order.addItem(DrinkType.LATTE, 2 + i, false);
            order.addItem(DrinkType.MOCHA, 3 + i, true);
            cafe.placeOrder(order);
            System.out.println(String.format("create order number: %s", i));
        }

        System.out.println("Hit 'Enter' to terminate");
        System.in.read();
        ctx.close();
    }

    @MessagingGateway
    public interface Cafe {

        @Gateway(requestChannel = "orders.input")
        void placeOrder(Order order);

    }

    private AtomicInteger hotDrinkCounter = new AtomicInteger();

    private AtomicInteger coldDrinkCounter = new AtomicInteger();

    @Bean(name = PollerMetadata.DEFAULT_POLLER)
    public PollerMetadata poller() {
        return Pollers.fixedDelay(1000).taskExecutor(Executors.newScheduledThreadPool(1)).get();
    }

    @Bean
    public IntegrationFlow cold() {
//        return IntegrationFlows.from("cold.input")
//                .channel(c -> c.queue(10))
//                .channel(c -> c.executor(Executors.newCachedThreadPool()))
//                .<OrderItem>handle((payload, headers) -> {
//                    sleepUninterruptibly(1, TimeUnit.SECONDS);
//                    System.out.println(Thread.currentThread().getName() +
//                            " prepared cold drink #" +
//                            this.coldDrinkCounter.incrementAndGet() +
//                            " for order #" + payload.getOrderNumber() + ": " + payload);
//                    return payload;
//                }).channel("aggregator.input").get();
        return flow -> flow
                .channel(c -> c.queue(10))
//                .channel(c -> c.executor(Executors.newSingleThreadExecutor()))
                .<OrderItem>handle((payload, headers) -> {
                    try {
                        Thread.sleep(1000);
//                        System.out.println(Thread.currentThread().getName());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
//                    sleepUninterruptibly(1, TimeUnit.SECONDS);
                    System.out.println(Thread.currentThread().getName() +
                            " prepared cold drink #" +
                            this.coldDrinkCounter.incrementAndGet() +
                            " for order #" + payload.getOrderNumber() + ": " + payload);
                    return payload;
                }).channel("aggregator.input");
    }

    @Bean
    public IntegrationFlow hot() {
        return IntegrationFlows.from("hot.input")
                .channel(c -> c.queue(10))
//                .channel(c -> c.executor(Executors.newCachedThreadPool()))
                .<OrderItem>handle((payload, headers) -> {
//                    sleepUninterruptibly(5, TimeUnit.SECONDS);
                    try {
                        Thread.sleep(5000);
//                        System.out.println(Thread.currentThread().getName());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println(Thread.currentThread().getName() +
                            " prepared hot drink #" +
                            Application.this.hotDrinkCounter.incrementAndGet() +
                            " for order #" + payload.getOrderNumber() + ": " + payload);
                    return payload;
                }, serviceActivatingHandlerGenericEndpointSpec -> {
                    serviceActivatingHandlerGenericEndpointSpec.poller(Pollers.fixedRate(1000).taskExecutor(Executors.newFixedThreadPool(5)));
                }).channel("aggregator.input").get();
//        return flow -> flow
//                .channel(c -> c.queue(10))
////                .channel(c -> c.executor(Executors.newCachedThreadPool()))
//                .<OrderItem>handle((payload, headers) -> {
//                    sleepUninterruptibly(5, TimeUnit.SECONDS);
//                    System.out.println(Thread.currentThread().getName() +
//                            " prepared hot drink #" +
//                            Application.this.hotDrinkCounter.incrementAndGet() +
//                            " for order #" + payload.getOrderNumber() + ": " + payload);
//                    return payload;
//                }, serviceActivatingHandlerGenericEndpointSpec -> {
//                    serviceActivatingHandlerGenericEndpointSpec.poller(
//                            Pollers.fixedRate(0)
//                                    .taskExecutor(Executors.newFixedThreadPool(5)));
//                })
//                .channel("aggregator.input");
    }

    @Bean
    public PollerMetadata getPoller() {
        PollerMetadata pollerMetadata = new PollerMetadata();
        pollerMetadata.setTaskExecutor(Executors.newCachedThreadPool());

        return pollerMetadata;
    }

//    @Bean
//    public IntegrationFlow myFlow() {
//        return IntegrationFlows.from("test")
//                .<Integer>handle(new GenericHandler<Integer>() {
//                    @Override
//                    public Object handle(Integer p, MessageHeaders h) {
//                        return p * 2;
//                    }
//                })
//                .channel("a").get();
//    }

    @Bean
    public IntegrationFlow aggregator() {
        return flow -> flow
                .<OrderItem, Drink>transform(orderItem ->
                        new Drink(orderItem.getOrderNumber(),
                                orderItem.getDrinkType(),
                                orderItem.isIced(),
                                orderItem.getShots()))
                .aggregate(aggregator -> aggregator
                        .outputProcessor(g ->
                                new Delivery(g.getMessages()
                                        .stream()
                                        .map(message -> (Drink) message.getPayload())
                                        .collect(Collectors.toList()))))
//						.correlationStrategy(m -> ((Drink) m.getPayload()).getOrderNumber()))
                .handle(CharacterStreamWritingMessageHandler.stdout());
    }

    @Bean
    public IntegrationFlow orders() {
        return f -> f
                .split(Order.class, Order::getItems)
//				.channel(c -> c.executor(Executors.newCachedThreadPool()))
//                .channel(MessageChannels.executor(Executors.newCachedThreadPool()))
                .<OrderItem, Boolean>route(OrderItem::isIced, mapping -> mapping
                        .subFlowMapping(false, subFlow -> subFlow.channel("hot.input"))
                        .subFlowMapping(true, subFlow -> subFlow.channel("cold.input")));
//                        .channelMapping(true, "cold.input")
//                        .channelMapping(false, "hot.input"));
//                        .subFlowMapping(true, sf -> sf
//                                .channel(c -> c.queue(10))
//                                .channel(c -> c.executor(Executors.newCachedThreadPool()))
//                                .wireTap(wt ->
//                                        wt.handle(m -> {
//                                            sleepUninterruptibly(1, TimeUnit.SECONDS);
//                                            OrderItem payload = (OrderItem) m.getPayload();
//                                            System.out.println(Thread.currentThread().getName() +
//                                                    " prepared cold drink #" +
//                                                    this.coldDrinkCounter.incrementAndGet() +
//                                                    " for order #" + payload.getOrderNumber() + ": " + payload);
//                                        }))
//								.publishSubscribeChannel(c -> c
//                                        .subscribe(s -> s.handle(m -> sleepUninterruptibly(1, TimeUnit.SECONDS)))
//										.subscribe(sub -> sub
//												.<OrderItem, String>transform(p ->
//														Thread.currentThread().getName() +
//																" prepared cold drink #" +
//																this.coldDrinkCounter.incrementAndGet() +
//																" for order #" + p.getOrderNumber() + ": " + p)
//												.handle(m -> System.out.println(m.getPayload()))))
//                                .bridge())
//                        .subFlowMapping(false, sf -> sf
//                                .channel(c -> c.queue(10))
//                                .channel(c -> c.executor(Executors.newCachedThreadPool()))
//                                .wireTap(wt ->
//                                        wt.handle(m -> {
//                                            sleepUninterruptibly(5, TimeUnit.SECONDS);
//                                            OrderItem payload = (OrderItem) m.getPayload();
//                                            System.out.println(Thread.currentThread().getName() +
//                                                    " prepared hot drink #" +
//                                                    this.hotDrinkCounter.incrementAndGet() +
//                                                    " for order #" + payload.getOrderNumber() + ": " + payload);
//                                        }))
//								.publishSubscribeChannel(c -> c
//										.subscribe(s -> s.handle(m -> sleepUninterruptibly(5, TimeUnit.SECONDS)))
//										.subscribe(sub -> sub
//												.<OrderItem, String>transform(p ->
//														Thread.currentThread().getName() +
//																" prepared hot drink #" +
//																this.hotDrinkCounter.incrementAndGet() +
//																" for order #" + p.getOrderNumber() + ": " + p)
//												.handle(m -> System.out.println(m.getPayload()))))
//                                .bridge()))
//                .<OrderItem, Drink>transform(orderItem ->
//                        new Drink(orderItem.getOrderNumber(),
//                                orderItem.getDrinkType(),
//                                orderItem.isIced(),
//                                orderItem.getShots()))
//                .aggregate(aggregator -> aggregator
//                        .outputProcessor(g ->
//                                new Delivery(g.getMessages()
//                                        .stream()
//                                        .map(message -> (Drink) message.getPayload())
//                                        .collect(Collectors.toList()))))
////						.correlationStrategy(m -> ((Drink) m.getPayload()).getOrderNumber()))
//                .handle(CharacterStreamWritingMessageHandler.stdout());
    }

    private static void sleepUninterruptibly(long sleepFor, TimeUnit unit) {
        boolean interrupted = false;
        try {
            unit.sleep(sleepFor);
        } catch (InterruptedException e) {
            interrupted = true;
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

}
