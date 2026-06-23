-- ANO-010 : colonnes pour le compte-rendu généré par LLM (§3.3 QualitOS)
ALTER TABLE circle_meetings
    ADD COLUMN IF NOT EXISTS minutes_summary TEXT,
    ADD COLUMN IF NOT EXISTS minutes_json    TEXT;
