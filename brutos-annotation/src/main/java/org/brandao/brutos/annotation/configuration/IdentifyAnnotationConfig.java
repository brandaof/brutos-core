/*
 * Brutos Web MVC http://www.brutosframework.com.br/
 * Copyright (C) 2009 Afonso Brandao. (afonso.rbn@gmail.com)
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

package org.brandao.brutos.annotation.configuration;

import org.brandao.brutos.*;
import org.brandao.brutos.annotation.*;
import org.brandao.brutos.annotation.bean.BeanPropertyAnnotation;
import org.brandao.brutos.mapping.StringUtil;
import org.brandao.brutos.type.TypeManager;

/**
 *
 * @author Brandao
 */
@Stereotype(target=Identify.class,executeAfter={Controller.class, Bean.class,Action.class})
public class IdentifyAnnotationConfig extends AbstractAnnotationConfig{

    public boolean isApplicable(Object source) {
        return (source instanceof ActionParamEntry) ||
                (source instanceof BeanPropertyAnnotation &&
                !((BeanPropertyAnnotation)source).isAnnotationPresent(Transient.class));
    }

    public Object applyConfiguration(Object source, Object builder, 
            ConfigurableApplicationContext applicationContext) {
        
        if(source instanceof ActionParamEntry)
            addIdentify((ActionParamEntry)source, (ActionBuilder)builder, applicationContext);
        else
            addIdentify((BeanPropertyAnnotation)source, builder, applicationContext);
        
        return source;
    }
    
    protected void addIdentify(ActionParamEntry source, ActionBuilder builder,
            ConfigurableApplicationContext applicationContext){

        ParameterBuilder paramBuilder;
        
        if(TypeManager.isStandardType(source.getType()))
            paramBuilder = buildParameter(builder,source,applicationContext);
        else
            paramBuilder = addParameter(source,builder,applicationContext);
        
        super.applyInternalConfiguration(source, paramBuilder, applicationContext);
        
    }
    
    protected void addIdentify(BeanPropertyAnnotation source, Object builder,
            ConfigurableApplicationContext applicationContext){
        
        PropertyBuilder propertyBuilder;        
        if(!TypeManager.isStandardType(source.getType()))
            propertyBuilder = buildProperty((BeanBuilder)builder, source, applicationContext);
        else
            propertyBuilder = addProperty(source, builder, applicationContext);
        
        super.applyInternalConfiguration(
                source, propertyBuilder, applicationContext);
        
    }
    
    protected PropertyBuilder addProperty(BeanPropertyAnnotation property,Object builder,
            ConfigurableApplicationContext applicationContext){
        
        String propertyName = getPropertyName(property);
        String name = getBeanName(property);
        ScopeType scope = getScope(property.getAnnotation(Identify.class));
        EnumerationType enumProperty = getEnumerationType(property.getAnnotation(Enumerated.class));
        String temporalProperty = getTemporalProperty(property.getAnnotation(Temporal.class));
        org.brandao.brutos.type.Type type = getType(property.getAnnotation(Type.class));

        PropertyBuilder propertyBuilder;
        if(builder instanceof BeanBuilder){
            propertyBuilder = addProperty((BeanBuilder)builder,property, propertyName,
                name, scope, enumProperty, temporalProperty, type,
                applicationContext);
        }
        else{
            propertyBuilder = addProperty((ControllerBuilder)builder,property, propertyName,
                name, scope, enumProperty, temporalProperty, type,
                applicationContext);
        }
        
        return propertyBuilder;
    }
    
    protected PropertyBuilder addProperty(BeanBuilder beanBuilder, 
        BeanPropertyAnnotation property, String propertyName,
        String name, ScopeType scope, EnumerationType enumProperty,
        String temporalProperty, org.brandao.brutos.type.Type type,
        ConfigurableApplicationContext applicationContext){
        
        PropertyBuilder builder = 
            beanBuilder.addProperty(name, propertyName, enumProperty, 
            temporalProperty, name, scope, null, false, type);
        
        return builder;
    }

    protected PropertyBuilder addProperty(ControllerBuilder controllerBuilder, 
        BeanPropertyAnnotation property, String propertyName,
        String name, ScopeType scope, EnumerationType enumProperty,
        String temporalProperty, org.brandao.brutos.type.Type type,
        ConfigurableApplicationContext applicationContext){
        
        PropertyBuilder builder = 
            controllerBuilder.addProperty(propertyName, name, scope, 
                enumProperty, temporalProperty, null, null, false, type);
        
        return builder;
    }

     protected PropertyBuilder buildProperty(BeanBuilder beanBuilder, 
            BeanPropertyAnnotation property, 
            ConfigurableApplicationContext applicationContext){
        super.applyInternalConfiguration(new BeanEntryProperty(property), beanBuilder, 
                applicationContext);
        
        return beanBuilder.getProperty(property.getName());
    }
    
    protected ParameterBuilder buildParameter(ActionBuilder builder, 
            ActionParamEntry property, 
            ConfigurableApplicationContext applicationContext){
        
        
        super.applyInternalConfiguration(property, builder, 
                applicationContext);
        
        return builder.getParameter(builder.getParametersSize()-1);
    }
    
    protected ParameterBuilder addParameter(ActionParamEntry source,ActionBuilder builder,
            ConfigurableApplicationContext applicationContext){
        
        String name = source.getName();
        ScopeType scope = getScope(source.getAnnotation(Identify.class));
        EnumerationType enumProperty = getEnumerationType(source.getAnnotation(Enumerated.class));
        String temporalProperty = getTemporalProperty(source.getAnnotation(Temporal.class));
        org.brandao.brutos.type.Type type = getType(source.getAnnotation(Type.class));

        return builder.addParameter(name, scope, enumProperty, 
                temporalProperty, null, type, null, false, source.getType());
        
    }
    
    private org.brandao.brutos.type.Type getType(Type value){
        try{
            if(value != null){
                Class typeClass = value.value();
                return (org.brandao.brutos.type.Type)ClassUtil.getInstance(typeClass);
            }
            else
                return null;
            
            
        }
        catch(Exception e){
            throw new BrutosException(e);
        }
    }
    
    private String getTemporalProperty(Temporal value){
        if(value != null)
            return value.value();
        else
            return BrutosConstants.DEFAULT_TEMPORALPROPERTY;
    }
    
    private EnumerationType getEnumerationType(Enumerated value){
        if(value != null)
            return EnumerationType.valueOf(value.value());
        else
            return BrutosConstants.DEFAULT_ENUMERATIONTYPE;
    }
    
    private ScopeType getScope(Identify value){
        
        if(value != null){
            String scope = StringUtil.adjust(value.scope());
            if(!StringUtil.isEmpty(scope))
                return ScopeType.valueOf(value.scope());
        }
        
        return BrutosConstants.DEFAULT_SCOPETYPE;
    }
    
    private String getPropertyName(BeanPropertyAnnotation param){
        return param.getName();
    }

    private String getBeanName(BeanPropertyAnnotation property){
        Identify id = property.getAnnotation(Identify.class);
        if(id != null){
            String bean = StringUtil.adjust(id.bean());
            if(!StringUtil.isEmpty(bean))
                return id.bean();
        }
        
        return property.getName();
    }
    
}