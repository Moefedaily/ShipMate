package com.shipmate.mapper.matching;

import com.shipmate.dto.response.matching.MatchResultResponse;
import com.shipmate.dto.response.matching.MatchingMetricsResponse;
import com.shipmate.mapper.shipment.ShipmentMapper;
import com.shipmate.model.shipment.Shipment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        uses = ShipmentMapper.class,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface MatchResultMapper {

    @Mapping(source = "shipment", target = "shipment")
    @Mapping(source = "metrics", target = "metrics")
    MatchResultResponse toResponse(
            Shipment shipment,
            MatchingMetricsResponse metrics
    );
}
