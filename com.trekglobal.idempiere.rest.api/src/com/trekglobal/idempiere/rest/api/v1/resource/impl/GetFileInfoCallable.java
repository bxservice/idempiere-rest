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

import java.io.File;
import java.io.Serializable;
import java.util.concurrent.Callable;

import org.compiere.util.Util;

/**
 * 
 * @author hengsin
 *
 */
public class GetFileInfoCallable implements Callable<FileInfo>, Serializable {

	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = -87388045962116357L;
	private String parentFolderName;
	private String fileName;
	private int blockSize;

	/**
	 * 
	 * @param parentFolderName
	 * @param fileName
	 * @param blockSize size of each block. 0 to load all as one block
	 */
	public GetFileInfoCallable(String parentFolderName, String fileName, int blockSize) {
		this.parentFolderName = parentFolderName;
		this.fileName = fileName;
		this.blockSize = blockSize;
	}

	@Override
	public FileInfo call() throws Exception {
		File parentFolder =  null;
		if (!Util.isEmpty(parentFolderName, true))
			parentFolder = new File(parentFolderName);
		
		File file = parentFolder != null ? new File(parentFolder, fileName) : new File(fileName);
		if (file.exists() && file.isFile()) {
			if (!file.canRead() || !FileAccess.isAccessible(file))
				return null;
			
			long length = file.length();
			int noOfBlocks = 1;
			if (blockSize > 0 && length > blockSize) {
				int v = (int) (((length - 1) / blockSize) + 1);
				if (v == 0)
					v = 1;
				noOfBlocks = v;
			}
			FileInfo fileInfo = new FileInfo(parentFolderName, fileName, length, blockSize, noOfBlocks);
			return fileInfo;
		} else {
			return null;
		}
	}	
}
