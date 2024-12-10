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
 * - BX Service GmbH                                                   *
 * - Carlos Ruiz                                                       *
 **********************************************************************/
package com.trekglobal.idempiere.rest.api.v1.resource.impl;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.Base64;
import java.util.logging.Level;

import javax.imageio.ImageIO;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.compiere.model.MChart;
import org.compiere.tools.FileUtil;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;

import com.google.gson.JsonObject;
import com.trekglobal.idempiere.rest.api.json.IDempiereRestException;
import com.trekglobal.idempiere.rest.api.util.ErrorBuilder;
import com.trekglobal.idempiere.rest.api.v1.resource.ChartResource;
import com.trekglobal.idempiere.rest.api.v1.resource.file.FileStreamingOutput;

/**
 * 
 * @author Carlos Ruiz
 *
 */
public class ChartResourceImpl implements ChartResource {

	private final static CLogger log = CLogger.getCLogger(ChartResourceImpl.class);

	public ChartResourceImpl() {
	}

	@Override
	public Response getChartImage(String id, int width, int height, String asJson) {
		try {
			boolean isUUID = Util.isUUID(id);
			int chartId = isUUID ? getChartID(id) : Integer.valueOf(id);

			BufferedImage img = null;
			MChart mc = new MChart(Env.getCtx(), chartId, null);
			if (mc.get_ID() == chartId)
				img = mc.getChartImage(width, height);
			if (img != null) {
				File file = FileUtil.createTempFile("chart" + chartId, ".png");
		        ImageIO.write(img, "png", file);
				if (asJson == null) {
					FileStreamingOutput fso = new FileStreamingOutput(file);
					return Response.ok(fso).header("Content-Type", "image/png").build();
				} else {
					JsonObject json = new JsonObject();
					byte[] binaryData = Files.readAllBytes(file.toPath());
					String data = Base64.getEncoder().encodeToString(binaryData);
					json.addProperty("data", data);
					return Response.ok(json.toString()).build();
				}
			}
			return Response.status(Status.NO_CONTENT).build();
		} catch (Exception ex) {
			Status status = Status.INTERNAL_SERVER_ERROR;
			if (ex instanceof IDempiereRestException)
				status = ((IDempiereRestException) ex).getErrorResponseStatus();

			log.log(Level.SEVERE, ex.getMessage(), ex);
			return Response.status(status)
					.entity(new ErrorBuilder().status(status)
							.title("GET Error")
							.append("Get status line with exception: ")
							.append(ex.getMessage())
							.build().toString())
					.build();
		}
	}

	private int getChartID(String uuid) {
		String sql = "SELECT AD_Chart_ID FROM AD_Chart WHERE AD_Chart_UU = ?";
		return DB.getSQLValue(null, sql, uuid);
	}

	@Override
	public Response getChartData(String id) {
		boolean isUUID = Util.isUUID(id);
		int chartId = isUUID ? getChartID(id) : Integer.valueOf(id);
		MChart mc = new MChart(Env.getCtx(), chartId, null);
		return Response.ok(mc.getData().toString()).build();
	}

}
