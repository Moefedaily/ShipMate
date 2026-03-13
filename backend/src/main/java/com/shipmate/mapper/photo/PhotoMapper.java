package com.shipmate.mapper.photo;

import com.shipmate.dto.response.photo.PhotoResponse;
import com.shipmate.model.photo.Photo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PhotoMapper {
    PhotoResponse toResponse(Photo photo);
}
