1. REST_OIDCProvider
- System level table
- "Rest OpenID Connect Provider" window and menu
- With record for Azure, Amazon Cognito, Google and Keycloak
- Only Keycloak and Amazon Cognito implemented for now

2. REST_OIDCService
- System and Tenant level table
- "Rest OpenID Connect Service" window and menu (Advanced)
- Tenant level configuration for OpenID Connect provider (System level is for System tenant configuration)
- Unique Identifier: Issue URL + Audience

3. Amazon Cognito and iDempiere mapping
- User pool name = iDempiere tenant (AD_Client.Value)
- App client name = iDempiere application end point (Web Client, Rest API, etc)
- Group = iDempiere tenant role (AD_Role.Name) without space
- User name = iDempiere tenant user (AD_User.Name)
- User > Email address = iDempiere tenant user (AD_User.Email)

4. HTTP header
- X-ID-Token is required to get custom attributes (X-ID-Organization, X-ID-Role, X-ID-Warehouse, X-ID-Language) and role mapping from Amazon Cognito
- Role mapping is not use if "Authorization" is turn off at "Rest OpenID Connect Service". Use http header as replacement.
- When user has access to > 1 role and/or organization, use http header to set selected role/organization.
- X-ID-Organization = AD_Org.Value
- X-ID-Role = AD_Role.Name
- X-ID-Language = AD_Language
- X-ID-Warehouse = M_Warehouse.Name

5. Mapping or HTTP header for Role is not needed if user has access to only 1 tenant role.