-- Les écarts qu'un dossier ouvert trop tôt avait privés de suite.
--
-- Ouvrir le dossier CAPA sur un écart encore OPEN -- ce que fait le
-- confinement du D3, écrit avant la recherche de cause -- ne posait que le
-- lien : le statut restait OPEN, puis passait UNDER_ANALYSIS au premier clic
-- sur « Analyser ». Or ACTION_DEFINED ne s'atteignait que par l'ouverture du
-- dossier, et un second dossier est refusé. Ces écarts ne pouvaient plus être
-- ni résolus ni clôturés, et le bouton restait éteint sans un mot.
--
-- La règle du domaine est « ouvrir la CAPA, c'est définir l'action » : un
-- écart en analyse qui porte déjà son dossier a son action écrite. On
-- applique ici au passé ce que startAnalysis applique désormais au présent.
UPDATE non_conformities
   SET status = 'ACTION_DEFINED',
       updated_at = now()
 WHERE status = 'UNDER_ANALYSIS'
   AND capa_case_id IS NOT NULL;
