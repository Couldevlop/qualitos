-- Nature des actions d'un dossier CAPA (§4.2, ISO 9001 §10.2, 8D étape D3).
--
-- Le type du DOSSIER (capa_cases.type : CORRECTIVE / PREVENTIVE) dit pourquoi il
-- a été ouvert. Il ne dit rien de ce que chaque action fait réellement, et les
-- deux ne se déduisent pas l'un de l'autre : un dossier correctif porte presque
-- toujours, en plus de ses corrections, une ou deux mesures prises le jour même
-- pour arrêter l'hémorragie — trier un lot, arrêter une ligne, prévenir un client.
--
-- Sans cette colonne, un dossier où l'on a seulement trié le lot suspect se lit
-- exactement comme un dossier où l'on a corrigé le réglage de la presse : les
-- deux affichent « toutes les actions faites ». Le second seul empêche la récidive.
--
-- DÉFAUT CORRECTIVE, y compris pour les lignes existantes. Ce n'est pas un choix
-- par commodité : aucune mesure d'endiguement n'a jamais pu être enregistrée comme
-- telle, faute de colonne pour le dire. Toutes les actions déjà saisies l'ont donc
-- été comme des actions correctives, et c'est exactement ce que le défaut inscrit.
ALTER TABLE capa_actions
    ADD COLUMN action_type VARCHAR(20) NOT NULL DEFAULT 'CORRECTIVE';

-- Le DEFAULT a rempli les lignes existantes ; on le retire pour que la valeur
-- vienne désormais de l'application, qui décide seule (et de façon testable) de
-- ce qu'est une action non qualifiée. Un défaut laissé en base finirait par
-- diverger silencieusement de celui du service.
ALTER TABLE capa_actions
    ALTER COLUMN action_type DROP DEFAULT;

-- Le domaine est fermé côté base aussi : une valeur inventée par un script
-- d'import ou une migration de données ferait échouer la lecture JPA bien plus
-- tard, au premier chargement du dossier, avec un message qui ne dirait pas d'où
-- elle vient.
ALTER TABLE capa_actions
    ADD CONSTRAINT chk_capa_actions_action_type
        CHECK (action_type IN ('CONTAINMENT', 'CORRECTIVE', 'PREVENTIVE'));
