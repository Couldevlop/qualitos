# QualitOS Edge Gateway — déploiement K3s (CLAUDE.md §9.5)

Gateway de site (usine, hôpital, exploitation, datacenter) : les équipements
publient en MQTT sur le **broker local** ; un **bridge TLS sortant uniquement**
remonte la télémétrie au IoT Hub QualitOS avec **store-and-forward** (tampon
disque 24-72 h pendant les coupures WAN, rejeu automatique).

```
capteurs/automates ──MQTT──▶ mosquitto (local, K3s) ──bridge mTLS 8883──▶ IoT Hub cloud
                              │ persistance disque (PVC)                  │
                              └── file de rejeu si WAN coupé              └─▶ api-iot-hub → NC/CAPA
```

## Prérequis

- K3s ≥ 1.29 sur l'appliance du site (ARM64 — Raspberry Pi/Jetson — ou x86 :
  Intel NUC, IPC industriel). Footprint : 512 Mo RAM mini (§9.5).
- Deux Secrets à créer lors de l'enrôlement du site :

```bash
# Certificat client X.509 du gateway + CA du hub (mTLS §9.8, rotation ≤ 90 j)
kubectl -n qualitos-edge create secret generic edge-gateway-mtls \
  --from-file=hub-ca.crt --from-file=gateway.crt --from-file=gateway.key

# Comptes MQTT locaux des capteurs (mosquitto_passwd)
kubectl -n qualitos-edge create secret generic mosquitto-local-credentials \
  --from-file=passwd
```

## Déploiement

```bash
kubectl apply -f namespace.yaml
# Renseigner l'adresse du hub avant apply (envsubst ou sed) :
QUALITOS_HUB_MQTT_HOST=mqtt.qualitos.example.com \
  envsubst < mosquitto-config.yaml | kubectl apply -f -
kubectl apply -f mosquitto.yaml
```

## Sécurité (§9.8 — zero-trust)

- **Aucune connexion entrante** : seul le bridge sortant TLS 1.3 + mTLS est ouvert.
- `allow_anonymous false` sur le listener local — chaque équipement a un compte.
- Pod durci : non-root, rootfs read-only, capabilities drop ALL, seccomp.
- Le hub résout le tenant à partir du **code device** (registry), jamais du flux
  (§18.2 règle 2) — un gateway compromis ne peut pas écrire chez un autre tenant.

## Vérifier le store-and-forward

```bash
# Couper le WAN, publier localement :
mosquitto_pub -h <ip-edge> -u sensor1 -P *** -t qualitos/FRIDGE-01/temperature \
  -m '{"value":7.2,"unit":"C"}' -q 1
# → mosquitto journalise la mise en file ; au retour du WAN le bridge rejoue
#   (cleansession false + persistence true).
```

## Étapes suivantes (backlog Edge)

- Inférence locale ONNX/TFLite (détection d'anomalies au plus près du capteur).
- OTA GitOps (ArgoCD edge) + firmware signé Cosign.
- Auto-discovery OPC-UA/mDNS et provisioning X.509 par QR code (§9.6).
