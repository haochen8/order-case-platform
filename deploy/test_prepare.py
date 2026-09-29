import unittest
from prepare import origin, host, realm

class ReleaseInputs(unittest.TestCase):
    def test_only_exact_https_origins(self):
        for value in ['http://ui.example.com','https://ui.example.com/path','https://x@ui.example.com','https://*.example.com','https://ui.example.com?x=1','https://ui.example.com:5173']:
            with self.subTest(value=value), self.assertRaises(ValueError): origin(value)
        self.assertEqual(origin('https://ui.example.com/'),'https://ui.example.com')
    def test_host_rejects_injection_and_wildcards(self):
        for value in ['*', 'api.example.com\nOTHER=value','https://api.example.com','api.example.com:80']:
            with self.subTest(value=value), self.assertRaises(ValueError): host(value)
    def test_realm_has_no_local_accounts_or_password_grant(self):
        r=realm('https://ui.example.com')
        self.assertNotIn('users',r)
        self.assertEqual(r['sslRequired'],'all')
        self.assertFalse(r['registrationAllowed'])
        self.assertEqual(len(r['clients']),1)
        c=r['clients'][0]
        self.assertFalse(c['directAccessGrantsEnabled'])
        self.assertEqual(c['redirectUris'],['https://ui.example.com/auth/callback'])
        self.assertEqual(c['webOrigins'],['https://ui.example.com'])
        self.assertEqual(c['attributes']['pkce.code.challenge.method'],'S256')
        self.assertEqual(c['attributes']['post.logout.redirect.uris'],'https://ui.example.com/')
if __name__=='__main__': unittest.main()
