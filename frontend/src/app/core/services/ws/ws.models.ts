export interface BookingStatusUpdateWsDto {
  bookingId: string;
  status: 'PENDING'
        | 'CONFIRMED'
        | 'IN_PROGRESS'
        | 'COMPLETED'
        | 'CANCELLED';
}

export interface UnreadCountWsDto {
  unreadCount: number;
}

export interface NotificationWsDto {
  id: string;
  title: string;
  message: string;
  type: 'BOOKING_UPDATE'
      | 'PAYMENT_STATUS'
      | 'DELIVERY_STATUS'
      | 'NEW_MESSAGE'
      | 'SYSTEM_ALERT';
  createdAt: string;
}

export interface MessageWsDto {
  id: number;
  bookingId: string;
  messageType: 'SYSTEM' | 'TEXT' | 'IMAGE' | 'LOCATION_UPDATE';
  messageContent: string;
  sentAt: string;
  senderId: string;

}
export interface ConversationUpdateWsDto {
  bookingId: string;
  bookingStatus: 'PENDING' | 'CONFIRMED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
  lastMessagePreview: string;
  lastMessageAt: string;
  unreadCount: number;
}

export interface TypingWsDto {
  userId: string;
  displayName: string;
}
