# Formulaire de contact du site : un service, une route.
#
# Il est public et sans authentification. Tout ce qu'il ne fait pas — base de
# données, session, fichier, page de documentation — est une surface qu'il
# n'offre pas. C'est pourquoi il vit à part de l'API du produit plutôt que d'y
# ajouter une adresse ouverte.
FROM python:3.13-alpine

# Règle §18.2.1 (0 CVE Critique/Haute) : correctifs alpine publiés après la
# construction de l'image de base.
RUN apk upgrade --no-cache

WORKDIR /service

COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

COPY main.py ./
COPY app ./app

# Utilisateur sans privilège : le cluster impose runAsNonRoot, et un service
# exposé sur internet n'a aucune raison d'être root.
RUN adduser -D -u 10001 service
USER 10001

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s CMD python -c "import urllib.request,sys; sys.exit(0 if urllib.request.urlopen('http://localhost:8080/healthz', timeout=2).status==200 else 1)"

# Un seul worker : le service ne fait qu'attendre un relais SMTP quelques
# dizaines de fois par jour. En ajouter n'ajouterait que des compteurs de débit
# indépendants, c'est-à-dire autant de fois la limite réelle.
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8080", "--workers", "1", "--proxy-headers"]
