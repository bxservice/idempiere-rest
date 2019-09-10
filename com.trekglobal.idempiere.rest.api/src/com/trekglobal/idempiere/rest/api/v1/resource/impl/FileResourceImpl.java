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

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.trekglobal.idempiere.rest.api.v1.resource.FileResource;

/**
 * 
 * @author hengsin
 *
 */
public class FileResourceImpl implements FileResource {

	public FileResourceImpl() {
	}

	@Override
	public Response getFile(String fileName, long length) {
		File tempFolder = new File(System.getProperty("java.io.tmpdir"));
		File file = new File(tempFolder, fileName);
		if (file.exists() && file.isFile()) {
			if (!file.canRead()) {
				return Response.status(Status.FORBIDDEN).build();
			} else if (file.length()==length) {
				String contentType = MediaType.APPLICATION_OCTET_STREAM;
				String lfn = fileName.toLowerCase();
				if (lfn.endsWith(".html") || lfn.endsWith(".htm"))
					contentType = MediaType.TEXT_HTML;
				else if (lfn.endsWith(".csv") || lfn.endsWith(".ssv"))
					contentType = MediaType.TEXT_PLAIN;
				FileStreamingOutput fso = new FileStreamingOutput(file);
				return Response.ok(fso, contentType).build();
			} else {
				return Response.status(Status.FORBIDDEN).build();
			}
		} else {
			return Response.status(Status.NOT_FOUND).build();
		}		
	}
}
