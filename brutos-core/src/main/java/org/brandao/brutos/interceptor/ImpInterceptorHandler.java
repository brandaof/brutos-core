/*
 * Brutos Web MVC http://brutos.sourceforge.net/
 * Copyright (C) 2009 Afonso Brandao. (afonso.rbn@gmail.com)
 *
 * This library is free software. You can redistribute it 
 * and/or modify it under the terms of the GNU General Public
 * License (GPL) version 3.0 or (at your option) any later 
 * version.
 * You may obtain a copy of the License at
 * 
 * http://www.gnu.org/licenses/gpl.html 
 * 
 * Distributed WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied.
 *
 */

package org.brandao.brutos.interceptor;

import org.brandao.brutos.AbstractApplicationContext;
import org.brandao.brutos.ResourceAction;

/**
 *
 * @author Afonso Brandao
 */
public class ImpInterceptorHandler implements InterceptorHandler{
    
    private String URI;

    private String requestId;

    private ResourceAction resourceAction;

    private AbstractApplicationContext context;
    
    private Object resource;

    public ImpInterceptorHandler() {
    }

    public String getURI() {
        return URI;
    }

    public void setURI(String URI) {
        this.URI = URI;
        this.setRequestId(URI);
    }

    public ResourceAction getResourceAction() {
        return resourceAction;
    }

    public void setResourceAction(ResourceAction resourceAction) {
        this.resourceAction = resourceAction;
    }

    public Object getResource() {
        return resource;
    }

    public void setResource(Object resource) {
        this.resource = resource;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String requestId() {
        return this.requestId;
    }

    public AbstractApplicationContext getContext(){
        return context;
    }

    public void setContext(AbstractApplicationContext context) {
        this.context = context;
    }

}
