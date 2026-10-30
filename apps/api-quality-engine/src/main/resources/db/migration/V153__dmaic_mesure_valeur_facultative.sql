-- La valeur d'une mesure DMAIC devient facultative : un relevé (mesureur, date,
-- référence) peut être noté avant sa valeur. La capabilité ignore les valeurs nulles.
ALTER TABLE dmaic_process_measures ALTER COLUMN value DROP NOT NULL;
