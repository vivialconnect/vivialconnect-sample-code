// Install dependencies (request, moment, crypto-js, url)
// Fill in the empty constants (ln 15 - ln 19)
// Execute the script with the following command: node vivial_connect_example.js
// This will send a message to the "TO_NUMBER" you provided

"use strict"

const request = require('request')
const moment = require('moment')
const cryptoJS = require('crypto-js')
const URL = require('url').URL;

const BASE_URL = 'https://api.vivialconnect.net/api/v1.0'
const CANONICALIZED_HEADER_NAMES = "accept;date;host"
const API_KEY = ''
const API_SECRET = ''
const ACCOUNT_ID = ''
const TO_NUMBER = ''
const FROM_NUMBER = ''

 function signRequest(method, url, data){

  let now = moment.utc()
  let requestTimestamp = now.format('YYYYMMDD[T]HHmmss[Z]')
  let dateForHeader = now.format('ddd, DD MMM YYYY HH:mm:ss [GMT]')
  let SHA256data = cryptoJS.SHA256(data)
  let myURL = new URL(url);
  let path = myURL.pathname
  let sortedParams = ''
  let canonicalizedHeaders = "accept:application/json" + '\n' + `date:${dateForHeader}` + '\n' +"host:api.vivialconnect.net"

  let canonicalRequest = method + '\n' + 
                         requestTimestamp + "\n" +
                         path + '\n' +
                         sortedParams + '\n' +
                         canonicalizedHeaders + '\n' +
                         CANONICALIZED_HEADER_NAMES + '\n' +
                         SHA256data


  let signature = cryptoJS.HmacSHA256(canonicalRequest, API_SECRET)

  let signatureData = {
    dateForHeader: dateForHeader,
    requestTimestamp: requestTimestamp,
    signature: signature
  }

  return signatureData
}

  function clientRequestor(method, url, data) {
    let signatureData = signRequest(method, url, data)
      let options = {
        url: url,
        method: method,
        headers: {
          'Accept': 'application/json',
          'Date': signatureData.dateForHeader,
          'Host': 'api.vivialconnect.net',
          'Content-Type': 'application/json',
          'X-Auth-Date': signatureData.requestTimestamp,
          'X-Auth-SignedHeaders': 'accept;date;host',
          Authorization: 'HMAC ' + API_KEY + ':' + signatureData.signature
        },
        body: data
      }

      request(options, function (error, response, body) {
          console.log('error:', error)
          console.log('statusCode:', response && response.statusCode)
          console.log('body:', body)
      })
  }


(function(){
  let data = { 'message': {'to_number': TO_NUMBER, 'from_number': FROM_NUMBER, 'body': 'Hello from Vivial Connect Node.js example'}}
  data = JSON.stringify(data)
  let url = `${BASE_URL}/accounts/${ACCOUNT_ID}/messages.json`
  clientRequestor("POST", url , data)
})();