#!/usr/bin/env python3
"""Exercise the local real-token workflow. Creates one closed case per run."""
import json
import os
import urllib.error
import urllib.parse
import urllib.request

API = os.environ.get('API_BASE_URL', 'http://localhost:8080').rstrip('/')
ISSUER = os.environ.get('OIDC_ISSUER', 'http://localhost:9090/realms/case-platform').rstrip('/')


def token(username, password):
    data = urllib.parse.urlencode({
        'grant_type': 'password', 'client_id': 'case-platform-cli',
        'username': username, 'password': password,
    }).encode()
    with urllib.request.urlopen(ISSUER + '/protocol/openid-connect/token', data, timeout=20) as response:
        return json.load(response)['access_token']


def request(method, path, access_token=None, body=None, expected=200):
    headers = {'Content-Type': 'application/json'}
    if access_token:
        headers['Authorization'] = 'Bearer ' + access_token
    req = urllib.request.Request(API + path, method=method, headers=headers,
                                 data=json.dumps(body).encode() if body is not None else None)
    try:
        with urllib.request.urlopen(req, timeout=20) as response:
            status, data = response.status, response.read()
    except urllib.error.HTTPError as error:
        status, data = error.code, error.read()
    if status != expected:
        raise RuntimeError(f'{method} {path}: expected {expected}, got {status}: {data.decode()}')
    return json.loads(data) if data else None


def main():
    operator = token('operator', 'operator-local-only')
    viewer = token('viewer', 'viewer-local-only')
    request('GET', '/actuator/health')
    request('GET', '/api/cases', expected=401)
    request('GET', '/api/cases', 'invalid-token', expected=401)
    request('POST', '/api/cases', viewer, {'title': 'Forbidden'}, expected=403)
    case = request('POST', '/api/cases', operator,
                   {'title': 'Smoke test: fiber installation'}, expected=201)
    case_path = '/api/cases/' + case['id']
    order = request('POST', '/api/orders', operator,
                    {'caseId': case['id'], 'type': 'FIBER_INSTALL'}, expected=201)
    order_path = '/api/orders/' + order['id']
    request('PUT', case_path, operator, {'version': case['version'], 'status': 'CLOSED'}, expected=409)
    order = request('PUT', order_path, operator, {'version': order['version'], 'status': 'SENT'})
    request('PUT', order_path, operator, {'version': 0, 'status': 'FAILED'}, expected=409)
    request('PUT', order_path, operator, {'version': order['version'], 'status': 'COMPLETED'})
    request('PUT', case_path, operator, {'version': case['version'], 'status': 'CLOSED'})
    history = request('GET', case_path + '/audit', viewer)
    assert history['totalElements'] == 5, history
    assert all(event['actor'] == '06e4e465-6d61-4efa-8e90-309375105026' for event in history['content'])
    schema = request('GET', '/v3/api-docs', viewer)
    assert '/api/orders/{id}' in schema['paths']
    print('PASS: real OIDC tokens, permissions, workflow, conflicts, audit history, and OpenAPI')
    print('Created closed smoke-test case:', case['id'])


if __name__ == '__main__':
    main()
