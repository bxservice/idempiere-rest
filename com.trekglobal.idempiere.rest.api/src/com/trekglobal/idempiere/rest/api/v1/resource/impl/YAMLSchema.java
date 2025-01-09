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
package com.trekglobal.idempiere.rest.api.v1.resource.impl;

import java.util.Date;

import org.compiere.model.MColumn;
import org.compiere.model.MLocation;
import org.compiere.model.MRefList;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.compiere.util.ValueNamePair;

import com.trekglobal.idempiere.rest.api.json.DateTypeConverter;
import com.trekglobal.idempiere.rest.api.model.MRestView;
import com.trekglobal.idempiere.rest.api.model.MRestViewColumn;
import com.trekglobal.idempiere.rest.api.model.MRestViewRelated;

/**
 * Static methods for generation of OpenAPI 3.0.0 YAML schema
 */
public class YAMLSchema {

	private YAMLSchema() {
	}

	/**
	 * Add view columns to builder
	 * @param view
	 * @param builder
	 * @param offset
	 */
	public static void addViewProperties(MRestView view, StringBuilder builder, int offset) {
		MTable table = MTable.get(view.getAD_Table_ID());
		String uidColumn = PO.getUUIDColumnName(table.getTableName());
		MRestViewColumn[] viewColumns = view.getColumns();
		for(MRestViewColumn viewColumn : viewColumns ) {
			MColumn column = MColumn.get(viewColumn.getAD_Column_ID());
			if (column.isKey())
				continue;
			if (column.getColumnName().equals(uidColumn))
				continue;
			if (column.getAD_Reference_ID() == DisplayType.Button)
				continue;
			if (column.getAD_Reference_ID() == DisplayType.Chart)
				continue;
			
			builder.append(" ".repeat(offset)).append(viewColumn.getName()).append(":");
			
			if (column.getAD_Reference_ID() == DisplayType.Image) {
				builder.append(" { $ref: '#/components/schemas/Image' }\n");
				continue;
			}
			if (column.getAD_Reference_ID() == DisplayType.Location && viewColumn.getREST_ReferenceView_ID() == 0) {
				builder.append(" { $ref: '#/components/schemas/Location' }\n");
				continue;
			}
			builder.append("\n");
			String columnDescription = column.get_Translation("Description");
			if (column.getAD_Reference_ID() == DisplayType.Location && viewColumn.getREST_ReferenceView_ID() > 0) {
				MRestView locationView = MRestView.get(viewColumn.getREST_ReferenceView_ID());
				builder.append(" ".repeat(offset+2)).append("type: object\n");
				builder.append(" ".repeat(offset+2)).append("properties:\n");
				builder.append(" ".repeat(offset+4)).append("id:\n");
				builder.append(" ".repeat(offset+6)).append("type: integer\n");
				builder.append(" ".repeat(offset+6)).append("description: record id\n");
				builder.append(" ".repeat(offset+4)).append("identifier:\n");
				builder.append(" ".repeat(offset+6)).append("type: string\n");
				builder.append(" ".repeat(offset+6)).append("description: record identifier\n");
				builder.append(" ".repeat(offset+4)).append("model-name:\n");
				builder.append(" ".repeat(offset+6)).append("type: string\n");
				builder.append(" ".repeat(offset+6)).append("enum:\n");
				builder.append(" ".repeat(offset+8)).append(" - '").append("c_location").append("'\n");
				builder.append(" ".repeat(offset+4)).append("view-name:\n");
				builder.append(" ".repeat(offset+6)).append("type: string\n");
				builder.append(" ".repeat(offset+6)).append("enum:\n");
				builder.append(" ".repeat(offset+8)).append(" - '").append(locationView.getName()).append("'\n");
				addViewProperties(locationView, builder, offset+4);
			} else if (DisplayType.isList(column.getAD_Reference_ID())) {
				addListProperty(builder, column, offset+2);
			} else if (DisplayType.isLookup(column.getAD_Reference_ID())) {
				addLookupProperty(builder, column, offset+2);
			} else if (column.getAD_Reference_ID() == DisplayType.Binary) {
				builder.append(" ".repeat(offset+2)).append("type: string\n");
				builder.append(" ".repeat(offset+2)).append("description: base64 encoded binary content\n");
			} else if (column.getAD_Reference_ID() == DisplayType.YesNo) {
				builder.append(" ".repeat(offset+2)).append("type: boolean\n");
				if (!Util.isEmpty(columnDescription))
					builder.append(" ".repeat(offset+2)).append("description: ").append(columnDescription).append("\n");
			} else if (column.getAD_Reference_ID() == DisplayType.Integer) {
				builder.append(" ".repeat(offset+2)).append("type: integer\n");
				if (!Util.isEmpty(columnDescription))
					builder.append(" ".repeat(offset+2)).append("description: ").append(columnDescription).append("\n");
			} else if (DisplayType.isNumeric(column.getAD_Reference_ID())) {
				builder.append(" ".repeat(offset+2)).append("type: number\n");
				if (!Util.isEmpty(columnDescription))
					builder.append(" ".repeat(offset+2)).append("description: ").append(columnDescription).append("\n");
			} else if (column.getAD_Reference_ID() == DisplayType.Date) {
				builder.append(" ".repeat(offset+2)).append("type: string\n");
				if (!Util.isEmpty(columnDescription))
					builder.append(" ".repeat(offset+2)).append("description: ").append(columnDescription).append("\n");
				builder.append(" ".repeat(offset+2)).append("example: ")
					.append(new DateTypeConverter().toJsonValue(DisplayType.Date, new Date())).append("\n");
			} else if (column.getAD_Reference_ID() == DisplayType.Time) {
				builder.append(" ".repeat(offset+2)).append("type: string\n");
				if (!Util.isEmpty(columnDescription))
					builder.append(" ".repeat(offset+2)).append("description: ").append(columnDescription).append("\n");
				builder.append(" ".repeat(offset+2)).append("example: ")
					.append(new DateTypeConverter().toJsonValue(DisplayType.Time, new Date())).append("\n");
			} else if (column.getAD_Reference_ID() == DisplayType.DateTime || column.getAD_Reference_ID() == DisplayType.TimestampWithTimeZone) {
				builder.append(" ".repeat(offset+2)).append("type: string\n");
				if (!Util.isEmpty(columnDescription))
					builder.append(" ".repeat(offset+2)).append("description: ").append(columnDescription).append("\n");
				builder.append(" ".repeat(offset+2)).append("example: ")
					.append(new DateTypeConverter().toJsonValue(DisplayType.DateTime, new Date())).append("\n");
			} else {
				builder.append(" ".repeat(offset+2)).append("type: string\n");
				if (!Util.isEmpty(columnDescription))
					builder.append(" ".repeat(offset+2)).append("description: ").append(columnDescription).append("\n");
			}
		}
		
		MRestViewRelated[] relatedViews = view.getRelatedViews();
		for (MRestViewRelated relatedView : relatedViews) {
			if (relatedView.isRestAutoExpand()) {
				builder.append(" ".repeat(offset)).append(relatedView.getName()).append(":\n");
				builder.append(" ".repeat(offset+2)).append("type: array\n");
				builder.append(" ".repeat(offset+2)).append("items:\n");
				builder.append(" ".repeat(offset+4)).append("$ref: '#/components/schemas/")
					.append(MRestView.get(relatedView.getREST_RelatedRestView_ID()).getName())
					.append("'\n");
			}
		}
	}

	/**
	 * Add Image schema to builder
	 * @param builder
	 */
	public static void addImageReference(StringBuilder builder) {
		builder.append(" ".repeat(4)).append("Image").append(":\n");
		builder.append(" ".repeat(6)).append("type: object\n");
		builder.append(" ".repeat(6)).append("properties:\n");
		builder.append(" ".repeat(8)).append("id:\n");
		builder.append(" ".repeat(10)).append("type: integer\n");
		builder.append(" ".repeat(10)).append("description: image id\n");
		builder.append(" ".repeat(8)).append("uid:\n");
		builder.append(" ".repeat(10)).append("type: string\n");
		builder.append(" ".repeat(10)).append("description: image uuid\n");
		builder.append(" ".repeat(8)).append("file_name:\n");
		builder.append(" ".repeat(10)).append("type: string\n");
		builder.append(" ".repeat(10)).append("description: image file name\n");
		builder.append(" ".repeat(8)).append("data:\n");
		builder.append(" ".repeat(10)).append("type: string\n");
		builder.append(" ".repeat(10)).append("description: base64 encoded image content\n");
	}
	
	/**
	 * Add C_Location schema to builder
	 * @param builder
	 * @param offset
	 */
	public static void addLocationReference(StringBuilder builder, int offset) {
		String label = Msg.getElement(Env.getCtx(), "C_Location_ID");
		builder.append(" ".repeat(offset)).append("Location").append(":\n");
		builder.append(" ".repeat(offset+2)).append("type: object\n");
		builder.append(" ".repeat(offset+2)).append("properties:\n");
		builder.append(" ".repeat(offset+4)).append("propertyLabel: \n");
		builder.append(" ".repeat(offset+6)).append("type: string\n");
		builder.append(" ".repeat(offset+6)).append("enum:\n");
		builder.append(" ".repeat(offset+8)).append(" - '").append(label).append("'\n");
		builder.append(" ".repeat(offset+4)).append("id:\n");
		builder.append(" ".repeat(offset+6)).append("type: integer\n");
		builder.append(" ".repeat(offset+6)).append("description: record id\n");
		builder.append(" ".repeat(offset+4)).append("identifier:\n");
		builder.append(" ".repeat(offset+6)).append("type: string\n");
		builder.append(" ".repeat(offset+6)).append("description: record identifier\n");
		builder.append(" ".repeat(offset+4)).append("model-name:\n");
		builder.append(" ".repeat(offset+6)).append("type: string\n");
		builder.append(" ".repeat(offset+6)).append("enum:\n");
		builder.append(" ".repeat(offset+8)).append(" - '").append("c_location").append("'\n");
		
		MTable locationTable = MTable.get(MLocation.Table_ID);
		StringBuilder locationYaml = new StringBuilder();
		addTableProperties(locationTable, locationYaml, offset+4);
		builder.append(locationYaml.toString());
	}

	/**
	 * Add column lookup reference property to builder 
	 * @param builder
	 * @param column
	 * @param offset
	 */
	public static void addLookupProperty(StringBuilder builder, MColumn column, int offset) {
		String label = Msg.getElement(Env.getCtx(), column.getColumnName());
		builder.append(" ".repeat(offset)).append("type: object\n");
		builder.append(" ".repeat(offset)).append("properties:\n");
		builder.append(" ".repeat(offset+2)).append("propertyLabel: \n");
		builder.append(" ".repeat(offset+4)).append("type: string\n");
		builder.append(" ".repeat(offset+4)).append("enum:\n");
		builder.append(" ".repeat(offset+6)).append(" - '").append(label).append("'\n");
		builder.append(" ".repeat(offset+2)).append("id:\n");
		builder.append(" ".repeat(offset+4)).append("type: integer\n");
		builder.append(" ".repeat(offset+4)).append("description: record id\n");
		builder.append(" ".repeat(offset+2)).append("identifier:\n");
		builder.append(" ".repeat(offset+4)).append("type: string\n");
		builder.append(" ".repeat(offset+4)).append("description: record identifier\n");
		builder.append(" ".repeat(offset+2)).append("model-name:\n");
		builder.append(" ".repeat(offset+4)).append("type: string\n");
		builder.append(" ".repeat(offset+4)).append("enum:\n");
		builder.append(" ".repeat(offset+6)).append(" - '").append(column.getReferenceTableName().toLowerCase()).append("'\n");
	}

	/**
	 * Add column list property to builder
	 * @param builder
	 * @param column
	 * @param offset
	 */
	public static void addListProperty(StringBuilder builder, MColumn column, int offset) {
		String label = Msg.getElement(Env.getCtx(), column.getColumnName());
		ValueNamePair[] referenceList = MRefList.getList(Env.getCtx(), column.getAD_Reference_Value_ID(), false);
		builder.append(" ".repeat(offset)).append("type: object\n");
		builder.append(" ".repeat(offset)).append("properties:\n");
		builder.append(" ".repeat(offset+2)).append("propertyLabel: \n");
		builder.append(" ".repeat(offset+4)).append("type: string\n");
		builder.append(" ".repeat(offset+4)).append("enum:\n");
		builder.append(" ".repeat(offset+6)).append(" - '").append(label).append("'\n");
		builder.append(" ".repeat(offset+2)).append("id:\n");
		builder.append(" ".repeat(offset+4)).append("type: string\n");
		builder.append(" ".repeat(offset+4)).append("description: list item id\n");
		if (referenceList.length > 0) {
			builder.append(" ".repeat(offset+4)).append("enum:\n");
			for(ValueNamePair pair : referenceList) {
				builder.append(" ".repeat(offset+6)).append("- ")
					.append("'").append(pair.getValue()).append("'\n");
			}
		}
		builder.append(" ".repeat(offset+2)).append("identifier:\n");
		builder.append(" ".repeat(offset+4)).append("type: string\n");
		builder.append(" ".repeat(offset+4)).append("description: list item identifier\n");
		if (referenceList.length > 0) {
			builder.append(" ".repeat(offset+4)).append("enum:\n");
			for(ValueNamePair pair : referenceList) {
				builder.append(" ".repeat(offset+6)).append("- ")
					.append("'").append(pair.getName()).append("'\n");
			}
		}
		builder.append(" ".repeat(offset+2)).append("model-name:\n");
		builder.append(" ".repeat(offset+4)).append("type: string\n");
		builder.append(" ".repeat(offset+4)).append("enum:\n");
		builder.append(" ".repeat(offset+6)).append(" - '").append("ad_ref_list").append("'\n");
	}

	/**
	 * Add table columns to builder
	 * @param table
	 * @param builder
	 * @param offset
	 */
	public static void addTableProperties(MTable table, StringBuilder builder, int offset) {
		String uidColumn = PO.getUUIDColumnName(table.getTableName());
		MColumn[] columns = table.getColumns(false);
		for(MColumn column : columns ) {
			if (column.isKey())
				continue;
			if (column.getColumnName().equals(uidColumn))
				continue;
			if (column.getAD_Reference_ID() == DisplayType.Button)
				continue;
			if (column.getAD_Reference_ID() == DisplayType.Chart)
				continue;
			builder.append(" ".repeat(offset)).append(column.getColumnName()).append(":");
			if (column.getAD_Reference_ID() == DisplayType.Image) {
				builder.append(" { $ref: '#/components/schemas/Image' }\n");
				continue;
			}
			if (column.getAD_Reference_ID() == DisplayType.Location) {
				builder.append(" { $ref: '#/components/schemas/Location' }\n");
				continue;
			}
			builder.append("\n");
			String columnDescription = column.get_Translation("Description");
			if (DisplayType.isList(column.getAD_Reference_ID())) {				
				addListProperty(builder, column, offset+2);
			} else if (DisplayType.isLookup(column.getAD_Reference_ID())) {
				addLookupProperty(builder, column, offset+2);
			} else if (column.getAD_Reference_ID() == DisplayType.Binary) {
				builder.append(" ".repeat(offset+2)).append("type: string\n");
				builder.append(" ".repeat(offset+2)).append("description: base64 encoded binary content\n");
			} else if (column.getAD_Reference_ID() == DisplayType.YesNo) {
				builder.append(" ".repeat(offset+2)).append("type: boolean\n");
				if (!Util.isEmpty(columnDescription))
					builder.append(" ".repeat(offset+2)).append("description: ").append(columnDescription).append("\n");
			} else if (column.getAD_Reference_ID() == DisplayType.Integer) {
				builder.append(" ".repeat(offset+2)).append("type: integer\n");
				if (!Util.isEmpty(columnDescription))
					builder.append(" ".repeat(offset+2)).append("description: ").append(columnDescription).append("\n");
			} else if (DisplayType.isNumeric(column.getAD_Reference_ID())) {
				builder.append(" ".repeat(offset+2)).append("type: number\n");
				if (!Util.isEmpty(columnDescription))
					builder.append(" ".repeat(offset+2)).append("description: ").append(columnDescription).append("\n");
			} else if (column.getAD_Reference_ID() == DisplayType.Date) {
				builder.append(" ".repeat(offset+2)).append("type: string\n");
				if (!Util.isEmpty(columnDescription))
					builder.append(" ".repeat(offset+2)).append("description: ").append(columnDescription).append("\n");
				builder.append(" ".repeat(offset+2)).append("example: ")
					.append(new DateTypeConverter().toJsonValue(DisplayType.Date, new Date())).append("\n");
			} else if (column.getAD_Reference_ID() == DisplayType.Time) {
				builder.append(" ".repeat(offset+2)).append("type: string\n");
				if (!Util.isEmpty(columnDescription))
					builder.append(" ".repeat(offset+2)).append("description: ").append(columnDescription).append("\n");
				builder.append(" ".repeat(offset+2)).append("example: ")
					.append(new DateTypeConverter().toJsonValue(DisplayType.Time, new Date())).append("\n");
			} else if (column.getAD_Reference_ID() == DisplayType.DateTime || column.getAD_Reference_ID() == DisplayType.TimestampWithTimeZone) {
				builder.append(" ".repeat(offset+2)).append("type: string\n");
				if (!Util.isEmpty(columnDescription))
					builder.append(" ".repeat(offset+2)).append("description: ").append(columnDescription).append("\n");
				builder.append(" ".repeat(offset+2)).append("example: ")
					.append(new DateTypeConverter().toJsonValue(DisplayType.DateTime, new Date())).append("\n");
			} else {
				builder.append(" ".repeat(offset+2)).append("type: string\n");
				if (!Util.isEmpty(columnDescription))
					builder.append(" ".repeat(offset+2)).append("description: ").append(columnDescription).append("\n");
			}
		}
	}
}
