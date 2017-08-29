# Install required gems
# Fill in empty constants (ln 14 - ln 18)
# Execute the script with the following command: ruby vivial_connect_example.rb
# This will send a message to the 'to_number' you provided

require 'json'
require 'faraday'
require 'digest'
require 'addressable'
require 'openssl'
require 'pp'

BASE_API_PATH = 'https://api.vivialconnect.net/api/v1.0/'
API_KEY =
API_SECRET =
ACCOUNT_ID =
TO_NUMBER =
FROM_NUMBER =


def build_canonical_request(http_verb, url, request_timestamp, canonicalized_headers, canonicalized_header_names, data={})
  canonicalized_query_string = ''
  canonical_request = http_verb + "\n" + request_timestamp + "\n" + Addressable::URI.encode(url.path) +
                      "\n" + canonicalized_query_string + "\n" + canonicalized_headers + "\n" +
                       canonicalized_header_names +  "\n" + Digest::SHA256.hexdigest(data.to_s)
end

def sign_request(http_verb, url, data={})
  set_request_timestamp
  set_date_for_date_header
  canonicalized_headers = "accept:application/json" + "\n" + "date:#{date_for_date_header}" + "\n" +"host:api.vivialconnect.net"
  canonicalized_header_names = "accept;date;host"
  canonical_request = build_canonical_request(http_verb, url, request_timestamp, canonicalized_headers, canonicalized_header_names, data)
  set_hmac_sha256(canonical_request)
end

def create_request_headers
  headers = {}
  headers['Content-Type'] = 'application/json'
  headers['Host'] = 'api.vivialconnect.net'
  headers['X-Auth-Date'] = request_timestamp
  headers['X-Auth-SignedHeaders'] = 'accept;date;host'
  headers['Authorization'] = "HMAC" + " " + API_KEY + ":" + hmac_sha256
  headers['Date'] = date_for_date_header
  headers['Accept'] = 'application/json'
  headers
end

def make_request(request_method, url, data={})
  base_path = BASE_API_PATH + "accounts/#{ACCOUNT_ID}"
  url = Addressable::URI.parse(base_path + url)
  sign_request(request_method, url, data)
  headers = create_request_headers
  request_method = request_method.downcase.to_sym
  connection = Faraday.new
  response = connection.run_request(request_method, url, data.to_s, headers)
end

def set_hmac_sha256(canonical_request)
  @hmac_sha256 = OpenSSL::HMAC.hexdigest('SHA256', API_SECRET, canonical_request)
end

def hmac_sha256
  @hmac_sha256
end

def set_request_timestamp
  @request_timestamp = Time.now.utc.strftime('%Y%m%dT%H%M%SZ')
end

def request_timestamp
  @request_timestamp
end

def set_date_for_date_header
  @date_for_date_header = Time.now.utc.strftime('%a, %d %b %Y %H:%M:%S GMT')
end

def date_for_date_header
  @date_for_date_header
end

def message_data(to_number, from_number)
  data = {}
  data['message'] = {}
  data['message']['to_number'] = to_number
  data['message']['from_number'] = from_number
  data['message']['body'] = "Hello World, from Vivial Connect"
  data.to_json
end

data = message_data(TO_NUMBER, FROM_NUMBER)
send_message_path = "/messages.json"
response = make_request("POST", send_message_path, data)
puts response.marshal_dump
