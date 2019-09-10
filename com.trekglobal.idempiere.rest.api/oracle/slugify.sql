CREATE OR REPLACE Function slugify
   ( name_in IN varchar2 )
   RETURN varchar2
IS
   name_out varchar2(2000);

BEGIN

   name_out := lower(regexp_replace(regexp_replace(regexp_replace(name_in, '[^a-z0-9A-Z-_]','-'),'-+$',''),'^-', ''));
   name_out := regexp_replace(name_out, '-{2,}', '-');

   RETURN name_out;

EXCEPTION
WHEN OTHERS THEN
   raise_application_error(-20001,'An error was encountered - '||SQLCODE||' -ERROR- '||SQLERRM);
END;
/
