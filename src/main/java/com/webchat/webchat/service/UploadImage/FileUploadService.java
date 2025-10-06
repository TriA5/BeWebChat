package com.webchat.webchat.service.UploadImage;

import org.springframework.web.multipart.MultipartFile;

public interface FileUploadService {
    String uploadFile(MultipartFile multipartFile, String name);
    void deleteFile(String fileUrl);
}
