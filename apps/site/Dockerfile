# Site de présentation : rien à construire, seulement à servir.
#
# Pas d'étape de build — le dossier EST le site. C'est voulu : une page vitrine
# qui exige une chaîne de compilation pour corriger une faute de frappe finit
# par ne plus être corrigée.
#
# Variante NON PRIVILÉGIÉE de nginx, pour la même raison que apps/web : le
# cluster impose runAsNonRoot, et l'image nginx classique démarre son maître en
# root. Celle-ci tourne sous l'UID 101 et écoute sur 8080.
FROM nginxinc/nginx-unprivileged:1.29-alpine

# Règle §18.2.1 (0 CVE Critique/Haute) : applique les correctifs alpine publiés
# après la construction de l'image de base. Sans coût quand elle est à jour.
USER root
RUN apk upgrade --no-cache
USER 101

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY index.html styles.css site.js robots.txt sitemap.xml /usr/share/nginx/html/
COPY assets /usr/share/nginx/html/assets

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s CMD wget -q -O- http://localhost:8080/healthz >/dev/null || exit 1
