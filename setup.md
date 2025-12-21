# Manual Steps for the Initial Setup of the Coffeehouse Example

## Host System

Add the hostname `keycloak` for `127.0.0.1` to `/etc/hosts`.

## APIM Console

Open the APIM Console in the browser and log in as Admin.

### Set Default API URL

The Default API URL is relevant for the correct display of URLs in the Developer Portal.

In the menu, go to `Organization -> Entrypoints & Sharding Tags`.

Set:
* `Default HTTP entrypoint` = `http://localhost:8082`
* `Default TCP port` = `8082`

### Set Up APIs

The following APIs must be configured and deployed in APIM for the example.
* `coffee-menu-api`
* `coffee-order-api`

Perform the following steps for the APIs.
The steps are described exemplarily using `coffee-menu-api`.

In the menu, go to `APIs`, select `+ Add API`, then select `Import V4 API`.

Set:
* `API format` = `OpenAPI specification`
* `File` = `coffeehouse/openapi-spec/coffee-menu-api.yml`
* `Add OpenAPI Specification Validation` = `false` (as this is an Enterprise feature)

Select `Import API`.

---
In the `Coffee Menu API (1.0.0)` menu, go to `Entrypoints`.

Set:
* `Context-path` = `/coffee-menu-api/`

Select `Save changes`.

---
In the `Coffee Menu API (1.0.0)` menu, go to `Endpoints`.

Under `default-group` / `default`, select the action `Edit endpoint`.

Set under `default-group` / `General`:
* `Target url` = `http://coffee-menu-service:8080`

Select `Validate my endpoints`.

---
In the `Coffee Menu API (1.0.0)` menu, go to `Consumers`.

Select `+ Add new plan` / `JWT`.

Set:
* `Name` = `JWT plan`

Select `Next`.

Set:
* `JWKS resolver` = `JWKS_URL`
* `Resolver parameter` = `http://keycloak:8080/realms/coffeehouse/protocol/openid-connect/certs`
* `Extract JWT Claims` = `true`
* `Propagate Authorization header` = `true`
* `Client ID claim` = `azp`
* `Ignore missing CNF` = `true`
* `Selection Rule` = `{#context.attributes['jwt'].claims['iss'] == 'http://keycloak:8080/realms/coffeehouse'}`

Select `Next` / `Create`.  
Under `JWT plan`, select the action `Publish the plan` / `Publish`.

---
In the `Coffee Menu API (1.0.0)` menu, go to `Policies`.

Under `Plan: JWT plan` / `Request phase`, add the `Token Exchange` policy and set:
* `Token Endpoint URL` = `http://keycloak:8080/realms/coffeehouse/protocol/openid-connect/token`
* `Token Exchange Client ID` = `api-gateway`
* `Token Exchange Client Secret` = `api-gateway`
* `Authorized Party Prefix` = `coffee-app-`
* `Matching Audience` = `api-gateway`
* `Not-matching Audience` = ``
* `Not-matching Scope` = ``
* `Target Scope` = `coffee-menu-api`

Select `Add policy`.  
Select `Save`.

---
In the `Coffee Menu API (1.0.0)` menu, go to `Deployment`.

Under `Reporter Settings`, set:
* `Entrypoint` = `active`
* `Endpoint` = `active`
* `Request` = `active`
* `Response` = `active`
* `Headers` = `active`
* `Payload` = `active`

Select `Save`.

Select `Deploy API`.

---
In the `Coffee Menu API (1.0.0)` menu, go to `Configuration`.

Select `Start the API` / `Start`.  
Select `Publish the API` / `Publish`.

### Configure Application for API Gateway

In the menu, go to `Applications`.

Select `+ Add Application`.

Set:
* `Name` = `API-GATEWAY-APP`
* `Description` = `Generische API-Gateway Application für den Zugriff auf alle APIs.`
* `Client ID` = `api-gateway`

Select `Create`.

In the `API-GATEWAY-APP` menu, go to `Subscriptions`.

Select `+ Create a subscription`.  
Select `Coffee Menu API` / `JWT plan` / `Create`.  
Select `Coffee Order API` / `JWT plan` / `Create`.

In the `Coffee Menu API (1.0.0)` menu, go to `Consumers` / `Subscriptions`.  
Select the action `Edit the subscription`.  
Select the action `Validate subscription` / `Validate`.

In the `Coffee Order API (1.0.0)` menu, go to `Consumers` / `Subscriptions`.  
Select the action `Edit the subscription`.  
Select the action `Validate subscription` / `Validate`.

### Configure Application for Coffee App 123

In the menu, go to `Applications`.

Select `+ Add Application`.

Set:
* `Name` = `Coffee App 123`
* `Description` = `Coffee App 123`
* `Client ID` = `coffee-app-123`

Select `Create`.

In the `Coffee App 123` menu, go to `Subscriptions`.

Select `+ Create a subscription`.  
Select `Coffee Menu API` / `JWT plan` / `Create`.  

In the `Coffee Menu API (1.0.0)` menu, go to `Consumers` / `Subscriptions`.  
Select the action `Edit the subscription`.  
Select the action `Validate subscription` / `Validate`.

### Configure Application for Coffee App 234

In the menu, go to `Applications`.

Select `+ Add Application`.

Set:
* `Name` = `Coffee App 234`
* `Description` = `Coffee App 234`
* `Client ID` = `coffee-app-234`

Select `Create`.

In the `Coffee App 234` menu, go to `Subscriptions`.

Select `+ Create a subscription`.  
Select `Coffee Order API` / `JWT plan` / `Create`.

In the `Coffee Order API (1.0.0)` menu, go to `Consumers` / `Subscriptions`.  
Select the action `Edit the subscription`.  
Select the action `Validate subscription` / `Validate`.
