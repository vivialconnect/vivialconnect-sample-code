<?php

/*
 * vivial_connect_example.php
 * 
 * send an example messge via the Vivial Connect API
 *
 * php vivial_connect_example.php
 * 
 */

/******************************************************************************/

/* Enter API Key, Secret and Account ID */
$apiKey = '';
$apiSecret = '';
$accountId = '';

/* enter to number, from number and message text */
$fromNumber = '+1XXXXXXXXXX';
$toNumber = '+1XXXXXXXXXX';
$message = 'Hello World from Vivial Connect';

/******************************************************************************/


$endpointUrl = 'https://api.vivialconnect.net/api/v1.0/accounts/' . $accountId . '/messages.json';

$method = "POST";

$gmt_tz = new DateTimeZone('GMT');
date_default_timezone_set('GMT');
$datenow = new DateTime();
$datenow->setTimezone($gmt_tz);
$requestTimeStamp = HmacAuth::getTimeStamp($datenow);

//Define headers 
$headers = [];
$headers["Date"] = $datenow->format(HmacAuth::$dateStringFormat);
$headers["Accept"] = "application/json";

$body = "";
if ($method == "POST") {
    $headers["Content-Type"] = "application/json";
    $obj = new stdClass();
    $obj->body = $message;
    $obj->from_number = $fromNumber;
    $obj->to_number = $toNumber;

    $result = new stdClass();
    $result->message = $obj;

    $body = json_encode($result);
}

$authorization = HmacAuth::computeSignature($endpointUrl, 
        $method, 
        $headers, 
        NULL, // no query parameters
        $body, 
        $apiKey, 
        $apiSecret,
        $requestTimeStamp);

$canonicalizedHeaderNames = HmacAuth::getCanonicalizeHeaderNames($headers);
$headers["X-Auth-SignedHeaders"] = $canonicalizedHeaderNames;
$headers["Authorization"] = $authorization;        
$headers["X-Auth-Date"] = $requestTimeStamp;


//Make https call
$api = new RestClient();
$response = $api->post($endpointUrl, $body, $headers);
print_r ($response->response);


class HmacAuth {

    public static $scheme = "HMAC";
    public static $timestampDayFormat = "Ymd";
    public static $timestampTimeFormat = "His";
    public static $dateStringFormat = "D, d M Y H:i:s T";

    public static function computeSignature($endpointUrl, 
                                   $httpMethod,
                                   array $headers = NULL,
                                   array $queryParameters = NULL,
                                   $body,
                                   $apiKey,
                                   $apiSecret,
                                   $requestTimeStamp) {
        
        $body = $body == NULL ? "" : $body;
        $bodyHash = hash('sha256', $body);
        
        $canonicalizedHeaderNames = self::getCanonicalizeHeaderNames($headers);
        $canonicalizedHeaders = self::getCanonicalizedHeaderString($headers);
        $canonicalizedQueryParameters = self::getCanonicalizedQueryString($queryParameters);
        $canonicalRequest = self::getCanonicalRequest($endpointUrl, $httpMethod,
                $canonicalizedQueryParameters, $canonicalizedHeaderNames,
                $canonicalizedHeaders, $bodyHash, $requestTimeStamp);
        
        $signature = self::sign($canonicalRequest, $apiSecret, "sha256");
        $authorizationHeader = self::$scheme." ".$apiKey.":".$signature;

        return $authorizationHeader;
    }

    public static function getTimeStamp($datenow) {
        $result = $datenow->format(self::$timestampDayFormat);
        $result .= 'T';
        $result .= $datenow->format(self::$timestampTimeFormat);
        $result .= 'Z';

        return $result;
    }
    
    public static function getCanonicalizeHeaderNames(array $headers) {
        if (empty($headers))
            return '';

        $sortedHeaders = array_map('strtolower', array_keys($headers));
        sort($sortedHeaders);
        
        return implode(';', $sortedHeaders);
    }
    
    public static function getCanonicalizedHeaderString(array $headers) {
        if (empty($headers))
            return '';
        
        // step1: sort the headers by case-insensitive order
        $sortedHeaders = array_keys($headers);
        sort($sortedHeaders);

        // step2: form the canonical header:value entries in sorted order. 
        // Don't add linebreak to last header
        $result = [];
        foreach ($sortedHeaders as &$key) {
            array_push($result, strtolower($key).':'.$headers[$key]);
        }

        return implode("\n", $result);
    }
    
    private static function getCanonicalRequest($endpoint, 
                                         $httpMethod,
                                         $canonicalizedQueryParameters, 
                                         $canonicalizedHeaderNames,
                                         $canonicalizedHeaders, 
                                         $bodyHash, 
                                         $requestTimeStamp) {
        $canonicalRequest =
                        $httpMethod . "\n" .
                        $requestTimeStamp . "\n" .
                        self::getCanonicalizedResourcePath($endpoint) . "\n" .
                        $canonicalizedQueryParameters . "\n" .
                        $canonicalizedHeaders . "\n" .
                        $canonicalizedHeaderNames . "\n" .
                        $bodyHash;
        return $canonicalRequest;
    }
    
    private static function getCanonicalizedResourcePath($endpoint) {
        if (empty($endpoint)) {
            return "/";
        }

        $path = parse_url($endpoint, PHP_URL_PATH);
        if (empty($path)) {
            return "/";
        }
        
        $encodedPath = implode('/', array_map('rawurlencode', explode('/', $path)));
        if (strpos($encodedPath, "/") === 0) {
            return $encodedPath;
        } else {
            return "/".$encodedPath;
        }
    }
    
    private static function getCanonicalizedQueryString(array $parameters = NULL) {
        
        if (empty($parameters)) {
            return "";
        }

        //step1: sorted parameter keys
        $sortedKeys = array_keys($parameters);
        sort($sortedKeys);

        $result = [];
        foreach ($sortedKeys as &$key) {
            array_push($result, rawurlencode($key).'='.rawurlencode($parameters[$key]));
        }

        return implode("&", $result);
    }
    
    private static function sign($stringData, $secret, $algorithm) {
        return hash_hmac($algorithm, utf8_encode($stringData), $secret, false);
    }
}

/**
 * PHP REST Client
 * https://github.com/tcdent/php-restclient
 * (c) 2013-2016 Travis Dent <tcdent@gmail.com>
 */
class RestClient {
    
    public $handle; // cURL resource handle.
    
    // Populated after execution:
    public $response; // Response body.
    public $headers; // Parsed reponse header object.
    public $info; // Response info object.
    public $error; // Response error string.
    public $response_status_lines; // indexed array of raw HTTP response status lines.
    
    // Request methods:
    public function post($url, $parameters=[], $headers=[]){
        return $this->execute($url, 'POST', $parameters, $headers);
    }
    
    public function execute($url, $method, $parameters=[], $headers=[]){
        $client = clone $this;
        $client->url = $url;
        $client->handle = curl_init();
        $curlopt = [
            CURLOPT_HEADER => TRUE, 
            CURLOPT_RETURNTRANSFER => TRUE, 
            CURLOPT_USERAGENT => "PHP RestClient/0.1.5"
        ];
        
        if(count($headers)) {
            $curlopt[CURLOPT_HTTPHEADER] = [];
            foreach($headers as $key => $values){
                foreach(is_array($values)? $values : [$values] as $value){
                    $curlopt[CURLOPT_HTTPHEADER][] = sprintf("%s:%s", $key, $value);
                }
            }
        }
                
        if(is_array($parameters)){
            $parameters_string = $client->format_query($parameters);
        }
        else
            $parameters_string = (string) $parameters;
        
        if(strtoupper($method) == 'POST'){
            $curlopt[CURLOPT_POST] = TRUE;
            $curlopt[CURLOPT_POSTFIELDS] = $parameters_string;
        }

        $curlopt[CURLOPT_URL] = $client->url;
        

        curl_setopt_array($client->handle, $curlopt);
        
        $client->parse_response(curl_exec($client->handle));
        $client->info = (object) curl_getinfo($client->handle);
        $client->error = curl_error($client->handle);
        
        curl_close($client->handle);
        return $client;
    }
    
    public function format_query($parameters, $primary='=', $secondary='&'){
        $query = "";
        foreach($parameters as $key => $values){
            foreach(is_array($values)? $values : [$values] as $value){
                $pair = [urlencode($key), urlencode($value)];
                $query .= implode($primary, $pair) . $secondary;
            }
        }
        return rtrim($query, $secondary);
    }
    
    public function parse_response($response){
        $headers = [];
        $this->response_status_lines = [];
        $line = strtok($response, "\n");
        do {
            if(strlen(trim($line)) == 0){
                // Since we tokenize on \n, use the remaining \r to detect empty lines.
                if(count($headers) > 0) break; // Must be the newline after headers, move on to response body
            }
            elseif(strpos($line, 'HTTP') === 0){
                // One or more HTTP status lines
                $this->response_status_lines[] = trim($line);
            }
            else { 
                // Has to be a header
                list($key, $value) = explode(':', $line, 2);
                $key = trim(strtolower(str_replace('-', '_', $key)));
                $value = trim($value);
                
                if(empty($headers[$key]))
                    $headers[$key] = $value;
                elseif(is_array($headers[$key]))
                    $headers[$key][] = $value;
                else
                    $headers[$key] = [$headers[$key], $value];
            }
        } while($line = strtok("\n"));
        
        $this->headers = (object) $headers;
        $this->response = strtok("");
    }
    
}

?>