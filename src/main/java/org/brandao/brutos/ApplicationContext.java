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

package org.brandao.brutos;

import java.util.Properties;

import org.brandao.brutos.logger.LoggerProvider;

/**
 * 
 * @author Brandao
 */
public interface ApplicationContext {

	Properties getConfiguration();

	MvcRequest getMvcRequest();

	MvcResponse getMvcResponse();

	DataType getRequestType();
	
	DataType getResponseType();
	
    String getActionParameterName();

    ActionType getActionType();
    
	DispatcherType getDispatcherType();
	
	LoggerProvider getLoggerProvider();
	
	boolean isAutomaticViewResolver();
	
	boolean isAutomaticExceptionMapping();
	
	boolean isAutomaticPropertyMapping();
	
    ScopeType getScopeType();
	
    FetchType getFetchType();
    
	String getViewPrefix();

	String getViewSuffix();

	String getViewIndex();

	String getSeparator();

	EnumerationType getEnumerationType();
	
    String getTemporalProperty();
	
	Scopes getScopes();

	Object getController(Class<?> clazz);

	Object getBean(Class<?> clazz);

	Object getBean(String name);

	RenderView getRenderView();
	
	RequestParser getRequestParser();
	
	Invoker getInvoker();

	ValidatorFactory getValidatorFactory();

	InterceptorManager getInterceptorManager();

	ControllerManager getControllerManager();

	ObjectFactory getObjectFactory();

	@Deprecated
	ControllerResolver getControllerResolver();

	ActionResolver getActionResolver();

	CodeGenerator getCodeGenerator();

	ViewResolver getViewResolver();

	TypeManager getTypeManager();

	ApplicationContext getParent();
	
}
