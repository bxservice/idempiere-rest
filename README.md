
# Experimental iDempiere Rest API

## Projects:
* com.trekglobal.idempiere.extensions.parent - parent pom project
* com.trekglobal.idempiere.rest.api - rest api project
* iDempiere version - Current Default (i.e 6.2z, the coming 7.0 version)

## Folder layout:
* idempiere
* idempiere-rest
  * com.trekglobal.idempiere.extensions.parent
  * com.trekglobal.idempiere.rest.api
## Rest Resources
* api/v1/auth - authentication and jwt token
* api/v1/models - working with PO and attachments
* api/v1/windows - working with AD_Window and AD_Tab
* api/v1/processes - working with process and reports
* api/v1/files - to access files created by api/v1/processes

## Testing
* postman/trekglobal-idempiere-rest.postman_collection.json
  * must run "POST api/v1/auth/tokens" and "PUT api/v1/auth/tokens" before you can test other calls. the securty token created by the 2 call is valid for 1 hour.

this is consider experimental at this point, so use it at your own risk.
