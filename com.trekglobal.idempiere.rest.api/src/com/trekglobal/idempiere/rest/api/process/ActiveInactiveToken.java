package com.trekglobal.idempiere.rest.api.process;

import org.compiere.process.SvrProcess;

import com.trekglobal.idempiere.rest.api.model.MAuthToken;

/**
 * 
 * @author matheus.marcelino
 *
 */
@org.adempiere.base.annotation.Process
public class ActiveInactiveToken extends SvrProcess {

	@Override
	protected void prepare() {
	}

	@Override
	protected String doIt() throws Exception {
		
		MAuthToken token = new MAuthToken(getCtx(), getRecord_ID(), get_TrxName());
		token.setIsActive(!token.isActive());		
		token.saveEx();
		
		return null;
	}

}
