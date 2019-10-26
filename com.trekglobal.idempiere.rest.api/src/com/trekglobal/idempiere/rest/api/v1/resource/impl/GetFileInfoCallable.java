package com.trekglobal.idempiere.rest.api.v1.resource.impl;

import java.io.File;
import java.io.Serializable;
import java.util.concurrent.Callable;

import org.compiere.util.Util;

public class GetFileInfoCallable implements Callable<GetFileInfoCallable.FileInfo>, Serializable {

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
		if ("java.io.tmpdir".equals(parentFolderName))
			parentFolder = new File(System.getProperty("java.io.tmpdir"));
		else if (!Util.isEmpty(parentFolderName, true))
			parentFolder = new File(parentFolderName);
		
		File file = parentFolder != null ? new File(parentFolder, fileName) : new File(fileName);
		if (file.exists() && file.isFile()) {
			if (!file.canRead())
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
	
	public static class FileInfo implements Serializable {
		
		/**
		 * generated serial id
		 */
		private static final long serialVersionUID = -8891201167549891241L;
		
		private String parentFolderName;
		private String fileName;
		private long length;
		private int blockSize;
		private int noOfBlocks;
		
		public FileInfo(String fileName, String parentFolderName, long length, int blockSize, int noOfBlocks) {
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
}
