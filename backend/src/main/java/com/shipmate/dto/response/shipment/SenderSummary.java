package com.shipmate.dto.response.shipment;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class SenderSummary {

    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
}