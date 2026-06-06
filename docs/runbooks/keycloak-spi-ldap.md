# Runbook — Keycloak SPI LDAP custom QualitOS

> Module : `libs/keycloak-spi-ldap` — `UserStorageProvider` (id **`qualitos-ldap-custom`**) qui
> fédère des annuaires LDAP non standards (Active Directory, OpenLDAP, FreeIPA) avec mapping
> d'attributs configurable, cache local TTL et fallback transparent. Réf. CLAUDE.md §13.4.

Le JAR est **déployé dans Keycloak** (`/opt/keycloak/providers/`), **pas** embarqué dans les
services Spring. Les artefacts Keycloak sont en scope `provided` : ils ne sont pas inclus dans le
jar — c'est le runtime Keycloak qui les fournit.

---

## 1. Build du jar

```powershell
$env:TMP='D:\tmp'; $env:TEMP='D:\tmp'
# C: saturé sur le poste de dev → repo Maven local sur D: (voir mémoire projet)
mvn '-Dmaven.repo.local=D:\tmp\m2repo' -pl libs/keycloak-spi-ldap -am package
```

Artefact produit :

```
libs/keycloak-spi-ldap/target/keycloak-spi-ldap-0.1.0-SNAPSHOT.jar
```

Le jar ne contient **que** les classes QualitOS + `META-INF/services/org.keycloak.storage.UserStorageProviderFactory`.
Aucune dépendance Keycloak n'est packagée (scope `provided`). Pas de shading nécessaire.

---

## 2. Déploiement dans Keycloak

### 2.1 Conteneur (image officielle quay.io/keycloak/keycloak:25.0)

```bash
# Copier le jar dans le répertoire des providers
docker cp libs/keycloak-spi-ldap/target/keycloak-spi-ldap-0.1.0-SNAPSHOT.jar \
  qualitos-keycloak:/opt/keycloak/providers/

# Reconstruire l'augmentation Quarkus puis redémarrer
docker exec qualitos-keycloak /opt/keycloak/bin/kc.sh build
docker restart qualitos-keycloak
```

> `kc.sh build` scanne `/opt/keycloak/providers/`, détecte la factory via le fichier
> `META-INF/services` et l'enregistre. Au démarrage, le log affiche :
> `Initialisation du provider de fédération LDAP custom QualitOS (id=qualitos-ldap-custom)`.

### 2.2 Build d'une image dédiée (recommandé en prod)

```dockerfile
FROM quay.io/keycloak/keycloak:25.0 AS kc-build
COPY keycloak-spi-ldap-0.1.0-SNAPSHOT.jar /opt/keycloak/providers/
RUN /opt/keycloak/bin/kc.sh build

FROM quay.io/keycloak/keycloak:25.0
COPY --from=kc-build /opt/keycloak /opt/keycloak
ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
```

---

## 3. Ajout au docker-compose (optionnel, profil `ldap-spi`)

Pour itérer en local sans rebuild d'image, monter le jar en volume. Bloc à ajouter au service
`keycloak` de `docker-compose.dev.yml` (le profil rend l'option non intrusive — actif seulement
si `--profile ldap-spi`) :

```yaml
  keycloak:
    # ... config existante ...
    volumes:
      - ./infra/keycloak/realm-export.json:/opt/keycloak/data/import/realm-export.json:ro
      # Monté uniquement si le jar a été buildé. Après montage : exécuter
      #   docker exec qualitos-keycloak /opt/keycloak/bin/kc.sh build && docker restart qualitos-keycloak
      - ./libs/keycloak-spi-ldap/target/keycloak-spi-ldap-0.1.0-SNAPSHOT.jar:/opt/keycloak/providers/keycloak-spi-ldap.jar:ro
```

> Le profil Compose n'est pas appliqué au montage de volume (Compose ne profile que des services
> entiers). Pour garder le montage **opt-in**, préférer un fichier override dédié :
> `docker compose -f docker-compose.dev.yml -f docker-compose.ldap-spi.yml up`, où l'override ne
> contient que le bloc `volumes` ci-dessus. Sans le jar buildé, ne pas activer l'override
> (le bind mount échouerait).

---

## 4. Activation & configuration dans le realm (User Federation)

1. Console admin Keycloak → realm **`qualitos`** → **User federation** → **Add provider**.
2. Sélectionner **`qualitos-ldap-custom`** (libellé : « QualitOS — Fédération LDAP custom… »).
3. Renseigner la configuration. Les attributs de mapping sont **tous configurables** ; valeurs
   selon le type d'annuaire :

| Champ                         | Active Directory                   | OpenLDAP                         | FreeIPA                                  |
| ----------------------------- | ---------------------------------- | -------------------------------- | ---------------------------------------- |
| URL de connexion              | `ldaps://ad.example.com:636`       | `ldap://ldap.example.com:389`    | `ldaps://ipa.example.com:636`            |
| Bind DN (compte de service)   | `CN=svc-kc,CN=Users,DC=ex,DC=com`  | `cn=admin,dc=example,dc=com`     | `uid=svc-kc,cn=users,cn=accounts,dc=ex`  |
| Bind credential (secret)      | *(masqué)*                         | *(masqué)*                       | *(masqué)*                               |
| Users DN (base de recherche)  | `CN=Users,DC=ex,DC=com`            | `ou=people,dc=example,dc=com`    | `cn=users,cn=accounts,dc=example,dc=com` |
| Object classes utilisateur    | `user,person`                      | `inetOrgPerson`                  | `inetOrgPerson,posixAccount`             |
| **Attribut username**         | `sAMAccountName`                   | `uid`                            | `uid`                                    |
| Attribut email                | `mail`                             | `mail`                           | `mail`                                   |
| Attribut prénom               | `givenName`                        | `givenName`                      | `givenName`                              |
| Attribut nom                  | `sn`                               | `sn`                             | `sn`                                     |
| Attribut identifiant unique   | `objectGUID`                       | `entryUUID`                      | `ipaUniqueID`                            |
| Filtre additionnel (option)   | `(memberOf=CN=qualitos,…)`         | `(memberOf=cn=qualitos,…)`       | `(memberOf=cn=qualitos,…)`               |
| TTL cache (s)                 | `300`                              | `300`                            | `300`                                    |
| Timeout connexion / lecture   | `5000` / `5000`                    | `5000` / `5000`                  | `5000` / `5000`                          |

4. **Save**. La validation refuse l'enregistrement si URL ou Users DN sont vides
   (`ComponentValidationException`).
5. Tester : se connecter à une application du realm avec un compte de l'annuaire. Le mot de passe
   est validé par **bind LDAP** sur le DN résolu de l'utilisateur.

---

## 5. Sécurité

- **`bindCredential`** : type `PASSWORD` + `secret(true)` → stocké chiffré par Keycloak, masqué
  dans l'UI, **jamais journalisé** (ni dans `LdapConfig.toString()`, ni dans les logs du client).
- **Injection LDAP** : toutes les valeurs de filtre sont échappées RFC 4515 (`LdapEscaper`).
- **Timeouts** : connexion et lecture bornés (défaut 5 s) pour éviter les blocages.
- **`ldaps://`** active automatiquement TLS (SSL) côté JNDI.
- **Mauvais mot de passe** ⇒ `false` (pas d'exception). **Annuaire injoignable** ⇒
  `LdapUnavailableException` interceptée → **fallback transparent** : le provider renvoie
  `null`/`false`, Keycloak délègue aux autres providers / au stockage local, le login local
  n'est pas cassé.

---

## 6. Dépannage

| Symptôme                                          | Cause probable / action                                                            |
| ------------------------------------------------- | ---------------------------------------------------------------------------------- |
| Provider absent de la liste « Add provider »      | `kc.sh build` non exécuté après copie du jar, ou Keycloak non redémarré.            |
| `Configuration LDAP invalide : 'connectionUrl'…`  | Champ obligatoire vide à l'enregistrement.                                          |
| Login échoue mais utilisateur visible             | DN résolu correct mais bind refusé : vérifier mot de passe / `userObjectClasses`.   |
| Tous les logins LDAP échouent silencieusement     | Annuaire injoignable → chercher `Fallback transparent` dans les logs Keycloak.      |
| Utilisateur introuvable                           | Mauvais `usernameAttribute` (AD = `sAMAccountName`, pas `uid`) ou `usersDn` erroné. |

---

## 7. Impact build / réacteur

- Module ajouté au `<modules>` du `pom.xml` parent.
- Les Dockerfiles `api-core`, `api-quality-engine`, `api-iot-hub` valident **tout** le réacteur :
  une ligne `COPY libs/keycloak-spi-ldap/pom.xml libs/keycloak-spi-ldap/` y a été ajoutée
  (placeholder requis, sinon l'image casse au chargement du réacteur).
- `blockchain-service` builde **hors réacteur** (son propre pom) → **non impacté**.
- Le module est une **lib sœur**, jamais une dépendance des services Spring : son scope `provided`
  Keycloak n'entre pas dans les jars des services. Vérifié : `mvn validate` sur le réacteur =
  BUILD SUCCESS ; `mvn -pl libs/keycloak-spi-ldap test` = 50 tests verts.
