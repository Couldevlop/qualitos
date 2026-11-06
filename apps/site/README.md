# Site de présentation QualitOS

Site vitrine statique : aucun cadre applicatif, aucune étape de construction.
Ouvrir `index.html`, ou servir le dossier tel quel.

```bash
python -m http.server 4300 --directory apps/site
```

## Ce qu'il contient, et ce qu'il ne contient pas

Pas de pages légales, pas de mentions de société : la structure juridique n'existe
pas encore, et une page « Mentions légales » vide dit au visiteur exactement cela.
Elles s'ajouteront quand il y aura une société à nommer.

Aucun logo client, aucun témoignage, aucun chiffre de référence client : il n'y en a
pas. Les valeurs chiffrées affichées sont des **cibles de spécification** du produit,
et la page le dit à chaque fois.

## Le formulaire de contact

Il n'y a pas encore de serveur. `site.js` lit `data-endpoint` sur le formulaire :

- **endpoint renseigné** → envoi `POST` en JSON, message de confirmation dans la page ;
- **endpoint vide** (cas actuel) → le formulaire compose un courriel vers
  `contact@qualitos.fr` avec les champs saisis et ouvre le client de messagerie.

Dans les deux cas le bouton fait quelque chose. Un formulaire qui ne part nulle part
est pire que pas de formulaire.

## Les images

`assets/qualitos-marque.png` est l'hexagone du logo, découpé une fois pour toutes
depuis `docs/logo.png` (le verrouillage complet, 1,2 Mo). La barre et le pied n'en
montrent que lui : faire télécharger l'affiche entière pour en afficher un sixième
coûtait 1,2 Mo à chaque visite. `assets/og-qualitos.jpg` (1200 × 630) est l'image de
partage, aux dimensions attendues par les réseaux.

Pour les régénérer après un changement de logo :

Les coordonnées ne sont pas devinées : l'hexagone occupe **x 532 à 1028, y 3 à
458** dans `docs/logo.png`, et le mot « QualitOS » commence à **y 460**. Toute
coupe carrée calée sur la largeur de l'hexagone mord donc sur le haut des
lettres — c'est ce qui s'était produit au premier essai, et cela se voyait. On
coupe au plus juste, puis on complète en blanc pour obtenir un carré.

```python
from PIL import Image
im = Image.open('docs/logo.png').convert('RGB')
coupe = im.crop((522, 0, 1038, 459))              # 516 x 459, sans lettrage
carre = Image.new('RGB', (516, 516), '#ffffff')
carre.paste(coupe, (0, (516 - 459) // 2))
carre.resize((256, 256)).save('apps/site/assets/qualitos-marque.png', optimize=True)
carre.resize((64, 64)).save('apps/site/assets/favicon-64.png', optimize=True)
```

Si le logo change, **re-mesurer** la boîte plutôt que reprendre ces nombres :

```python
import numpy as np
m = (np.asarray(im).min(axis=2) < 200)
ys, xs = np.nonzero(m[:460]); print(xs.min(), xs.max(), ys.min(), ys.max())
```

## Référencement

`robots.txt` et `sitemap.xml` désignent `https://qualitos.fr/` — **le domaine n'est
pas encore en ligne**. Les trois blocs JSON-LD de `index.html` décrivent
l'organisation, le logiciel et la FAQ. Le bloc FAQ est dérivé des `<details>` de la
page : si une question y change, régénérer le bloc plutôt que de le retoucher, sinon
les deux divergent et la donnée structurée devient fausse.

## Mise en ligne

Le site est servi par le cluster, dans son propre namespace (`qualitos-site`),
à côté — et non à l'intérieur — de la préproduction. Les deux partagent une
machine et rien d'autre.

Tout est décrit dans [`docs/runbooks/site-qualitos-fr.md`](../../docs/runbooks/site-qualitos-fr.md) :
enregistrements DNS chez OVH, boîte `contact@qualitos.fr`, secret du relais,
certificat, et les vérifications qui prouvent que la chaîne tient.
