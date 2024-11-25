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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.compiere.Adempiere;
import org.compiere.util.CLogFile;
import org.compiere.util.CLogger;

/**
 * 
 * @author hengsin
 *
 */
public class FileAccess {

	private static final String s_dirAccessFileName = "dirAccess.txt";
	private static final CLogger log = CLogger.getCLogger(FileAccess.class);
	
	private FileAccess() {
	}

	/**
	 * 
	 * @return list of directories accessible
	 */
	private static List<String> getDirAcessList() {
		final List<String> dirAccessList = new ArrayList<String>();

		// by default has access to log and tmp directory
		CLogFile fileHandler = CLogFile.get(true, null, false);
		File logDir = fileHandler.getLogDirectory();
		dirAccessList.add(logDir.getAbsolutePath());
		File tempFolder = new File(System.getProperty("java.io.tmpdir"));
		dirAccessList.add(tempFolder.getAbsolutePath());

		// load from dirAccess.txt file
		String dirAccessPathName = Adempiere.getAdempiereHome() + File.separator + s_dirAccessFileName;
		File dirAccessFile = new File(dirAccessPathName);
		if (dirAccessFile.exists()) {
			try (BufferedReader br = new BufferedReader(new FileReader(dirAccessFile))) {				
				while (true) {
					String pathName = br.readLine();
					if (pathName == null)
						break;
					File pathDir = new File(pathName);
					if (pathDir.exists() && !dirAccessList.contains(pathDir.getAbsolutePath()))
						dirAccessList.add(pathDir.getAbsolutePath());
				}
			} catch (Exception e) {
				log.log(Level.SEVERE, dirAccessPathName + " - " + e.toString());
			}
		}
		
		return dirAccessList;
	}
	
	/**
	 * 
	 * @param file
	 * @return true if file is accessible, false otherwise
	 * @throws IOException
	 */
	public static boolean isAccessible(File file) {
		boolean found = false;
		List<String> dirAccessList = getDirAcessList();

		for (String dir : dirAccessList) {
			if (file.getAbsolutePath().startsWith(dir)) {
				found = true;
				break;
			}
		}

		if (!found) {
			log.warning("Couldn't find file in directories that allowed to access: " + file.getAbsolutePath());
			return false;
		}
		
		return true;
	}
}
