import { Component, OnInit } from '@angular/core';
import { Observable, forkJoin } from 'rxjs';
import { map, shareReplay } from 'rxjs/operators';

import { StandardsService } from '../../standards.service';
import { AdoptionResponse, StandardSummary } from '../../standards.types';

@Component({
  selector: 'qos-standards-list',
  templateUrl: './standards-list.component.html',
  styleUrls: ['./standards-list.component.scss'],
  standalone: false
})
export class StandardsListComponent implements OnInit {

  readonly catalogCols = ['code', 'fullName', 'family', 'status', 'cycle'];
  readonly adoptCols = ['code', 'status', 'scope', 'body', 'target'];

  catalog$!: Observable<StandardSummary[]>;
  adoptions$!: Observable<AdoptionResponse[]>;

  constructor(private readonly svc: StandardsService) {}

  ngOnInit(): void {
    this.catalog$ = this.svc.listCatalog().pipe(map(p => p.content), shareReplay(1));
    this.adoptions$ = this.svc.listAdoptions().pipe(map(p => p.content), shareReplay(1));
  }
}
