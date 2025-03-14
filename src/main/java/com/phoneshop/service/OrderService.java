package com.phoneshop.service;

import com.phoneshop.model.Order;
import com.phoneshop.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface OrderService {

    List<Order> getAllOrders();

    Optional<Order> getOrderById(long id);

    void deleteOrderById(long id);

    void updateOrder(Order order);

    List<Order> fetchOrderByUser(User user);
}
