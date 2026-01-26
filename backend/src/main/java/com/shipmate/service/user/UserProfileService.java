package com.shipmate.service.user;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.shipmate.dto.request.user.UpdateUserProfileRequest;
import com.shipmate.dto.response.user.UserProfileResponse;
import com.shipmate.mapper.UserProfileMapper;
import com.shipmate.model.user.User;
import com.shipmate.repository.user.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UserProfileService {

    private final UserRepository userRepository;
    private final UserProfileMapper mapper;
    private final Cloudinary cloudinary;

    @Value("${app.file.max-size:10485760}")
    private long maxFileSize;

    private static final List<String> ALLOWED_IMAGE_TYPES = List.of(
        "image/jpeg",
        "image/png",
        "image/webp"
    );

    // ===================== PROFILE =====================

    public UserProfileResponse getMyProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return mapper.toResponse(user);
    }


    public UserProfileResponse updateMyProfile(UUID userId, UpdateUserProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        mapper.updateEntity(user, request);
        userRepository.save(user);

        return mapper.toResponse(user);
    }

    // ===================== AVATAR =====================

    public UserProfileResponse updateAvatar(UUID userId, MultipartFile file) {
        validateImage(file);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Delete old avatar if exists
        if (user.getAvatarPublicId() != null) {
            deleteFromCloudinary(user.getAvatarPublicId());
        }

        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                    "folder", "shipmate/users/" + userId + "/avatar",
                    "resource_type", "image"
                )
            );

            user.setAvatarUrl((String) uploadResult.get("secure_url"));
            user.setAvatarPublicId((String) uploadResult.get("public_id"));

            userRepository.save(user);
            return mapper.toResponse(user);

        } catch (IOException e) {
            throw new RuntimeException("Failed to upload avatar", e);
        }
    }

    public void deleteAvatar(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getAvatarPublicId() == null) {
            return;
        }

        deleteFromCloudinary(user.getAvatarPublicId());

        user.setAvatarUrl(null);
        user.setAvatarPublicId(null);
        userRepository.save(user);
    }

    // ===================== HELPERS =====================

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("File size exceeds maximum limit");
        }

        if (!ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Invalid image type");
        }
    }

    private void deleteFromCloudinary(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (Exception e) {
        }
    }
}
