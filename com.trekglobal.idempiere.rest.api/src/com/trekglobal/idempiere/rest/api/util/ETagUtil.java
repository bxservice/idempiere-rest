package com.trekglobal.idempiere.rest.api.util;

import java.io.Serializable;

import javax.ws.rs.core.EntityTag;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.MessageDigestAlgorithms;
import org.apache.commons.lang3.SerializationUtils;

/**
 * E-Tag 
 * @author Igor Pojzl, Cloudempiere
 *
 */
public class ETagUtil {

	public static EntityTag getHash(Object object) {
		byte[] data = SerializationUtils.serialize((Serializable) object);
		String tag = new DigestUtils(MessageDigestAlgorithms.SHA_1).digestAsHex(data);
		EntityTag entityTag = new EntityTag(tag);
		return entityTag;
	}
	
	public static boolean evaluate(EntityTag datatag, EntityTag requestTag) {
		if(!requestTag.isWeak() && !datatag.isWeak() && requestTag.equals(datatag)) {
			return true;
		}		
		return false;
	}

}
