package com.shipmate.service.photo;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.shipmate.model.insuranceClaim.InsuranceClaim;
import com.shipmate.model.message.Message;
import com.shipmate.model.photo.Photo;
import com.shipmate.model.shipment.Shipment;
import com.shipmate.model.user.User;
import com.shipmate.repository.photo.PhotoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class PhotoService {

    private final PhotoRepository photoRepository;
    private final Cloudinary cloudinary;

    @Value("${app.file.max-size:10485760}")
    private long maxFileSize;

    private static final List<String> ALLOWED_IMAGE_TYPES = List.of(
        "image/jpeg",
        "image/png",
        "image/webp"
    );

    public Photo uploadAvatar(User user, MultipartFile file) {
        validateImage(file);
        
        String folder = "shipmate/users/" + user.getId() + "/avatar";
        Map<?, ?> uploadResult = uploadToCloudinary(file, folder);

        Photo photo = Photo.builder()
                .url((String) uploadResult.get("secure_url"))
                .publicId((String) uploadResult.get("public_id"))
                .photoType("AVATAR")
                .user(user)
                .build();

        return photoRepository.save(photo);
    }

    public List<Photo> uploadShipmentPhotos(Shipment shipment, List<MultipartFile> files) {
        List<Photo> uploadedPhotos = new ArrayList<>();
        String folder = "shipmate/shipments/" + shipment.getId();

        for (MultipartFile file : files) {
            validateImage(file);
            Map<?, ?> uploadResult = uploadToCloudinary(file, folder);

            Photo photo = Photo.builder()
                    .url((String) uploadResult.get("secure_url"))
                    .publicId((String) uploadResult.get("public_id"))
                    .photoType("SHIPMENT_PACKAGE")
                    .shipment(shipment)
                    .build();

            uploadedPhotos.add(photoRepository.save(photo));
        }
        return uploadedPhotos;
    }

    public List<Photo> uploadInsuranceClaimPhotos(InsuranceClaim claim, List<MultipartFile> files) {
        List<Photo> uploadedPhotos = new ArrayList<>();
        String folder = "shipmate/claims/" + claim.getId();

        for (MultipartFile file : files) {
            validateImage(file);
            Map<?, ?> uploadResult = uploadToCloudinary(file, folder);

            Photo photo = Photo.builder()
                    .url((String) uploadResult.get("secure_url"))
                    .publicId((String) uploadResult.get("public_id"))
                    .photoType("CLAIM_PROOF")
                    .insuranceClaim(claim)
                    .build();

            uploadedPhotos.add(photoRepository.save(photo));
        }
        return uploadedPhotos;
    }

    public void deletePhoto(Photo photo) {
        deleteFromCloudinary(photo.getPublicId());
        photoRepository.delete(photo);
    }

    private Map<?, ?> uploadToCloudinary(MultipartFile file, String folder) {
        try {
            return cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                    "folder", folder,
                    "resource_type", "image"
                )
            );
        } catch (IOException e) {
            log.error("Failed to upload to Cloudinary", e);
            throw new RuntimeException("Image upload failed", e);
        }
    }

    private void deleteFromCloudinary(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            log.warn("Failed to delete from Cloudinary: {}", publicId, e);
        }
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("File size exceeds maximum limit");
        }
        if (file.getContentType() == null || !ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Invalid image type. Allowed: JPEG, PNG, WEBP");
        }
    }
}
