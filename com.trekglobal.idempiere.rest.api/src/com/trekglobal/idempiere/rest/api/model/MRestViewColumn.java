/**********************************************************************
* This file is part of iDempiere ERP Open Source                      *
* http://www.idempiere.org                                            *
*                                                                     *
* Copyright (C) Contributors                                          *
*                                                                     *
* This program is free software; you can redistribute it and/or       *
* modify it under the terms of the GNU General Public License         *
* as published by the Free Software Foundation; either version 2      *
* of the License, or (at your option) any later version.              *
*                                                                     *
* This program is distributed in the hope that it will be useful,     *
* but WITHOUT ANY WARRANTY; without even the implied warranty of      *
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the        *
* GNU General Public License for more details.                        *
*                                                                     *
* You should have received a copy of the GNU General Public License   *
* along with this program; if not, write to the Free Software         *
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,          *
* MA 02110-1301, USA.                                                 *
*                                                                     *
* Contributors:                                                       *
* - Trek Global Corporation                                           *
* - Heng Sin Low                                                      *
**********************************************************************/
package com.trekglobal.idempiere.rest.api.model;

import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.model.MColumn;
import org.compiere.model.PO;
import org.compiere.util.DB;
import org.compiere.util.DefaultEvaluatee;
import org.compiere.util.DefaultEvaluatee.DataProvider;
import org.compiere.util.Env;
import org.compiere.util.Evaluator;
import org.compiere.util.Util;
import org.idempiere.cache.ImmutablePOSupport;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class MRestViewColumn extends X_REST_ViewColumn implements ImmutablePOSupport {

	private static final long serialVersionUID = -480873135108532861L;

	public MRestViewColumn(Properties ctx, int REST_ViewColumn_ID, String trxName) {
		super(ctx, REST_ViewColumn_ID, trxName);
	}

	public MRestViewColumn(Properties ctx, int REST_ViewColumn_ID, String trxName, String... virtualColumns) {
		super(ctx, REST_ViewColumn_ID, trxName, virtualColumns);
	}

	public MRestViewColumn(Properties ctx, String REST_ViewColumn_UU, String trxName) {
		super(ctx, REST_ViewColumn_UU, trxName);
	}

	public MRestViewColumn(Properties ctx, String REST_ViewColumn_UU, String trxName, String... virtualColumns) {
		super(ctx, REST_ViewColumn_UU, trxName, virtualColumns);
	}

	public MRestViewColumn(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	@Override
	protected boolean beforeSave(boolean newRecord) {
		if (newRecord) {
			if (getSeqNo() <= 0) {
				int seqNo = DB.getSQLValueEx(get_TrxName(), "SELECT Coalesce(Max(SeqNo),0)+10 FROM REST_ViewColumn WHERE REST_View_ID=?", getREST_View_ID());
				setSeqNo(seqNo);
			}
		}
		return true;
	}

	@Override
	public PO markImmutable() {
		if (is_Immutable())
			return this;
		
		makeImmutable();
		return this;
	}

	/**
	 * Is column read only logic satisfied
	 * @param json JSON object for context variable evaluation
	 * @return true if read only logic is satisfied
	 */
	public boolean isReadOnly(JsonObject json) {
		if (Util.isEmpty(getReadOnlyLogic(), true))
			return false;
		
		if (getReadOnlyLogic().startsWith(MColumn.VIRTUAL_UI_COLUMN_PREFIX))
		{
			return Evaluator.parseSQLLogic(getReadOnlyLogic(), Env.getCtx(), 0, -1, MColumn.getColumnName(Env.getCtx(), getAD_Column_ID()));
		}
		else
		{
			DataProvider provider = new DataProvider() {

				@Override
				public Object getValue(String columnName) {
					JsonElement element = json.get(columnName);
					return element == null ? null : element.getAsString();
				}

				@Override
				public Object getProperty(String propertyName) {
					return null;
				}

				@Override
				public MColumn getColumn(String columnName) {
					return null;
				}

				@Override
				public String getTrxName() {
					return null;
				}
				
			};
			DefaultEvaluatee evaluatee = new DefaultEvaluatee(provider);
			return Evaluator.evaluateLogic(evaluatee, getReadOnlyLogic());
		}
	}
	
	/**
	 * Is column mandatory logic satisfied
	 * @param json JSON object for context variable evaluation
	 * @return true if mandatory logic is satisfied
	 */
	public boolean isMandatory(JsonObject json) {
		if (Util.isEmpty(getMandatoryLogic(), true))
			return false;
		
		if (getMandatoryLogic().startsWith(MColumn.VIRTUAL_UI_COLUMN_PREFIX))
		{
			return Evaluator.parseSQLLogic(getMandatoryLogic(), Env.getCtx(), 0, -1, MColumn.getColumnName(Env.getCtx(), getAD_Column_ID()));
		}
		else
		{
			DataProvider provider = new DataProvider() {

				@Override
				public Object getValue(String columnName) {
					JsonElement element = json.get(columnName);
					return element == null ? null : element.getAsString();
				}

				@Override
				public Object getProperty(String propertyName) {
					return null;
				}

				@Override
				public MColumn getColumn(String columnName) {
					return null;
				}

				@Override
				public String getTrxName() {
					return null;
				}
				
			};
			DefaultEvaluatee evaluatee = new DefaultEvaluatee(provider);
			return Evaluator.evaluateLogic(evaluatee, getMandatoryLogic());
		}
	}
}
