package com.nimbleways.springboilerplate.services.implementations;

import com.nimbleways.springboilerplate.dto.product.ProcessOrderResponse;
import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.exceptions.OrderNotFoundException;
import com.nimbleways.springboilerplate.repositories.OrderRepository;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Set;

@Service
public class ProductService {

    @Autowired
    ProductRepository productRepository;

    @Autowired
    NotificationService notificationService;
    @Autowired
    OrderRepository orderRepository;

    @Autowired
    private Clock clock;


    private void decreaseProductAvailability(Product product) {
        product.setAvailable(product.getAvailable() - 1);
        productRepository.save(product);
    }

    public void handleNormalProduct(Product product) {
        if (product.getAvailable() > 0) {
            decreaseProductAvailability(product);
        } else {
            int leadTime = product.getLeadTime();
            if (leadTime > 0) {
                notificationService.sendDelayNotification(leadTime, product.getName());
            }
        }
    }

    private void handleSeasonalProduct(Product product) {
        LocalDate now = LocalDate.now(clock);
        if ((!now.isBefore(product.getStartDate()) && !now.isAfter(product.getEndDate()) && product.getAvailable() > 0)) {
            decreaseProductAvailability(product);
        } else {
            if (!LocalDate.now(clock).plusDays(product.getLeadTime()).isBefore(product.getEndDate())) {
                notificationService.sendOutOfStockNotification(product.getName());
                product.setAvailable(0);
                productRepository.save(product);
            } else if (!product.getStartDate().isBefore(LocalDate.now(clock))) {
                notificationService.sendOutOfStockNotification(product.getName());
                productRepository.save(product);
            } else {
                notificationService.sendDelayNotification(product.getLeadTime(), product.getName());
            }
        }
    }

    private void handleExpirableProduct(Product product) {
        LocalDate now = LocalDate.now(clock);
        if (product.getAvailable() > 0 && !product.getEndDate().isBefore(now)) {
            decreaseProductAvailability(product);
        } else {
            notificationService.sendExpirationNotification(product.getName(), product.getEndDate());
            product.setAvailable(0);
            productRepository.save(product);
        }
    }

    private void handleFlashSaleProduct(Product product) {
        LocalDate now = LocalDate.now(clock);
        if ((!now.isBefore(product.getStartDate()) && !now.isAfter(product.getEndDate()) && product.getAvailable() > 0)) {
            decreaseProductAvailability(product);
        } else {
            notificationService.sendOutOfStockNotification(product.getName());
        }
    }

    public ProcessOrderResponse processOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);

        if (order == null){
            throw new OrderNotFoundException("Order not found with id: " + orderId);
        }
        Set<Product> products = order.getItems();
        for (Product product : products) {
            switch (product.getType()) {
                case NORMAL -> handleNormalProduct(product);
                case SEASONAL -> handleSeasonalProduct(product);
                case EXPIRABLE -> handleExpirableProduct(product);
                case FLASHSALE -> handleFlashSaleProduct(product);
            }
        }
        return new ProcessOrderResponse(order.getId());
    }
}