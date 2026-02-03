package com.shipmate.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.shipmate.dto.response.driver.AssignedDriverResponse;
import com.shipmate.model.user.User;

@Mapper(componentModel = "spring")
public interface AssignedDriverMapper {

    @Mapping(source = "id", target = "id")
    @Mapping(source = "firstName", target = "firstName")
    @Mapping(source = "avatarUrl", target = "avatarUrl")
    AssignedDriverResponse from(User user);
}
