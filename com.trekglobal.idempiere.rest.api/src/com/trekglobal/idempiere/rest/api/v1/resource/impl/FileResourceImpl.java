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
import java.util.logging.Level;

import javax.activation.MimetypesFileTypeMap;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.compiere.util.CLogger;
import org.compiere.util.Util;
import org.idempiere.distributed.IClusterMember;
import org.idempiere.distributed.IClusterService;

import com.trekglobal.idempiere.rest.api.v1.resource.FileResource;

/**
 * 
 * @author hengsin
 *
 */
public class FileResourceImpl implements FileResource {
	
	protected static final int BLOCK_SIZE = 1024 * 1024 * 5;
	private CLogger log = CLogger.getCLogger(getClass());
	
	public FileResourceImpl() {
	}

	@Override
	public Response getFile(String fileName, long length, String nodeId) {
		if (Util.isEmpty(nodeId)) {
			return getLocalFile(fileName, true, length);
		} else {
			IClusterService service = ClusterUtil.getClusterService();
			if (service == null) 
				return getLocalFile(fileName, true, length);
			
			IClusterMember local = service.getLocalMember();
			if (local != null && local.getId().equals(nodeId))
				return getLocalFile(fileName, true, length);
			
			return getRemoteFile(fileName, true, length, nodeId);
		}
	}

	/**
	 * 
	 * @param fileName
	 * @param nodeId
	 * @return response
	 */
	public Response getFile(String fileName, String nodeId) {
		if (Util.isEmpty(nodeId)) {
			return getLocalFile(fileName, false, 0);
		} else {
			IClusterService service = ClusterUtil.getClusterService();
			if (service == null) 
				return getLocalFile(fileName, false, 0);
			
			IClusterMember local = service.getLocalMember();
			if (local != null && local.getId().equals(nodeId))
				return getLocalFile(fileName, false, 0);
			
			return getRemoteFile(fileName, false, 0, nodeId);
		}
	}
	
	private Response getLocalFile(String fileName, boolean verifyLength, long length) {
		File file = new File(fileName);
		if (file.exists() && file.isFile()) {
			if (!file.canRead() || !FileAccess.isAccessible(file)) {
				return Response.status(Status.FORBIDDEN)
						.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("File not readable").append("File not readable: ").append(fileName).build().toString())
						.build();
			} else if (!verifyLength || file.length()==length) {
				String contentType = null;
				String lfn = fileName.toLowerCase();
				if (lfn.endsWith(".html") || lfn.endsWith(".htm")) {
					contentType = MediaType.TEXT_HTML;
				} else if (lfn.endsWith(".csv") || lfn.endsWith(".ssv") || lfn.endsWith(".log")) {
					contentType = MediaType.TEXT_PLAIN;
				} else {
					MimetypesFileTypeMap map = new MimetypesFileTypeMap();
					contentType = map.getContentType(file);
				}
				if (Util.isEmpty(contentType, true))
					contentType = MediaType.APPLICATION_OCTET_STREAM;
				
				FileStreamingOutput fso = new FileStreamingOutput(file);
				return Response.ok(fso, contentType).build();
			} else {
				return Response.status(Status.FORBIDDEN)
						.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for file: ").append(fileName).build().toString())
						.build();
			}
		} else {
			return Response.status(Status.NOT_FOUND)
					.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("File not found").append("File not found: ").append(fileName).build().toString())
					.build();
		}
	}

	private Response getRemoteFile(String fileName, boolean verifyLength, long length, String nodeId) {
		IClusterService service = ClusterUtil.getClusterService();
		IClusterMember member = ClusterUtil.getClusterMember(nodeId);
		if (member == null) {
			return Response.status(Status.NOT_FOUND)
					.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid Node Id").append("No match found for node id: ").append(nodeId).build().toString())
					.build(); 
		}
		
		try {
			GetFileInfoCallable infoCallable = new GetFileInfoCallable(null, fileName, BLOCK_SIZE);
			FileInfo fileInfo = service.execute(infoCallable, member).get();
			if (fileInfo == null) {
				return Response.status(Status.NOT_FOUND)
						.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid File Name").append("File does not exists or not readable: ").append(fileName).build().toString())
						.build(); 
			}
			if (verifyLength && length != fileInfo.getLength()) {
				return Response.status(Status.FORBIDDEN)
						.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for file: ").append(fileName).build().toString())
						.build();
			}
			
			String contentType = null;
			String lfn = fileName.toLowerCase();
			if (lfn.endsWith(".html") || lfn.endsWith(".htm")) {
				contentType = MediaType.TEXT_HTML;
			} else if (lfn.endsWith(".csv") || lfn.endsWith(".ssv") || lfn.endsWith(".log")) {
				contentType = MediaType.TEXT_PLAIN;
			} else {
				MimetypesFileTypeMap map = new MimetypesFileTypeMap();
				contentType = map.getContentType(fileInfo.getFileName());
			}
			if (Util.isEmpty(contentType, true))
				contentType = MediaType.APPLICATION_OCTET_STREAM;
			
			RemoteFileStreamingOutput rfso = new RemoteFileStreamingOutput(fileInfo, member);
			return Response.ok(rfso, contentType).build();
		} catch (Exception ex) {
			log.log(Level.SEVERE, ex.getMessage(), ex);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(new ErrorBuilder().status(Status.INTERNAL_SERVER_ERROR).title("Server error").append("Server error with exception: ").append(ex.getMessage()).build().toString())
					.build();
		}
	}			
}
