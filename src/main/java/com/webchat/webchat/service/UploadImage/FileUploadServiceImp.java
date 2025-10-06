package com.webchat.webchat.service.UploadImage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class FileUploadServiceImp implements FileUploadService {
    private final Cloudinary cloudinary;

    @Override
    public String uploadFile(MultipartFile multipartFile, String name) {
        String url = "";
        try {
            // Cloudinary tự động xác định resource_type dựa trên file
            url = cloudinary.uploader()
                    .upload(multipartFile.getBytes(), 
                        Map.of(
                            "public_id", name,
                            "resource_type", "auto" // auto detect: image, video, raw (documents)
                        ))
                    .get("url")
                    .toString();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to upload file: " + e.getMessage());
        }
        return url;
    }

    @Override
    public void deleteFile(String fileUrl) {
        try {
            String publicId = getPublicId(fileUrl);
            // Try different resource types
            try {
                cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image"));
            } catch (Exception e) {
                try {
                    cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "raw"));
                } catch (Exception ex) {
                    cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "video"));
                }
            }
        } catch (Exception e) {
            System.out.println("Lỗi khi xóa file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String getPublicId(String fileUrl) {
        String[] parts = fileUrl.split("/");
        String publicIdWithFormat = parts[parts.length - 1];
        String[] publicIdAndFormat = publicIdWithFormat.split("\\.");
        return publicIdAndFormat[0];
    }
}
