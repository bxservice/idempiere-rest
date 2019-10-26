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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

import org.idempiere.distributed.IClusterMember;
import org.idempiere.distributed.IClusterService;

import com.trekglobal.idempiere.rest.api.v1.resource.impl.GetFileInfoCallable.FileInfo;

/**
 * 
 * @author hengsin
 *
 */
public class RemoteFileStreamingOutput implements StreamingOutput {

	private FileInfo fileInfo;
	private IClusterMember member;

	RemoteFileStreamingOutput(GetFileInfoCallable.FileInfo fileInfo, IClusterMember member) {
		this.fileInfo = fileInfo;
		this.member = member;
	}
	
	@Override
	public void write(OutputStream output) throws IOException, WebApplicationException {
		if (fileInfo.getLength() == 0)
			return;
		
		IClusterService service = ClusterUtil.getClusterService();
		BufferedOutputStream bos = new BufferedOutputStream(output);
		for(int i = 0; i < fileInfo.getNoOfBlocks(); i++) {
			ReadFileCallable callable = new ReadFileCallable(fileInfo.getParentFolderName(), fileInfo.getFileName(), fileInfo.getBlockSize(), i);
			byte[] contents;
			try {
				contents = service.execute(callable, member).get();
				if (contents == null || contents.length == 0)
					break;
				bos.write(contents);
			} catch (InterruptedException | ExecutionException e) {
				throw new WebApplicationException(e);
			}								
		}
		bos.flush();
	}	
}