package com.shipmate.mapper.user;

import com.shipmate.dto.response.admin.AdminUserResponse;
import com.shipmate.model.user.User;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AdminUserMapper {

    AdminUserResponse toResponse(User user);
}