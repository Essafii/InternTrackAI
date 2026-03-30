export interface StagiaireDto {
  id: number;
  userId: number;
  email: string;
  fullName: string;
  sujet: string;
  equipe: string;
  dateDebut: string;
  dateFin: string;
  statut: string;
  etablissement: string;
  niveauEtude: string;
  specialite: string;
  encadrantId: number;
  encadrantNom: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
