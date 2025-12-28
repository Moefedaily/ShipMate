package com.shipmate.service.user;

import java.util.UUID;

import org.springframework.stereotype.Service;

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

}
