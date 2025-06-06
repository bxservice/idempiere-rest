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
package com.trekglobal.idempiere.rest.api.v1.resource;

import java.io.InputStream;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.trekglobal.idempiere.rest.api.json.QueryOperators;

@Path("v1/uploads")
public interface UploadResource {

	/**
	 * Request body for the upload initiation.
	 * 
	 * @param fileName The name of the file to be uploaded.
	 * @param contentType The MIME type of the file (e.g., "image/png", "application/pdf").
	 * @param fileSize The total size of the file in bytes.
	 * @param chunkSize The preferred size of each chunk in the upload, in bytes.
	 * @param sha256 The SHA-256 hash of the file content for integrity verification.
	 */
	static record UploadInitiationRequest(String fileName, String contentType, long fileSize, long chunkSize, String sha256) {		
	};
	
	/**
	 * Response for the upload initiation request.
	 * 
	 * @param uploadId The unique ID of the upload session.
	 * @param chunkSize The preferred size of each chunk in the upload, in bytes.
	 * @param expiresAt The expiration time of the upload session.
	 */
	static record UploadInitiationResponse(String uploadId ,long chunkSize, String expiresAt ) {		
	};
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * Initiates a new upload session for a file.
	 * 
	 * @param request Metadata for the file to be uploaded.
	 * @return {@link UploadInitiationResponse}
	 */
	Response initiateUpload(UploadInitiationRequest request);
	
	/**
	 * Response for the upload chunk request.
	 * 
	 * @param uploadId The unique ID of the upload session.
	 * @param chunkOrder The order of the chunk in the upload sequence.
	 * @param receivedSize The size of the chunk that has been received.
	 * @param message Additional message or details about the upload processing.
	 */
	static record UploadChunkResponse(String uploadId, int chunkOrder, long receivedSize, String message) {		
	};
	
	@Path("{uploadId}/chunks/{chunkOrder}")
	@PUT
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * Uploads a chunk of the file to the server.
	 * 
	 * @param uploadId id of the upload session
	 * @param chunkOrder order of the chunk
	 * @param sha256 hash of the chunk data
	 * @param chunkData chunk data to be uploaded
	 * @return Response containing the detailed of the upload ({@link UploadChunkResponse})
	 */
	Response uploadChunk(@PathParam("uploadId") String uploadId, @PathParam("chunkOrder") int chunkOrder,
			@HeaderParam("X-Content-SHA256") String sha256,
			InputStream chunkData);
	
	/**
	 * Represents a chunk that has been successfully uploaded.
	 * @param chunkOrder The order of the chunk in the upload sequence.
	 * @param receivedSize The size of the chunk that has been received.
	 */
	static record UploadedChunk(
			int chunkOrder,
			long receivedSize) {
	}
	
	/**
	 * Response for the upload status request.
	 * 
	 * @param uploadId The unique ID of the upload session.
	 * @param fileName The name of the file being uploaded.
	 * @param fileSize The total size of the file being uploaded.
	 * @param chunkSize The preferred size of each chunk in the upload, in bytes.
	 * @param status Status of the upload processing (INITIATED, UPLOADING, PROCESSING, COMPLETED, CANCELED or FAILED).
	 * @param uploadedChunks List of chunks that have been successfully uploaded ({@link UploadedChunk}).
	 * @param totalReceivedSize Total size of all received chunks so far.
	 * @param message Additional message or details about the upload processing.
	 */
	static record UploadStatusResponse(String uploadId, String fileName, long fileSize, long chunkSize, String status,
			List<UploadedChunk> uploadedChunks, long totalReceivedSize, String message) {
	};
	
	/**
     * Retrieves the current status of an ongoing or completed upload.
     *
     * @param uploadId The unique ID of the upload session.
     * @return Response containing the detailed status of the upload ({@link UploadStatusResponse}).
     */
    @GET
    @Path("/{uploadId}")
    @Produces(MediaType.APPLICATION_JSON)
    Response getUploadStatus(@PathParam("uploadId") String uploadId);
    
    /**
     * Request body for the upload completion.
     * @param fileName The final name of the file being uploaded (optional).
     * @param totalChunks The total number of chunks expected for the file upload.
     * @param uploadLocation optional The upload location for the file upload, e.g. archive (default), image or attachment.
     */
    static record UploadCompletionRequest(String fileName, String totalChunks, String uploadLocation) {    	
	};
	
	/**
	 * Response for the upload completion request.
	 * 
	 * @param uploadId The unique ID of the upload session.
	 * @param status Status of the upload processing (INITIATED, UPLOADING, PROCESSING, COMPLETED, CANCELED or FAILED).
	 * @param message Additional message or details about the upload processing.
	 */
    static record UploadCompletionResponse(String uploadId, String status, String message) {    	
    };
    
    /**
     * Notifies the server that all chunks have been uploaded and the file
     * should be reassembled.
     *
     * @param uploadId The unique ID of the upload session.
     * @param completionRequest Request body containing final verification details like total chunks.
     * @return Response indicating that the file processing has been accepted or completed ({@link UploadCompletionResponse}).
     */
    @POST
    @Path("/{uploadId}/complete")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Response finalizeUpload(@PathParam("uploadId") String uploadId, UploadCompletionRequest completionRequest);

    /**
     * Cancels an ongoing upload and cleans up any temporarily stored chunks.
     *
     * @param uploadId The unique ID of the upload session to be canceled.
     * @return Response indicating the outcome of the cancellation.
     */
    @DELETE
    @Path("/{uploadId}")
    @Produces(MediaType.APPLICATION_JSON)
    Response cancelUpload(@PathParam("uploadId") String uploadId);
    
    @GET
    @Path("/{uploadId}/file")
	@Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.TEXT_HTML, MediaType.TEXT_PLAIN})
	/**
	 * Get uploaded file content as binary stream or json
	 * @param uploadId id of the upload session
	 * @param asJson if provided, the response will be in JSON format with file content as base64 encoded string.
	 * @return response
	 */
	public Response getFile(@PathParam("uploadId") String uploadId, @QueryParam(QueryOperators.AS_JSON) String asJson);
    
    @GET
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * Get pending uploads from current user.
	 * @return json array of upload
	 */
	public Response getPendingUploads();
    
    /**
     * Request body for the copy uploaded file.
     * 
     * @param copyLocation optional The copy location of the destination record, e.g. archive (default), image or attachment.
     * @param tableName optional The table name of the destination record.
     * @param recordId optional The record id/uuid of the destination record.
     */
    static record CopyUploadedFileRequest(String copyLocation, String tableName, String recordId) {    	
	};
	
	/**
     * Response for the copy uploaded file.
     * 
     * @param uploadId The unique ID of the upload session.
     * @param tableName The table name of the destination record.
     * @param recordId The record id of the destination record.
     * @param recordUU The record uuid of the destination record.
     * @param copyLocation The copy location of the destination record, e.g. archive (default), image or attachment.
     * @param fileName The name of the file being copied.
     * @param contentType The MIME type of the file being copied (e.g., "image/png", "application/pdf").
     * @param fileSize The size of the file being copied.
     */
    static record CopyUploadedFileResponse(String uploadId, String tableName, int recordId, String recordUU, 
    		String copyLocation, String fileName, String contentType, int fileSize) {    	
	};
    
    /**
     * Copy uploaded file to attachment, image or archive (edited)
     *
     * @param uploadId The unique ID of the upload session.
     * @param copyRequest optional Request body containing destination record like table name and record id/uuid.
     * @return Response indicating that the file has been copied ({@link CopyUploadedFileResponse}).
     */
    @POST
    @Path("/{uploadId}/copy")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Response copyUploadedFile(@PathParam("uploadId") String uploadId, CopyUploadedFileRequest copyRequest);
}
