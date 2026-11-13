# -*- coding: utf-8 -*-
"""Le client HTTP partagé par les scripts de peuplement.

Pourquoi un module commun. Deux scripts peuplent une instance — les données de
démonstration transverses (`donnees-demo.py`) et le lot d'écarts
(`lot-nc.py`) — et tous deux ont besoin de la même chose : un jeton, des
appels authentifiés, et un relevé de ce qui passe et de ce qui casse. Recopié,
ce client aurait fini par diverger : un en-tête corrigé ici, pas là, et deux
scripts qui n'appellent plus tout à fait la même API.

Pourquoi par l'API et non par du SQL. Une insertion directe en base contourne
exactement ce qu'on veut éprouver — la validation, les transitions d'état, les
règles de tenant, les événements d'audit. Ces scripts créent donc des
enregistrements comme un utilisateur le ferait, et **rapportent chaque refus**.
Un module qui répond 400 ou 500 ici est un module qui répondrait pareil à
l'écran : la sortie vaut autant comme jeu de données que comme relevé de
fonctionnement.

Aucune dépendance hors bibliothèque standard : ces scripts se lancent depuis un
poste d'exploitation, où `pip install` n'est pas toujours une option.
"""
import base64
import hashlib
import json
import urllib.error
import urllib.request


class Client:
    """Appelle l'API et tient le compte de ce qui passe et de ce qui casse."""

    def __init__(self, api, jeton, langue):
        self.api = api.rstrip('/')
        self.jeton = jeton
        self.langue = langue
        self.reussites = []
        self.echecs = []
        self.sujet = self._sujet()
        self.nom = self._nom()

    def _charge(self):
        """La charge utile du jeton, décodée.

        <p>Lue, jamais vérifiée : ce script n'est pas un service, il ne décide
        d'aucun droit à partir de ce contenu. C'est le serveur qui valide la
        signature, et lui seul.
        """
        charge = self.jeton.split('.')[1]
        charge += '=' * (-len(charge) % 4)
        return json.loads(base64.urlsafe_b64decode(charge))

    def _sujet(self):
        """Le `sub` du jeton : plusieurs API attendent un identifiant d'acteur."""
        return self._charge()['sub']

    def _nom(self):
        """Le nom affichable du porteur du jeton.

        <p>Utile partout où une personne se désigne : l'équipe d'un écart, le
        pilote d'une CAPA. Un identifiant de compte ne s'affiche jamais à
        l'écran (ADR 0092) — il faut donc le nom, pas seulement le `sub`.
        """
        c = self._charge()
        return (c.get('name') or c.get('preferred_username')
                or c.get('email') or 'Compte de service')

    def poste(self, chemin, corps, quoi):
        return self._appelle('POST', chemin, corps, quoi)

    def patche(self, chemin, corps, quoi):
        """Plusieurs transitions d'etat sont des PATCH, et non des POST."""
        return self._appelle('PATCH', chemin, corps, quoi)

    def met(self, chemin, corps, quoi):
        """Remplacer une valeur entiere est un PUT : la reponse a une question l'est."""
        return self._appelle('PUT', chemin, corps, quoi)

    def lit(self, chemin, quoi, silencieux=False):
        """Une lecture, pour retrouver les objets auxquels on va se rattacher.

        Un rattachement se fait vers un objet QUI EXISTE : fabriquer un
        identifiant produirait un lien qui ne mene nulle part, et c'est
        precisement ce qu'on veut eprouver.

        <p>`silencieux` sert aux lectures de REPLI, celles dont l'echec n'est
        pas un defaut : le script sait quoi faire sans elles. Les compter parmi
        les refus ferait passer pour casse un appel dont on a prevu l'absence.
        """
        return self._appelle('GET', chemin, None, quoi, silencieux=silencieux)

    def depose(self, chemin, nom_fichier, type_mime, octets, quoi):
        """Depose un fichier : le seul appel qui ne porte pas du JSON.

        <p>Les preuves se deposent en `multipart/form-data`, comme depuis un
        navigateur, et passent par les memes controles — type declare, taille,
        signature binaire, nombre de pieces par dossier. Un refus ici dit donc
        quelque chose de vrai sur le module, pas sur le script.
        """
        type_contenu, corps = multipart('file', nom_fichier, type_mime, octets)
        return self._appelle('POST', chemin, corps, quoi, type_contenu=type_contenu)

    def _appelle(self, methode, chemin, corps, quoi, silencieux=False, type_contenu=None):
        requete = urllib.request.Request(
            self.api + chemin,
            data=self._donnees(corps, type_contenu),
            method=methode,
            headers={
                'Authorization': 'Bearer ' + self.jeton,
                'Content-Type': type_contenu or 'application/json',
                'Accept-Language': self.langue,
            })
        try:
            with urllib.request.urlopen(requete, timeout=60) as reponse:
                contenu = reponse.read().decode('utf8')
                self.reussites.append((quoi, reponse.status))
                if not silencieux:
                    print('  OK   %-46s %s' % (quoi, reponse.status))
                return json.loads(contenu) if contenu else {}
        except urllib.error.HTTPError as erreur:
            detail = erreur.read().decode('utf8', 'replace')[:200]
            if not silencieux:
                self.echecs.append((quoi, erreur.code, detail))
                print('  ECHEC %-45s %s  %s' % (quoi, erreur.code, detail))
            return None
        except Exception as erreur:                     # noqa: BLE001
            if not silencieux:
                self.echecs.append((quoi, 0, str(erreur)[:200]))
                print('  ECHEC %-45s  %s' % (quoi, erreur))
            return None

    @staticmethod
    def _donnees(corps, type_contenu):
        """Le corps de la requete : des octets deja prets, ou du JSON.

        <p>Un depot de fichier arrive ici deja encode — le serialiser en JSON
        le detruirait.
        """
        if corps is None:
            return None
        if type_contenu:
            return corps
        return json.dumps(corps).encode('utf8')

    def bilan(self):
        """Le relevé final, et le code de sortie qu'il mérite."""
        print('\n%d appels passés, %d refusés.' % (len(self.reussites), len(self.echecs)))
        if self.echecs:
            print('\nRefus :')
            for quoi, code, detail in self.echecs:
                print('  %-46s %s  %s' % (quoi, code, detail))
        return 1 if self.echecs else 0


def jeton(keycloak, realm, client_id, utilisateur, secret):
    corps = ('grant_type=password&client_id=%s&username=%s&password=%s'
             % (client_id, utilisateur, secret)).encode('utf8')
    url = '%s/realms/%s/protocol/openid-connect/token' % (keycloak.rstrip('/'), realm)
    with urllib.request.urlopen(urllib.request.Request(url, data=corps), timeout=30) as r:
        return json.load(r)['access_token']


def multipart(nom_champ, nom_fichier, type_mime, octets):
    """Fabrique un corps `multipart/form-data` a une seule piece.

    <p>Ecrit a la main parce que la bibliotheque standard ne sait pas le faire :
    `urllib` poste du formulaire encode en URL, et rien d'autre. La seule
    alternative serait `requests`, c'est-a-dire une dependance a installer sur
    un poste d'exploitation ou `pip` n'est pas toujours disponible.

    Rend le couple (type de contenu a declarer, corps en octets).
    """
    frontiere = '----qualitos' + base64.urlsafe_b64encode(
        hashlib.sha256(octets + nom_fichier.encode('utf8')).digest()[:12]).decode()
    entete = ('--%s\r\n'
              'Content-Disposition: form-data; name="%s"; filename="%s"\r\n'
              'Content-Type: %s\r\n\r\n' % (frontiere, nom_champ, nom_fichier, type_mime))
    corps = entete.encode('utf8') + octets + ('\r\n--%s--\r\n' % frontiere).encode('utf8')
    return 'multipart/form-data; boundary=' + frontiere, corps


def pdf_minimal(titre, lignes):
    """Un PDF d'une page, valide, fabrique sans aucune dependance.

    <p>POURQUOI UN VRAI PDF. Le depot de preuve refuse un fichier dont les
    premiers octets ne correspondent pas au type declare : un `.txt` rebaptise
    serait rejete, et c'est voulu — une piece d'audit qui ment sur sa nature
    n'est pas une piece. Ces preuves-ci sont donc de vrais PDF, courts, avec un
    texte qui dit de quoi ils sont la preuve.

    <p>Le texte est encode en latin-1, l'encodage de la police standard
    utilisee ici : un caractere hors de sa portee devient un point
    d'interrogation plutot que de produire un fichier illisible.
    """
    def echappe(t):
        t = t.encode('latin-1', 'replace').decode('latin-1')
        return t.replace('\\', '\\\\').replace('(', '\\(').replace(')', '\\)')

    flux = ['BT', '/F1 14 Tf', '72 760 Td', '(%s) Tj' % echappe(titre), 'ET',
            'BT', '/F1 10 Tf', '72 730 Td', '14 TL']
    for ligne in lignes:
        flux.append('(%s) Tj T*' % echappe(ligne))
    flux.append('ET')
    contenu = '\n'.join(flux).encode('latin-1')

    objets = [
        b'<< /Type /Catalog /Pages 2 0 R >>',
        b'<< /Type /Pages /Kids [3 0 R] /Count 1 >>',
        b'<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] '
        b'/Resources << /Font << /F1 5 0 R >> >> /Contents 4 0 R >>',
        b'<< /Length %d >>\nstream\n' % len(contenu) + contenu + b'\nendstream',
        b'<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica '
        b'/Encoding /WinAnsiEncoding >>',
    ]

    sortie = bytearray(b'%PDF-1.4\n')
    adresses = []
    for numero, objet in enumerate(objets, start=1):
        adresses.append(len(sortie))
        sortie += b'%d 0 obj\n' % numero + objet + b'\nendobj\n'

    depart = len(sortie)
    sortie += b'xref\n0 %d\n' % (len(objets) + 1)
    sortie += b'0000000000 65535 f \n'
    for adresse in adresses:
        sortie += b'%010d 00000 n \n' % adresse
    sortie += (b'trailer\n<< /Size %d /Root 1 0 R >>\nstartxref\n%d\n%%%%EOF\n'
               % (len(objets) + 1, depart))
    return bytes(sortie)
