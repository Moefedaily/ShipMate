package com.shipmate.dto.response.photo;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhotoResponse {
    private UUID id;
    private String url;
    private String photoType;
    private Instant createdAt;
}
