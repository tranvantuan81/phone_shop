package com.phoneshop.service.impl;

import com.phoneshop.dto.ProductCriteriaDTO;
import com.phoneshop.exception.ResourceNotFoundException;
import com.phoneshop.model.*;
import com.phoneshop.repository.*;
import com.phoneshop.service.ProductService;
import com.phoneshop.service.UserService;
import com.phoneshop.service.specification.ProductSpecs;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    private final CartDetailRepository cartDetailRepository;
    private final UserService userService;
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;

    @Override
    public Product saveProduct(Product product) {
        return productRepository.save(product);
    }

    @Override
    public Page<Product> getAllProducts(Pageable page) {
        return this.productRepository.findAll(page);
    }

    @Override
    public Page<Product> getAllProductsWithSpec(Pageable page, ProductCriteriaDTO productCriteriaDTO) {
        if (productCriteriaDTO.getTarget() == null
                && productCriteriaDTO.getFactory() == null
                && productCriteriaDTO.getPrice() == null) {
            return this.productRepository.findAll(page);
        }

        Specification<Product> combinedSpec = Specification.where(null);

        if (productCriteriaDTO.getTarget() != null && productCriteriaDTO.getTarget().isPresent()) {
            Specification<Product> currentSpecs = ProductSpecs.matchListTarget(productCriteriaDTO.getTarget().get());
            combinedSpec = combinedSpec.and(currentSpecs);
        }
        if (productCriteriaDTO.getFactory() != null && productCriteriaDTO.getFactory().isPresent()) {
            Specification<Product> currentSpecs = ProductSpecs.matchListFactory(productCriteriaDTO.getFactory().get());
            combinedSpec = combinedSpec.and(currentSpecs);
        }

        if (productCriteriaDTO.getPrice() != null && productCriteriaDTO.getPrice().isPresent()) {
            Specification<Product> currentSpecs = this.buildPriceSpecification(productCriteriaDTO.getPrice().get());
            combinedSpec = combinedSpec.and(currentSpecs);
        }

        return this.productRepository.findAll(combinedSpec, page);
    }

    public Specification<Product> buildPriceSpecification(List<String> price) {
        Specification<Product> combinedSpec = Specification.where(null);
        for (String p : price) {
            double min = 0;
            double max = 0;

            switch (p) {
                case "duoi-10-trieu":
                    min = 1;
                    max = 10000000;
                    break;
                case "10-15-trieu":
                    min = 10000000;
                    max = 15000000;
                    break;
                case "15-20-trieu":
                    min = 15000000;
                    max = 20000000;
                    break;
                case "tren-20-trieu":
                    min = 20000000;
                    max = 200000000;
                    break;
            }

            if (min != 0 && max != 0) {
                Specification<Product> rangeSpec = ProductSpecs.matchMultiplePrice(min, max);
                combinedSpec = combinedSpec.or(rangeSpec);
            }
        }
        return combinedSpec;
    }

    @Override
    public void deleteProduct(Long productId) {
        productRepository.deleteById(productId);
    }

    @Override
    public void updateProduct(Product request, Long productId) {
        Product product = getProductById(productId);
        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product.setDetailDesc(request.getDetailDesc());
        product.setShortDesc(request.getShortDesc());
        product.setQuantity(request.getQuantity());
        product.setFactory(request.getFactory());
        product.setTarget(request.getTarget());
        product.setImage(request.getImage());
        productRepository.save(product);
    }

    @Override
    public Product getProduct(Long productId) {
        return getProductById(productId);
    }

    @Override
    public void addProductToCart(String email, long productId, HttpSession session, long quantity) {
        User user = this.userService.getUserByEmail(email);
        if (user != null) {
            Cart cart = getOrCreateCart(user);
            Optional<Product> productOptional = this.productRepository.findById(productId);
            if (productOptional.isPresent()) {
                Product product = productOptional.get();
                addOrUpdateCartDetail(cart, product, quantity);
                session.setAttribute("sum", cart.getSum());
            }
        }
    }

    @Override
    public Cart getByUser(User user) {
        return cartRepository.findByUser(user);
    }

    @Override
    public void deleteCartDetail(long cartDetailId, HttpSession session) {
        Optional<CartDetail> cartDetailOptional = cartDetailRepository.findById(cartDetailId);
        if (cartDetailOptional.isPresent()) {
            CartDetail cartDetail = cartDetailOptional.get();
            Cart cart = cartDetail.getCart();
            cartDetailRepository.deleteById(cartDetailId);
            updateCartAndSessionAfterDelete(cart, session);
        } else {
            throw new ResourceNotFoundException("CartDetail not found with id: " + cartDetailId);
        }
    }

    @Override
    public void handleUpdateCartBeforeCheckout(List<CartDetail> cartDetails) {
        for (CartDetail cartDetail : cartDetails) {
            Optional<CartDetail> cdOptional = this.cartDetailRepository.findById(cartDetail.getId());
            if (cdOptional.isPresent()) {
                CartDetail currentCartDetail = cdOptional.get();
                currentCartDetail.setQuantity(cartDetail.getQuantity());
                this.cartDetailRepository.save(currentCartDetail);
            }
        }
    }

    @Override
    @Transactional
    public void handlePlaceOrder(
            User user, HttpSession session,
            String receiverName, String receiverAddress, String receiverPhone,
            String paymentMethod, String uuid) {

        Cart cart = this.cartRepository.findByUser(user);
        if (cart != null) {
            List<CartDetail> cartDetails = cart.getCartDetails();
            if (cartDetails != null) {
                // Kiểm tra số lượng sản phẩm trước khi đặt hàng
                validateProductQuantities(cartDetails);

                double totalPrice = cartDetails.stream().mapToDouble(cd -> cd.getPrice() * cd.getQuantity()).sum();
                Order order = createOrder(user, receiverName, receiverAddress, receiverPhone, paymentMethod, uuid, totalPrice);
                createOrderDetails(order, cartDetails);

                // Trừ số lượng sản phẩm trong kho và tăng số lượng đã bán (sold)
                updateProductQuantities(cartDetails);

                deleteCartAfterOrder(cart);
                session.setAttribute("sum", 0);
            }
        }
    }

    @Override
    public void updatePaymentStatus(String paymentRef, String paymentStatus) {
        Optional<Order> orderOptional = this.orderRepository.findByPaymentRef(paymentRef);
        if (orderOptional.isPresent()) {
            Order order = orderOptional.get();
            order.setPaymentStatus(paymentStatus);
            this.orderRepository.save(order);
        }
    }

    private Product getProductById(long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    private Cart getOrCreateCart(User user) {
        Cart cart = this.cartRepository.findByUser(user);
        if (cart == null) {
            cart = new Cart();
            cart.setUser(user);
            cart.setSum(0);
            cart = this.cartRepository.save(cart);
        }
        return cart;
    }

    private void addOrUpdateCartDetail(Cart cart, Product product, long quantity) {
        CartDetail oldDetail = this.cartDetailRepository.findByCartAndProduct(cart, product);
        if (oldDetail == null) {
            CartDetail cd = new CartDetail();
            cd.setCart(cart);
            cd.setProduct(product);
            cd.setPrice(product.getPrice());
            cd.setQuantity(quantity);
            this.cartDetailRepository.save(cd);

            // Update cart sum
            cart.setSum(cart.getSum() + 1);
            this.cartRepository.save(cart);
        } else {
            oldDetail.setQuantity(oldDetail.getQuantity() + quantity);
            this.cartDetailRepository.save(oldDetail);
        }
    }

    private void updateCartAndSessionAfterDelete(Cart cart, HttpSession session) {
        if (cart.getSum() > 1) {
            int s = cart.getSum() - 1;
            cart.setSum(s);
            session.setAttribute("sum", s);
            cartRepository.save(cart);
        } else {
            cartRepository.deleteById(cart.getId());
            session.setAttribute("sum", 0);
        }
    }

    private Order createOrder(User user, String receiverName, String receiverAddress, String receiverPhone, String paymentMethod, String uuid, double totalPrice) {
        Order order = new Order();
        order.setUser(user);
        order.setReceiverName(receiverName);
        order.setReceiverAddress(receiverAddress);
        order.setReceiverPhone(receiverPhone);
        order.setStatus("PENDING");
        order.setPaymentMethod(paymentMethod);
        order.setPaymentStatus("PAYMENT_UNPAID");
        order.setPaymentRef(paymentMethod.equals("COD") ? "UNKNOWN" : uuid);
        order.setTotalPrice(totalPrice);
        return this.orderRepository.save(order);
    }

    private void createOrderDetails(Order order, List<CartDetail> cartDetails) {
        for (CartDetail cd : cartDetails) {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrder(order);
            orderDetail.setProduct(cd.getProduct());
            orderDetail.setPrice(cd.getPrice());
            orderDetail.setQuantity(cd.getQuantity());
            this.orderDetailRepository.save(orderDetail);
        }
    }

    private void deleteCartAfterOrder(Cart cart) {
        for (CartDetail cd : cart.getCartDetails()) {
            this.cartDetailRepository.deleteById(cd.getId());
        }
        this.cartRepository.deleteById(cart.getId());
    }

    private void validateProductQuantities(List<CartDetail> cartDetails) {
        for (CartDetail cartDetail : cartDetails) {
            Product product = cartDetail.getProduct();
            long orderedQuantity = cartDetail.getQuantity();
            long currentQuantity = product.getQuantity();

            if (currentQuantity < orderedQuantity) {
                throw new RuntimeException("Not enough stock for product: " + product.getName());
            }
        }
    }

    private void updateProductQuantities(List<CartDetail> cartDetails) {
        for (CartDetail cartDetail : cartDetails) {
            Product product = cartDetail.getProduct();
            long orderedQuantity = cartDetail.getQuantity();
            long currentQuantity = product.getQuantity();

            // Kiểm tra số lượng sản phẩm trong kho
            if (currentQuantity < orderedQuantity) {
                throw new RuntimeException("Not enough stock for product: " + product.getName());
            }

            // Trừ số lượng sản phẩm trong kho
            product.setQuantity(currentQuantity - orderedQuantity);

            // Tăng số lượng sản phẩm đã bán (sold)
            long currentSold = product.getSold(); // Lấy giá trị hiện tại của sold
            product.setSold(currentSold + orderedQuantity); // Tăng sold lên

            // Lưu cập nhật vào cơ sở dữ liệu
            productRepository.save(product);
        }
    }
}