package com.phoneshop.controller.admin;

import com.phoneshop.model.User;
import com.phoneshop.service.UploadService;
import com.phoneshop.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/user")
public class UserController {

    private final UserService userService;
    private final UploadService uploadService;
    private final PasswordEncoder passwordEncoder;


    // Home Page
    @GetMapping("/home")
    public String getHomePage(Model model) {
        List<User> users = userService.getAllUsersByEmail("admin@gmail.com");
        System.out.println(users);
        model.addAttribute("tuan", "test");
        return "hello";
    }

    // Get all users
    @GetMapping("/")
    public String getAllUsers(Model model, @RequestParam("page") Optional<String> pageOptional) {

        int page = 1;
        try {
            page = pageOptional.map(Integer::parseInt).orElse(1);
        } catch (NumberFormatException e) {
            page = 1; // Nếu lỗi parse, giữ mặc định là 1
        }

        Pageable pageable = PageRequest.of(page - 1, 6);
        Page<User> usersPage = userService.getAllUsers(pageable);
        List<User> users = usersPage.getContent();

        model.addAttribute("users", users);  // Đã đổi từ "user" thành "users"
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", usersPage.getTotalPages());

        return "admin/user/show";
    }

    // Create user
    @GetMapping("/create")
    public String getUserPage(Model model) {
        model.addAttribute("newUser", new User());
        return "admin/user/create";
    }

    @PostMapping("/create")
    public String createUser(@ModelAttribute("newUser") User user, @RequestParam("imgFile") MultipartFile file) {

        // Kiểm tra nếu file được tải lên
        String avatar = null;
        if (!file.isEmpty()) {
            avatar = uploadService.handleSaveUploadFile(file, "avatar");

            // Nếu lưu ảnh thất bại, có thể log hoặc đặt giá trị mặc định
            if (avatar == null) {
                avatar = "default-avatar.png"; // Đặt ảnh mặc định nếu cần
            }
        }

        // Cập nhật avatar cho user
        user.setAvatar(avatar);
        String hashPassword = passwordEncoder.encode(user.getPassword());

        user.setAvatar(avatar);
        user.setPassword(hashPassword);
        user.setRole(userService.getRoleByName(user.getRole().getName()));
        // Lưu user vào database
        userService.saveUser(user);
        return "redirect:/admin/user/";
    }

    // Get user details
    @GetMapping("/{userId}")
    public String getUserDetailPage(Model model, @PathVariable long userId) {
        User user = userService.getUser(userId);
        model.addAttribute("user", user);
        return "admin/user/detail";
    }

    // Update user
    @GetMapping("/update/{userId}")
    public String getUpdateUserPage(Model model, @PathVariable long userId) {
        User user = userService.getUser(userId);
        model.addAttribute("newUser", user);
        return "admin/user/update";
    }

    @PostMapping("/update")
    public String postUpdateUser(@ModelAttribute("newUser") User user) {
        userService.updateUser(user, user.getId());
        return "redirect:/admin/user/";
    }

    // Delete user
    @GetMapping("/delete/{userId}")
    public String getDeleteUserPage(Model model, @PathVariable long userId) {
        model.addAttribute("userId", userId);
        model.addAttribute("user", userService.getUser(userId));
        return "admin/user/delete";
    }

    @PostMapping("/delete")
    public String postDeleteUser(@RequestParam("userId") long userId) {
        userService.deleteUser(userId);
        return "redirect:/admin/user/";
    }
}