package com.nimbleways.springboilerplate.controllers;

import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.OrderRepository;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.services.implementations.NotificationService;
import com.nimbleways.springboilerplate.utils.Annotations.SetupDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.nimbleways.springboilerplate.entities.ProductType.*;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Specify the controller class you want to test
// This indicates to spring boot to only load UsersController into the context
// Which allows a better performance and needs to do less mocks
@SetupDatabase
@SpringBootTest
@AutoConfigureMockMvc
public class MyControllerIntegrationTests {
        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private NotificationService notificationService;

        @Autowired
        private OrderRepository orderRepository;

        @Autowired
        private ProductRepository productRepository;

        @Test
        public void processOrderShouldReturn() throws Exception {
                List<Product> allProducts = createProducts();
                Set<Product> orderItems = new HashSet<Product>(allProducts);
                Order order = createOrder(orderItems);
                productRepository.saveAll(allProducts);
                order = orderRepository.save(order);
                mockMvc.perform(post("/orders/{orderId}/processOrder", order.getId())
                                .contentType("application/json"))
                        .andExpect(status().isOk());
                Order resultOrder = orderRepository.findById(order.getId()).get();
                assertEquals(resultOrder.getId(), order.getId());
        }

        private static Order createOrder(Set<Product> products) {
                Order order = new Order();
                order.setItems(products);
                return order;
        }

        private static List<Product> createProducts() {

                // I was focused more on refactoring, than the integration tests
                List<Product> products = new ArrayList<>();
                products.add(new Product(null, 15, 30, NORMAL, "sugar", null, null));
                products.add(new Product(null, 10, 0, NORMAL, "sugar2", null, null));
                products.add(new Product(null, 15, 30, EXPIRABLE, "sugar3", null, LocalDate.now().plusDays(26)));
                products.add(new Product(null, 90, 6, EXPIRABLE, "sugar4", null, LocalDate.now().minusDays(2)));
                products.add(new Product(null, 15, 30, SEASONAL, "sugar5", LocalDate.now().minusDays(2),
                        LocalDate.now().plusDays(58)));
                products.add(new Product(null, 15, 30, SEASONAL, "sugar6", LocalDate.now().plusDays(180),
                        LocalDate.now().plusDays(240)));
                products.add(new Product(null, 15, 30, FLASHSALE, "sugar7", LocalDate.now().plusDays(180),
                        LocalDate.now().plusDays(240)));
                products.add(new Product(null, 15, 30, FLASHSALE, "sugar8", LocalDate.now().plusDays(180),
                        LocalDate.now().plusDays(240)));
                return products;
        }
}