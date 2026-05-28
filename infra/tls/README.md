# TLS hybride post-quantique (WS5, ADR 0015)

Active le groupe TLS 1.3 **`X25519MLKEM768`** (X25519 + ML-KEM-768) sur les flux
entrants, avec repli `X25519` pour les clients non-PQ. Source de vérité des
groupes : la suite **`tls-hybrid-p3`** de `CryptoSuiteConfig` (§11.4).

Deux modes de terminaison (cf. ADR 0015) :

## Mode A — Ingress (recommandé, §10.1)

Terminer le TLS hybride au bord (Envoy/Istio compilé BoringSSL+OQS, ou un
reverse-proxy **nginx + oqs-provider**), puis mTLS Istio en interne.

- `nginx-oqs.conf` : exemple de reverse-proxy nginx (OpenSSL 3 + oqs-provider)
  devant l'API Gateway, négociant `X25519MLKEM768`.
- Vérification : `openssl s_client -connect host:443 -groups X25519MLKEM768`
  (avec un OpenSSL+oqs-provider) doit afficher le groupe négocié.

## Mode B — App-level (mono-service / on-prem sans ingress)

L'app termine TLS via le **provider JSSE Bouncy Castle** (`bctls` dev /
**`bctls-fips` prod**). Voir `application-tls.yml` : `server.ssl` + enregistrement
du provider BC-JSSE + `jdk.tls.namedGroups=X25519MLKEM768`.

> ⚠ Le JDK 21 (SunJSSE) **ne négocie pas** `X25519MLKEM768` seul — d'où BC-JSSE
> ou un terminateur OQS. Deps `bctls`/`bctls-fips` non téléchargées ici (réseau) :
> ce mode est une **config de référence**, à valider en CI/infra.

## Migration FIPS (prod régulée)

Basculer `bctls` → `bctls-fips` (et `bcprov-jdk18on` → `bc-fips` pour les
signatures, ADR 0011), mode *approved*, revalidation des suites + MAJ du CBOM
(`docs/security/cbom.md`). Le SPI crypto rend la bascule transparente côté app.
