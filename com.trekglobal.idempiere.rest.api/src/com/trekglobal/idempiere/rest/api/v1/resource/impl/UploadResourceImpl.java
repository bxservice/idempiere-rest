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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.logging.Level;

import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.adempiere.util.ContextRunnable;
import org.compiere.Adempiere;
import org.compiere.model.MArchive;
import org.compiere.model.MAttachment;
import org.compiere.model.MAttachmentEntry;
import org.compiere.model.MImage;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.MimeType;
import org.compiere.util.Trx;
import org.compiere.util.Util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.trekglobal.idempiere.rest.api.json.POParser;
import com.trekglobal.idempiere.rest.api.json.ResponseUtils;
import com.trekglobal.idempiere.rest.api.json.RestUtils;
import com.trekglobal.idempiere.rest.api.model.MRestUpload;
import com.trekglobal.idempiere.rest.api.model.MRestUploadChunk;
import com.trekglobal.idempiere.rest.api.model.MRestView;
import com.trekglobal.idempiere.rest.api.model.X_REST_Upload;
import com.trekglobal.idempiere.rest.api.v1.resource.UploadResource;

@Path("v1/uploads")
public class UploadResourceImpl implements UploadResource {

    private static final ChunkStorageService chunkStorageService = new ChunkStorageService();
    private static final String STATUS_INITIATED = X_REST_Upload.REST_UPLOADSTATUS_Initiated;
    private static final String STATUS_UPLOADING = X_REST_Upload.REST_UPLOADSTATUS_Uploading;
    private static final String STATUS_COMPLETED = X_REST_Upload.REST_UPLOADSTATUS_Completed;
    private static final String STATUS_PROCESSING = X_REST_Upload.REST_UPLOADSTATUS_Processing;
    private static final String STATUS_FAILED = X_REST_Upload.REST_UPLOADSTATUS_Failed;
    private static final String STATUS_CANCELED = X_REST_Upload.REST_UPLOADSTATUS_Canceled;

    private final static CLogger log = CLogger.getCLogger(UploadResourceImpl.class);
    		
    @Override
    public Response initiateUpload(UploadInitiationRequest request) {
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);

        MRestUpload upload = new MRestUpload(Env.getCtx(), 0, null);
        upload.setFileName(request.fileName());
        upload.setContentType(request.contentType());
        upload.setFileSize(BigDecimal.valueOf(request.fileSize()));
        upload.setChunkSize(request.chunkSize());
        upload.setStatus(STATUS_INITIATED);
        upload.setExpiresAt(Timestamp.valueOf(expiresAt));
        upload.setREST_SHA256(request.sha256());

        try {
            upload.saveEx();
            String uploadId = upload.getREST_Upload_UU(); // Use UUID for upload ID
            UploadInitiationResponse response = new UploadInitiationResponse(
                    uploadId,
                    upload.getChunkSize(),
                    expiresAt.format(DateTimeFormatter.ISO_DATE_TIME));
            return Response.status(Response.Status.CREATED).entity(response).build();
        } catch (Exception e) {
            return ResponseUtils.getResponseErrorFromException(e, "Failed to initiate upload");
        }
    }

    @Override
    public Response uploadChunk(String uploadId, int chunkOrder, String sha256, InputStream chunkData) {
        MRestUpload upload = MRestUpload.get(uploadId);

        if (upload == null) {
        	return ResponseUtils.getResponseError(Response.Status.NOT_FOUND, "Upload session not found: ", uploadId, "");
        }

        // check if the upload is expired
        if (upload.getExpiresAt() != null && LocalDateTime.now().isAfter(upload.getExpiresAt().toLocalDateTime())) {
             upload.setStatus(STATUS_FAILED);
            try {
                upload.saveEx();
            } catch (Exception e) { 
            	return ResponseUtils.getResponseErrorFromException(e, "Error saving upload status");
            }
            return ResponseUtils.getResponseError(Response.Status.GONE, "Upload session has expired: ", uploadId, "");
        }
        
        // check if the upload is already completed, processing or failed
        if (STATUS_COMPLETED.equals(upload.getREST_UploadStatus()) || STATUS_PROCESSING.equals(upload.getREST_UploadStatus()) 
        		|| STATUS_FAILED.equals(upload.getREST_UploadStatus())) {
        	return ResponseUtils.getResponseError(Response.Status.CONFLICT, "Upload session is already: "
        			+ upload.getREST_UploadStatus().toLowerCase(), uploadId, "");
        }

        Trx trx = Trx.get(Trx.createTrxName(), true);
        try {
        	upload.set_TrxName(trx.getTrxName());
            ChunkStorageService.ChunkDetails details = chunkStorageService.storeChunk(upload, chunkOrder, chunkData, sha256);

            if (STATUS_INITIATED.equals(upload.getREST_UploadStatus())) {
                upload.setStatus(STATUS_UPLOADING);
                upload.saveEx();
            }

            trx.commit(true);
            UploadChunkResponse response = new UploadChunkResponse(
                    uploadId,
                    chunkOrder,
                    details.size,
                    "Chunk uploaded successfully.");
            return Response.ok(response).build();

        } catch (Exception e) {
        	trx.rollback();
        	return ResponseUtils.getResponseErrorFromException(e, "Failed to upload chunk");
        } finally {
			trx.close();
		}
    }
        
    @Override
    public Response getUploadStatus(String uploadId) {
        MRestUpload upload = MRestUpload.get(uploadId);

        if (upload == null) {
        	return ResponseUtils.getResponseError(Response.Status.NOT_FOUND, "Upload session not found: ", uploadId, "");
        }

        // get uploaded chunks
        List<MRestUploadChunk> chunks = MRestUploadChunk.findByUploadId(upload.getREST_Upload_ID());
        chunks.sort(Comparator.comparingInt(MRestUploadChunk::getSeqNo));
        List<UploadedChunk> uploadedChunkOrders = new ArrayList<>();
        long totalReceivedSize = chunks.stream()
				.mapToLong(MRestUploadChunk::getReceivedSize)
				.sum();
        chunks.forEach(chunk -> {
			uploadedChunkOrders.add(new UploadedChunk(
					chunk.getSeqNo(),
					chunk.getReceivedSize()));
		});

        String message = "Status for upload ID " + uploadId;
        switch (upload.getREST_UploadStatus()) {
            case STATUS_COMPLETED:
                message = "File upload completed and processed.";
                break;
            case STATUS_PROCESSING:
                message = "File is currently being processed.";
                break;
            case STATUS_FAILED:
                message = "File processing failed or upload expired before completion.";
                break;
            case STATUS_UPLOADING:
                 message = "File upload in progress. " + uploadedChunkOrders.size() + " chunks received.";
                 break;
            case STATUS_INITIATED:
                 message = "File upload initiated, awaiting chunks.";
                 break;
        }
        
        if (!STATUS_FAILED.equals(upload.getREST_UploadStatus()) && !STATUS_COMPLETED.equals(upload.getREST_UploadStatus()) &&
            upload.getExpiresAt() != null && LocalDateTime.now().isAfter(upload.getExpiresAt().toLocalDateTime())) {
            message = "Upload session has expired.";
            if(!STATUS_PROCESSING.equals(upload.getREST_UploadStatus())){ // Don't mark as failed if it's already processing
                upload.setStatus(STATUS_FAILED);
                try{ upload.saveEx(); } catch (Exception e) { 
                	return ResponseUtils.getResponseErrorFromException(e, "Error saving upload status");
                }
            }
        }

        UploadStatusResponse response = new UploadStatusResponse(
                upload.getREST_Upload_UU(),
                upload.getFileName(),
                upload.getFileSize().longValue(),
                upload.getChunkSize(),
                upload.getREST_UploadStatus(),
                uploadedChunkOrders,
                totalReceivedSize,
                message);

        return Response.ok(response).build();
    }

    @Override
    public Response finalizeUpload(String uploadId, UploadCompletionRequest completionRequest) {
        MRestUpload upload = MRestUpload.get(uploadId);

        if (upload == null) {
        	return ResponseUtils.getResponseError(Response.Status.NOT_FOUND, "Upload session not found: ", uploadId, "");
        }
        if (STATUS_PROCESSING.equals(upload.getREST_UploadStatus())) {
            UploadCompletionResponse response = new UploadCompletionResponse(uploadId, upload.getREST_UploadStatus(), "Upload is already being processed.");
            return Response.status(Response.Status.ACCEPTED).entity(response).build();
        }
        if (STATUS_COMPLETED.equals(upload.getREST_UploadStatus())) {
            UploadCompletionResponse response = new UploadCompletionResponse(uploadId, upload.getREST_UploadStatus(), "Upload has already been completed.");
            return Response.status(Response.Status.OK).entity(response).build(); 
        }

        if (upload.getExpiresAt() != null && LocalDateTime.now().isAfter(upload.getExpiresAt().toLocalDateTime())) {
            upload.setStatus(STATUS_FAILED);
            try{ upload.saveEx(); } catch (Exception e) { 
            	return ResponseUtils.getResponseErrorFromException(e, "Error saving upload status");
            }
            return ResponseUtils.getResponseError(Response.Status.GONE, "Upload session has expired: ", uploadId, "");
        }
        
        List<MRestUploadChunk> chunks = MRestUploadChunk.findByUploadId(upload.getREST_Upload_ID());
        
        int expectedTotalChunks;
        try {
            expectedTotalChunks = Integer.parseInt(completionRequest.totalChunks());
        } catch (NumberFormatException e) {
             return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Invalid totalChunks format in request.\"}")
                    .build();
        }

        if (chunks.size() != expectedTotalChunks) {
            // Do not mark as FAILED immediately, client might still be uploading or there's a mismatch.
            // The getUploadStatus will reflect the current chunk count.
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Mismatch in chunk count. Expected: " + expectedTotalChunks + ", Received: " + chunks.size() + ". Please ensure all chunks are uploaded.\"}")
                    .build();
        }
        
        long totalUploadedSize = chunks.stream().mapToLong(MRestUploadChunk::getReceivedSize).sum();
        if (upload.getFileSize().longValue() > 0 && totalUploadedSize != upload.getFileSize().longValue()) {
        	return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Mismatch in total received size. Expected: " + upload.getFileSize().longValue() + ", Received: " + totalUploadedSize + ". Please ensure all chunks are uploaded.\"}")
                    .build();
        }
        
        if (!Util.isEmpty(completionRequest.fileName(), true))
        	upload.setFileName(completionRequest.fileName());
        
        if (!Util.isEmpty(completionRequest.uploadLocation(), true)) {
        	if (!completionRequest.uploadLocation().equals(MRestUpload.REST_UPLOADLOCATION_Archive)
        			&& !completionRequest.uploadLocation().equals(MRestUpload.REST_UPLOADLOCATION_Attachment)
        			&& !completionRequest.uploadLocation().equals(MRestUpload.REST_UPLOADLOCATION_Image))
        		return Response.status(Response.Status.BAD_REQUEST)
        				.entity("{\"error\":\"Invalid uploadLocation in request.\"}")
                        .build();
        	upload.setREST_UploadLocation(completionRequest.uploadLocation());
        }
        
        // All checks passed, transition to PROCESSING and start async assembly
        upload.setStatus(STATUS_PROCESSING);
        try {
            upload.saveEx();
        } catch (Exception e) {
        	return ResponseUtils.getResponseErrorFromException(e, "Error saving upload status");
        }

        ContextRunnable runnable = new ContextRunnable() {
			@Override
			protected void doRun() {

	        	Trx trx = Trx.get(Trx.createTrxName(), true);
	            try {
	                // Re-fetch the upload object within the async thread to ensure fresh state if needed,
	                // or pass necessary immutable data. For status updates, fetching is safer.
	                MRestUpload currentUpload = MRestUpload.get(uploadId);
	                if (currentUpload == null) {
	                	log.log(Level.SEVERE, "Async Task: Upload " + uploadId + " not found before assembly.");
	                    return;
	                }
	                
	                List<MRestUploadChunk> currentChunks = MRestUploadChunk.findByUploadId(currentUpload.getREST_Upload_ID());
	                // Sort chunks by order before assembly
	                currentChunks.sort(Comparator.comparingInt(MRestUploadChunk::getSeqNo));

	                // Actual reassembly
	                currentUpload.set_TrxName(trx.getTrxName());
	                chunkStorageService.reassembleFile(currentUpload, currentUpload.getFileName(), currentChunks);

	                // After successful reassembly, delete individual chunk files
	                for (MRestUploadChunk chunk : currentChunks) {
	                	chunk.set_TrxName(trx.getTrxName());
	                    chunkStorageService.deleteChunk(chunk);
	                    chunk.deleteEx(true);
	                }

	                currentUpload.setStatus(STATUS_COMPLETED);
	                currentUpload.saveEx();
	                trx.commit(true);
	            } catch (Exception e) {
	            	trx.rollback();
	                log.log(Level.SEVERE, "Async Task: Error during file assembly for upload " + uploadId, e);                
	                MRestUpload failedUpload = MRestUpload.get(uploadId);
	                if (failedUpload != null) {
	                    failedUpload.setStatus(STATUS_FAILED);
	                    try {
	                        failedUpload.saveEx();
	                    } catch (Exception ex) {
	                    	log.log(Level.SEVERE, "Async Task: CRITICAL - Failed to update upload " + uploadId + " status to FAILED", e);
	                    }
	                }
	            } finally {
					trx.close();
				}
	        
			}
        };
        
        // Submit the assembly task to the executor
        Adempiere.getThreadPoolExecutor().submit(runnable);

        UploadCompletionResponse response = new UploadCompletionResponse(
                uploadId,
                upload.getREST_UploadStatus(), // Will be PROCESSING
                "File finalization accepted. Assembly is in progress. Check status endpoint for updates.");
        return Response.status(Response.Status.ACCEPTED).entity(response).build();
    }

    @Override
    public Response cancelUpload(String uploadId) {
        MRestUpload upload = MRestUpload.get(uploadId);

        if (upload == null) {
        	return ResponseUtils.getResponseError(Response.Status.NOT_FOUND, "Upload session not found: ", uploadId, "");
        }

        Trx trx = Trx.get(Trx.createTrxName(), true);
        try {
        	upload.set_TrxName(trx.getTrxName());
			// Delete all chunks associated with this upload
        	List<MRestUploadChunk> chunks = MRestUploadChunk.findByUploadId(upload.getREST_Upload_ID());
        	chunks.forEach(chunk -> {
        		chunk.set_TrxName(trx.getTrxName());
				chunkStorageService.deleteChunk(chunk);
				chunk.deleteEx(true);
			});
        	
        	chunkStorageService.deleteUploadFile(upload);
            
            // Update the main upload record to CANCELED
            upload.setStatus(STATUS_CANCELED);
            upload.saveEx();
            trx.commit(true);

            return Response.ok("{\"message\":\"Upload " + uploadId + " canceled successfully.\"}").build();
        } catch (Exception e) {
        	return ResponseUtils.getResponseErrorFromException(e, "Failed to cancel upload: ", uploadId);
        } finally {
        	trx.close();
        }
    }
    
    @Override
	public Response getFile(String uploadId, String asJson) {
    	MRestUpload upload = MRestUpload.get(uploadId);

        if (upload == null) {
        	return ResponseUtils.getResponseError(Response.Status.NOT_FOUND, "Upload session not found: ", uploadId, "");
        }
        
        if (!STATUS_COMPLETED.equals(upload.getREST_UploadStatus())) {
        	return ResponseUtils.getResponseError(Response.Status.BAD_REQUEST, 
					"Upload is not completed yet or cancelled. Current status: " + upload.getStatus(), uploadId, "");
        }
        
        ChunkStorageService.UploadDetails uploadDetails = chunkStorageService.getUploadDetails(upload);
        if (uploadDetails == null) {
			return ResponseUtils.getResponseError(Response.Status.NOT_FOUND, "File not found for upload: ", uploadId, "");
		}
        
        if (asJson == null) {
			String contentType = uploadDetails.contentType();
			if (Util.isEmpty(contentType, true)) {
				String lfn = uploadDetails.fileName().toLowerCase();
				if (lfn.endsWith(".html") || lfn.endsWith(".htm")) {
					contentType = MediaType.TEXT_HTML;
				} else if (lfn.endsWith(".csv") || lfn.endsWith(".ssv") || lfn.endsWith(".log")) {
					contentType = MediaType.TEXT_PLAIN;
				} else {
					contentType = MimeType.getMimeType(uploadDetails.fileName());
				}
			}
			if (Util.isEmpty(contentType, true))
				contentType = MediaType.APPLICATION_OCTET_STREAM;
			
			StreamingOutput streamingOutput = os -> {
				os.write(uploadDetails.data());
			};
			return Response.ok(streamingOutput, contentType).build();
		} else {
			JsonObject json = new JsonObject();
			String data = Base64.getEncoder().encodeToString(uploadDetails.data());
			json.addProperty("data", data);
			return Response.ok(json.toString()).build();
		}
	}
    
    @Override
   	public Response getPendingUploads() {
   		try {
   			String whereClause = MRestUpload.COLUMNNAME_REST_UploadStatus + " NOT IN (?,?) AND " + MRestUpload.COLUMNNAME_CreatedBy + "=?";
   			Query query = new Query(Env.getCtx(), MRestUpload.Table_Name, whereClause, null);
   			query.setOnlyActiveRecords(true).setApplyAccessFilter(true);
   			query.setParameters(STATUS_COMPLETED, STATUS_CANCELED, Env.getAD_User_ID(Env.getCtx()));
   			List<MRestUpload> uploads = query.setOrderBy(MRestUpload.COLUMNNAME_REST_Upload_ID).list();
   			JsonArray array = new JsonArray();
   			for (MRestUpload upload : uploads) {
   				Response response = getUploadStatus(upload.getREST_Upload_UU());
				Gson gson = new GsonBuilder().create();
				JsonElement jsonElement = gson.toJsonTree(response.getEntity());
				array.add(jsonElement);
   			}
   			JsonObject json = new JsonObject();
   			json.add("uploads", array);
   			return Response.ok(json.toString()).build();			
   		} catch (Exception ex) {
   			return ResponseUtils.getResponseErrorFromException(ex, "GET Error");
   		}
   	}
    
    @Override
    public Response copyUploadedFile(String uploadId, CopyUploadedFileRequest copyRequest) {
    	MRestUpload upload = MRestUpload.get(uploadId);

        if (upload == null) {
        	return ResponseUtils.getResponseError(Response.Status.NOT_FOUND, "Upload session not found: ", uploadId, "");
        }
        
        if (!STATUS_COMPLETED.equals(upload.getREST_UploadStatus())) {
        	return ResponseUtils.getResponseError(Response.Status.BAD_REQUEST, 
					"Upload is not completed yet or cancelled. Current status: " + upload.getStatus(), uploadId, "");
        }
        
        if (Util.isEmpty(copyRequest.copyLocation(), true)) {
        	return Response.status(Response.Status.BAD_REQUEST)
    				.entity("{\"error\":\"copyLocation is required in request.\"}")
                    .build();
        }
        
    	if (!copyRequest.copyLocation().equals(MRestUpload.REST_UPLOADLOCATION_Archive)
    			&& !copyRequest.copyLocation().equals(MRestUpload.REST_UPLOADLOCATION_Attachment)
    			&& !copyRequest.copyLocation().equals(MRestUpload.REST_UPLOADLOCATION_Image))
    		return Response.status(Response.Status.BAD_REQUEST)
    				.entity("{\"error\":\"Invalid copyLocation in request.\"}")
                     .build();
        
        if (!copyRequest.copyLocation().equals(MRestUpload.REST_UPLOADLOCATION_Image) 
        		&& (Util.isEmpty(copyRequest.tableName(), true) || Util.isEmpty(copyRequest.recordId(), true))) {
        	return Response.status(Response.Status.BAD_REQUEST)
    				.entity("{\"error\":\"tableName and recordId are required in request.\"}")
                    .build();
        }
        
        ChunkStorageService.UploadDetails uploadDetails = chunkStorageService.getUploadDetails(upload);
        if (uploadDetails == null) {
			return ResponseUtils.getResponseError(Response.Status.NOT_FOUND, "File not found for upload: ", uploadId, "");
		}
        
        if (!copyRequest.copyLocation().equals(MRestUpload.REST_UPLOADLOCATION_Image)) {
        	String tableName = copyRequest.tableName();
        	String id = copyRequest.recordId();
	        MRestView view = RestUtils.getView(tableName);
			if (view != null)
				tableName = MTable.getTableName(Env.getCtx(), view.getAD_Table_ID());
            POParser poParser = new POParser(tableName, id, true, true);
    		if (poParser.isValidPO()) {
    			PO po = poParser.getPO();
    			if (copyRequest.copyLocation().equals(MRestUpload.REST_UPLOADLOCATION_Attachment)) {
                	MAttachment attachment = po.getAttachment();
                	if (attachment == null)
                		attachment = po.createAttachment();
                	try {
                		attachment.addEntry(uploadDetails.fileName, uploadDetails.data);
                    	attachment.saveEx();
        			} catch (Exception ex) {
        				return ResponseUtils.getResponseErrorFromException(ex, "Save error");
        			}
                } else {
    	            MArchive archive = new Query(Env.getCtx(), MArchive.Table_Name, "AD_Table_ID=? AND Record_ID=?", upload.get_TrxName())
    						.setParameters(po.get_Table_ID(), po.get_ID()).first();
    	            if (archive == null) {
    					archive = new MArchive(Env.getCtx(), 0, upload.get_TrxName());
    					archive.setAD_Table_ID(po.get_Table_ID());
    					archive.setRecord_ID(po.get_ID());
    					archive.setRecord_UU(po.get_UUID());
    				}
    	            try {
        	            archive.setName(uploadDetails.fileName);
        	            archive.setBinaryData(uploadDetails.data);
        	            archive.saveEx();
        			} catch (Exception ex) {
        				return ResponseUtils.getResponseErrorFromException(ex, "Save error");
        			}
                }
    			CopyUploadedFileResponse response = new CopyUploadedFileResponse(
    					uploadId, 
    					copyRequest.tableName(), 
    					po.get_ID(),
    					po.get_UUID(), 
    					copyRequest.copyLocation(),
    					uploadDetails.fileName,
    					uploadDetails.contentType,
    					uploadDetails.data.length);
    			return Response.ok(response).build();
    		} else {
    			return poParser.getResponseError();
    		}
        } else {
        	MImage image = new MImage(Env.getCtx(), 0, upload.get_TrxName());
        	try {
        		image.setName(uploadDetails.fileName);
            	image.setBinaryData(uploadDetails.data);
            	image.saveEx();
			} catch (Exception ex) {
				return ResponseUtils.getResponseErrorFromException(ex, "Save error");
			}
        	CopyUploadedFileResponse response = new CopyUploadedFileResponse(
					uploadId, 
					image.get_TableName(), 
					image.get_ID(),
					image.get_UUID(),
					copyRequest.copyLocation(),
					uploadDetails.fileName,
					uploadDetails.contentType,
					uploadDetails.data.length);
			return Response.ok(response).build();
        }
    }
    
    static class ChunkStorageService {

        public ChunkStorageService() {
        }

        /**
         * Delete associated chunks from the storage.
         * @param chunk
         */
        public void deleteChunk(MRestUploadChunk chunk) {
			MArchive archive = getArchive(chunk);
			if (archive != null) {
				archive.set_TrxName(chunk.get_TrxName());
				archive.deleteEx(true); // Delete the archive record and its binary data
			}
		}

		static record ChunkDetails(MRestUploadChunk chunkRecord, long size, String sha256) {
        }

		/**
		 * Stores a chunk of data for the upload.
		 * @param upload
		 * @param chunkOrder
		 * @param data
		 * @param expectedSha256
		 * @return chunk details
		 * @throws IOException
		 * @throws NoSuchAlgorithmException
		 */
        public ChunkDetails storeChunk(MRestUpload upload, int chunkOrder, InputStream data, String expectedSha256) throws IOException, NoSuchAlgorithmException {
            long size = 0;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (InputStream digestingInputStream = new DigestInputStream(data, md)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = digestingInputStream.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                    size += bytesRead;
                }
            }
                                 
            HexFormat hex = HexFormat.of();
            String calculatedSha256 = hex.formatHex(md.digest());

            if (expectedSha256 != null && !expectedSha256.equals(calculatedSha256)) {
                throw new IOException("SHA-256 mismatch for chunk " + chunkOrder + ". Expected: " + expectedSha256 + ", Calculated: " + calculatedSha256);
            }

            // Create or update the chunk record
            MRestUploadChunk chunkRecord = new Query(Env.getCtx(), MRestUploadChunk.Table_Name, "REST_Upload_ID=? AND SeqNo=?", upload.get_TrxName())
					.setParameters(upload.getREST_Upload_ID(), chunkOrder)
					.first();
            if (chunkRecord == null) {
				chunkRecord = new MRestUploadChunk(Env.getCtx(), 0, upload.get_TrxName());
				chunkRecord.setREST_Upload_ID(upload.getREST_Upload_ID());
	            chunkRecord.setSeqNo(chunkOrder);
			}            
            chunkRecord.setREST_ReceivedSize(BigDecimal.valueOf(size));
            chunkRecord.setREST_SHA256(calculatedSha256); 
            chunkRecord.saveEx();
            
            // Store the chunk data in MArchive
            MArchive archive = new Query(Env.getCtx(), MArchive.Table_Name, "AD_Table_ID=? AND Record_ID=?", upload.get_TrxName())
            		.setParameters(MRestUploadChunk.Table_ID, chunkRecord.get_ID()).first();
            if (archive == null) {
            	archive = new MArchive(Env.getCtx(), 0, upload.get_TrxName());
            	archive.setAD_Table_ID(MRestUploadChunk.Table_ID);
                archive.setRecord_ID(chunkRecord.getREST_UploadChunk_ID());
                archive.setRecord_UU(chunkRecord.getREST_UploadChunk_UU());
            }
            archive.setName(upload.getFileName());
            archive.setDescription(upload.getFileName() + "_chunk_" + chunkOrder);            
            archive.setBinaryData(baos.toByteArray());
            archive.saveEx();
            
            return new ChunkDetails(chunkRecord, size, calculatedSha256);
        }
                
        /**
         * Reassembles the file from the uploaded chunks.
         * @param upload
         * @param finalFileName
         * @param chunks
         * @throws IOException
         * @throws NoSuchAlgorithmException 
         */
        public void reassembleFile(MRestUpload upload, String finalFileName, List<MRestUploadChunk> chunks) 
        		throws IOException, NoSuchAlgorithmException {
            // Ensure chunks are sorted by order
            chunks.sort((c1, c2) -> Integer.compare(c1.getSeqNo(), c2.getSeqNo()));

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            for (MRestUploadChunk chunk : chunks) {
            	MArchive archive = getArchive(chunk);
            	if (archive != null) {
            		baos.write(archive.getBinaryData());
				}            	
            }
                        
            HexFormat hex = HexFormat.of();
            String sha256 = hex.formatHex(md.digest(baos.toByteArray()));
            if (upload.getREST_SHA256() != null && !upload.getREST_SHA256().equals(sha256)) {
				throw new IOException("SHA-256 mismatch for final file. Expected: " + upload.getREST_SHA256() + ", Calculated: " + sha256);
			}
            
            if (upload.getREST_UploadLocation().equals(MRestUpload.REST_UPLOADLOCATION_Image)) {
            	MImage image = new MImage(Env.getCtx(), 0, upload.get_TrxName());
            	image.setName(upload.getFileName());
            	image.setBinaryData(baos.toByteArray());
            	image.saveEx();
            	upload.setAD_Image_ID(image.get_ID());
            	upload.saveEx();
            } else if (upload.getREST_UploadLocation().equals(MRestUpload.REST_UPLOADLOCATION_Attachment)) {
            	MAttachment attachment = upload.getAttachment();
            	if (attachment == null)
            		attachment = upload.createAttachment();
            	attachment.addEntry(upload.getFileName(), baos.toByteArray());
            	attachment.saveEx();
            } else {
	            MArchive archive = new Query(Env.getCtx(), MArchive.Table_Name, "AD_Table_ID=? AND Record_ID=?", upload.get_TrxName())
						.setParameters(MRestUpload.Table_ID, upload.getREST_Upload_ID()).first();
	            if (archive == null) {
					archive = new MArchive(Env.getCtx(), 0, upload.get_TrxName());
					archive.setAD_Table_ID(MRestUpload.Table_ID);
					archive.setRecord_ID(upload.getREST_Upload_ID());
					archive.setRecord_UU(upload.getREST_Upload_UU());
				}
	            archive.setName(upload.getFileName());
	            archive.setBinaryData(baos.toByteArray());
	            archive.saveEx();
            }
        }

        /**
         * Retrieves all archives associated with a given chunk.
         * @param chunk
         * @return archives associated with a given chunk or null if not found
         */
		public MArchive getArchive(MRestUploadChunk chunk) {
			MArchive[] archives = MArchive.get(Env.getCtx(), " AND AD_Table_ID="+MRestUploadChunk.Table_ID+" AND Record_ID="+chunk.get_ID(), null);
			return archives != null && archives.length == 1 ? archives[0]: null;
		}
		
		static record UploadDetails(String fileName, String contentType, byte[] data) {			
		}
		
		/**
		 * Retrieves the upload details from the archive/image/attachment associated with the upload.
		 * @param upload
		 * @return UploadDetails containing file name, content type and binary data, or null if not found
		 */
		public UploadDetails getUploadDetails(MRestUpload upload) {
			if (upload.getREST_UploadLocation().equals(MRestUpload.REST_UPLOADLOCATION_Image)) {
				if (upload.getAD_Image_ID() > 0) {
					MImage image = new MImage(Env.getCtx(), upload.getAD_Image_ID(), upload.get_TrxName());
					return new UploadDetails(
							upload.getFileName(),
							upload.getContentType(),
							image.getBinaryData());
				}
			} else if (upload.getREST_UploadLocation().equals(MRestUpload.REST_UPLOADLOCATION_Attachment)) {
				MAttachment attachment = upload.getAttachment();
				if (attachment != null && attachment.getEntryCount() > 0) {
					MAttachmentEntry[] entries = attachment.getEntries();
					for (MAttachmentEntry entry : entries) {
						if (entry.getName().equals(upload.getFileName())) {
							return new UploadDetails(
									upload.getFileName(),
									upload.getContentType(),
									entry.getData());
						}
					}
				}
			} else {
				MArchive[] archives = MArchive.get(Env.getCtx(), " AND AD_Table_ID="+MRestUpload.Table_ID+" AND Record_ID="+upload.get_ID(), null);
				if (archives != null && archives.length == 1) {
					return new UploadDetails(
						upload.getFileName(),
						upload.getContentType(),
						archives[0].getBinaryData());
				}
			}
			return null;
		}
		
		/**
         * Delete archive/image/attachment associated with the upload.
         * @param upload
         */
        public void deleteUploadFile(MRestUpload upload) {
        	if (upload.getREST_UploadLocation().equals(MRestUpload.REST_UPLOADLOCATION_Image)) {
				if (upload.getAD_Image_ID() > 0) {
					MImage image = new MImage(Env.getCtx(), upload.getAD_Image_ID(), upload.get_TrxName());
					image.deleteEx(true);
					upload.setAD_Image_ID(0);
				}
			} else if (upload.getREST_UploadLocation().equals(MRestUpload.REST_UPLOADLOCATION_Attachment)) {
				MAttachment attachment = upload.getAttachment();
				if (attachment != null && attachment.getEntryCount() > 0) {
					MAttachmentEntry[] entries = attachment.getEntries();
					for (MAttachmentEntry entry : entries) {
						if (entry.getName().equals(upload.getFileName())) {
							attachment.deleteEntry(entry.getIndex());
						}
					}
				}
			} else {
				MArchive[] archives = MArchive.get(Env.getCtx(), " AND AD_Table_ID="+MRestUpload.Table_ID+" AND Record_ID="+upload.get_ID(), null);
				if (archives != null && archives.length == 1) {
					if (archives[0] != null) {
						archives[0].set_TrxName(upload.get_TrxName());
						archives[0].deleteEx(true);
					}
				}
			}
		}
    }	
}