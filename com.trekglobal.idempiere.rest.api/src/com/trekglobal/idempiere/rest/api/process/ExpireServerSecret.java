package com.trekglobal.idempiere.rest.api.process;

import java.util.UUID;

import org.compiere.model.MSysConfig;
import org.compiere.process.SvrProcess;
import org.compiere.util.CacheMgt;
import org.compiere.util.DB;

import com.trekglobal.idempiere.rest.api.v1.jwt.SysConfigTokenSecretProvider;

/**
 * 
 * @author matheus.marcelino
 *
 */
@org.adempiere.base.annotation.Process
public class ExpireServerSecret extends SvrProcess {

	@Override
	protected void prepare() {
	}

	@Override
	protected String doIt() throws Exception {

		DB.executeUpdateEx("UPDATE AD_SysConfig SET Value = ? WHERE AD_Client_ID = ? AND AD_Org_ID = ? AND Name = ?",
				new Object[] { UUID.randomUUID().toString(), 0, 0, SysConfigTokenSecretProvider.REST_TOKEN_SECRET },
				get_TrxName());
		
		CacheMgt.get().reset(MSysConfig.Table_Name);
		
		DB.executeUpdateEx("UPDATE REST_AuthToken SET IsActive = 'N', IsExpired = 'Y'", get_TrxName());

		return null;
	}

}
