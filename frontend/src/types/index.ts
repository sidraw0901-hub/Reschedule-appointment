export type AppointmentStatus = 'BOOKED' | 'RESCHEDULE_REQUIRED' | 'RESCHEDULED' | 'CANCELLED';
export type SuggestionStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'EXPIRED' | 'FAILED';
export type TriggerType = 'AUTO_PROVIDER_OFFLINE' | 'AUTO_SLOT_CANCELLED' | 'MANUAL';
export type StrategyType = 'RULE_BASED' | 'AI_POWERED' | 'FALLBACK';

export interface AppointmentDetailDTO {
  appointmentId: string;
  patientId: string;
  patientName: string;
  patientPhone: string;
  priorityLevel: string;
  providerId: string;
  providerName: string;
  specialty: string;
  slotId: string;
  startTime: string;
  endTime: string;
  roomName: string;
  status: AppointmentStatus;
}

export interface SuggestionDetailDTO {
  suggestionId: string;
  appointmentId: string;
  patientName: string;
  patientPhone: string;
  priorityLevel: string;
  originalProviderName: string;
  originalSlotTime: string;
  recommendedSlotId: string;
  recommendedProviderName: string;
  recommendedSlotTime: string;
  roomName: string;
  confidence: number;
  triggerType: TriggerType;
  strategyType: StrategyType;
  status: SuggestionStatus;
  explanation: string;
}

export interface SystemStatusDTO {
  totalAppointments: number;
  affectedAppointments: number;
  pendingSuggestions: number;
  rescheduledAppointments: number;
  totalOutboxEvents: number;
  workerStatus: string;
}
