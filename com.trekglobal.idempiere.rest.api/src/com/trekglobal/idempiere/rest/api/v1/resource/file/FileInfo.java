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
package com.trekglobal.idempiere.rest.api.v1.resource.file;

import java.io.Serializable;

/**
 * 
 * @author hengsin
 *
 */
public class FileInfo implements Serializable {
	
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = -8891201167549891241L;
	
	private String parentFolderName;
	private String fileName;
	private long length;
	private int blockSize;
	private int noOfBlocks;
	
	public FileInfo(String parentFolderName, String fileName, long length, int blockSize, int noOfBlocks) {
		this.parentFolderName = parentFolderName;
		this.fileName = fileName;
		this.length = length;
		this.blockSize = blockSize;
		this.noOfBlocks = noOfBlocks;
	}
	
	/**
	 * @return the parentFolderName
	 */
	public String getParentFolderName() {
		return parentFolderName;
	}

	/**
	 * @return the fileName
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * @return the length
	 */
	public long getLength() {
		return length;
	}

	/**
	 * @return the blockSize
	 */
	public int getBlockSize() {
		return blockSize;
	}

	/**
	 * @return the noOfBlocks
	 */
	public int getNoOfBlocks() {
		return noOfBlocks;
	}			
}