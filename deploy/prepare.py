#!/usr/bin/env python3
"""Generate local release inputs; never contacts or deploys to a host."""
import argparse
import json
import os
from pathlib import Path
import re
import secrets
from urllib.parse import urlsplit

ROOT = Path(__file__).resolve().parent

def origin(value):
    u = urlsplit(value)
    if u.scheme != 'https' or not u.hostname or u.username or u.password or u.query or u.fragment or u.path not in ('', '/') or u.port not in (None,443):
        raise ValueError('Frontend must be an exact HTTPS origin without path or credentials')
    host(u.hostname)
    return 'https://' + u.netloc

def host(value):
    if not re.fullmatch(r'[a-z0-9](?:[a-z0-9.-]*[a-z0-9])?',value) or '.' not in value or '..' in value:
        raise ValueError('Use a DNS hostname without scheme, port, path or wildcards')
    return value

def realm(frontend):
    local=json.loads((ROOT.parent/'dev/keycloak-realm.json').read_text())
    web=next(c for c in local['clients'] if c['clientId']=='case-platform-web')
    web['redirectUris']=[frontend+'/auth/callback']
    web['webOrigins']=[frontend]
    web['attributes']['post.logout.redirect.uris']=frontend+'/'
    return {'realm':'case-platform','enabled':True,'sslRequired':'all',
            'registrationAllowed':False,'resetPasswordAllowed':False,
            'bruteForceProtected':True,'roles':local['roles'],'clients':[web]}

def main():
    p=argparse.ArgumentParser(description=__doc__)
    for name in ['api-host','auth-host','frontend-origin','email','api-image']:
        p.add_argument('--'+name,required=True)
    a=p.parse_args()
    api,auth=host(a.api_host),host(a.auth_host)
    front=origin(a.frontend_origin)
    if api==auth or urlsplit(front).hostname in (api,auth):
        p.error('Use distinct frontend, API and identity hostnames')
    if not re.fullmatch(r'[A-Za-z0-9._+@-]+',a.email) or '@' not in a.email:
        p.error('Use a valid email address')
    if not re.fullmatch(r'[A-Za-z0-9./_:@-]+',a.api_image):
        p.error('Invalid image reference')
    env=ROOT/'.env'; out=ROOT/'generated/case-platform-realm.json'
    if env.exists() or out.exists():
        p.error('Inputs already exist; review/edit them explicitly rather than overwrite secrets')
    values={'API_HOST':api,'AUTH_HOST':auth,'FRONTEND_ORIGIN':front,'ACME_EMAIL':a.email,
            'API_IMAGE':a.api_image,'ADMIN_USERNAME':'release-admin',
            'ADMIN_PASSWORD':secrets.token_urlsafe(32),'APP_DB_PASSWORD':secrets.token_urlsafe(32),
            'IDENTITY_DB_PASSWORD':secrets.token_urlsafe(32)}
    os.umask(0o077)
    out.parent.mkdir(parents=True,exist_ok=True)
    out.write_text(json.dumps(realm(front),indent=2)+'\n')
    env.write_text(''.join(k+'='+v+'\n' for k,v in values.items()))
    print('Created private deploy/.env and generated realm. No users, local passwords or CLI grant client included.')
if __name__=='__main__': main()
