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
{{- printf "%s/%s:%s" .root.Values.global.imageRegistry .svc.image $tag -}}
{{- end }}
