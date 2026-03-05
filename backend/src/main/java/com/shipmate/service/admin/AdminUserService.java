package com.shipmate.service.admin;

import com.shipmate.dto.response.admin.AdminUserResponse;
import com.shipmate.mapper.user.AdminUserMapper;
import com.shipmate.model.user.Role;
import com.shipmate.model.user.User;
import com.shipmate.model.user.UserType;
import com.shipmate.repository.user.UserRepository;
import com.shipmate.repository.user.UserSpecifications;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {

    private final UserRepository userRepository;
    private final AdminUserMapper userMapper;

    public Page<AdminUserResponse> getUsers(
            Role role,
            UserType userType,
            Boolean active,
            String search,
            Pageable pageable
    ) {

        Specification<User> spec = (root, query, cb) -> cb.conjunction();
        spec = spec.and(UserSpecifications.hasRole(role));
        spec = spec.and(UserSpecifications.hasUserType(userType));
        spec = spec.and(UserSpecifications.isActive(active));
        spec = spec.and(UserSpecifications.search(search));

        return userRepository
                .findAll(spec, pageable)
                .map(userMapper::toResponse);
    }

    public AdminUserResponse getUser(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        return userMapper.toResponse(user);
    }

    @Transactional
    public void deactivateUser(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        if (!user.isActive()) {
            return;
        }

        user.setActive(false);
        userRepository.save(user);
    }

    @Transactional
    public void activateUser(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        if (user.isActive()) {
            return;
        }

        user.setActive(true);
        userRepository.save(user);
    }
}