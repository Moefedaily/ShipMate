export type ShipmentStatus =
  | 'CREATED'
  | 'ASSIGNED'
  | 'IN_TRANSIT'
  | 'DELIVERED'
  | 'CANCELLED';

export interface ConversationSummary {
  shipmentId: string;
  shipmentStatus: ShipmentStatus;

  lastMessagePreview: string | null;
  lastMessageAt: string | null;

  unreadCount: number;

  otherUserName?: string;
  otherUserAvatarUrl?: string;
}
