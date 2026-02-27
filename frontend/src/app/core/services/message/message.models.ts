export interface MessageResponse {
  id: number;
  shipmentId: string;
  messageType: 'SYSTEM' | 'TEXT' | 'IMAGE' | 'LOCATION_UPDATE';
  messageContent: string;
  senderId: string;
  receiverId: string;
  isRead: boolean;
  sentAt: string;
}
