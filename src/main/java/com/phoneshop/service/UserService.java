package com.phoneshop.service;

import com.phoneshop.dto.RegisterRequestDTO;
import com.phoneshop.model.Role;
import com.phoneshop.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;


public interface UserService {

    User saveUser(User request);

    Page<User> getAllUsers(Pageable page);

    List<User> getAllUsersByEmail(String email);

    User getUser(long userId);

    void updateUser(User request, Long userId);

    void deleteUser(Long userId);

    Role getRoleByName(String name);

    User registerUser(RegisterRequestDTO request);

    boolean checkEmailExist(String email);

    User getUserByEmail(String email);

    long countUsers();

    public long countProducts();

    public long countOrders();
}