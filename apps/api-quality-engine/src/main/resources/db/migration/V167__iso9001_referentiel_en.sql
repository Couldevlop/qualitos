-- ISO 9001:2015 — le référentiel passe en anglais (ADR 0116).
--
-- Pourquoi : une norme ISO est publiée en anglais, et c'est cette formulation
-- que l'auditeur cite, que le certificat mentionne et que les autres normes
-- reprennent mot pour mot dans la structure HLS commune. Le référentiel semé en
-- français obligeait à retraduire mentalement chaque exigence pour la confronter
-- au texte opposable, et deux équipes de deux pays ne lisaient pas la même
-- phrase. ISO 9001 est la norme PILOTE de ce passage : la méthode se valide ici
-- avant d'être étendue aux autres référentiels par vagues.
--
-- Comment : des UPDATE, et non une réécriture de V10 — une migration appliquée
-- ne se modifie pas, sans quoi la somme de contrôle de Flyway casse en préprod.
-- Les clés sont les CODES (« 4.1 », « 9.2.1 »), stables par définition : ce sont
-- ceux de la norme. Rien n'est supprimé, rien n'est créé : seul le texte change,
-- donc les preuves déjà liées aux exigences le restent.
--
-- Le texte n'est pas une copie de la norme (elle est sous droits) : c'est la même
-- reformulation fidèle que le français qu'il remplace, dans la langue de
-- publication.

-- ---------------------------------------------------------------------------
-- La fiche de la norme
-- ---------------------------------------------------------------------------
UPDATE standards SET
    full_name = 'ISO 9001:2015 — Quality management systems — Requirements',
    description = 'International standard specifying the requirements for a quality management system. Applicable to any organization, whatever its size, sector or products and services.',
    updated_at = now()
WHERE id = '11111111-1111-1111-1111-111111111111';

-- ---------------------------------------------------------------------------
-- Les sections 4 à 10
-- ---------------------------------------------------------------------------
UPDATE standard_sections SET title = 'Context of the organization',
    description = 'Understanding the internal and external context and the interested parties, and the scope of the QMS.'
WHERE standard_id = '11111111-1111-1111-1111-111111111111' AND code = '4';

UPDATE standard_sections SET title = 'Leadership',
    description = 'Top management commitment, quality policy, roles and responsibilities.'
WHERE standard_id = '11111111-1111-1111-1111-111111111111' AND code = '5';

UPDATE standard_sections SET title = 'Planning',
    description = 'Risks and opportunities, quality objectives, planning of changes.'
WHERE standard_id = '11111111-1111-1111-1111-111111111111' AND code = '6';

UPDATE standard_sections SET title = 'Support',
    description = 'Resources, competence, awareness, communication, documented information.'
WHERE standard_id = '11111111-1111-1111-1111-111111111111' AND code = '7';

UPDATE standard_sections SET title = 'Operation',
    description = 'Operational planning, customer requirements, design, purchasing, production, release.'
WHERE standard_id = '11111111-1111-1111-1111-111111111111' AND code = '8';

UPDATE standard_sections SET title = 'Performance evaluation',
    description = 'Monitoring, measurement, customer satisfaction, internal audit, management review.'
WHERE standard_id = '11111111-1111-1111-1111-111111111111' AND code = '9';

UPDATE standard_sections SET title = 'Improvement',
    description = 'Nonconformity, corrective action, continual improvement.'
WHERE standard_id = '11111111-1111-1111-1111-111111111111' AND code = '10';

-- ---------------------------------------------------------------------------
-- Les clauses
-- ---------------------------------------------------------------------------
UPDATE standard_clauses SET title = 'Understanding the organization and its context'
WHERE id = 'c0000041-0000-0000-0000-000000000001';
UPDATE standard_clauses SET title = 'Understanding the needs and expectations of interested parties'
WHERE id = 'c0000041-0000-0000-0000-000000000002';
UPDATE standard_clauses SET title = 'Determining the scope of the quality management system'
WHERE id = 'c0000041-0000-0000-0000-000000000003';
UPDATE standard_clauses SET title = 'Quality management system and its processes'
WHERE id = 'c0000041-0000-0000-0000-000000000004';

UPDATE standard_clauses SET title = 'Leadership and commitment'
WHERE id = 'c0000051-0000-0000-0000-000000000001';
UPDATE standard_clauses SET title = 'Policy'
WHERE id = 'c0000051-0000-0000-0000-000000000002';
UPDATE standard_clauses SET title = 'Organizational roles, responsibilities and authorities'
WHERE id = 'c0000051-0000-0000-0000-000000000003';

UPDATE standard_clauses SET title = 'Monitoring, measurement, analysis and evaluation'
WHERE id = 'c0000091-0000-0000-0000-000000000001';
UPDATE standard_clauses SET title = 'Internal audit'
WHERE id = 'c0000091-0000-0000-0000-000000000002';
UPDATE standard_clauses SET title = 'Management review'
WHERE id = 'c0000091-0000-0000-0000-000000000003';

UPDATE standard_clauses SET title = 'General'
WHERE id = 'c0000101-0000-0000-0000-000000000001';
UPDATE standard_clauses SET title = 'Nonconformity and corrective action'
WHERE id = 'c0000101-0000-0000-0000-000000000002';
UPDATE standard_clauses SET title = 'Continual improvement'
WHERE id = 'c0000101-0000-0000-0000-000000000003';

-- ---------------------------------------------------------------------------
-- Les exigences : le texte, les preuves attendues, les critères mesurables
-- ---------------------------------------------------------------------------
UPDATE standard_requirements SET
    text = 'The organization shall determine the external and internal issues relevant to its purpose and strategic direction.',
    evidence_types = 'SWOT, PESTEL, strategic document',
    measurable_criteria = 'Issues documented, reviewed at least annually'
WHERE clause_id = 'c0000041-0000-0000-0000-000000000001' AND code = '4.1';

UPDATE standard_requirements SET
    text = 'The organization shall determine the interested parties relevant to the quality management system and their requirements.',
    evidence_types = 'Register of interested parties',
    measurable_criteria = 'Exhaustive list, updated at least once a year'
WHERE clause_id = 'c0000041-0000-0000-0000-000000000002' AND code = '4.2';

UPDATE standard_requirements SET
    text = 'The organization shall determine the boundaries and applicability of the quality management system to establish its scope.',
    evidence_types = 'Quality manual, scope statement',
    measurable_criteria = 'Scope formalized and published'
WHERE clause_id = 'c0000041-0000-0000-0000-000000000003' AND code = '4.3';

UPDATE standard_requirements SET
    text = 'The organization shall establish, implement, maintain and continually improve a quality management system.',
    evidence_types = 'Process map',
    measurable_criteria = 'Processes identified with inputs, outputs and KPIs'
WHERE clause_id = 'c0000041-0000-0000-0000-000000000004' AND code = '4.4.1';

UPDATE standard_requirements SET
    text = 'The organization shall maintain the documented information needed for the effectiveness of the quality management system.',
    evidence_types = 'Quality documents',
    measurable_criteria = 'Procedures under version control and signed'
WHERE clause_id = 'c0000041-0000-0000-0000-000000000004' AND code = '4.4.2';

UPDATE standard_requirements SET
    text = 'Top management shall demonstrate leadership and commitment with respect to the quality management system.',
    evidence_types = 'Management review minutes, commitment plan',
    measurable_criteria = 'Management review documented at least once a year'
WHERE clause_id = 'c0000051-0000-0000-0000-000000000001' AND code = '5.1.1';

UPDATE standard_requirements SET
    text = 'Top management shall establish, implement and maintain a quality policy.',
    evidence_types = 'Signed quality policy',
    measurable_criteria = 'Policy dated, signed and communicated'
WHERE clause_id = 'c0000051-0000-0000-0000-000000000002' AND code = '5.2.1';

UPDATE standard_requirements SET
    text = 'Top management shall assign the responsibilities and authorities for the relevant roles.',
    evidence_types = 'Organization chart, job descriptions',
    measurable_criteria = 'Every QMS role assigned'
WHERE clause_id = 'c0000051-0000-0000-0000-000000000003' AND code = '5.3';

UPDATE standard_requirements SET
    text = 'The organization shall conduct internal audits at planned intervals.',
    evidence_types = 'Internal audit programme and reports',
    measurable_criteria = 'Internal audit at least once a year, full coverage'
WHERE clause_id = 'c0000091-0000-0000-0000-000000000002' AND code = '9.2.1';

UPDATE standard_requirements SET
    text = 'Top management shall review the quality management system at planned intervals.',
    evidence_types = 'Management review minutes',
    measurable_criteria = 'Management review at least once a year, with documented decisions'
WHERE clause_id = 'c0000091-0000-0000-0000-000000000003' AND code = '9.3.1';

UPDATE standard_requirements SET
    text = 'When a nonconformity occurs, the organization shall react to it and take appropriate corrective action.',
    evidence_types = 'CAPA register, nonconformity records',
    measurable_criteria = 'Closure within 30 days for high criticality'
WHERE clause_id = 'c0000101-0000-0000-0000-000000000002' AND code = '10.2.1';

UPDATE standard_requirements SET
    text = 'The organization shall continually improve the suitability, adequacy and effectiveness of the quality management system.',
    evidence_types = 'PDCA cycles, improvement indicators',
    measurable_criteria = 'Improvement plan documented and followed up'
WHERE clause_id = 'c0000101-0000-0000-0000-000000000003' AND code = '10.3';

-- ---------------------------------------------------------------------------
-- Les clauses et exigences ajoutées ensuite (V57 : §5 complément, §6, §7, §8,
-- §9.1, §10.1). Même règle : on ne touche qu'au texte, jamais aux codes.
-- ---------------------------------------------------------------------------
UPDATE standard_requirements SET
    text = 'Top management shall demonstrate leadership with respect to customer focus by ensuring that customer and regulatory requirements are determined and met.',
    evidence_types = 'Review of customer requirements, satisfaction indicators',
    measurable_criteria = 'Customer requirements identified and monitored'
WHERE clause_id = 'c0000051-0000-0000-0000-000000000001' AND code = '5.1.2';

UPDATE standard_clauses SET title = 'Actions to address risks and opportunities'
WHERE id = 'c0000061-0000-0000-0000-000000000001';
UPDATE standard_clauses SET title = 'Quality objectives and planning to achieve them'
WHERE id = 'c0000061-0000-0000-0000-000000000002';
UPDATE standard_clauses SET title = 'Planning of changes'
WHERE id = 'c0000061-0000-0000-0000-000000000003';

UPDATE standard_requirements SET
    text = 'When planning the quality management system, the organization shall consider the issues (4.1) and requirements (4.2) and determine the risks and opportunities to be addressed.',
    evidence_types = 'Risk and opportunity analysis',
    measurable_criteria = 'Risks and opportunities documented and traced back to the issues'
WHERE clause_id = 'c0000061-0000-0000-0000-000000000001' AND code = '6.1.1';

UPDATE standard_requirements SET
    text = 'The organization shall plan the actions to address risks and opportunities, how to integrate them into the quality management system processes, and how to evaluate their effectiveness.',
    evidence_types = 'Risk action plan, FMEA',
    measurable_criteria = 'Actions planned and effectiveness evaluated'
WHERE clause_id = 'c0000061-0000-0000-0000-000000000001' AND code = '6.1.2';

UPDATE standard_requirements SET
    text = 'The organization shall establish quality objectives at the relevant functions, levels and processes, consistent with the quality policy, measurable and monitored.',
    evidence_types = 'SMART quality objectives, KPI dashboard',
    measurable_criteria = 'Measurable objectives set and monitored'
WHERE clause_id = 'c0000061-0000-0000-0000-000000000002' AND code = '6.2.1';

UPDATE standard_requirements SET
    text = 'The organization shall plan how to achieve its quality objectives: what will be done, the resources, the responsibilities, the deadlines and how the results will be evaluated.',
    evidence_types = 'Action plans, milestones',
    measurable_criteria = 'Plan for achieving the objectives documented'
WHERE clause_id = 'c0000061-0000-0000-0000-000000000002' AND code = '6.2.2';

UPDATE standard_requirements SET
    text = 'Where the organization determines the need for changes to the quality management system, the changes shall be carried out in a planned manner.',
    evidence_types = 'Change management, impact analysis',
    measurable_criteria = 'Changes to the QMS planned and recorded'
WHERE clause_id = 'c0000061-0000-0000-0000-000000000003' AND code = '6.3';

UPDATE standard_clauses SET title = 'Resources' WHERE id = 'c0000071-0000-0000-0000-000000000001';
UPDATE standard_clauses SET title = 'Competence' WHERE id = 'c0000071-0000-0000-0000-000000000002';
UPDATE standard_clauses SET title = 'Awareness' WHERE id = 'c0000071-0000-0000-0000-000000000003';
UPDATE standard_clauses SET title = 'Communication' WHERE id = 'c0000071-0000-0000-0000-000000000004';
UPDATE standard_clauses SET title = 'Documented information' WHERE id = 'c0000071-0000-0000-0000-000000000005';

UPDATE standard_requirements SET
    text = 'The organization shall determine and provide the resources needed to establish, implement, maintain and continually improve the quality management system.',
    evidence_types = 'Resource plan, quality budget',
    measurable_criteria = 'Resources allocated and documented'
WHERE clause_id = 'c0000071-0000-0000-0000-000000000001' AND code = '7.1.1';

UPDATE standard_requirements SET
    text = 'The organization shall determine and provide the resources for monitoring and measurement, and ensure the validity of the results (calibration, metrological traceability).',
    evidence_types = 'Calibration plan, MSA records',
    measurable_criteria = 'Measuring equipment calibrated and traceable'
WHERE clause_id = 'c0000071-0000-0000-0000-000000000001' AND code = '7.1.5';

UPDATE standard_requirements SET
    text = 'The organization shall determine the knowledge necessary for the operation of its processes and keep it up to date.',
    evidence_types = 'Knowledge base, lessons learned',
    measurable_criteria = 'Organizational knowledge captured'
WHERE clause_id = 'c0000071-0000-0000-0000-000000000001' AND code = '7.1.6';

UPDATE standard_requirements SET
    text = 'The organization shall determine the necessary competence, ensure that persons are competent, and retain documented information as evidence.',
    evidence_types = 'Competence matrix, training records',
    measurable_criteria = 'Competence assessed, gaps addressed'
WHERE clause_id = 'c0000071-0000-0000-0000-000000000002' AND code = '7.2';

UPDATE standard_requirements SET
    text = 'Persons doing work under the control of the organization shall be aware of the quality policy, the objectives and their contribution to them.',
    evidence_types = 'Awareness material, attendance sheets',
    measurable_criteria = 'Staff awareness (coverage rate)'
WHERE clause_id = 'c0000071-0000-0000-0000-000000000003' AND code = '7.3';

UPDATE standard_requirements SET
    text = 'The organization shall determine the internal and external communications relevant to the quality management system: what, when, with whom, how and who communicates.',
    evidence_types = 'Communication plan',
    measurable_criteria = 'Communication plan formalized'
WHERE clause_id = 'c0000071-0000-0000-0000-000000000004' AND code = '7.4';

UPDATE standard_requirements SET
    text = 'The quality management system shall include the documented information required by the standard and that determined as necessary by the organization.',
    evidence_types = 'Master list of documents',
    measurable_criteria = 'QMS documentation established'
WHERE clause_id = 'c0000071-0000-0000-0000-000000000005' AND code = '7.5.1';

UPDATE standard_requirements SET
    text = 'When creating and updating documented information, the organization shall ensure appropriate identification, format, and review and approval.',
    evidence_types = 'Document control procedure',
    measurable_criteria = 'Documents identified, reviewed and approved'
WHERE clause_id = 'c0000071-0000-0000-0000-000000000005' AND code = '7.5.2';

UPDATE standard_requirements SET
    text = 'Documented information shall be controlled: availability, protection, distribution, access, storage, control of changes and retention.',
    evidence_types = 'Document management under version control, access control',
    measurable_criteria = 'Versioning active, access controlled, retention defined'
WHERE clause_id = 'c0000071-0000-0000-0000-000000000005' AND code = '7.5.3';

UPDATE standard_clauses SET title = 'Operational planning and control'
WHERE id = 'c0000081-0000-0000-0000-000000000001';
UPDATE standard_clauses SET title = 'Requirements for products and services'
WHERE id = 'c0000081-0000-0000-0000-000000000002';
UPDATE standard_clauses SET title = 'Design and development of products and services'
WHERE id = 'c0000081-0000-0000-0000-000000000003';
UPDATE standard_clauses SET title = 'Control of externally provided processes, products and services'
WHERE id = 'c0000081-0000-0000-0000-000000000004';
UPDATE standard_clauses SET title = 'Production and service provision'
WHERE id = 'c0000081-0000-0000-0000-000000000005';
UPDATE standard_clauses SET title = 'Release of products and services'
WHERE id = 'c0000081-0000-0000-0000-000000000006';
UPDATE standard_clauses SET title = 'Control of nonconforming outputs'
WHERE id = 'c0000081-0000-0000-0000-000000000007';

UPDATE standard_requirements SET
    text = 'The organization shall plan, implement and control the processes needed to provide products and services (criteria, resources, documented information).',
    evidence_types = 'Quality plans, operation sheets',
    measurable_criteria = 'Operational processes planned and controlled'
WHERE clause_id = 'c0000081-0000-0000-0000-000000000001' AND code = '8.1';

UPDATE standard_requirements SET
    text = 'The organization shall establish processes for communicating with customers (product information, enquiries, feedback, complaints).',
    evidence_types = 'Customer relations procedure, complaints register',
    measurable_criteria = 'Customer communication channels defined'
WHERE clause_id = 'c0000081-0000-0000-0000-000000000002' AND code = '8.2.1';

UPDATE standard_requirements SET
    text = 'The organization shall determine the requirements for the products and services offered to customers, both statutory and its own.',
    evidence_types = 'Specification, bid review',
    measurable_criteria = 'Product requirements determined'
WHERE clause_id = 'c0000081-0000-0000-0000-000000000002' AND code = '8.2.2';

UPDATE standard_requirements SET
    text = 'The organization shall review its ability to meet the requirements before committing to supply products and services to the customer.',
    evidence_types = 'Contract or order review',
    measurable_criteria = 'Review formalized before commitment'
WHERE clause_id = 'c0000081-0000-0000-0000-000000000002' AND code = '8.2.3';

UPDATE standard_requirements SET
    text = 'Where applicable, the organization shall establish a design and development process appropriate to ensure subsequent provision.',
    evidence_types = 'Design procedure, design milestones',
    measurable_criteria = 'Design process controlled (where applicable)'
WHERE clause_id = 'c0000081-0000-0000-0000-000000000003' AND code = '8.3.1';

UPDATE standard_requirements SET
    text = 'The organization shall ensure that externally provided processes, products and services conform to requirements.',
    evidence_types = 'Supplier evaluation, contracts',
    measurable_criteria = 'External providers evaluated and selected'
WHERE clause_id = 'c0000081-0000-0000-0000-000000000004' AND code = '8.4.1';

UPDATE standard_requirements SET
    text = 'The organization shall define the type and extent of control applied to external providers and to their deliverables.',
    evidence_types = 'Supplier monitoring plan, supplier audits',
    measurable_criteria = 'Control of suppliers proportionate to the risk'
WHERE clause_id = 'c0000081-0000-0000-0000-000000000004' AND code = '8.4.2';

UPDATE standard_requirements SET
    text = 'The organization shall implement production and service provision under controlled conditions (instructions, equipment, monitoring).',
    evidence_types = 'Work instructions, production records',
    measurable_criteria = 'Controlled conditions applied'
WHERE clause_id = 'c0000081-0000-0000-0000-000000000005' AND code = '8.5.1';

UPDATE standard_requirements SET
    text = 'The organization shall identify the outputs and their status, and control unique identification where traceability is a requirement.',
    evidence_types = 'Identification and traceability system (batches, serial numbers)',
    measurable_criteria = 'Traceability ensured where required'
WHERE clause_id = 'c0000081-0000-0000-0000-000000000005' AND code = '8.5.2';

UPDATE standard_requirements SET
    text = 'The organization shall verify that the requirements for products and services have been met before their release to the customer.',
    evidence_types = 'Release checks, acceptance records',
    measurable_criteria = 'Release conditional on conformity'
WHERE clause_id = 'c0000081-0000-0000-0000-000000000006' AND code = '8.6';

UPDATE standard_requirements SET
    text = 'The organization shall identify and control nonconforming outputs to prevent their unintended use or delivery.',
    evidence_types = 'Nonconformity procedure, nonconformity register',
    measurable_criteria = 'Nonconforming outputs segregated and dealt with'
WHERE clause_id = 'c0000081-0000-0000-0000-000000000007' AND code = '8.7';

UPDATE standard_requirements SET
    text = 'The organization shall determine what needs to be monitored and measured, the methods, when, and the analysis, in order to evaluate the performance and effectiveness of the quality management system.',
    evidence_types = 'Monitoring and measurement plan, KPI dashboards',
    measurable_criteria = 'Indicators defined and analysed periodically'
WHERE clause_id = 'c0000091-0000-0000-0000-000000000001' AND code = '9.1.1';

UPDATE standard_requirements SET
    text = 'The organization shall monitor the perception of customers of the degree to which their needs and expectations have been fulfilled.',
    evidence_types = 'Satisfaction surveys, NPS, complaint analysis',
    measurable_criteria = 'Customer satisfaction measured and acted upon'
WHERE clause_id = 'c0000091-0000-0000-0000-000000000001' AND code = '9.1.2';

UPDATE standard_requirements SET
    text = 'The organization shall analyse and evaluate the appropriate data and information arising from monitoring and measurement.',
    evidence_types = 'Indicator reviews, statistical analyses',
    measurable_criteria = 'Data analysed to support decisions'
WHERE clause_id = 'c0000091-0000-0000-0000-000000000001' AND code = '9.1.3';

UPDATE standard_requirements SET
    text = 'The organization shall determine and select opportunities for improvement and take the necessary actions to meet customer requirements and enhance customer satisfaction.',
    evidence_types = 'Improvement plan, DMAIC and PDCA projects',
    measurable_criteria = 'Improvement opportunities identified and acted upon'
WHERE clause_id = 'c0000101-0000-0000-0000-000000000001' AND code = '10.1';
