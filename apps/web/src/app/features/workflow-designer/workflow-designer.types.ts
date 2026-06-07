import { SpringPage } from '../pdca/pdca.types';

export type WorkflowStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';

/** Vue allégée (liste / cartes) — sans le XML BPMN. */
export interface WorkflowSummary {
  id: string;
  tenantId?: string;
  name: string;
  description?: string;
  status: WorkflowStatus;
  version: number;
  createdBy?: string;
  updatedBy?: string;
  createdAt: string;
  updatedAt: string;
}

/** Vue complète (éditeur) — inclut le diagramme BPMN. */
export interface WorkflowDefinition extends WorkflowSummary {
  bpmnXml: string;
}

export interface CreateWorkflowRequest {
  name: string;
  description?: string;
  bpmnXml: string;
}

export interface UpdateWorkflowRequest {
  name?: string;
  description?: string;
  bpmnXml?: string;
}

export type WorkflowPage = SpringPage<WorkflowSummary>;

/** Squelette BPMN 2.0 minimal d'un nouveau diagramme (start event + diagram). */
export const EMPTY_BPMN_XML = `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                  xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
                  id="Definitions_qos" targetNamespace="http://qualitos.io/bpmn">
  <bpmn:process id="Process_1" isExecutable="false">
    <bpmn:startEvent id="StartEvent_1" name="Début" />
  </bpmn:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="Process_1">
      <bpmndi:BPMNShape id="StartEvent_1_di" bpmnElement="StartEvent_1">
        <dc:Bounds x="173" y="102" width="36" height="36" />
      </bpmndi:BPMNShape>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>`;
