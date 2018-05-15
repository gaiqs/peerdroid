/*
 * Copyright (c) 2001 Sun Microsystems, Inc.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *       Sun Microsystems, Inc. for Project JXTA."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Sun", "Sun Microsystems, Inc.", "JXTA" and "Project JXTA" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact Project JXTA at http://www.jxta.org.
 *
 * 5. Products derived from this software may not be called "JXTA",
 *    nor may "JXTA" appear in their name, without prior written
 *    permission of Sun.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL SUN MICROSYSTEMS OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of Project JXTA.  For more
 * information on Project JXTA, please see
 * <http://www.jxta.org/>.
 *
 * This license is based on the BSD license adopted by the Apache Foundation.
 *
 * $Id: RouteResponseMsg.java,v 1.1 2005/05/03 06:43:36 hamada Exp $
 */

package net.jxta.protocol;

import net.jxta.document.Document;
import net.jxta.document.MimeMediaType;

/**
 * This class defines the EndpointRouter RouteResponse message 
 * The default behavior of this abstract class is simply  a
 *
 * This message is part of the Endpoint Routing Protocol.
 *
 *
 * <pre>
 *      &lt;?xml version="1.0" standalone='yes'?&gt;
 *       &lt;jxta:ERR&gt;
 *        &lt;Dst&gt;Route adv response&lt;/Dst&gt;
 *        &lt;Src&gt;Route adv of the responder's src&lt;/Src&gt;
 *       &lt;/jxta:ERR&gt;
 * </pre>
 *
 *
 * @see net.jxta.protocol.RouteQueryMsg
 *
 */

public abstract class RouteResponseMsg {

  private RouteAdvertisement dstRoute = null;
  private RouteAdvertisement srcRoute = null;

  /**
   * All messages have a type (in xml this is !doctype)
   * which identifies the message
   * @return String "jxta:ERR"
   */

    public static String getAdvertisementType() {
        return "jxta:ERR" ;
    }

  /**
   * set the destination route we were looking for
   *
   * @param dst destination route
   */
 
  public void setDestRoute(RouteAdvertisement dst) {
    dstRoute = dst;
  }

  /**
   * returns the destination route we were looking for
   *
   * @return route destination route advertisement
   */

    public RouteAdvertisement getDestRoute() {
        return  dstRoute;
    }


  /**
   * set the Route advertisement of the source peer that is originating
   * the query
   *
   * @param route RouteAdvertisement of the source
   */
 
  public void setSrcRoute(RouteAdvertisement route) {
    srcRoute = route;
  }

  /**
   * returns the route of the src peer that responded
   *
   * @return route RouteAdvertisement of the source peer
   * that responded to the query
   */

    public RouteAdvertisement getSrcRoute() {
        return  srcRoute;
    }

  /**
   * Write advertisement into a document. asMimeType is a mime media-type
   * specification and provides the form of the document which is being
   * requested. Two standard document forms are defined. "text/text" encodes
   * the document in a form nice for printing out, and "text/xml" which
   * provides an XML representation.
   *
   * @param asMimeType mime-type format requested
   * @return Document representation of the document as an advertisement
   */

    public abstract Document getDocument ( MimeMediaType asMimeType );

}







