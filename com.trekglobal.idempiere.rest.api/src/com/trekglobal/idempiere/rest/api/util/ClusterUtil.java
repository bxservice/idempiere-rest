package com.trekglobal.idempiere.rest.api.util;

import java.util.Collection;

import org.adempiere.base.IServiceHolder;
import org.adempiere.base.Service;
import org.idempiere.distributed.IClusterMember;
import org.idempiere.distributed.IClusterService;

public class ClusterUtil {

	private ClusterUtil() {
	}

	/**
	 * 
	 * @return cluster service
	 */
	public static IClusterService getClusterService() {
		IServiceHolder<IClusterService> holder = Service.locator().locate(IClusterService.class);
		IClusterService service = holder != null ? holder.getService() : null;
		return service;
	}
	
	/**
	 * 
	 * @param nodeId
	 * @return cluster member node
	 */
	public static IClusterMember getClusterMember(String nodeId) {
		IClusterService service = getClusterService();
		if (service != null) {
			Collection<IClusterMember> members = service.getMembers();
			for(IClusterMember member : members) {
				if (member.getId().equals(nodeId))
					return member;
			}
		}
		return null;
	}
}
