export interface MessageResponse {
  id: number;
  bookingId: string;
  messageType: 'SYSTEM' | 'TEXT' | 'IMAGE' | 'LOCATION_UPDATE';
  messageContent: string;
  senderId: string;
  receiverId: string;
  isRead: boolean;
  sentAt: string;
}
