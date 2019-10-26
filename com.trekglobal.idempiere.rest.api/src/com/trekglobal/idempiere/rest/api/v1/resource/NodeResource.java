package com.trekglobal.idempiere.rest.api.v1.resource;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * 
 * @author hengsin
 *
 */
@Path("v1/nodes")
public interface NodeResource {

	public static final String LOCAL_ID="local";
	
	public static final String CURRENT_FILE_NAME = "current";
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getNodes();
	
	@Path("{id}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getNodeInfo(@PathParam("id") String id);

	@Path("{id}/logs")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getNodeLogs(@PathParam("id") String id);
	
	@Path("{id}/logs/{fileName}")
	@GET
	@Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.TEXT_HTML, MediaType.TEXT_PLAIN})
	public Response getNodeLogFile(@PathParam("id") String id, @PathParam("fileName") String fileName);
	
	@Path("{id}/logs")
	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteNodeLogs(@PathParam("id") String id);
	
	@Path("{id}/logs/rotate")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public Response rotateNodeLogs(@PathParam("id") String id);
}
