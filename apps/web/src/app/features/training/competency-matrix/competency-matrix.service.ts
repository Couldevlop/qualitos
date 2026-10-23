import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import {
  AddOperatorRequest, AddSkillRequest, CompetencyMatrixView, MatrixCell, SetLevelRequest, WorkshopsView
} from './competency-matrix.types';

/**
 * La matrice de compétences éditable (ADR 0098). Le client et l'auteur des
 * écritures viennent du jeton côté serveur, jamais d'ici.
 *
 * Inscrire ou retirer quelqu'un rend la matrice entière : une personne
 * réinscrite retrouve ses évaluations, et la couverture de toutes les
 * colonnes peut en changer.
 */
@Injectable({ providedIn: 'root' })
export class CompetencyMatrixService {

  private readonly base = `${environment.apiBaseUrl}/api/v1/training/competencies`;

  constructor(private readonly http: HttpClient) {}

  matrix(): Observable<CompetencyMatrixView> {
    return this.http.get<CompetencyMatrixView>(`${this.base}/matrix`);
  }

  addOperator(req: AddOperatorRequest): Observable<CompetencyMatrixView> {
    return this.http.post<CompetencyMatrixView>(`${this.base}/matrix/operators`, req);
  }

  removeOperator(operatorId: string): Observable<CompetencyMatrixView> {
    return this.http.delete<CompetencyMatrixView>(`${this.base}/matrix/operators/${encodeURIComponent(operatorId)}`);
  }

  addSkill(req: AddSkillRequest): Observable<CompetencyMatrixView> {
    return this.http.post<CompetencyMatrixView>(`${this.base}/matrix/skills`, req);
  }

  removeSkill(skillId: string): Observable<CompetencyMatrixView> {
    return this.http.delete<CompetencyMatrixView>(`${this.base}/matrix/skills/${encodeURIComponent(skillId)}`);
  }

  /** Valeur absolue : un double envoi ne fait pas tourner le niveau deux fois. */
  setLevel(req: SetLevelRequest): Observable<MatrixCell> {
    return this.http.put<MatrixCell>(`${this.base}/matrix/levels`, req);
  }

  workshops(): Observable<WorkshopsView> {
    return this.http.get<WorkshopsView>(`${this.base}/workshops`);
  }

  updateWorkshops(names: string[]): Observable<WorkshopsView> {
    return this.http.put<WorkshopsView>(`${this.base}/workshops`, { names });
  }
}
