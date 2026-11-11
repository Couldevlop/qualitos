# ADR 0112 — Les règles de Nelson détectées sur la carte de contrôle de l'étude

- **Statut** : Accepté
- **Date** : 2026-11-11
- **Owners** : @Couldevlop
- **Portée** : apps/web (`features/spc/capability` : `spc-capability.nelson.ts`,
  `spc-capability.charts.ts`, `ControlChartComponent`, `CapabilityStudyComponent`)
- **Complète** : ADR 0018 (les huit règles de Nelson dans `ai-service`) et
  ADR 0096 (études de capabilité, cartes X̄ – R / X̄ – S). Aucune des deux n'est
  révisée : les règles restent calculées au même endroit, par le même chemin.

## Contexte

Les huit règles de Nelson existent depuis l'ADR 0018 et tournent dans
`ai-service` (NumPy pur), derrière `POST /api/v1/ai/spc/analyze`. Mais un seul
écran les montrait : « Analyses IA », où l'on colle une série à la main ou l'on
désigne un KPI.

L'écran où un qualiticien lit réellement une carte de contrôle — la fiche
d'étude de capabilité, avec sa grille de mesures, ses limites et son Cpk — n'en
savait rien. Elle ne signalait que la règle 1, et seulement par accident : un
point au-delà de l'UCL se voit sans statistique. Les sept autres — neuf points
du même côté, six en tendance, deux sur trois au-delà de 2σ — c'est-à-dire
**tout ce qui se dérègle sans encore sortir des limites**, restaient invisibles
là où la décision se prend.

## Décision

**La carte X̄ de l'étude est analysée automatiquement**, sans bouton à trouver :
une détection qu'il faut déclencher ne tourne jamais. L'analyse repart quand le
**tracé** change (série ou limites), jamais quand la fiche change — corriger la
référence de la pièce ne consomme aucun appel IA. L'attente est de 600 ms, une
seule analyse est en vol, et une réponse en retard sur une série abandonnée est
jetée (`switchMap`).

**La baseline est celle de la carte, jamais une estimation.** L'appel porte
`center = X̿` et `σ = (UCL − X̿) / 3` — le σ que les facteurs A2/A3 de l'étude
ont déjà posé. Laisser le service estimer ses propres limites sur les moyennes
(étendue mobile, d2 pour n = 2) lui ferait juger la série contre des limites
**invisibles à l'écran** : il désignerait des points qui ne sortent pas des
limites tracées juste à côté. Un faux signal, pas une détection.

**La carte de dispersion (R ou S) est exclue.** Les règles de série supposent
une loi symétrique autour de la ligne centrale ; la distribution d'une étendue
ou d'un écart-type ne l'est pas. Les appliquer là produirait des alertes dont la
statistique ne répond pas. La portée est écrite dans l'écran, sous le titre du
panneau.

**Ce qui est détecté est posé sur le graphe**, pas seulement listé : halo et
abrégé (« N2 », « N5 ») sur chaque point visé, infobulle nommant ses règles, et
les frontières ±1σ / ±2σ tracées sur cette carte — sans elles, un point marqué
« 2 sur 3 au-delà de 2σ » n'est pas lisible. Sous la carte, une ligne par règle
déclenchée : son titre, sa sévérité, et les **numéros de sous-groupes**
concernés, jamais les rangs internes de la série envoyée.

**La détection est un plus, pas une dépendance.** Service indisponible, quota
atteint, réponse mal formée : l'écran le dit, propose de relancer, et le reste
de l'étude — limites, Cp, Cpk, verdict — demeure exact. Une panne IA n'emporte
pas un calcul de capabilité.

**L'IA signale, l'humain décide** (§12.3) : le panneau le rappelle, et aucune
règle déclenchée ne change le statut du procédé de lui-même.

## Conséquences

- Un dérèglement qui n'est pas encore une sortie de limites se voit sur l'écran
  où l'on décide, à l'instant où la grille se remplit.
- Les sous-groupes incomplets n'ont pas de moyenne : ils sont omis de la série
  envoyée, et les règles de série traitent donc comme consécutifs deux relevés
  séparés par un trou. C'est l'ordre des prélèvements qui compte, et il est
  conservé ; le chemin de retour (`columns`) rend à chaque rang son numéro de
  sous-groupe.
- La géométrie des cartes ne détecte rien : elle place ce qu'on lui donne. Les
  règles restent calculées à un seul endroit, côté serveur, et restent testables
  sans navigateur.
- Un appel IA par série modifiée, au plus un toutes les 600 ms et par étude : le
  quota par client (ADR 0017) reste tenu.
