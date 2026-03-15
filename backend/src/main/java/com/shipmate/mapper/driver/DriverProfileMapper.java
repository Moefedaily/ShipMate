package com.shipmate.mapper.driver;

import com.shipmate.dto.response.driver.DriverProfileResponse;
import com.shipmate.mapper.vehicle.VehicleMapper;
import com.shipmate.model.DriverProfile.DriverProfile;
import com.shipmate.model.photo.Photo;
import com.shipmate.model.user.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.shipmate.dto.request.driver.DriverApplyRequest;

import java.util.List;

@Mapper(componentModel = "spring", uses = {VehicleMapper.class})
public interface DriverProfileMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", source = "user")
    @Mapping(target = "licenseNumber", source = "request.licenseNumber")
    @Mapping(target = "licensePhotos", ignore = true)
    @Mapping(target = "licenseExpiry", source = "request.licenseExpiry")
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "approvedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "vehicles", ignore = true)
    @Mapping(target = "strikeCount", ignore = true)
    @Mapping(target = "lastLatitude", ignore = true)
    @Mapping(target = "lastLongitude", ignore = true)
    @Mapping(target = "lastLocationUpdatedAt", ignore = true)
    DriverProfile toEntity(DriverApplyRequest request, User user);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "firstName", source = "user.firstName")
    @Mapping(target = "lastName", source = "user.lastName")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "licenseNumber", source = "licenseNumber")
    @Mapping(target = "licensePhotoUrl", expression = "java(firstLicensePhotoUrl(entity.getLicensePhotos()))")
    @Mapping(target = "licensePhotoUrls", expression = "java(toLicensePhotoUrls(entity.getLicensePhotos()))")
    @Mapping(target = "licenseExpiry", source = "licenseExpiry")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "vehicles", source = "vehicles")
    @Mapping(target = "activeVehicle", expression = "java(vehicleMapper.toResponse(entity.getActiveVehicle()))")
    @Mapping(target = "lastLatitude", source = "lastLatitude")
    @Mapping(target = "lastLongitude", source = "lastLongitude")
    @Mapping(target = "lastLocationUpdatedAt", source = "lastLocationUpdatedAt")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "approvedAt", source = "approvedAt")
    @Mapping(target = "strikeCount", source = "strikeCount")
    DriverProfileResponse toResponse(DriverProfile entity);

    default String firstLicensePhotoUrl(List<Photo> photos) {
        List<String> urls = toLicensePhotoUrls(photos);
        if (urls.isEmpty()) {
            return null;
        }
        return urls.get(0);
    }

    default List<String> toLicensePhotoUrls(List<Photo> photos) {
        if (photos == null || photos.isEmpty()) {
            return List.of();
        }
        return photos.stream()
                .filter(photo -> "DRIVER_LICENSE".equals(photo.getPhotoType()))
                .map(Photo::getUrl)
                .toList();
    }
}
