package com.trekglobal.idempiere.rest.api.model;

import java.util.Objects;
import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.adempiere.base.annotation.Callout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.compiere.util.Env;

@Callout(tableName = "REST_View", columnName = "AD_Table_ID")
public class CalloutRestView implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value, Object oldValue) {
		if (!mTab.isNew()) {
			Object originalValue = DB.getSQLValueEx(null, "SELECT AD_Table_ID FROM REST_View WHERE REST_View_ID=?", mTab.getKeyID(mTab.getCurrentRow()));
			if (!Objects.equals(originalValue, value)) {
				Query query = new Query(Env.getCtx(), MRestViewColumn.Table_Name, "REST_View_ID=?", null);
				int count = query.setParameters(mTab.getValue(MRestView.COLUMNNAME_REST_View_ID)).count();
				if (count > 0) {
					mTab.fireDataStatusEEvent("REST_ChangeViewTableWarning", null, false);
				}
			}
		}
		return null;
	}

}
