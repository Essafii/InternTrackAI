export interface NotificationDto {
  id: number;
  titre: string;
  message: string;
  type: string;
  lue: boolean;
  createdAt: string;
  entiteId: number;
  entiteType: string;
}
