export interface TacheDto {
  id: number;
  stagiaireId: number;
  stagiaireNom: string;
  titre: string;
  description: string;
  etat: 'A_FAIRE' | 'EN_COURS' | 'TERMINE' | 'EN_RETARD';
  dateDebut: string;
  deadline: string;
  dateCompletion: string;
  priorite: number;
  commentaire: string;
}

export interface TacheRequest {
  titre: string;
  description?: string;
  dateDebut?: string;
  deadline?: string;
  priorite?: number;
  etat?: string;
  commentaire?: string;
}
