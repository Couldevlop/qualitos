# ADR 0046 — Corpus RAG : uniquement des documents réels et licenciés, jamais de texte normatif reconstitué

- **Statut** : Accepté
- **Date** : 2026-08-01
- **Owners** : Architecte principal

## Contexte

Le module RAG (§12.2) est câblé **à moitié**. Le chemin de requête est complet :
filtre anti-injection, redaction PII, embeddings BGE-M3, recherche vectorielle
bornée au tenant, complétion LLM avec citations `[doc_id]`, journal d'audit.

Le chemin d'**alimentation n'existe pas**. `VectorStore.upsert` est déclaré dans
le port, implémenté dans les deux adaptateurs — et n'a **aucun appelant** : ni
endpoint, ni service, ni ordonnanceur. L'index n'est pas vide à cause d'un
défaut : rien n'a jamais pu l'alimenter. Deux symptômes confirment que le contrat
n'a jamais été exercé :

- `InMemoryVectorStore.upsert` range `[0.0]` comme vecteur — un document ainsi
  indexé a une similarité cosinus nulle avec toute requête, donc reste
  introuvable ;
- `QdrantVectorStore.upsert` attend l'embedding **pré-calculé** par l'appelant et
  transmis dans `metadata['embedding']` sous forme de chaîne CSV, contrat que
  personne ne remplit et qui place le calcul du vecteur hors du magasin.

La tentation évidente — amorcer le corpus avec du contenu fabriqué pour « avoir
quelque chose à chercher » — est précisément ce qu'il ne faut pas faire, et pour
une raison qui n'est pas seulement esthétique. Un assistant qualité qui cite
`[iso-9001-4.1]` en restituant une reformulation produit une réponse **qui a
l'apparence de l'autorité sans en avoir la substance**. Devant un auditeur de
certification, c'est pire que pas de réponse du tout.

Le catalogue `standard_requirements.text` existant illustre le risque : son texte
est une reformulation proche des clauses ISO (le script d'amorçage le qualifie
lui-même de « synthétique »). Utile comme aide-mémoire dans un écran où
l'utilisateur sait ce qu'il lit ; inacceptable comme source citée par une IA.

## Benchmark des sources — ce qui est réellement exploitable

Vérifié auprès des éditeurs, pas supposé.

| Source | Statut juridique | Exploitable en corpus RAG |
| --- | --- | --- |
| **Documents du tenant** (`document_versions.content`, statut `PUBLISHED`) | Données du client, hébergées pour son compte | **Oui** — source primaire |
| Enregistrements qualité du tenant (audits, NC, CAPA, revues, comptes rendus de cercle) | Idem | **Oui** |
| **Droit de l'Union** — AI Act 2024/1689, RGPD 2016/679, NIS 2 2022/2555, DORA 2022/2554, MDR 2017/745, IVDR 2017/746 | EUR-Lex, **CC BY 4.0** (décision 2011/833/UE) | **Oui**, avec attribution |
| **Droit et doctrine France** — codes, décrets, délibérations CNIL | Légifrance / DILA via l'API PISTE, **Licence Ouverte Etalab 2.0** (usage commercial autorisé) | **Oui**, avec attribution |
| **FDA** — 21 CFR Part 11, Part 820, guidances | Œuvre du gouvernement fédéral, domaine public / CC0 (17 U.S.C. §105) | **Oui** |
| **Normes ISO/IEC** — 9001, 27001, 13485, 14001, 45001, 22301, 17025… | Sous droit d'auteur. Copie et distribution interdites sans autorisation écrite. Les organismes de normalisation interdisent **explicitement** l'introduction de normes dans des applications d'IA | **Non** |
| **IATF 16949, AS9100/EN 9100, VDA, NADCAP** | Idem, éditeurs privés | **Non** |
| Reformulations maison de clauses ISO (`standard_requirements.text`) | Dérivé d'une œuvre protégée, et non fidèle | **Non** |

## Décision

1. **Aucun document n'entre dans le corpus sans source vérifiable.** Chaque
   fragment indexé porte l'origine, la licence et la version du document dont il
   est issu. Un fragment sans provenance n'est pas indexé.

2. **Source primaire : les documents du tenant.** L'ingestion part de
   `document_versions` au statut `PUBLISHED` — le référentiel documentaire que le
   client a lui-même rédigé et approuvé. C'est ce que promet l'invite système
   (« the tenant's quality corpus »), c'est réel, et cela ne pose aucune question
   de licence.

3. **Les normes ISO ne sont jamais ingérées, ni en texte, ni en reformulation.**
   Le Standards Hub conserve la **structure** (numérotation, intitulés de clauses,
   liens vers les preuves) — qui relève de la référence factuelle — mais le RAG ne
   restitue jamais de texte normatif. Interrogé sur une exigence ISO, l'assistant
   répond sur ce qu'il détient réellement : les preuves du tenant rattachées à la
   clause, et renvoie l'utilisateur à son exemplaire sous licence pour le texte.

4. **Corpus réglementaire public en second temps**, par connecteurs dédiés et
   traçables (EUR-Lex, PISTE/Légifrance, FDA), chacun avec sa mention de licence
   restituée dans les citations. Ces textes-là sont, eux, opposables.

5. **Le calcul des embeddings appartient à la couche d'application**, pas aux
   adaptateurs de stockage. Le contrat actuel — vecteur transporté en CSV dans
   `metadata` — est abandonné : `upsert` reçoit les fragments **et** leurs
   vecteurs.

## Conséquences

- Un tenant qui n'a encore rédigé aucun document a un corpus vide, et l'assistant
  le dit (« No relevant documents found »). C'est le comportement correct : la
  valeur du RAG vient des documents du client, pas d'un fonds fabriqué.
- Les questions purement normatives (« que dit ISO 9001 §8.5 ? ») n'obtiennent pas
  de citation du texte. C'est une limite assumée et juridiquement contrainte, à
  afficher dans l'interface plutôt qu'à contourner.
- Les questions de conformité réellement utiles restent servies : « suis-je
  couvert sur §8.5 ? » se répond avec les preuves du tenant, qui sont
  précisément ce que le moteur d'alignement (§8.7) sait rattacher.
- Le corpus réglementaire UE/FR/FDA, lui, autorise la citation littérale — c'est
  là que le RAG pourra être opposable.

## Sources

- ISO — Copyright : <https://www.iso.org/copyright.html>
- ISO — *How to best use IEC and ISO standards* : <https://www.iso.org/publication/PUB100206.html>
- SFS — *Correct use of standards, pay attention to copyright* : <https://sfs.fi/en/keep-in-mind-the-correct-use-of-standards-pay-attention-to-copyright/>
- EUR-Lex — Réutilisation des contenus : <https://eur-lex.europa.eu/content/help/data-reuse/reuse-contents-eurlex-details.html>
- Union européenne — Avis juridique : <https://european-union.europa.eu/legal-notice_en>
- Légifrance — Open data et API : <https://www.legifrance.gouv.fr/contenu/pied-de-page/open-data-et-api>
- Etalab — Licence Ouverte : <https://www.etalab.gouv.fr/les-bases-legi-kali-et-circulaires-sont-disponibles-en-open-data-sur-data-gouv-fr-sous-licence-ouverte/>
- FDA — Website policies : <https://www.fda.gov/about-fda/about-website/website-policies>
- openFDA — Terms : <https://open.fda.gov/terms/>
