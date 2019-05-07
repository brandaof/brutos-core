/*
 * Brutos Web MVC http://www.brutosframework.com.br/
 * Copyright (C) 2009-2017 Afonso Brandao. (afonso.rbn@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.brandao.brutos.proxy;

import java.lang.reflect.Method;

import org.brandao.brutos.ConfigurableApplicationContext;
import org.brandao.brutos.Invoker;
import org.brandao.brutos.MutableMvcRequest;
import org.brandao.brutos.RequestProvider;
import org.brandao.brutos.mapping.Controller;
import org.brandao.brutos.mapping.Action;

/**
 * 
 * @author Brandao
 */
public abstract class ActionEntityProxyHandler extends AbstractEntityProxyHandler {

	private Object resource;
	
	private Controller controller;
	
	private ConfigurableApplicationContext context;
	
	private Invoker invoker;

	public ActionEntityProxyHandler(Object resource, Controller controller,
			ConfigurableApplicationContext context, Invoker invoker) {
		this.resource   = resource;
		this.context    = context;
		this.controller = controller;
		this.invoker    = invoker;
		super.target    = resource;
	}

	public Object invoke(Object self, Method thisMethod, Method proceed,
			Object[] args) throws Throwable {
		
		if(super.isGetEntityProxyHandlerMethod(thisMethod)){
			return this;
		}
		
		Action action = controller.getMethod(thisMethod);
		return invoker.invoke(controller, context.getActionResolver()
				.getResourceAction(action, (MutableMvcRequest)RequestProvider.getRequest()), resource, args);
	}
	
}