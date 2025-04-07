{{/*
Return the name to use for the redis deployment.
*/}}
{{- define "redis.name" }}
{{- .Values.redis.name | default "itb-redis" -}}
{{- end }}

{{/*
Return the name to use for the redis service.
*/}}
{{- define "redis.serviceName" }}
{{- .Values.redis.serviceName | default .Values.redis.name | default "itb-redis" -}}
{{- end }}

{{/*
Return the port to use for the redis deployment.
*/}}
{{- define "redis.port" }}
{{- .Values.redis.port | default 6379 -}}
{{- end }}

{{/*
Return the port to use for the redis service.
*/}}
{{- define "redis.servicePort" }}
{{- .Values.redis.servicePort | default .Values.redis.port | default 6379 -}}
{{- end }}

{{/*
Return the name to use for the mysql deployment.
*/}}
{{- define "mysql.name" }}
{{- .Values.mysql.name | default "itb-mysql" -}}
{{- end }}

{{/*
Return the name to use for the mysql service.
*/}}
{{- define "mysql.serviceName" }}
{{- .Values.mysql.serviceName | default .Values.mysql.name | default "itb-mysql" -}}
{{- end }}

{{/*
Return the port to use for the mysql deployment.
*/}}
{{- define "mysql.port" }}
{{- .Values.mysql.port | default 3306 -}}
{{- end }}

{{/*
Return the port to use for the mysql service.
*/}}
{{- define "mysql.servicePort" }}
{{- .Values.mysql.servicePort | default .Values.mysql.port | default 3306 -}}
{{- end }}

{{/*
Return the name to use for the itb-srv deployment.
*/}}
{{- define "srv.name" }}
{{- .Values.srv.name | default "itb-srv" -}}
{{- end }}

{{/*
Return the name to use for the itb-srv service.
*/}}
{{- define "srv.serviceName" }}
{{- .Values.srv.serviceName | default .Values.srv.name | default "itb-srv" -}}
{{- end }}

{{/*
Return the port to use for the itb-srv deployment.
*/}}
{{- define "srv.port" }}
{{- .Values.srv.port | default 8080 -}}
{{- end }}

{{/*
Return the port to use for the itb-srv service.
*/}}
{{- define "srv.servicePort" }}
{{- .Values.srv.servicePort | default .Values.srv.port | default 8080 -}}
{{- end }}

{{/*
Return the name to use for the itb-ui deployment.
*/}}
{{- define "ui.name" }}
{{- .Values.ui.name | default "itb-ui" -}}
{{- end }}

{{/*
Return the name to use for the itb-ui service.
*/}}
{{- define "ui.serviceName" }}
{{- .Values.ui.serviceName | default .Values.ui.name | default "itb-ui" -}}
{{- end }}

{{/*
Return the port to use for the itb-ui deployment.
*/}}
{{- define "ui.port" }}
{{- .Values.ui.port | default 9000 -}}
{{- end }}

{{/*
Return the port to use for the itb-ui service.
*/}}
{{- define "ui.servicePort" }}
{{- .Values.ui.servicePort | default .Values.ui.port | default 9000 -}}
{{- end }}

{{/*
Return the port to use for the itb-ui deployment to receive callbacks from itb-srv.
*/}}
{{- define "ui.callbackPort" }}
{{- .Values.ui.callbackPort | default 9090 -}}
{{- end }}

{{/*
Return the port to use for the itb-ui service to receive callbacks from itb-srv.
*/}}
{{- define "ui.serviceCallbackPort" }}
{{- .Values.ui.serviceCallbackPort | default .Values.ui.callbackPort | default 9090 -}}
{{- end }}

{{/*
Return the name to use for the database data persistent volume.
*/}}
{{- define "dbVolumeName" }}
{{- .Values.volume.database.name | default "itb-dbdata" -}}
{{- end }}

{{/*
Return the name to use for the file repository persistent volume.
*/}}
{{- define "repoVolumeName" }}
{{- .Values.volume.repository.name | default "itb-repo" -}}
{{- end }}

{{/*
Return the ingress path to use for itb-ui.
*/}}
{{- define "ingress.uiPath" }}
{{- .Values.ingress.path.ui | default "/itb" -}}
{{- end }}

{{/*
Return the ingress path to use for itb-srv.
*/}}
{{- define "ingress.srvPath" }}
{{- .Values.ingress.path.srv | default "/itbsrv" -}}
{{- end }}

{{/*
Return the context path to use for itb-ui.
*/}}
{{- define "ui.contextRoot" }}
{{- $path := include "ingress.uiPath" . -}}
{{- if and .Values.ui .Values.ui.env }}
  {{- .Values.ui.env.WEB_CONTEXT_ROOT | default (include "ingress.uiPath" .) -}}
{{- end }}
{{- $path -}}
{{- end }}

{{/*
Return the context path to use for itb-srv.
*/}}
{{- define "srv.contextRoot" }}
{{- $path := include "ingress.srvPath" . -}}
{{- if and .Values.srv .Values.srv.env }}
  {{- $path = index .Values.srv.env "server.servlet.context-path" | default (include "ingress.srvPath" .) }}
{{- end }}
{{- $path -}}
{{- end }}

{{/*
Return the authentication cookie path to use for itb-ui.
*/}}
{{- define "ui.authenticationCookiePath" }}
{{- include "ingress.uiPath" . -}}
{{- end }}

{{/*
Return the home link to use for itb-ui.
*/}}
{{- define "ui.homeLink" -}}
{{- $tlsHost := "" -}}
{{- $tlsSecret := "" -}}
{{- if .Values.ingress.tls }}
  {{- $tlsHost = .Values.ingress.tls.host | default "" -}}
  {{- $tlsSecret = .Values.ingress.tls.secretName | default "" -}}
{{- end }}
{{- $scheme := "http" -}}
{{- if and $tlsHost $tlsSecret }}
  {{- $scheme = "https" }}
{{- end }}
{{- $host := $tlsHost | default "localhost" -}}
{{- $path := (include "ingress.uiPath" .) -}}
{{- printf "%s://%s%s" $scheme $host $path -}}
{{- end }}

{{/*
Return the callback root URL to use for itb-srv.
*/}}
{{- define "srv.callbackRoot" -}}
{{- $tlsHost := "" -}}
{{- $tlsSecret := "" -}}
{{- if .Values.ingress.tls }}
  {{- $tlsHost = .Values.ingress.tls.host | default "" -}}
  {{- $tlsSecret = .Values.ingress.tls.secretName | default "" -}}
{{- end }}
{{- $scheme := "http" -}}
{{- if and $tlsHost $tlsSecret }}
  {{- $scheme = "https" }}
{{- end }}
{{- $host := $tlsHost | default "localhost" -}}
{{- $path := (include "ingress.srvPath" .) -}}
{{- printf "%s://%s%s" $scheme $host $path -}}
{{- end }}