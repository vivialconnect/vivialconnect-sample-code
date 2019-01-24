# Vivial Connect Code Samples

A collection of basic examples of how to access the Vivial Connect API in several languages. Insert your API key, API secret, and your account id before running these examples.

### A Note About HMAC

These examples utilize HMAC authentication. HMAC is useful because it verifies both the authentication and the data-integrity of each request. We chose to use HMAC for these examples because it illustrates the most secure approach to communicate with the Vivial Connect API.

That said, HMAC can be difficult to implement from scratch. If you want to use HMAC for your project we *highly recommend* you utilize one of our client libraries to handle authentication. If the client libraries don't meet your needs, and you don't have urgent security concerns, you might be happier choosing Basic Authentication.

See the Vivial Connection API documentation for more information.
