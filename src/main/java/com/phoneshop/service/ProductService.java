package com.phoneshop.service;

import java.util.List;
import java.util.Optional;

import com.phoneshop.dto.ProductCriteriaDTO;
import com.phoneshop.model.Cart;
import com.phoneshop.model.CartDetail;
import com.phoneshop.model.Product;
import com.phoneshop.model.User;
import com.phoneshop.repository.ProductRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


public interface ProductService {

    Product saveProduct(Product product);

    Page<Product> getAllProducts(Pageable page);

    Page<Product> getAllProductsWithSpec(Pageable page, ProductCriteriaDTO productCriteriaDTO);

    void deleteProduct(Long productId);

    void updateProduct(Product product, Long productId);

    Product getProduct(Long productId);

    void addProductToCart(String email, long productId, HttpSession session, long quantity);

    Cart getByUser(User user);

    void deleteCartDetail(long cartDetailId, HttpSession session);

    void handleUpdateCartBeforeCheckout(List<CartDetail> cartDetails);

    void handlePlaceOrder(User user, HttpSession session,
                          String receiverName, String receiverAddress, String receiverPhone,
                          String paymentMethod, String uuid);
    void updatePaymentStatus(String paymentRef, String paymentStatus);

}
