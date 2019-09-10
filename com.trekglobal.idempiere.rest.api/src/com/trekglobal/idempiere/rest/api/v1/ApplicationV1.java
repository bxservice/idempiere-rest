/**
 * 
 */
package com.trekglobal.idempiere.rest.api.v1;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.glassfish.jersey.jackson.JacksonFeature;

import com.trekglobal.idempiere.rest.api.v1.auth.filter.RequestFilter;
import com.trekglobal.idempiere.rest.api.v1.auth.filter.ResponseFilter;
import com.trekglobal.idempiere.rest.api.v1.auth.impl.AuthServiceImpl;
import com.trekglobal.idempiere.rest.api.v1.resource.impl.FileResourceImpl;
import com.trekglobal.idempiere.rest.api.v1.resource.impl.ModelResourceImpl;
import com.trekglobal.idempiere.rest.api.v1.resource.impl.ProcessResourceImpl;
import com.trekglobal.idempiere.rest.api.v1.resource.impl.WindowResourceImpl;

/**
 * @author hengsin
 *
 */
public class ApplicationV1 extends Application {

	/**
	 * 
	 */
	public ApplicationV1() {
	}

	@Override
    public Set<Class<?>> getClasses() {
        final Set<Class<?>> classes = new HashSet<Class<?>>();
        
        classes.add(AuthServiceImpl.class);
        classes.add(RequestFilter.class);
        classes.add(ResponseFilter.class);
        classes.add(JacksonFeature.class);
        classes.add(ModelResourceImpl.class);
        classes.add(WindowResourceImpl.class);
        classes.add(ProcessResourceImpl.class);
        classes.add(FileResourceImpl.class);
        
        return classes;
    }	
}
