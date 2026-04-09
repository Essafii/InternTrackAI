export interface AuthResponse {
  token: string;
  refreshToken: string;
  email: string;
  role: 'RH' | 'ENCADRANT' | 'STAGIAIRE' | 'ADMIN';
  fullName: string;
  userId: number;
}

export interface User {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  department?: string;
  enabled: boolean;
  createdAt?: string;
}

export interface Stagiaire {
  id: number;
  user: User;
  encadrant: User;
  sujet: string;
  equipe: string;
  dateDebut: string;
  dateFin: string;
  etablissement: string;
  niveauEtude: string;
  specialite: string;
  statut: 'ACTIF' | 'EN_RETARD' | 'TERMINE' | 'INACTIF';
  scoreGlobal?: number;
}

export interface Tache {
  id: number;
  stagiaire: Stagiaire;
  titre: string;
  description: string;
  dateDebut: string;
  deadline: string;
  priorite: number;
  etat: 'A_FAIRE' | 'EN_COURS' | 'TERMINE' | 'EN_RETARD';
  dateCompletion?: string;
}

export interface Absence {
  id: number;
  stagiaire: Stagiaire;
  date: string;
  motif?: string;
  justifie: boolean;
  justificatifUrl?: string;
}

export interface Evaluation {
  id: number;
  stagiaire: Stagiaire;
  encadrant: User;
  periode: string;
  autonomie: number;
  competencesTechniques: number;
  communication: number;
  ponctualite: number;
  resolutionProblemes: number;
  travailEquipe: number;
  commentaire?: string;
  scoreGlobal: number;
  createdAt?: string;
}

export interface Livrable {
  id: number;
  stagiaire: Stagiaire;
  titre: string;
  description?: string;
  fichierUrl?: string;
  fichierNom?: string;
  statut: 'EN_ATTENTE' | 'VALIDE' | 'REJETE';
  commentaireEncadrant?: string;
  dateDepot?: string;
  dateValidation?: string;
}

export interface Notification {
  id: number;
  userId: number;
  message: string;
  type: 'INFO' | 'WARNING' | 'ALERT' | 'SUCCESS';
  read: boolean;
  createdAt: string;
}

export interface ClassementEntry {
  rang: number;
  stagiaireId: number;
  stagiaireNom: string;
  equipe: string;
  scoreGlobal: number;
  tauxAssiduite: number;
  tauxTachesTerminees: number;
  noteMoyenne: number;
  tauxRetard: number;
  risqueIA: number;
}

export interface ActionLog {
  id: number;
  userId: number;
  userEmail: string;
  action: string;
  entite: string;
  entiteId?: number;
  details?: string;
  createdAt: string;
}

export interface DashboardStats {
  totalStagiaires: number;
  stagiairesActifs: number;
  stagiairesEnRetard: number;
  stagiairesTermines: number;
  tauxAssiduiteGlobal: number;
  tachesEnCours: number;
  tachesTerminees: number;
  evaluationsEnAttente: number;
  livrablesEnAttente: number;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface ReportJob {
  id: number;
  type: string;
  statut: 'EN_COURS' | 'TERMINE' | 'ERREUR';
  fichierUrl?: string;
  createdAt: string;
}
