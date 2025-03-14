package com.phoneshop.service;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.ServletContext;

@Service
@RequiredArgsConstructor
public class UploadService {


    private final ServletContext servletContext;

    public String  handleSaveUploadFile(MultipartFile file, String targetFolder) {
        // relative path: absolute path
        String baseDir = "src/main/resources/static/images"; // Đường dẫn lưu file
        String finalName = "";

        try {
            // Tạo thư mục nếu chưa tồn tại
            Path dirPath = Paths.get(baseDir, targetFolder);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            // Tạo tên file duy nhất
            String fileExtension = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
            finalName = UUID.randomUUID().toString() + fileExtension;

            // Lưu file
            Path filePath = dirPath.resolve(finalName);
            Files.write(filePath, file.getBytes());

            return finalName;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
