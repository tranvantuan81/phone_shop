package com.phoneshop.service.impl;


import com.phoneshop.dto.RegisterRequestDTO;
import com.phoneshop.model.Role;
import com.phoneshop.model.User;
import com.phoneshop.repository.OrderRepository;
import com.phoneshop.repository.ProductRepository;
import com.phoneshop.repository.RoleRepository;
import com.phoneshop.repository.UserRepository;
import com.phoneshop.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;


    @Override
    public User saveUser(User user) {
        return this.userRepository.save(user);
    }

    @Override
    public Page<User> getAllUsers(Pageable page) {
        return this.userRepository.findAll(page);
    }

    @Override
    public List<User> getAllUsersByEmail(String email) {
        return userRepository.findOneByEmail(email);
    }

    @Override
    public User getUser(long userId) {
        User user = getUserById(userId);
        return User.builder()
                .id(userId)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .address(user.getAddress())
                .role(user.getRole())
                .build();
    }

    @Override
    public void updateUser(User request, Long userId) {
        User user = getUserById(userId);
        user.setFullName(request.getFullName());
        user.setAddress(request.getAddress());
        user.setPhone(request.getPhone());
        userRepository.save(user);
    }

    @Override
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }

    @Override
    public Role getRoleByName(String name) {
        return roleRepository.findByName(name);
    }

    @Override
    public User registerUser(RegisterRequestDTO request) {

        User user = new User();
        user.setFullName(request.getFirstName() + " " + request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());

        return user;
    }

    @Override
    public boolean checkEmailExist(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public long countUsers() {
        return this.userRepository.count();
    }

    @Override
    public long countProducts() {
        return this.productRepository.count();
    }

    @Override
    public long countOrders() {
        return this.orderRepository.count();
    }

    private User getUserById(long userId) {
        return userRepository.findById(userId);
    }
}