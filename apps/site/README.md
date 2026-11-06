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

```python
from PIL import Image
im = Image.open('docs/logo.png').convert('RGB')
im.crop((505, 0, 1045, 540)).resize((256, 256)).save('apps/site/assets/qualitos-marque.png', optimize=True)
im.crop((505, 0, 1045, 540)).resize((64, 64)).save('apps/site/assets/favicon-64.png', optimize=True)
```

## Référencement

`robots.txt` et `sitemap.xml` désignent `https://qualitos.fr/` — **le domaine n'est
pas encore en ligne**. Les trois blocs JSON-LD de `index.html` décrivent
l'organisation, le logiciel et la FAQ. Le bloc FAQ est dérivé des `<details>` de la
page : si une question y change, régénérer le bloc plutôt que de le retoucher, sinon
les deux divergent et la donnée structurée devient fausse.
