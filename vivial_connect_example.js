/* 
 Requires node version 6+
 Install dependencies (request, moment, crypto-js, url)
 Fill in the empty constants (ln 15 - ln 19)
 Execute the script with the following command: node vivial_connect_example.js
 This will send a message to the 'to_number' you provided
*/

'use strict';

const request = require('request');
const moment = require('moment');
const cryptoJS = require('crypto-js');
const url_lib = require('url').URL;

const base_url = 'https://api.vivialconnect.net/api/v1.0';
const canonicalized_header_names = 'accept;date;host';
const api_key = '';
const api_secret = '';
const account_id = '';
const to_number = '';
const from_number = '';

function signRequest(method, url, data){

    var now = moment.utc();
    var requestTimestamp = now.format('YYYYMMDD[T]HHmmss[Z]');
    var dateForHeader = now.format('ddd, DD MMM YYYY HH:mm:ss [GMT]');
    var SHA256data = cryptoJS.SHA256(data);
    var myURL = new url_lib(url);
    var path = myURL.pathname;
    var sortedParams = '';
    var canonicalizedHeaders = 'accept:application/json' + '\n' + `date:${dateForHeader}` + '\n' +'host:api.vivialconnect.net';

    var canonicalRequest = method + '\n' + 
                         requestTimestamp + '\n' +
                         path + '\n' +
                         sortedParams + '\n' +
                         canonicalizedHeaders + '\n' +
                         canonicalized_header_names + '\n' +
                         SHA256data;


    var signature = cryptoJS.HmacSHA256(canonicalRequest, api_secret);

    var signatureData = {
        'dateForHeader': dateForHeader,
        'requestTimestamp': requestTimestamp,
        'signature': signature
    };

    return signatureData
}

function makeRequest(method, url, data) {
    var signatureData = signRequest(method, url, data);
    var options = {
        'url': url,
        'method': method,
        'headers': {
            'Accept': 'application/json',
            'Date': signatureData.dateForHeader,
            'Host': 'api.vivialconnect.net',
            'Content-Type': 'application/json',
            'X-Auth-Date': signatureData.requestTimestamp,
            'X-Auth-SignedHeaders': 'accept;date;host',
            'Authorization': 'HMAC ' + api_key + ':' + signatureData.signature
        },
        body: data
    };

    request(options, function (error, response, body) {
        console.log('error:', error)
        console.log('statusCode:', response && response.statusCode)
        console.log('body:', body)
    });
}

(function(){
    var data = { 
        'message': {
            'to_number': to_number, 
            'from_number': from_number, 
            'body': 'Hello from the Vivial Connect Node.js example'
        }
    };

    data = JSON.stringify(data)
    var url = `${base_url}/accounts/${account_id}/messages.json`
    makeRequest('POST', url , data)
})();