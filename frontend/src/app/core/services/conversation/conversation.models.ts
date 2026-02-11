export type BookingStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'CANCELLED';

export interface ConversationSummary {
  bookingId: string;
  bookingStatus: BookingStatus;

  lastMessagePreview: string | null;
  lastMessageAt: string | null;

  unreadCount: number;

  otherUserName?: string;
  otherUserAvatarUrl?: string;
}
