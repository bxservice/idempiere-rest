
# Experimental iDempiere Rest API

## Projects:
* com.trekglobal.idempiere.extensions.parent - parent pom project
* com.trekglobal.idempiere.rest.api - rest api project
* com.trekglobal.idempiere.extensions.p2 - project to build p2 repository
* iDempiere version - Current Default (i.e 7.1z, the coming 8.1 version)

## Folder layout:
* idempiere
* idempiere-rest
  * com.trekglobal.idempiere.extensions.parent
  * com.trekglobal.idempiere.rest.api
  * com.trekglobal.idempiere.extensions.p2

## Rest Resources
* api/v1/auth - authentication and jwt token
* api/v1/models - working with PO and attachments
* api/v1/windows - working with AD_Window and AD_Tab
* api/v1/processes - working with process and reports
* api/v1/files - to access files created by api/v1/processes
* api/v1/caches - get caches info, reset cache
* api/v1/nodes - get nodes info, get log files, reset and rotate logs.
* api/v1/servers - servers and schedulers resource
* api/v1/infos - info windows

## Testing
* postman/trekglobal-idempiere-rest.postman_collection.json
  * must run "POST api/v1/auth/tokens" and "PUT api/v1/auth/tokens" before you can test other calls. the security token created by the 2 call is valid for 1 hour.

## p2 deployment
* at idempiere-rest, run mvn verify 
* copy update-rest-extensions.sh to your idempiere instance's root folder
* at your idempiere instance's root folder (for instance, /opt/idempiere), run ./update-rest-extensions.sh <file or url path to com.trekglobal.idempiere.extensions.p2/target/repository>
* for e.g, if your source is at /ws/idempiere-rest, ./update-rest-extensions.sh file:////ws/idempiere-rest/com.trekglobal.idempiere.extensions.p2/target/repository
* if the bundle doesn't auto start after deployment (with STARTING status), at osgi console, run "sta com.trekglobal.idempiere.rest.api" to activate the plugin

this is consider experimental at this point, so use it at your own risk.
