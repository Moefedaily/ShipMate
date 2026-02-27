package com.shipmate.dto.response.payment;

import java.util.UUID;

public record AdminRefundResponse(
        UUID paymentId,
        String status
) {}
