1. REST_OIDCProvider
- System level table
- "Rest OpenID Connect Provider" window and menu
- With record for Azure, Amazon Cognito, Google and Keycloak
- Only Keycloak implemented for now

2. REST_OIDCService
- System and Tenant level table
- "Rest OpenID Connect Service" window and menu (Advanced)
- Tenant level configuration for OpenID Connect provider (System level is for System tenant configuration)
- Unique Identifier: Issue URL + Audience

3. Keycloak and iDempiere mapping
- Realm = iDempiere tenant (AD_Client.Value)
- Client = iDempiere application end point (Web Client, Rest API, etc)
- Group = iDempiere organization (AD_Org.Value)
- Client Role = iDempiere tenant role (AD_Role.Name)
- User = iDempiere tenant user (AD_User.Name or AD_User.Email)

4. HTTP header
- Role and organization mapping is not use if "Authorization" is turn off at "Rest OpenID Connect Service". Use http header as replacement.
- When user has access to > 1 role and/or organization, use http header to set selected role/organization.
- X-ID-Organization = AD_Org.Value
- X-ID-Role = AD_Role.Name
- X-ID-Language = AD_Language
- X-ID-Warehouse = M_Warehouse.Name

5. Mapping or HTTP header for Role and Organization is not needed if user has access to only 1 organization and 1 tenant role.
