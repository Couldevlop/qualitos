{{/*
Labels communs — appliqués à toutes les ressources du chart.
*/}}
{{- define "qualitos.labels" -}}
app.kubernetes.io/part-of: qualitos
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ printf "%s-%s" .Chart.Name .Chart.Version }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}

{{/*
Image complète d'un service : registry/image:tag (tag global surchargable par service).
Usage : include "qualitos.image" (dict "root" $ "svc" $svc)
*/}}
{{- define "qualitos.image" -}}
{{- $tag := .svc.tag | default .root.Values.global.imageTag -}}
{{- /*
Un tag vide produisait `image: registre/service:` — un deux-points en fin de
valeur, que YAML interprète comme le début d'une table imbriquée. Le rendu
partait alors en « mapping values are not allowed in this context », erreur qui
ne dit rien du vrai problème. `required` échoue tout de suite avec un message
actionnable. Les fichiers de valeurs par environnement laissent volontairement le
tag vide, afin qu'il soit fourni explicitement au déploiement et qu'on sache
toujours quelle version tourne.
*/ -}}
{{- $tag = required "global.imageTag est vide : passez la version à déployer, par exemple --set global.imageTag=v0.1.0" $tag -}}
{{- printf "%s/%s:%s" .root.Values.global.imageRegistry .svc.image $tag -}}
{{- end }}
