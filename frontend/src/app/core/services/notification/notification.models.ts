export interface NotificationResponse {
  id: string;
  title: string;
  message: string;
  notificationType: 
      | 'BOOKING_UPDATE'
      | 'PAYMENT_STATUS'
      | 'DELIVERY_STATUS'
      | 'NEW_MESSAGE'
      | 'SYSTEM_ALERT';
  referenceId?: string | null;
  referenceType?: 
      | 'SHIPMENT'
      | 'BOOKING'
      | 'PAYMENT'
      | 'MESSAGE'
      | 'SYSTEM'
      | null;
  isRead: boolean;
  createdAt: string;
}
