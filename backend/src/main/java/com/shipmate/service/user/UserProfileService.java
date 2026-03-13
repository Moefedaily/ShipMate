package com.shipmate.service.user;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.shipmate.dto.request.user.UpdateUserProfileRequest;
import com.shipmate.dto.response.user.UserProfileResponse;
import com.shipmate.mapper.user.UserProfileMapper;
import com.shipmate.model.photo.Photo;
import com.shipmate.model.user.User;
import com.shipmate.repository.user.UserRepository;
import com.shipmate.service.photo.PhotoService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UserProfileService {

    private final UserRepository userRepository;
    private final UserProfileMapper mapper;
    private final PhotoService photoService;

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


    public UserProfileResponse updateAvatar(UUID userId, MultipartFile file) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getAvatar() != null) {
            photoService.deletePhoto(user.getAvatar());
        }

        Photo photo = photoService.uploadAvatar(user, file);
        user.setAvatar(photo);

        userRepository.save(user);
        return mapper.toResponse(user);
    }

    public void deleteAvatar(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getAvatar() == null) {
            return;
        }

        photoService.deletePhoto(user.getAvatar());
        user.setAvatar(null);
        userRepository.save(user);
    }

    @Transactional
    public void deleteAccount(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setActive(false);

        user.setEmail("deleted-" + user.getId() + "@deleted.local");
        user.setFirstName("Deleted");
        user.setLastName("User");

        userRepository.save(user);
    }
}
