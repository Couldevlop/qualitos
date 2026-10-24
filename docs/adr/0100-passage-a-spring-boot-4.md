# ADR 0100 — Passage à Spring Boot 4, Jackson 2 conservé

- **Statut** : Accepté
- **Date** : 2026-10-24
- **Owners** : @Couldevlop
- **Portée** : pom racine, api-core, api-quality-engine, api-iot-hub,
  blockchain-service ; `.trivyignore`
- **Remplace** : la ligne « Spring Boot 3.5, Spring Security 6.4, Hibernate 6.6 »
  de CLAUDE.md §10.2, pour les versions

## Contexte

Le 2026-10-10, Trivy a signalé deux failles de Spring Framework 6.2.19
(spring-webmvc) : CVE-2026-47884 (critique, vue XSLT) et CVE-2026-47890 (flux
SSE avec rendu de vue). Aucune 6.2.x corrigée n'existe : 6.2.19 est la
dernière de la ligne, et Spring Boot 3.5.16 la dernière de la 3.5. Le
correctif n'existe qu'en Spring Framework 7.0, donc en Spring Boot 4.

Les deux failles ont été mises en exception jusqu'au 2026-11-10, avec un job
CI (« Composants Spring exposés ») qui casse dès qu'une vue XSLT ou un flux SSE
apparaît. La règle 18.2.1 n'admet pas de reconduire une exception sans
correctif en vue.

## Décisions

### 1. Spring Boot 4.0.9 partout

Le parent passe en 4.0.9 (Spring Framework 7.0.10, Spring Security 7.0.8,
Hibernate 7.2), y compris pour blockchain-service, qui a son propre parent.
Les surcharges de sécurité de netty, tomcat, jackson et postgresql
disparaissent : Boot 4.0.9 gère des versions égales ou plus récentes.

### 2. Démarreurs et modules renommés

- `web` → `webmvc`, `oauth2-resource-server` → `security-oauth2-resource-server`,
  `aop` → `aspectj`, `flyway-core` → `spring-boot-starter-flyway`.
- Le démarreur `restclient` est ajouté : `RestClient.Builder` n'est plus fourni
  par `webmvc`.
- Tests : `spring-boot-starter-test-classic` (toutes les tranches de test) et
  `spring-boot-starter-security-test` ; Testcontainers 2
  (`testcontainers-postgresql`, `testcontainers-junit-jupiter`).
- Les annotations et types déplacés (tranches `WebMvcTest`, `DataJpaTest`,
  `AutoConfigureMockMvc`, `AutoConfigureTestDatabase`,
  `HibernatePropertiesCustomizer`, `FlywayConfigurationCustomizer`,
  `RestTemplateBuilder`) prennent leur paquet Boot 4 ; `@MockBean` devient
  `@MockitoBean`.

### 3. Jackson 2 conservé, Jackson 3 plus tard

Boot 4 sérialise en Jackson 3 (`tools.jackson`). Le code utilise Jackson 2
dans une cinquantaine de classes de production et une centaine de tests
(`ObjectMapper`, `JsonNode`, modules `jsr310`). Tout réécrire dans la même
PR que le changement de framework mêlerait deux risques.

- `spring-boot-jackson2` fournit l'`ObjectMapper` Jackson 2 de chaque service ;
- `spring.http.converters.preferred-json-mapper: jackson2` fait passer les
  réponses HTTP par ce même moteur : le format JSON des API ne change pas ;
- les clients HTTP qui lisent des `JsonNode` (Keycloak, ERP) posent
  explicitement le convertisseur Jackson 2.

La migration vers Jackson 3 est une dette ouverte, à traiter dans une PR
dédiée, avant que Spring retire le convertisseur Jackson 2 (déprécié en 7.0).

### 4. Fin des exceptions Trivy

CVE-2026-47884 et CVE-2026-47890 sortent de `.trivyignore`. Le job « Composants
Spring exposés » n'a plus d'objet et est retiré.

## Conséquences

- Plus de composant Spring hors support ; les correctifs de sécurité
  redeviennent une simple montée de version de correctif.
- Le convertisseur Jackson 2 et `spring-boot-jackson2` sont dépréciés : ils
  produisent des avertissements de compilation jusqu'à la migration Jackson 3.

## Vérification

- `mvn verify` vert sur tous les modules, dont les tests `contextLoads` de
  core et engine, qui attrapent les beans manquants (`RestClient.Builder`).
- Trivy (SCA) vert sans exception Spring.
- Préprod : connexion, listes et création d'enregistrements après déploiement.

## Références

CLAUDE.md §10.2, §18.2.1 ; guide de migration Spring Boot 4.0 ;
`.trivyignore` (entrée du 2026-10-10).
