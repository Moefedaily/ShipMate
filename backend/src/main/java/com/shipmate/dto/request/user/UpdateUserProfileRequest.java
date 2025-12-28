package com.shipmate.dto.request.user;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserProfileRequest {

    private String firstName;

    private String lastName;

    @Pattern(
        regexp = "^\\+?[1-9]\\d{1,14}$", 
        message = "Phone number must be valid (E.164 format recommended)"
    )
    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    private String phone;
}
