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

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.brandao.brutos.interceptor.ConfigurableInterceptorHandler;
import org.brandao.brutos.interceptor.InterceptorHandlerImp;
import org.brandao.brutos.logger.Logger;
import org.brandao.brutos.logger.LoggerProvider;
import org.brandao.brutos.mapping.Action;
import org.brandao.brutos.mapping.Controller;
import org.brandao.brutos.mapping.DataTypeMap;
import org.brandao.brutos.mapping.ParameterAction;
import org.brandao.brutos.mapping.PropertyController;
import org.brandao.brutos.mapping.ThrowableSafeData;
import org.brandao.brutos.scope.Scope;

/**
 * 
 * @author Brandao
 */
public class Invoker {

	private static final ThreadLocal<ConfigurableApplicationContext> currentApp;

	static {
		currentApp = new ThreadLocal<ConfigurableApplicationContext>();
	}

	protected Logger logger = LoggerProvider.getCurrentLoggerProvider()
			.getLogger(Invoker.class);

	protected ObjectFactory objectFactory;
	
	protected ControllerManager controllerManager;
	
	protected ActionResolver actionResolver;
	
	protected ConfigurableApplicationContext applicationContext;
	
	protected ConfigurableRenderView renderView;
	
	protected ConfigurableRequestParser requestParser;
	
	protected RequestParserListener requestParserListener;
	
	protected CodeGenerator codeGenerator;
	
	public CodeGenerator getCodeGenerator() {
		return codeGenerator;
	}

	public void setCodeGenerator(CodeGenerator codeGenerator) {
		this.codeGenerator = codeGenerator;
	}

	public ObjectFactory getObjectFactory() {
		return objectFactory;
	}

	public void setObjectFactory(ObjectFactory objectFactory) {
		this.objectFactory = objectFactory;
	}

	public ControllerManager getControllerManager() {
		return controllerManager;
	}

	public void setControllerManager(ControllerManager controllerManager) {
		this.controllerManager = controllerManager;
	}

	public ActionResolver getActionResolver() {
		return actionResolver;
	}

	public void setActionResolver(ActionResolver actionResolver) {
		this.actionResolver = actionResolver;
	}

	public ConfigurableApplicationContext getApplicationContext() {
		return applicationContext;
	}

	public void setApplicationContext(
			ConfigurableApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	public ConfigurableRenderView getRenderView() {
		return renderView;
	}

	public void setRenderView(ConfigurableRenderView renderView) {
		this.renderView = renderView;
	}

	public ConfigurableRequestParser getRequestParser() {
		return requestParser;
	}

	public void setRequestParser(ConfigurableRequestParser requestParser) {
		this.requestParser = requestParser;
	}

	public RequestParserListener getRequestParserListener() {
		return requestParserListener;
	}

	public void setRequestParserListener(RequestParserListener requestParserListener) {
		this.requestParserListener = requestParserListener;
	}

	public RequestInstrument getRequestInstrument() {
		Scopes scopes = applicationContext.getScopes();
		Scope requestScope = scopes.get(ScopeType.REQUEST);

		RequestInstrument requestInstrument = getRequestInstrument(requestScope);

		return requestInstrument;
	}

	public StackRequest getStackRequest() {
		RequestInstrument requestInstrument = getRequestInstrument();
		return getStackRequest(requestInstrument);
	}

	public StackRequest getStackRequest(RequestInstrument value) {
		return (StackRequest) value;
	}

	public StackRequestElement getStackRequestElement() {
		return getStackRequest().getCurrent();
	}
	
	public Object invoke(Controller controller, ResourceAction action,
			Object[] parameters) throws InvokerException{
		return invoke(controller, action, null, parameters);
	}
	
	public Object invoke(Controller controller, ResourceAction action,
			Object resource, Object[] parameters) throws InvokerException{

		if (controller == null)
			throw new InvokerException("controller not found");

		if (action == null)
			throw new InvokerException("action not found");

		MutableMvcRequest request   = new DefaultMvcRequest();
		MutableMvcResponse response = new DefaultMvcResponse();
		
		request.setResource(resource);
		request.setResourceAction(action);
		request.setParameters(parameters);
		
		this.invoke(request, response);
		return response.getResult();
	}
	
	public Object invoke(Class<?> controllerClass, String actionId) {
		Controller controller = 
				applicationContext
				.getControllerManager()
				.getController(controllerClass);

		ResourceAction resourceAction = 
				actionResolver.getResourceAction(
						controller, 
						actionId, 
						(MutableMvcRequest)RequestProvider.getRequest()
				);
		
		if(resourceAction == null){
			return false;
		}
		else{
			return this.invoke(controller, resourceAction, null);
		}
	}

	public boolean invoke(MutableMvcRequest request, MutableMvcResponse response) throws InvokerException{

		request.setApplicationContext(this.applicationContext);
		
		if(!this.resolveAction(request, response)){
			return false;
		}
		
		boolean pushStackRequest      = false;
		StackRequest stackRequest     = null;
		MvcRequest oldRequest         = null;
		MvcResponse oldresponse       = null;
		
		try{
			currentApp.set(this.applicationContext);
			
			StackRequestElement element = createStackRequestElement();
			oldRequest                  = RequestProvider.init(request);
			oldresponse                 = ResponseProvider.init(response);
			
			RequestInstrument requestInstrument = getRequestInstrument();
			stackRequest = this.getStackRequest(requestInstrument);
			
			request.setRequestInstrument(requestInstrument);
			request.setStackRequestElement(element);
			
			stackRequest.push(element);
			pushStackRequest = true;
			
			this.invokeApplication(request, response, element, requestInstrument);
			
			this.updateActionResult(request, response);
			this.updateRequest(element.getController(), element.getResource(), request);
			
			if(!requestInstrument.isHasViewProcessed()){
				this.renderView(requestInstrument, request, response, element);
			}
			
			return true;
		}
		catch(Throwable e){
			throw new InvokerException(e);
		}
		finally {
			RequestProvider.destroy(oldRequest);
			ResponseProvider.destroy(oldresponse);
			
			if(pushStackRequest){
				stackRequest.pop();
			}
			
			currentApp.remove();			
		}
	}
	
	protected void resolveTypes(MutableMvcRequest request, 
			MutableMvcResponse response) throws RequestTypeException{
		
		ResourceAction resourceAction = request.getResourceAction();
		
		if(!this.isSupportedRequestType(resourceAction, request)){
			throw new RequestTypeException("request type not supported");
		}

		DataType responseDataType = this.selectResponseType(resourceAction, request);
		
		if(responseDataType == null){
			throw new ResponseTypeException("response type not supported");
		}

		response.setType(responseDataType);
		
	}
	
	protected boolean resolveAction(MutableMvcRequest request, 
			MutableMvcResponse response){
		
		if(request.getResourceAction() != null){
			if(request.getResource() == null){
				request
					.setResource(
						request.getResourceAction().getController().getInstance(objectFactory));
			}
			return true;
		}
		
		ResourceAction resourceAction = 
				actionResolver.getResourceAction(controllerManager, request);
		
		if(resourceAction == null){
			return false;
		}
		
		request.setResource(resourceAction.getController().getInstance(objectFactory));
		request.setResourceAction(resourceAction);
		
		return true;
	}
	
	protected void parseRequest(MutableMvcRequest request, MutableMvcResponse response){
		MutableRequestParserEvent event = new MutableRequestParserEventImp();
		event.setRequest(request);
		event.setResponse(response);
		
		try{
			this.requestParserListener.started(event);
			this.requestParser.parserContentType(request, 
					request.getType(), 
					this.applicationContext.getConfiguration(), event, 
					this.codeGenerator);
		}
		finally{
			this.requestParserListener.finished(event);
		}
	}
	
	protected void updateRequest(Controller controller, Object resource,
			MutableMvcRequest request) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException{
		
		List<PropertyController> properties = controller.getProperties();
		
		for(PropertyController property: properties){
			
			if(!property.canGet()){
				continue;
			}
			
			Object value = property.getValueFromSource(resource);
			
			Scope scope = this.applicationContext.getScopes().get(property.getScopeType());
			
			if(scope == null){
				throw new BrutosException("scope not found " + property.getScopeType());
			}
			
			if(value != null){
				scope.put(property.getName(), value);
			}
			else{
				scope.remove(property.getName());
			}
		}
		
	}
	
	protected void updateActionResult(MutableMvcRequest request, 
			MutableMvcResponse response){
		
		ResourceAction resourceAction = request.getResourceAction();
		
		if(resourceAction == null){
			return;
		}

		Action action = resourceAction.getMethodForm();
		
		if(action == null){
			return;
		}
		
		String resultName = action.getResultAction().getName();
		
		request.setProperty(
			resultName == null? 
				BrutosConstants.DEFAULT_RETURN_NAME : 
				resultName, 
			response.getResult()
		);

		ThrowableSafeData throwData = 
				request.getStackRequestElement().getThrowableSafeData();
		
		List<ParameterAction> params = action.getParameters();
		Object[] values = request.getParameters();
		
		if(values != null){
			int i=0;
			
			for(ParameterAction param: params){
				request.setProperty(
					param.getName() == null? 
						param.getRealName() : 
						param.getName(), 
					values[i++]
				);
			}
		}

		if(throwData != null){
			request.setProperty(throwData.getParameterName(), request.getThrowable());
			
			if(throwData.getAction().getExecutor() != null){
				ActionResolver actionResolver = this.applicationContext.getActionResolver();
				ResourceAction thrResourceAction = actionResolver.getResourceAction(throwData.getAction(), request);
				Object oldValue = request.getParameter(throwData.getParameterName());
				try{
					request.setParameter(throwData.getParameterName(), request.getThrowable());
					this.invoke(resourceAction.getController(), thrResourceAction, request.getResource(), null);
				}
				finally{
					request.setParameter(throwData.getParameterName(), oldValue);
				}
			}
			
		}
		
	}
	
	protected void invokeApplication(
			MutableMvcRequest request,
			MutableMvcResponse response,
			StackRequestElement element,
			RequestInstrument requestInstrument
			) throws Throwable{

		this.resolveTypes(request, response);
		
		try{
			this.parseRequest(request, response);
		}
		catch(Throwable e){
			request.setThrowable(e);
		}
		
		element.setAction(request.getResourceAction());
		element.setController(request.getResourceAction().getController());
		element.setRequest(request);
		element.setResponse(response);
		element.setResource(request.getResource());
		element.setObjectThrow(request.getThrowable());
		
		ConfigurableInterceptorHandler ih = new InterceptorHandlerImp(request, response);
		Controller controller = element.getController();
		
		controller.proccessBrutosAction(ih);
		
	}
	
	protected void renderView(
			RequestInstrument requestInstrument, MutableMvcRequest request, 
			MutableMvcResponse response, StackRequestElement element) 
					throws IllegalAccessException, IllegalArgumentException, 
					InvocationTargetException{
		renderView.show(request, response);
		requestInstrument.setHasViewProcessed(true);
	}
	
	protected boolean isSupportedRequestType(ResourceAction action, MutableMvcRequest request){
		
    	DataTypeMap supportedRequestTypes = action.getRequestTypes();
    	
    	if(supportedRequestTypes.isEmpty()){
    		return true;
    	}
    	else{
    		return supportedRequestTypes.accept(request.getType());
    	}
    	
	}

	protected DataType selectResponseType(ResourceAction action, MutableMvcRequest request){
		
    	DataTypeMap supportedResponseTypes = action.getResponseTypes();
    	List<DataType> responseTypes       = request.getAcceptResponse();
    	
    	if(supportedResponseTypes.isEmpty()){
    		
    		DataType defaultDataType = this.renderView.getDefaultRenderViewType();
    		
	    	for(DataType dataType: responseTypes){
	    		if(defaultDataType.equals(dataType)){
	    			return dataType;
	    		}
	    	}
	    	
    	}
    	else{
    		
	    	for(DataType dataType: responseTypes){
	    		if(supportedResponseTypes.accept(dataType)){
	    			return dataType;
	    		}
	    	}
	    	
    	}
    	
    	return null;
	}
	
	private RequestInstrument getRequestInstrument(Scope scope) {
		RequestInstrument requestInstrument = (RequestInstrument) scope
				.get(BrutosConstants.REQUEST_INSTRUMENT);

		if (requestInstrument == null) {
			requestInstrument = new RequestInstrumentImp(
					this.applicationContext, this.objectFactory,
					this.renderView);

			scope.put(BrutosConstants.REQUEST_INSTRUMENT, requestInstrument);
		}

		return requestInstrument;
	}

	protected StackRequestElement createStackRequestElement() {
		return new StackRequestElementImp();
	}

	public static ApplicationContext getCurrentApplicationContext() {
		return currentApp.get();
	}

	public static Invoker getInstance() {
		ConfigurableApplicationContext context = 
				(ConfigurableApplicationContext)getCurrentApplicationContext();

		if (context == null){
			throw new BrutosException("can't get invoker");
		}

		return context.getInvoker();
	}

	public void flush() {
	}
	
}
