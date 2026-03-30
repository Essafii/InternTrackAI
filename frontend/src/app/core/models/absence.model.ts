export interface AbsenceDto {
  id: number;
  stagiaireId: number;
  stagiaireNom: string;
  dateAbsence: string;
  type: string;
  motif: string;
  justifiee: boolean;
  validee: boolean;
  justificatifNom: string;
  tauxAssiduite: number;
}

export interface AbsenceRequest {
  dateAbsence: string;
  type: string;
  motif?: string;
  justifiee: boolean;
}
