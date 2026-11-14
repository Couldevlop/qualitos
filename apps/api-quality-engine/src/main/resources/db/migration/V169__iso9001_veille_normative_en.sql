-- ISO 9001:2015 — la veille normative rejoint la langue du référentiel (ADR 0116).
--
-- Pourquoi : la V167 a fait passer en anglais la fiche, les sections, les clauses
-- et les 42 exigences, mais a laissé en français les quatre lignes de veille
-- normative. Or celles-ci ne parlent que de la norme : son édition 2008 remplacée,
-- l'édition 2015 en vigueur, l'amendement climat de 2024, la révision annoncée.
-- Elles citent la norme, donc elles tombent du côté anglais de la frontière posée
-- par l'ADR 0116 — ce que le client produit reste dans sa langue, ce qui cite la
-- norme est dans la langue où elle sera opposée. Résultat visible en préproduction :
-- un utilisateur travaillant en anglais ouvrait la fiche ISO 9001 et y lisait des
-- exigences anglaises sous un onglet de veille rédigé en français.
--
-- Ce qui n'est PAS touché, et volontairement : la bibliothèque documentaire
-- (manuel qualité, politique, procédures) et les processus types restent en
-- français. Ce sont les livrables du client, pas la norme ; un organisme français
-- remet à son auditeur un manuel qualité en français. C'est la décision de
-- l'ADR 0116, et elle est confirmée.
--
-- Comment : des UPDATE, comme la V167 — une migration appliquée ne se réécrit pas,
-- sa somme de contrôle Flyway casserait en préproduction. La clé est l'édition
-- (`version`), que la contrainte `uk_srev_standard_version` rend unique par norme.
-- Les deux éditions dont le libellé change (l'amendement, la révision annoncée)
-- sont renommées sans collision possible : aucune autre ligne ne porte le libellé
-- d'arrivée. Rien n'est créé, rien n'est supprimé.
--
-- Le texte n'est pas une copie de la norme (elle est sous droits d'auteur) : c'est
-- la même reformulation fidèle que le français qu'elle remplace.

-- L'édition 2008, remplacée.
UPDATE standard_revisions SET
    summary = 'Previous edition, organized by requirement across eight clauses. Superseded by the 2015 edition.',
    impact_note = 'Migration completed. Kept for the record.'
WHERE standard_id = '11111111-1111-1111-1111-111111111111'
  AND version = '2008';

-- L'édition 2015, en vigueur.
UPDATE standard_revisions SET
    summary = 'High-level structure (HLS), risk-based thinking, and a stronger emphasis on leadership and organizational context.',
    impact_note = 'Edition in force — the active reference for the quality management system.'
WHERE standard_id = '11111111-1111-1111-1111-111111111111'
  AND version = '2015';

-- L'amendement climat de 2024. Le libellé lui-même est en français : il change.
UPDATE standard_revisions SET
    version = '2024 amendment (climate action)',
    summary = 'Climate Action amendment: climate change must be considered among the external issues (clause 4.1) and among the expectations of interested parties (clause 4.2).',
    impact_note = 'To be taken up: determine whether climate change is a relevant issue, and whether interested parties expect it to be addressed.'
WHERE standard_id = '11111111-1111-1111-1111-111111111111'
  AND version = 'Amendement 2024 (climat)';

-- La révision annoncée. Libellé en français lui aussi.
UPDATE standard_revisions SET
    version = '2025-2026 (planned)',
    summary = 'Major revision expected around 2025-2026: modernization, digital practices, and alignment with the revised high-level structure.',
    impact_note = 'To watch: anticipate the likely additions (digital, data, sustainability) so the transition can be spread over time.'
WHERE standard_id = '11111111-1111-1111-1111-111111111111'
  AND version = '2025-2026 (planifiée)';
