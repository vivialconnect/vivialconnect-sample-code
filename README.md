# Vivial Connect Code Samples

A collection of basic examples of how to access the Vivial Connect API in several languages. Insert your API key, API secret, and your account id before running these examples.

### A Note About HMAC

These examples utilize HMAC authentication. HMAC is useful because it verifies both the authentication and the data-integrity of each request. We chose to use HMAC for these examples because it illustrates the most secure approach to communicate with the Vivial Connect API. However, HMAC can be difficult to implement from scratch.

OAuth Personal Tokens are the recommended authentication method for users who choose not to utilize one of our SDKs. Personal Tokens are secure and easy to implement. See the Vivial Connection API documentation for more information.
