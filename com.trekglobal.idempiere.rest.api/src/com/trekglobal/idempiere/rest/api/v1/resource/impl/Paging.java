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

/**
 * 
 * @author hengsin
 *
 */
public class Paging {
	/** # of items per page. */
	private int _pgsz = 20;
	/** total # of items. */
	private int _ttsz = 0;
	/** # of pages. */
	private int _npg = 1;
	/** the active page. */
	private int _actpg = 0;
	
	public Paging() {
	}

	/** Constructor.
	 *
	 * @param totalsz the total # of items
	 * @param pagesz the # of items per page
	 */
	public Paging(int totalsz, int pagesz) {
		this();
		setTotalSize(totalsz);
		setPageSize(pagesz);
	}
	
	public int getPageSize() {
		return _pgsz;
	}

	/**Sets the items to show in each page
	 * 
	 */
	public void setPageSize(int size) {
		if (size <= 0)
			throw new IllegalArgumentException("positive only");

		if (_pgsz != size) {
			_pgsz = size;
			updatePageNum();
		}
	}

	public int getTotalSize() {
		return _ttsz;
	}

	/**Sets total size of items
	 * 
	 */
	public void setTotalSize(int size) {
		if (size < 0)
			throw new IllegalArgumentException("non-negative only");

		if (_ttsz != size) {
			_ttsz = size;
			updatePageNum();
		}
	}

	private void updatePageNum() {
		int v = (_ttsz - 1) / _pgsz + 1;
		if (v == 0)
			v = 1;
		if (v != _npg) {
			_npg = v;
			if (_actpg >= _npg) {
				_actpg = _npg - 1;
			}
		}
	}

	public int getPageCount() {
		return _npg;
	}

	public int getActivePage() {
		return _actpg;
	}

	/**
	 * Set the active page
	 * <p>Note: In server side, active page starts from 0. But in browser UI, it starts from 1
	 */
	public void setActivePage(int pg) throws IllegalArgumentException {
		if (pg >= _npg || pg < 0)
			throw new IllegalArgumentException("Unable to set active page to " + pg + " since only " + _npg + " pages");
		if (_actpg != pg) {
			_actpg = pg;
		}
	}
}
