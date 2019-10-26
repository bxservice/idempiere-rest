/**
 * 
 */
package com.trekglobal.idempiere.rest.api.v1.resource.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.Callable;
import java.util.logging.Level;

import org.compiere.util.CLogger;
import org.compiere.util.Util;

/**
 * @author hengsin
 *
 */
public class ReadFileCallable implements Callable<byte[]> {

	private String parentFolderName;
	private String fileName;
	private int blockSize;
	private int blockNo;

	private CLogger log = CLogger.getCLogger(getClass());	
	
	/**
	 * 
	 * @param parentFolderName
	 * @param fileName
	 * @param blockSize size of each block. 0 to load all as one block
	 * @param blockNo 0 base block index
	 */
	public ReadFileCallable(String parentFolderName, String fileName, int blockSize, int blockNo) {
		this.parentFolderName = parentFolderName;
		this.fileName = fileName;
		this.blockSize = blockSize;
		this.blockNo = blockNo;
	}

	@Override
	public byte[] call() throws Exception {
		File parentFolder =  null;
		if ("java.io.tmpdir".equals(parentFolderName))
			parentFolder = new File(System.getProperty("java.io.tmpdir"));
		else if (!Util.isEmpty(parentFolderName, true))
			parentFolder = new File(parentFolderName);
		
		File file = parentFolder != null ? new File(parentFolder, fileName) : new File(fileName);
		if (file.length() == 0) {
			return new byte[0];
		}

		try(RandomAccessFile raf = new RandomAccessFile(file, "r"); FileChannel channel = raf.getChannel();) {			
			if (blockNo > 0 && blockSize > 0) {
				channel.position(blockNo * blockSize);
			}
			ByteBuffer buffer = ByteBuffer.allocate(1024);
			ByteArrayOutputStream baos = new ByteArrayOutputStream(2048);
			int bytesRead = 0;
			int totalRead = 0;
			while((bytesRead = channel.read(buffer)) > 0) {
				totalRead += bytesRead;
				if (blockSize > 0 && totalRead > blockSize) {
					int diff = totalRead - blockSize;
					bytesRead = bytesRead - diff;
					totalRead = blockSize;
				}
				baos.write(buffer.array(), 0, bytesRead);
				buffer.clear();
				if (totalRead == blockSize)
					break;
			}
			return baos.toByteArray();
		} catch (Exception ex) {
			log.log(Level.SEVERE, "stream" + ex);
			return null;
		}		
	}

}
