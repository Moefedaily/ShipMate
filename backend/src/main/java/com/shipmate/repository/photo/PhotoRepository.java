package com.shipmate.repository.photo;

import com.shipmate.model.photo.Photo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, UUID> {
    
    List<Photo> findByShipment_Id(UUID shipmentId);
    
    @Query("SELECT p FROM Photo p WHERE p.shipment.id IN :shipmentIds")
    List<Photo> findByShipmentIdIn(@Param("shipmentIds") List<UUID> shipmentIds);
}
