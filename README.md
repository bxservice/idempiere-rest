
# iDempiere Rest API

You can find the documentation on how to use it here: https://bxservice.github.io/idempiere-rest-docs/.

## Projects:
* com.trekglobal.idempiere.extensions.parent - parent pom project
* com.trekglobal.idempiere.rest.api - rest api project
* com.trekglobal.idempiere.extensions.p2 - project to build p2 repository
* iDempiere version - Current Default 12

## Folder layout:
* idempiere
* idempiere-rest
  * com.trekglobal.idempiere.extensions.parent
  * com.trekglobal.idempiere.rest.api
  * com.trekglobal.idempiere.extensions.p2

## Rest Resources
* api/v1/auth - authentication and jwt token
* api/v1/models - working with PO and attachments
* api/v1/windows - working with windows and tabs
* api/v1/forms - working with forms
* api/v1/processes - working with process and reports
* api/v1/files - to access files created by api/v1/processes
* api/v1/reference - get information about reference and lists
* api/v1/caches - get caches info, reset cache
* api/v1/nodes - get nodes info, get log files, reset and rotate logs
* api/v1/servers - servers and schedulers resource
* api/v1/infos - info windows
* api/v1/workflow - get workflow nodes, approve, reject, forward, acknowledge, setuserchoice

## System administrator request headers
The following request headers are only honored for JWT tokens of the System client (`AD_Client_ID=0`) and an administrator user. They are ignored for all other users and clients.

* `X-ID-LogMigrationScript` (`Y`/`N`) - enable/disable generation of database migration scripts for dictionary changes
* `X-ID-AdempiereSys` (`Y`/`N`) - enable/disable AdempiereSys (centralized ID) behavior for system records
* `X-ID-MigrationScriptComment` - ticket or short description used for the generated migration script file. CR/LF and repeated whitespace are normalized to a single line

The migration script file is opened when a dictionary change first generates SQL. When a request supplies the log/comment headers, the generated migration script files are flushed and closed at the end of that request, so the next request starts with its own header values. Within the same authenticated session (`AD_Session`), subsequent requests with the same `X-ID-MigrationScriptComment` (or the same `IDEMPIERE-<number>` ticket, or when the comment is omitted) reuse and append to the same migration script file until the comment changes or the session logs out.

## Testing
* postman/trekglobal-idempiere-rest.postman_collection.json
  * must run "POST api/v1/auth/tokens" and "PUT api/v1/auth/tokens" before you can test other calls. the security token created by the 2 calls is valid for 1 hour.

## p2 deployment
* at idempiere-rest, run mvn verify 
* copy update-rest-extensions.sh to your idempiere instance's root folder
* at your idempiere instance's root folder (for instance, /opt/idempiere), run ./update-rest-extensions.sh <file or url path to com.trekglobal.idempiere.extensions.p2/target/repository>
* for e.g, if your source is at /ws/idempiere-rest, ./update-rest-extensions.sh file:////ws/idempiere-rest/com.trekglobal.idempiere.extensions.p2/target/repository
* if the bundle doesn't auto start after deployment (with STARTING status), at osgi console, run "sta com.trekglobal.idempiere.rest.api" to activate the plugin
