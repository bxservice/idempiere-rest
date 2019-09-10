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

import org.compiere.model.GridTab;

/**
 * 
 * @author hengsin
 *
 */
public class GridTabPaging {
	private Paging paging;
	private GridTab gridTab;
	
	public GridTabPaging(GridTab gridTab, Paging paging) {
		this.gridTab = gridTab;
		this.paging = paging;
	}

	/**
	 * Get number of rows for current page
	 * @return int
	 */
	public int getSize() {
		int total = gridTab.getRowCount(); 
		if (paging.getPageSize() <= 0)
			return total;
		else if ((total - ( paging.getActivePage() * paging.getPageSize())) < 0) {
			paging.setActivePage(0);
			return paging.getPageSize() > total ? total : paging.getPageSize();
		} else {
			int end = (paging.getActivePage() + 1) * paging.getPageSize();
			if (end > total)
				return total - ( paging.getActivePage() * paging.getPageSize());
			else
				return paging.getPageSize();
		}
	}
	
	/**
	 * Set current row for current page
	 * @param rowIndex
	 */
	public boolean setCurrentRow(int rowIndex) {
		if (paging.getPageSize() > 0) {
			rowIndex = (paging.getActivePage() * paging.getPageSize()) + rowIndex;
		}
		if (rowIndex < gridTab.getRowCount()) {
			gridTab.setCurrentRow(rowIndex);
			return true;
		} else {
			return false;
		}		
	}
}
