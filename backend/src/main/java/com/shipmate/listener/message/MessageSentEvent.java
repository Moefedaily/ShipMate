package com.shipmate.listener.message;

import java.util.UUID;

public record MessageSentEvent(UUID messageId) {}
