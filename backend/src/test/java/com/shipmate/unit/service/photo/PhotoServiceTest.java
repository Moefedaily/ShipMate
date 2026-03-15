package com.shipmate.unit.service.photo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.shipmate.model.DriverProfile.DriverProfile;
import com.shipmate.model.photo.Photo;
import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.user.User;
import com.shipmate.repository.photo.PhotoRepository;
import com.shipmate.service.photo.PhotoService;

@ExtendWith(MockitoExtension.class)
class PhotoServiceTest {

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    @InjectMocks
    private PhotoService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "maxFileSize", 1024L * 1024);
    }

    @Test
    void uploadShipmentPhotos_shouldUploadAndPersistPhotos() throws Exception {
        Shipment shipment = Shipment.builder().id(UUID.randomUUID()).build();
        MockMultipartFile file = new MockMultipartFile("file", "box.jpg", "image/jpeg", new byte[] {1, 2, 3});
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), any(Map.class)))
                .thenReturn(Map.of("secure_url", "https://img.test/1.jpg", "public_id", "shipmate/test/1"));
        when(photoRepository.save(any(Photo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<Photo> photos = service.uploadShipmentPhotos(shipment, List.of(file));

        assertThat(photos).hasSize(1);
        assertThat(photos.get(0).getPhotoType()).isEqualTo("SHIPMENT_PACKAGE");
        assertThat(photos.get(0).getShipment()).isEqualTo(shipment);
    }

    @Test
    void uploadDriverLicensePhotos_shouldAttachDriverAndUser() throws Exception {
        User user = User.builder().id(UUID.randomUUID()).build();
        DriverProfile profile = DriverProfile.builder().id(UUID.randomUUID()).user(user).build();
        MockMultipartFile file = new MockMultipartFile("file", "license.png", "image/png", new byte[] {1});
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), any(Map.class)))
                .thenReturn(Map.of("secure_url", "https://img.test/license.png", "public_id", "shipmate/test/license"));
        when(photoRepository.save(any(Photo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<Photo> photos = service.uploadDriverLicensePhotos(profile, List.of(file));

        assertThat(photos).hasSize(1);
        assertThat(photos.get(0).getDriverProfile()).isEqualTo(profile);
        assertThat(photos.get(0).getUser()).isEqualTo(user);
        assertThat(photos.get(0).getPhotoType()).isEqualTo("DRIVER_LICENSE");
    }

    @Test
    void uploadShipmentPhotos_shouldRejectInvalidType() {
        Shipment shipment = Shipment.builder().id(UUID.randomUUID()).build();
        MockMultipartFile file = new MockMultipartFile("file", "bad.gif", "image/gif", new byte[] {1});

        assertThatThrownBy(() -> service.uploadShipmentPhotos(shipment, List.of(file)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid image type");
    }

    @Test
    void uploadShipmentPhotos_shouldRejectOversizedFile() {
        Shipment shipment = Shipment.builder().id(UUID.randomUUID()).build();
        MockMultipartFile file = new MockMultipartFile("file", "big.jpg", "image/jpeg", new byte[2 * 1024 * 1024]);

        assertThatThrownBy(() -> service.uploadShipmentPhotos(shipment, List.of(file)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File size exceeds maximum limit");
    }

    @Test
    void deletePhoto_shouldDetachFromDriverProfileBeforeDelete() throws Exception {
        DriverProfile profile = DriverProfile.builder().id(UUID.randomUUID()).build();
        Photo photo = Photo.builder()
                .id(UUID.randomUUID())
                .publicId("shipmate/test/license")
                .driverProfile(profile)
                .build();
        profile.setLicensePhotos(new java.util.ArrayList<>(List.of(photo)));
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(any(String.class), any(Map.class))).thenReturn(Map.of("result", "ok"));

        service.deletePhoto(photo);

        assertThat(profile.getLicensePhotos()).isEmpty();
        assertThat(photo.getDriverProfile()).isNull();
        verify(photoRepository).delete(photo);
    }
}
