import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  Idea, IdeaBoard, ImpactRequest, RejectIdeaRequest, SubmitIdeaRequest
} from './ideas.types';

/**
 * La boîte à idées.
 *
 * <p>Chaque écriture rend l'IDÉE entière, compteur de voix compris : le laisser
 * recalculer à l'écran ferait diverger deux navigateurs ouverts côte à côte.
 */
@Injectable({ providedIn: 'root' })
export class IdeasService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/ideas`;

  constructor(private readonly http: HttpClient) {}

  board(): Observable<IdeaBoard> {
    return this.http.get<IdeaBoard>(this.endpoint);
  }

  submit(input: SubmitIdeaRequest): Observable<Idea> {
    return this.http.post<Idea>(this.endpoint, input);
  }

  vote(ideaId: string): Observable<Idea> {
    return this.http.post<Idea>(`${this.endpoint}/${ideaId}/vote`, {});
  }

  unvote(ideaId: string): Observable<Idea> {
    return this.http.delete<Idea>(`${this.endpoint}/${ideaId}/vote`);
  }

  review(ideaId: string): Observable<Idea> {
    return this.http.patch<Idea>(`${this.endpoint}/${ideaId}/review`, {});
  }

  approve(ideaId: string): Observable<Idea> {
    return this.http.patch<Idea>(`${this.endpoint}/${ideaId}/approve`, {});
  }

  reject(ideaId: string, input: RejectIdeaRequest): Observable<Idea> {
    return this.http.patch<Idea>(`${this.endpoint}/${ideaId}/reject`, input);
  }

  implement(ideaId: string): Observable<Idea> {
    return this.http.patch<Idea>(`${this.endpoint}/${ideaId}/implement`, {});
  }

  measure(ideaId: string, input: ImpactRequest): Observable<Idea> {
    return this.http.patch<Idea>(`${this.endpoint}/${ideaId}/impact`, input);
  }
}
