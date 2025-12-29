package com.shipmate.unit.service.profile;


import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.shipmate.dto.request.user.UpdateUserProfileRequest;
import com.shipmate.dto.response.user.UserProfileResponse;
import com.shipmate.mapper.UserProfileMapper;
import com.shipmate.model.user.Role;
import com.shipmate.model.user.User;
import com.shipmate.repository.user.UserRepository;
import com.shipmate.service.user.UserProfileService;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileMapper mapper;

    @InjectMocks
    private UserProfileService userProfileService;

   @Test
    void updateMyProfile_shouldUpdateNamesOnly() {
        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .email("user@shipmate.com")
                .firstName("Old")
                .lastName("Name")
                .role(Role.USER)
                .build();

        UpdateUserProfileRequest request = UpdateUserProfileRequest.builder()
                .firstName("New")
                .lastName("Name")
                .build();

        UserProfileResponse response = UserProfileResponse.builder()
                .id(userId)
                .firstName("New")
                .lastName("Name")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(mapper.toResponse(user)).thenReturn(response);

        UserProfileResponse result = userProfileService.updateMyProfile(userId, request);

        verify(userRepository).findById(userId);
        verify(mapper).updateEntity(user, request);
        verify(userRepository).save(user);

        assertThat(result.getFirstName()).isEqualTo("New");
        assertThat(result.getLastName()).isEqualTo("Name");
    }


    @Test
    void getMyProfile_shouldReturnMappedResponse() {
        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .email("user@shipmate.com")
                .firstName("John")
                .lastName("Doe")
                .role(Role.USER)
                .build();

        UserProfileResponse response = UserProfileResponse.builder()
                .id(userId)
                .firstName("John")
                .lastName("Doe")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(mapper.toResponse(user)).thenReturn(response);

        UserProfileResponse result = userProfileService.getMyProfile(userId);

        verify(userRepository).findById(userId);
        assertThat(result.getId()).isEqualTo(userId);
        assertThat(result.getFirstName()).isEqualTo("John");
    }
}
