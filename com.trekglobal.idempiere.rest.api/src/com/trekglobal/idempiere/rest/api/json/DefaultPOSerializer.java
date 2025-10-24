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
package com.trekglobal.idempiere.rest.api.json;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.GridField;
import org.compiere.model.GridFieldVO;
import org.compiere.model.MColumn;
import org.compiere.model.MSysConfig;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.POInfo;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.osgi.service.component.annotations.Component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.trekglobal.idempiere.rest.api.model.MRestView;
import com.trekglobal.idempiere.rest.api.model.MRestViewColumn;

/**
 * 
 * Default serializer implementation for PO
 * @author hengsin
 *
 */
@Component(name = "com.trekglobal.idempiere.rest.api.json.DefaultPOSerializer", service = IPOSerializerFactory.class, 
	property = {"service.ranking:Integer=0"}, immediate = true)
public class DefaultPOSerializer implements IPOSerializer, IPOSerializerFactory {

	/**
	 * default constructor
	 */
	public DefaultPOSerializer() {
	}

	@Override
	public JsonObject toJson(PO po, String[] includes, String[] excludes) {
		return toJson(po, null, includes, excludes);				
	}
	
	@Override
	public JsonObject toJson(PO po, MRestView view, String[] includes, String[] excludes) {
		JsonObject json = new JsonObject();
		MTable table = MTable.get(po.get_Table_ID());
		String keyColumn = null;
		String uidColumn = po.getUUIDColumnName();
		if (table.isIDKeyTable()) {
			//always include id and uid
			String[] keyColumns = po.get_KeyColumns();
			if (keyColumns != null && keyColumns.length == 1) {
				json.addProperty("id", po.get_ID());
				keyColumn = keyColumns[0];
			}
		} else {
			keyColumn = uidColumn;
		}
		if (po.get_ColumnIndex(uidColumn) >= 0) {
			String uid = po.get_ValueAsString(uidColumn);
			if (!Util.isEmpty(uid, true)) {
				json.addProperty("uid", uid);
			}
		}		
		//loops through columns from view or PO definition
		MRestViewColumn[] viewColumns = view != null ? view.getColumns() : null;
		POInfo poInfo = POInfo.getPOInfo(Env.getCtx(), po.get_Table_ID());
		int count = view != null ? viewColumns.length : poInfo.getColumnCount(); 
		for(int i=0;i < count ; i++) {
			String columnName = view != null ? MColumn.getColumnName(Env.getCtx(), viewColumns[i].getAD_Column_ID()) : poInfo.getColumnName(i);
			if (keyColumn != null && keyColumn.equalsIgnoreCase(columnName))
				continue;
			if (uidColumn != null && uidColumn.equalsIgnoreCase(columnName))
				continue;
			if (!include(columnName, includes))
				continue;
			if (exclude(columnName, excludes))
				continue;
			int columnId = view != null ? viewColumns[i].getAD_Column_ID() : poInfo.getAD_Column_ID(columnName); 
			MColumn column = MColumn.get(Env.getCtx(), columnId);
			if (column.isSecure() || column.isEncrypted())
				continue;
			if (!RestUtils.hasRoleColumnAccess(po.get_Table_ID(), column.getAD_Column_ID(), true))
				continue;
			//find view column (if using view)
			MRestViewColumn viewColumn = null;
			if (viewColumns != null) {
				Optional<MRestViewColumn> optional = Arrays.stream(viewColumns).filter(e -> e.getAD_Column_ID() == column.getAD_Column_ID()).findFirst();
				if (optional.isPresent())
					viewColumn = optional.get();
				else
					continue;
			}

			Object value ;
			if (column.isTranslated())
				value = po.get_Translation(column.getColumnName());
			else
				value = po.get_Value(column.getColumnName());

			if (value != null) {
				//get property name from view definition or default conversion
				String propertyName = viewColumn != null ? viewColumn.getName()
						: MSysConfig.getBooleanValue("REST_COLUMNNAME_TOLOWERCASE", false) ? TypeConverterUtils.toPropertyName(columnName) : columnName;
				Object jsonValue = TypeConverterUtils.toJsonValue(column, value, viewColumn != null && viewColumn.getREST_ReferenceView_ID() > 0 
						? MRestView.get(viewColumn.getREST_ReferenceView_ID()) : null);
				if (jsonValue != null) {					
					JsonObject target = json;
					//rest view support json path mapping to nested json value object
					String[] jsonPath = viewColumns != null ? propertyName.split("[.]") : null;
					if (jsonPath != null && jsonPath.length > 1) {
						for(int p = 0; p < jsonPath.length-1; p++) {
							JsonElement tmp = target.get(jsonPath[p]);
							if (tmp == null) {
								tmp = new JsonObject();
								target.add(jsonPath[p], tmp);
								target = (JsonObject) tmp;
							} else if (!tmp.isJsonObject()) {
								target = null;
								break;
							} else {
								target = tmp.getAsJsonObject();
							}
						}
						if (target == null)
							continue;
						propertyName = jsonPath[jsonPath.length-1];
					}
					if (jsonValue instanceof Number)
						target.addProperty(propertyName, (Number)jsonValue);
					else if (jsonValue instanceof Boolean)
						target.addProperty(propertyName, (Boolean)jsonValue);
					else if (jsonValue instanceof String)
						target.addProperty(propertyName, (String)jsonValue);
					else if (jsonValue instanceof JsonElement)
						target.add(propertyName, (JsonElement) jsonValue);
					else
						target.addProperty(propertyName, jsonValue.toString());					
				}
			}
		}
		if (!exclude("model-name", excludes)) {
			json.addProperty("model-name", poInfo.getTableName().toLowerCase());
			if (view != null) {
				json.addProperty("view-name", view.getName());
			}
		}
		return json;
	}

	@Override
	public PO fromJson(JsonObject json, MTable table) {
		return fromJson(json, table, null);
	}
	
	@Override
	public PO fromJson(JsonObject json, MTable table, MRestView view) {
		PO po = table.isUUIDKeyTable() ? table.getPOByUU(PO.UUID_NEW_RECORD, null) : table.getPO(0, null);
		POInfo poInfo = POInfo.getPOInfo(Env.getCtx(), table.getAD_Table_ID());
		validateJsonFields(json, po, view);
		Set<String> jsonFields = json.keySet();
		List<String> mandatoryColumns = new ArrayList<>();
		//loops through columns from view or PO definition
		MRestViewColumn[] viewColumns = view != null ? view.getColumns() : null;
		int count = view != null ? viewColumns.length : poInfo.getColumnCount(); 
		for(int i = 0; i < count; i++) {
			MRestViewColumn viewColumn = viewColumns != null ? viewColumns[i] : null;
			String columnName = viewColumn != null ? MColumn.getColumnName(Env.getCtx(), viewColumn.getAD_Column_ID()) : poInfo.getColumnName(i);
			MColumn column = table.getColumn(columnName);
			String propertyName = null;
			if (viewColumns != null) {
				propertyName = viewColumns[i].getName();
			} else {
				propertyName = TypeConverterUtils.toPropertyName(columnName);				
			}
			//rest view support json path mapping to nested json value object
			String[] jsonPath = viewColumns != null ? propertyName.split("[.]") : null;
			if (jsonPath != null && jsonPath.length > 1)
				propertyName = jsonPath[0];
			if (!jsonFields.contains(propertyName) && (viewColumns != null || !jsonFields.contains(columnName))) {
				setDefaultValue(po, column, viewColumn, propertyName, json, mandatoryColumns);
				continue;
			}
			
			JsonElement field = json.get(propertyName);
			if (field == null && viewColumns == null)
				field = json.get(columnName);
			if (field == null) {
				setDefaultValue(po, column, viewColumn, propertyName, json, mandatoryColumns);
				continue;
			}
			//nested json value object
			if (jsonPath != null && jsonPath.length > 1) {				
				for(int p = 1; p < jsonPath.length && field != null; p++) {
					if (!field.isJsonObject()) {
						field = null;
						break;
					}
					JsonObject valueObject = field.getAsJsonObject();
					field = valueObject.get(jsonPath[p]);
				}
				if (field == null) {
					setDefaultValue(po, column, viewColumn, propertyName, json, mandatoryColumns);
					continue;
				}
			}
			Object value = TypeConverterUtils.fromJsonValue(column, field, viewColumn != null && viewColumn.getREST_ReferenceView_ID() > 0 
					? MRestView.get(viewColumn.getREST_ReferenceView_ID()) : null);
			if (! isValueUpdated(po.get_ValueOfColumn(column.getAD_Column_ID()), value))
				continue;
			if (viewColumn != null && !Util.isEmpty(viewColumn.getReadOnlyLogic(), true)) {
				if (viewColumn.isReadOnly(json)) {
					setDefaultValue(po, column, viewColumn, propertyName, json, mandatoryColumns);
					continue;
				}
			}
			else if (! isUpdatable(column, false, po)) {
				setDefaultValue(po, column, viewColumn, propertyName, json, mandatoryColumns);
				continue;
			}
			if (   value != null
				&& value instanceof Integer) {
				if (((Integer)value).intValue() < 0 && DisplayType.isID(column.getAD_Reference_ID())) {
					value = null;
				} else if (((Integer)value).intValue() == 0 && DisplayType.isLookup(column.getAD_Reference_ID())) {
					if (! MTable.isZeroIDTable(column.getReferenceTableName()))
						value = null;
				}
			}
			po.set_ValueOfColumn(column.getAD_Column_ID(), value);
			if (value == null && viewColumn != null && viewColumn.isMandatory(json))
				mandatoryColumns.add(propertyName);
		}
		
		if (!mandatoryColumns.isEmpty()) {
			StringBuilder error = new StringBuilder("Mandatory fields missing: ");
			for(String mandatoryColumn : mandatoryColumns) {
				error.append(mandatoryColumn).append(", ");
			}
			error.delete(error.length()-2, error.length());
			throw new AdempiereException(error.toString());
		}
		
		return po;
	}

	/**
	 * Set default value for column
	 * @param po
	 * @param column
	 * @param viewColumn
	 * @param propertyName input property name
	 * @param json input json object
	 * @param mandatoryColumns list to store mandatory columns that are not filled
	 */
	private void setDefaultValue(PO po, MColumn column, MRestViewColumn viewColumn, String propertyName,
			JsonObject json, List<String> mandatoryColumns) {
		boolean set = setDefaultValue(po, column, viewColumn);
		if (!set && viewColumn != null && viewColumn.isMandatory(json))
			mandatoryColumns.add(propertyName);
	}

	@Override
	public PO fromJson(JsonObject json, PO po) {
		return fromJson(json, po, null);
	}
	
	@Override
	public PO fromJson(JsonObject json, PO po, MRestView view) {
		MTable table = MTable.get(Env.getCtx(), po.get_Table_ID());
		POInfo poInfo = POInfo.getPOInfo(Env.getCtx(), table.getAD_Table_ID());
		validateJsonFields(json, po, view);
		Set<String> jsonFields = json.keySet();
		List<String> mandatoryColumns = new ArrayList<>();
		//loops through columns from view or PO definition
		MRestViewColumn[] viewColumns = view != null ? view.getColumns() : null;
		int count = view != null ? viewColumns.length : poInfo.getColumnCount();
		for(int i = 0; i < count; i++) {
			String columnName = viewColumns != null ? MColumn.getColumnName(Env.getCtx(), viewColumns[i].getAD_Column_ID()) : poInfo.getColumnName(i);
			MColumn column = table.getColumn(columnName);
			String propertyName = null;
			if (viewColumns != null) {
				propertyName = viewColumns[i].getName();
			} else {
				propertyName = TypeConverterUtils.toPropertyName(columnName);				
			}
			//rest view support json path mapping to nested json value object
			String[] jsonPath = viewColumns != null ? propertyName.split("[.]") : null;
			if (jsonPath != null && jsonPath.length > 1)
				propertyName = jsonPath[0];
			if (!jsonFields.contains(propertyName) && (viewColumns != null || !jsonFields.contains(columnName)))
				continue;
			JsonElement field = json.get(propertyName);
			if (field == null && viewColumns == null)
				field = json.get(columnName);
			if (field == null)
				continue;
			//nested json value object
			if (jsonPath != null && jsonPath.length > 1) {				
				for(int p = 1; p < jsonPath.length && field != null; p++) {
					if (!field.isJsonObject()) {
						field = null;
						break;
					}
					JsonObject valueObject = field.getAsJsonObject();
					field = valueObject.get(jsonPath[p]);
				}
				if (field == null)
					continue;
			}
			Object value = TypeConverterUtils.fromJsonValue(column, field);
			if (! isValueUpdated(po.get_ValueOfColumn(column.getAD_Column_ID()), value))
				continue;
			MRestViewColumn viewColumn = viewColumns != null ? viewColumns[i] : null;
			if (viewColumn != null && !Util.isEmpty(viewColumn.getReadOnlyLogic(), true)) {
				if (viewColumn.isReadOnly(json)) {
					if (MSysConfig.getBooleanValue("REST_ERROR_ON_NON_UPDATABLE_COLUMN", true))
						throw new AdempiereException("Cannot update column " + viewColumn.getName());
					else
						continue;
				}
			}
			else if (! isUpdatable(column, true, po))
				continue;
			if (   value != null
				&& value instanceof Integer) {
				if (((Integer)value).intValue() < 0 && DisplayType.isID(column.getAD_Reference_ID())) {
					value = null;
				} else if (((Integer)value).intValue() == 0 && DisplayType.isLookup(column.getAD_Reference_ID())) {
					if (! MTable.isZeroIDTable(column.getReferenceTableName()))
						value = null;
				}
			}
			
			if (value == null && viewColumn != null && viewColumn.isMandatory(json))
				mandatoryColumns.add(propertyName);
			else
				po.set_ValueOfColumn(column.getAD_Column_ID(), value);
		}
		
		if (!mandatoryColumns.isEmpty()) {
			StringBuilder error = new StringBuilder("Field is mandatory: ");
			for(String mandatoryColumn : mandatoryColumns) {
				error.append(mandatoryColumn).append(", ");
			}
			error.delete(error.length()-2, error.length());
			throw new AdempiereException(error.toString());
		}
		
		return po;
	}

	/**
	 * Validate if a value has been modified
	 * @param oldValue 
	 * @param newValue 
	 * @return true if it has been modified
	 */
	public static boolean isValueUpdated(Object oldValue, Object newValue) {
		if (oldValue == null && newValue == null)
			return false; // both values are null, nothing to update
		if (   oldValue != null
			&& newValue != null) {
			if (   oldValue.getClass().equals(newValue.getClass())
			    && oldValue.equals(newValue))
				// both objects have the same class and value, nothing to update
				return false;
			if (   oldValue instanceof Integer
				&& newValue instanceof BigDecimal
				&& ((BigDecimal)newValue).intValue() == ((Integer)((Integer) oldValue).intValue()))
				return false;
		}

		return true;
	}

	/**
	 * Validate if a column can be updated
	 * @param column
	 * @param validateUpdateable
	 * @param po 
	 * @param prevValue 
	 * @return true if it can be updated, throws AdempiereException depending on the SysConfig keys REST_ERROR_ON_NON_UPDATABLE_COLUMN and REST_ALLOW_UPDATE_SECURE_COLUMN
	 */
	public static boolean isUpdatable(MColumn column, boolean validateUpdateable, PO po) {
		boolean errorOnNonUpdatable = MSysConfig.getBooleanValue("REST_ERROR_ON_NON_UPDATABLE_COLUMN", true);

		if (validateUpdateable && !column.isUpdateable()) {
			if (errorOnNonUpdatable)
				throw new AdempiereException("Cannot update column " + column.getColumnName());
			else
				return false;
		}
		if (column.isVirtualColumn()) {
			if (errorOnNonUpdatable)
				throw new AdempiereException("Cannot update virtual column " + column.getColumnName());
			else
				return false;
		}
		boolean allowUpdateSecure = MSysConfig.getBooleanValue("REST_ALLOW_UPDATE_SECURE_COLUMN", true);
		if (! allowUpdateSecure) {
			if (column.isSecure() || column.isEncrypted()) {
				if (errorOnNonUpdatable)
					throw new AdempiereException("Cannot update secure/encrypted column " + column.getColumnName());
				else
					return false;
			}
		}

		if (!RestUtils.hasRoleColumnAccess(column.getAD_Table_ID(), column.getAD_Column_ID(), false)) {
			if (errorOnNonUpdatable)
				throw new AdempiereException("No access to update column " + column.getColumnName());
			else
				return false;
		}

		if (! po.is_new()) {
			if (po.get_ColumnIndex("processed") >= 0) {
				if (po.get_ValueAsBoolean("processed")) {
					if (!column.isAlwaysUpdateable()) {
						if (errorOnNonUpdatable)
							throw new AdempiereException("Cannot update " + column.getColumnName() + " on processed record");
						else
							return false;
					}
				}
			}
			if (po.get_ColumnIndex("posted") >= 0) {
				if (po.get_ValueAsBoolean("posted")) {
					if (!column.isAlwaysUpdateable()) {
						if (errorOnNonUpdatable)
							throw new AdempiereException("Cannot update " + column.getColumnName() + " on posted record");
						else
							return false;
					}
				}
			}
		}
		return true;
	}

	final static List<String> ALLOWED_EXTRA_COLUMNS = new ArrayList<>(
			List.of(
					"doc-action",
					"id",
					"identifier",
					"model-name",
					"tableName",
					"uid"
					));

	/**
	 * Validate that all json fields exist as columns and are properly named
	 * @param json
	 * @param po
	 */
	public static void validateJsonFields(JsonObject json, PO po) {
		validateJsonFields(json, po, null);
	}
	
	/**
	 * Validate that all json fields exist as columns and are properly named
	 * @param json
	 * @param po
	 * @param view if not null, validate json fields against view definition
	 */
	public static void validateJsonFields(JsonObject json, PO po, MRestView view) {
		boolean errorOnNonExisting = MSysConfig.getBooleanValue("REST_ERROR_ON_NON_EXISTING_COLUMN", true);
		Set<String> jsonFields = json.keySet();
		if (errorOnNonExisting) {
			for (String jsonField : jsonFields) {
				if (ALLOWED_EXTRA_COLUMNS.contains(jsonField))
					continue;
				JsonElement jsonObj = json.get(jsonField);
				if (jsonObj instanceof JsonArray)
					continue;
				String columnName = jsonField;
				if (view != null) {
					List<String> paths = new ArrayList<String>();
					if (jsonObj.isJsonObject()) {						
						flatten(view, jsonObj, jsonField, paths);
					} else {
						paths.add(jsonField);
					}
					for(String path : paths) {
						columnName = view.toColumnName(path);
						if (columnName == null) {
							String errorPath = path;
							Optional<MRestViewColumn> optional = Arrays.stream(view.getColumns()).filter(e -> e.getName().equalsIgnoreCase(path)).findFirst();
							//handle .id and .identifier for lookup
							if (!optional.isPresent() && (path.endsWith(".id") || path.endsWith(".identifier"))) {
								String p = path.endsWith(".id") ? path.substring(0, path.length()-".id".length()) : path.substring(0, path.length()-".identifier".length());
								optional = Arrays.stream(view.getColumns()).filter(e -> e.getName().equalsIgnoreCase(p)).findFirst();
								if (optional.isPresent()) 
									errorPath = p;
							}
							if (optional.isPresent()) {
								StringBuilder error = new StringBuilder("Wrong name for column ")
										.append(errorPath).append(", you must use ")
										.append(optional.get().getName());
								throw new AdempiereException(error.toString());
							} else {
								throw new AdempiereException("Column " + errorPath + " does not exist");
							}
						}
						int colIdx = po.get_ColumnIndex(columnName);
						if (colIdx < 0)
							throw new AdempiereException("Column " + jsonField + " does not exist");
					}					
					continue;
				}
				int colIdx = po.get_ColumnIndex(columnName);
				if (colIdx < 0)
					throw new AdempiereException("Column " + jsonField + " does not exist");
				columnName = po.get_ColumnName(colIdx);
				
				String propertyName = TypeConverterUtils.toPropertyName(columnName);
				if (! jsonField.equals(propertyName) && !jsonField.equals(columnName))
					throw new AdempiereException("Wrong name for column " + jsonField + ", you must use " + propertyName +
							(propertyName.equals(columnName) ? "" : " or " + columnName));
			}
		}
	}

	private static void flatten(MRestView view, JsonElement jsonElement, String currentPath, List<String> paths) {
        if (jsonElement.isJsonPrimitive() || view.toColumnName(currentPath) != null) {
            paths.add(currentPath);
        } else if (jsonElement.isJsonObject()) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            for (String key : jsonObject.keySet()) {
                String newPath = currentPath.isEmpty() ? key : currentPath + "." + key;
                flatten(view, jsonObject.get(key), newPath, paths);
            }
        }
    }
	
	private boolean exclude(String columnName, String[] excludes) {
		if (excludes == null || excludes.length == 0)
			return false;
		for(String exclude : excludes) {
			if (exclude.equals(columnName))
				return true;
		}
		return false;
	}

	private boolean include(String columnName, String[] includes) {
		if (includes == null || includes.length == 0)
			return true;
		for(String include : includes) {
			if (include.equalsIgnoreCase(columnName))
				return true;
		}
		return false;
	}
	
	private boolean setDefaultValue(PO po, MColumn column, MRestViewColumn viewColumn) {
		if (!column.isVirtualColumn() && (!Util.isEmpty(column.getDefaultValue(), true) || (viewColumn != null && !Util.isEmpty(viewColumn.getDefaultValue(), true)))) {
			// Get window number from ThreadLocal to enable context variable resolution
			int windowNo = RestUtils.getContextWindowNo();
			GridFieldVO vo = GridFieldVO.createParameter(Env.getCtx(), windowNo, 0, 0, column.getAD_Column_ID(), column.getColumnName(), column.getName(), 
						DisplayType.isLookup(column.getAD_Reference_ID()) 
						? (DisplayType.isText(column.getAD_Reference_ID()) || DisplayType.isList(column.getAD_Reference_ID()) ? DisplayType.String : DisplayType.ID) 
						: column.getAD_Reference_ID(), 0, false, false, "");
			vo.DefaultValue = viewColumn != null && !Util.isEmpty(viewColumn.getDefaultValue(), true) ? viewColumn.getDefaultValue() : column.getDefaultValue();
			GridField gridField = new GridField(vo);
			Object defaultValue = gridField.getDefault();
			if (defaultValue != null) {
				po.set_ValueOfColumn(column.getAD_Column_ID(), defaultValue);
				return true;
			}
		}		
		return false;
	}

	@Override
	public IPOSerializer getPOSerializer(String tableName, Class<?> modelClass) {
		if ("*".equals(tableName)) {
			return this;
		}
		return null;
	}
}
