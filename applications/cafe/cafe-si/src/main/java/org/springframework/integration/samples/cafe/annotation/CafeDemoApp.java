/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.integration.samples.cafe.annotation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.samples.cafe.Cafe;
import org.springframework.integration.samples.cafe.DrinkType;
import org.springframework.integration.samples.cafe.Order;

/**
 * Provides the 'main' method for running the Cafe Demo application. When an
 * order is placed, the Cafe will send that order to the "orders" channel.
 * The channels are defined within the configuration file ("cafeDemo.xml"),
 * and the relevant components are configured with annotations (such as the
 * OrderSplitter, DrinkRouter, and Barista classes).
 *
 * @author Mark Fisher
 * @author Marius Bogoevici
 */
public class CafeDemoApp {

    private static Log logger = LogFactory.getLog(org.springframework.integration.samples.cafe.xml.CafeDemoApp.class);

    public static void main(String[] args) throws Exception{
        AbstractApplicationContext context =
                new ClassPathXmlApplicationContext("/META-INF/spring/integration/cafeDemo-annotation.xml", CafeDemoApp.class);

        Cafe cafe = (Cafe) context.getBean("cafe");
        for (int i = 1; i <= 15; i++) {
            Order order = new Order(i);
            order.addItem(DrinkType.LATTE, 2 + i, false);
            order.addItem(DrinkType.MOCHA, 3 + i, true);
            cafe.placeOrder(order);
            logger.info(String.format("create order number: %s", i));
        }
        System.out.println("Hit 'Enter' to terminate");
        System.in.read();
        context.close();
    }
}
