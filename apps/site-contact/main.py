"""Point d'entrée : uvicorn main:app."""

from app.api import construis

app = construis()
