/*
 *  soapUI, copyright (C) 2004-2008 eviware.com 
 *
 *  soapUI is free software; you can redistribute it and/or modify it under the 
 *  terms of version 2.1 of the GNU Lesser General Public License as published by 
 *  the Free Software Foundation.
 *
 *  soapUI is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without 
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 *  See the GNU Lesser General Public License for more details at gnu.org.
 */

package com.eviware.soapui.impl.wsdl.submit.filters;

import com.eviware.soapui.impl.rest.RestRequest;
import com.eviware.soapui.impl.rest.support.XmlBeansRestParamsTestPropertyHolder;
import com.eviware.soapui.impl.rest.support.XmlBeansRestParamsTestPropertyHolder.RestParamProperty;
import com.eviware.soapui.impl.wsdl.submit.transports.http.BaseHttpRequestTransport;
import com.eviware.soapui.impl.wsdl.support.PathUtils;
import com.eviware.soapui.model.iface.Attachment;
import com.eviware.soapui.model.iface.SubmitContext;
import com.eviware.soapui.model.propertyexpansion.PropertyExpansionUtils;
import com.eviware.soapui.support.StringUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.xmlbeans.XmlBoolean;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * RequestFilter that adds SOAP specific headers
 *
 * @author Ole.Matzura
 */

public class RestRequestFilter extends AbstractRequestFilter
{
   @SuppressWarnings( "deprecation" )
   @Override
   public void filterRestRequest( SubmitContext context, RestRequest request )
   {
      HttpMethod httpMethod = (HttpMethod) context.getProperty( BaseHttpRequestTransport.HTTP_METHOD );

      String path = request.getPath();
      StringBuffer query = new StringBuffer();

      XmlBeansRestParamsTestPropertyHolder params = request.getParams();
      for( int c = 0; c < params.getPropertyCount(); c++ )
      {
         RestParamProperty param = params.getPropertyAt( c );

         String value = PropertyExpansionUtils.expandProperties( context, param.getValue() );
         if( !StringUtils.hasContent( value ) && !param.getRequired() )
            continue;

         switch( param.getStyle() )
         {
            case HEADER:
               httpMethod.setRequestHeader( param.getName(), value );
               break;
            case QUERY:
               if( query.length() > 0 )
                  query.append( '&' );

               query.append( URLEncoder.encode( param.getName() ) );
               if( StringUtils.hasContent( value ) )
                  query.append( '=' ).append( URLEncoder.encode( value ) );
               break;
            case TEMPLATE:
               path = path.replaceAll( "\\{" + param.getName() + "\\}", URLEncoder.encode( value ) );
               break;
            case MATRIX:
               if( param.getType().equals( XmlBoolean.type.getName() ) )
               {
                  if( value.toUpperCase().equals( "TRUE" ) || value.equals( "1" ) )
                  {
                     path += ";" + param.getName();
                  }
               }
               else
               {
                  path += ";" + param.getName();
                  if( StringUtils.hasContent( value ) )
                  {
                     path += "=" + URLEncoder.encode( value );
                  }
               }
         }
      }

      if( query.length() > 0 && !request.isPostQueryString() )
      {
         httpMethod.setQueryString( query.toString() );
      }

      if( PathUtils.isHttpPath( path ) )
      {
         try
         {
            httpMethod.setURI( new URI( path ) );
         }
         catch( Exception e )
         {
            e.printStackTrace();
         }
      }
      else
      {
         httpMethod.setPath( path );
      }

      String acceptEncoding = request.getAccept();
      if( StringUtils.hasContent( acceptEncoding ) )
      {
         httpMethod.setRequestHeader( "Accept", acceptEncoding );
      }

      String encoding = StringUtils.unquote( request.getEncoding() );

      if( request.hasRequestBody() && httpMethod instanceof EntityEnclosingMethod )
      {
         httpMethod.setRequestHeader( "Content-Type", request.getMediaType() );

         if( request.isPostQueryString())
         {
            ((EntityEnclosingMethod) httpMethod).setRequestEntity( new StringRequestEntity( query.toString() ) );
         }
         else
         {
            String requestContent = request.getRequestContent();
            Attachment[] attachments = request.getAttachments();

            if( StringUtils.hasContent( requestContent ) )
            {
               if( attachments.length == 0 )
               {
                  try
                  {
                     byte[] content = encoding == null ? requestContent.getBytes() : requestContent.getBytes( encoding );
                     ((EntityEnclosingMethod) httpMethod).setRequestEntity( new ByteArrayRequestEntity( content ) );
                  }
                  catch( UnsupportedEncodingException e )
                  {
                     ((EntityEnclosingMethod) httpMethod).setRequestEntity( new ByteArrayRequestEntity( requestContent.getBytes() ) );
                  }
               }
               else
               {

               }
            }
            else if( attachments.length > 0 )
            {
               if( attachments.length == 1 )
               {
                  try
                  {
                     ((EntityEnclosingMethod) httpMethod).setRequestEntity( new InputStreamRequestEntity(
                             attachments[0].getInputStream() ) );

                     httpMethod.setRequestHeader( "Content-Type", attachments[0].getContentType() );
                  }
                  catch( Exception e )
                  {
                     e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                  }
               }
               else
               {

               }
            }
         }
      }
   }
}
