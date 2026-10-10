#!/usr/bin/env python3
"""Parité du mode léger avec l'installation Kubernetes (ADR 0084).

    compose_parity.py <rendu helm on-premise> <docker-compose.yml>

Chaque variable que le chart rend EN CLAIR pour un service (nom et valeur) doit
se retrouver, identique, dans le service compose du même nom. Les secrets
(envFrom côté chart, ${...} du .env côté compose) ne sont comparés que par leur
présence dans compose. Sans ce banc, une variable ajoutée au chart (un nouveau
réglage du moteur, par exemple) manquerait au mode léger sans que rien ne le
signale — jusqu'à la panne chez un client.

Lecture textuelle volontaire : aucune dépendance (PyYAML) n'est exigée.
"""
import io
import re
import sys

# Les valeurs que l'installateur met dans .env pour le rendu de référence de la CI
# (QOS_HOST=qualitos.example.local, IA dans le serveur, modèle par défaut).
ENV = {
    'QOS_HOST': 'qualitos.example.local',
    'QOS_OLLAMA_BASE_URL': 'http://ollama:11434',
    'QOS_OLLAMA_MODEL': 'hf.co/OpenLLM-France/Lucie-7B-Instruct-v1.1-gguf:Q4_K_M',
    'QOS_OLLAMA_HOSTNAME': 'ollama',
}
SERVICES = ['api-core', 'api-quality-engine', 'api-iot-hub', 'ai-service', 'web']


def helm_env(text):
    found = {}
    for doc in re.split(r'(?m)^---\s*$', text):
        if not re.search(r'(?m)^kind: Deployment\s*$', doc):
            continue
        name = re.search(r'(?m)^metadata:\n(?:  .*\n)*?  name: (\S+)', doc).group(1)
        found[name] = dict(re.findall(r'- name: (\S+)\n\s+value: "(.*)"', doc))
    return found


def unquote(v):
    v = v.strip()
    if len(v) >= 2 and v[0] == v[-1] and v[0] in '"\'':
        return v[1:-1]
    return v


def substitute(v):
    def repl(m):
        var, default = m.group(1), m.group(3)
        if var in ENV:
            return ENV[var]
        return default if default is not None else '<secret:%s>' % var
    return re.sub(r'\$\{(\w+)(:-([^}]*))?\}', repl, v)


def compose_env(text):
    found, svc, in_env = {}, None, False
    in_services = False
    for line in text.split('\n'):
        if re.match(r'^\S', line):
            in_services = line.startswith('services:')
            svc, in_env = None, False
            continue
        if not in_services or not line.strip() or line.lstrip().startswith('#'):
            continue
        indent = len(line) - len(line.lstrip())
        if indent == 2:
            svc, in_env = line.strip().rstrip(':'), False
            found.setdefault(svc, {})
        elif indent == 4:
            in_env = line.strip() == 'environment:'
        elif indent == 6 and in_env and svc:
            key, _, value = line.strip().partition(':')
            found[svc][key] = substitute(unquote(value))
    return found


def main(argv):
    if len(argv) != 3:
        raise SystemExit(__doc__)
    helm = helm_env(io.open(argv[1], encoding='utf-8').read())
    compose = compose_env(io.open(argv[2], encoding='utf-8').read())
    errors = []
    for svc in SERVICES:
        if svc not in helm:
            errors.append('%s : absent du rendu helm' % svc)
            continue
        if svc not in compose:
            errors.append('%s : absent de docker-compose.yml' % svc)
            continue
        for key, value in sorted(helm[svc].items()):
            got = compose[svc].get(key)
            if got is None:
                errors.append('%s : %s manque au mode léger (chart : « %s »)' % (svc, key, value))
            elif got != value:
                errors.append('%s : %s vaut « %s » en mode léger, « %s » dans le chart' % (svc, key, got, value))
        print('%-20s %2d variables du chart retrouvées' % (svc, len(helm[svc])))
    if errors:
        print('\n'.join(errors), file=sys.stderr)
        raise SystemExit(1)
    print('parité : le mode léger porte tous les réglages du chart on-premise')


if __name__ == '__main__':
    main(sys.argv)
