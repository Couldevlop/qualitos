-- APQP : reprendre les cycles que personne n'a touchés.
--
-- Le référentiel d'amorçage a changé (libellés du document de référence,
-- astérisques PPAP, genres). Un client qui n'a jamais rien adapté n'a aucune
-- raison de rester sur l'ancienne liste française ; un client qui l'a adaptée ne
-- doit RIEN perdre.
--
-- On SUPPRIME au lieu de réécrire : l'amorçage est paresseux, donc la prochaine
-- ouverture de l'écran reconstruit le cycle depuis le code. Réécrire ligne à
-- ligne ici ferait exister deux définitions du référentiel — celle du code et
-- celle de cette migration — qui divergeraient au premier ajustement.
--
-- « Non touché » demande DEUX conditions, et les deux sont nécessaires : les
-- libellés d'origine (un client peut avoir renommé sans que la date bouge, si
-- la reprise d'une migration antérieure l'a réécrit) et l'horodatage intact
-- (un client peut avoir réorganisé son cycle sans rien renommer).

WITH ancien_titre (valeur) AS (
    VALUES ('Planifier et définir'),
           ('Conception du produit'),
           ('Conception du processus'),
           ('Validation'),
           ('Production série et retour d''expérience')
),
ancien_libelle (valeur) AS (
    VALUES
      ('Voix du client (attentes, réclamations, retours de garantie)'),
      ('Plan d''affaires et stratégie marketing'),
      ('Étude comparative produit et processus (benchmark)'),
      ('Hypothèses produit et processus'),
      ('Études de fiabilité produit'),
      ('Objectifs de conception'),
      ('Objectifs de fiabilité et de qualité'),
      ('Nomenclature préliminaire'),
      ('Schéma de flux du processus préliminaire'),
      ('Liste préliminaire des caractéristiques spéciales'),
      ('Plan d''assurance produit'),
      ('Engagement de la direction'),
      ('AMDEC produit (DFMEA)'),
      ('Conception pour la fabrication et l''assemblage'),
      ('Vérification de la conception'),
      ('Revues de conception'),
      ('Plan de surveillance prototype'),
      ('Dessins et spécifications d''ingénierie'),
      ('Spécifications matières'),
      ('Modifications de dessins et de spécifications'),
      ('Exigences en équipements, outillages et moyens de contrôle'),
      ('Caractéristiques spéciales produit et processus'),
      ('Engagement de faisabilité de l''équipe'),
      ('Normes d''emballage'),
      ('Revue du système qualité produit et processus'),
      ('Schéma de flux du processus'),
      ('Plan d''implantation des postes'),
      ('Matrice des caractéristiques'),
      ('AMDEC processus (PFMEA)'),
      ('Plan de surveillance de pré-lancement'),
      ('Instructions de travail'),
      ('Plan d''analyse des systèmes de mesure'),
      ('Plan des études de capabilité préliminaires'),
      ('Soutien de la direction'),
      ('Essai de production significative'),
      ('Analyse des systèmes de mesure (MSA)'),
      ('Étude de capabilité préliminaire du processus'),
      ('Approbation des pièces de production (PPAP)'),
      ('Essais de validation de production'),
      ('Évaluation de l''emballage'),
      ('Plan de surveillance de production'),
      ('Clôture de la planification qualité'),
      ('Réduction de la variation'),
      ('Satisfaction client'),
      ('Performance de livraison et de service'),
      ('Leçons apprises et bonnes pratiques')
),
intact AS (
    SELECT p.tenant_id
      FROM apqp_phases p
     GROUP BY p.tenant_id
    HAVING count(*) = 5
       -- Aucun titre hors de l'ancien référentiel…
       AND bool_and(p.title IN (SELECT valeur FROM ancien_titre))
       -- …et aucune phase retouchée depuis l'amorçage, qui écrit les deux dates
       -- égales.
       AND bool_and(p.updated_at = p.created_at)
       AND NOT EXISTS (
             SELECT 1
               FROM apqp_deliverables d
              WHERE d.tenant_id = p.tenant_id
                AND (d.label NOT IN (SELECT valeur FROM ancien_libelle)
                     OR d.updated_at <> d.created_at))
)
DELETE FROM apqp_phases
 WHERE tenant_id IN (SELECT tenant_id FROM intact);

-- Les livrables et leurs pièces partent en cascade (V124 et V126).
