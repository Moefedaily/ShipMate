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
  shipmentId: string;
  messageType: 'SYSTEM' | 'TEXT' | 'IMAGE' | 'LOCATION_UPDATE';
  messageContent: string;
  sentAt: string;
  senderId: string;

}
export interface ConversationUpdateWsDto {
  shipmentId: string;
  shipmentStatus: 'CREATED' | 'ASSIGNED' | 'IN_TRANSIT' | 'DELIVERED' | 'CANCELLED';
  lastMessagePreview: string;
  lastMessageAt: string;
  unreadCount: number;
}

export interface TypingWsDto {
  userId: string;
  displayName: string;
}
export interface DeliveryCodeWsDto {
  shipmentId: string;
  code: string;
  expiresAt?: string;
}
