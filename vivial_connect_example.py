#### 
## vivial_connect_example.py
# 
# Send an example message via the Vivial Connect API.
# For usage information, run from your command line like:
#
# python vivial_connect_example.py -h
#
####

import sys
import hmac
import json
import hashlib
import datetime
import argparse

try:
    import urlparse
    from urllib import quote as url_quote
except ImportError:
    import urllib.parse as urlparse
    from urllib.parse import quote as url_quote

import requests

if sys.version_info >= (3, 0):
    def str_to_bytes(s):
        if isinstance(s, str):
            return s.encode()
        return s
else:
    def str_to_bytes(s):
        if isinstance(s, unicode):
            return s.encode()
        return s

ISO_8601_BASIC_FORMAT = '%Y%m%dT%H%M%SZ'
HTTP_TIME_FORMAT = '%a, %d %b %Y %H:%M:%S GMT'

API_KEY = ''
API_SECRET = ''
ACCOUNT_ID = ''
BASE_URL = 'https://api.vivialconnect.net/api/v1.0'

class HmacAuth(object):

    @classmethod
    def sign_request(cls, endpoint_url, http_method,
                     headers, query_params, body,
                     api_key, api_secret, iso_8601):
        body = body or ''
        body_hash = hashlib.sha256(str_to_bytes(body)).hexdigest()
        canon_header_names = cls.build_canon_header_names(headers)
        canon_headers = cls._build_canon_headers(headers)

        # If any query string parameters have been supplied, canonicalize them
        canon_query_params = cls._build_canon_query_string(query_params)
        parsed_url = urlparse.urlparse(endpoint_url)
        canonical_request = (
            http_method.upper() + '\n' +
            iso_8601 + '\n' +
            cls._uri_encode(parsed_url.path, encode_slash=False) + '\n' +
            canon_query_params + '\n' +
            canon_headers + '\n' +
            canon_header_names + '\n' +
            body_hash)
        signature = cls._sign(canonical_request, api_secret)

        # Generate authorization header
        return 'HMAC ' + api_key + ":" + signature

    @classmethod
    def build_canon_header_names(cls, headers):
        api_hmac_used_signed_headers = []
        for key in headers.keys():
            api_hmac_used_signed_headers.append(key.lower())
        api_hmac_used_signed_headers.sort()
        return ';'.join(api_hmac_used_signed_headers).lower()

    @classmethod
    def _sign(cls, data, api_secret):
        h = hmac.new(str_to_bytes(api_secret), b'', hashlib.sha256)
        h.update(str_to_bytes(data))
        return h.hexdigest()

    @classmethod
    def _build_canon_query_string(cls, params=None):
        if params is None:
            return ''
        for item in params:
            canonical_query_string.append(
                self._uri_encode(item[0], encode_slash=True) +
                '=' +
                self._uri_encode(item[1], encode_slash=True))
        canonical_query_string.sort()
        return '&'.join(canonical_query_string)

    @classmethod
    def _uri_encode(cls, data, encode_slash=False):
        safe = '_-~.'
        if not encode_slash:
            safe += '/'
        return url_quote(data, safe=safe)

    @classmethod
    def _build_canon_headers(cls, headers):
        canonical_headers = []
        for key in headers.keys():
            canonical_headers.append(key.lower() + ':' + headers[key])
        canonical_headers.sort()
        return '\n'.join(canonical_headers)

def main():
    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers(help='commands', dest='method')
    get_parser = subparsers.add_parser('GET',
                                       help='Run GET example (get messages)')
    post_parser = subparsers.add_parser('POST',
                                        help='Run POST example (send message)')
    post_parser.add_argument('-b', '--message-body',
                             help='Message body', required=True)
    post_parser.add_argument('-f', '--from-number',
                             help='From number', required=True)
    post_parser.add_argument('-t', '--to-number',
                             help='To number', required=True)
    args = parser.parse_args()

    datenow = datetime.datetime.utcnow()

    headers = {}
    headers['Accept'] = 'application/json'
    headers['Date'] = datenow.strftime(HTTP_TIME_FORMAT)

    url = BASE_URL + '/accounts/' + ACCOUNT_ID + '/messages.json'
    parsed_url = urlparse.urlparse(BASE_URL)
    headers['Host'] = parsed_url.hostname + (
        ':' + str(parsed_url.port) if parsed_url.port else '')

    # Prepare body for http request. Body is empty for GET request
    if args.method == 'POST':
        headers['Content-Type'] = 'application/json'
        body = json.dumps({'message': {'from_number': args.from_number,
                                       'to_number': args.to_number,
                                       'body': args.message_body}})
    else:
        body = ''

    iso_8601 = datenow.strftime(ISO_8601_BASIC_FORMAT)
    # Generate signature using Hmac Authentication
    authorization = HmacAuth.sign_request(url, args.method,
                                          headers, None, body,
                                          API_KEY, API_SECRET, iso_8601)

    canon_header_names = HmacAuth.build_canon_header_names(headers)
    headers['X-Auth-SignedHeaders'] = canon_header_names

    # Place the computed signature into a formatted 'Authorization' header
    headers['Authorization'] = authorization
    headers['X-Auth-Date'] = iso_8601

    response = requests.request(args.method, url, headers=headers, data=body)
    print(response.content)

if __name__ == '__main__':
    main()

