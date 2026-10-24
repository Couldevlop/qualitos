import { BPMN_STYLES_HREF, ensureBpmnStyles } from './bpmn-styles';

describe('ensureBpmnStyles', () => {
  afterEach(() => document.getElementById('qos-bpmn-styles')?.remove());

  it('ajoute la feuille bpmn au head', () => {
    ensureBpmnStyles(document);
    const link = document.getElementById('qos-bpmn-styles') as HTMLLinkElement;
    expect(link).not.toBeNull();
    expect(link.rel).toBe('stylesheet');
    expect(link.getAttribute('href')).toBe(BPMN_STYLES_HREF);
    expect(link.parentElement).toBe(document.head);
  });

  it("ne l'ajoute qu'une fois", () => {
    ensureBpmnStyles(document);
    ensureBpmnStyles(document);
    expect(document.querySelectorAll('#qos-bpmn-styles').length).toBe(1);
  });
});
