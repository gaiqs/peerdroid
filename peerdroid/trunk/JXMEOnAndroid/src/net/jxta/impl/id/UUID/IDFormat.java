/************************************************************************
 *
 * $Id: IDFormat.java,v 1.1 2005/05/03 06:49:59 hamada Exp $
 *
 * Copyright (c) 2001 Sun Microsystems, Inc.  All rights reserved.
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
 * 4. The names "Sun", "Sun Microsystems, Inc.", "JXTA" and "Project JXTA"
 *    must not be used to endorse or promote products derived from this
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
 *
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of Project JXTA.  For more
 * information on Project JXTA, please see
 * <http://www.jxta.org/>.
 *
 * This license is based on the BSD license adopted by the Apache Foundation.
 ********************************************************************************/

package net.jxta.impl.id.UUID;

import java.net.URI;
import java.net.URL;

import java.lang.reflect.UndeclaredThrowableException;
import java.net.MalformedURLException;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import net.jxta.id.ID;
import net.jxta.id.IDFactory;

/**
 *  A general purpose JXTA ID Format implementing all of the six standard ID
 *  Types. It was originally created for the Java 2 SE reference implementation.
 *  The 'uuid' format uses randomly generated UUIDs as the mechanism for
 *  generating canonical values for the ids it provides.
 *
 *  <p/>For IDs constructed using "seed" varient constructors, the first 16
 *  bytes of the seed are used literally as the UUID value. The value is masked
 *  to make it a valid version 4 IETF varient UUID.
 *
 *  @see net.jxta.id.ID
 *  @see <a href="http://spec.jxta.org/nonav/v1.0/docbook/JXTAProtocols.html#ids" target="_blank">JXTA Protocols Specification : IDs</a>
 *  @see <a href="http://spec.jxta.org/nonav/v1.0/docbook/JXTAProtocols.html#refimpls-ids-jiuft" target="_blank">JXTA Protocols Specification : UUID ID Format</a>
 */
public class IDFormat {

    /**
     *  Log4J Logger
     */
    private static final transient Logger LOG = Logger.getLogger(IDFormat.class.getName());

    /**
     *  number of bytes in the byte array
     */
    public final static int IdByteArraySize = 64;

    /**
     *  The size of a UUID in bytes
     */
    public final static int uuidSize = 16;

    /**
     *  The size of the flags field
     */
    public final static int flagsSize = 1;

    /**
     *  Location of the type field within the flags field
     */
    public final static int flagsIdTypeOffset = IDFormat.flagsSize - 1;

    /**
     *  Type value for Codat
     */
    public final static byte flagCodatID = 0x01;

    /**
     *  Type value for PeerGroup
     */
    public final static byte flagPeerGroupID = 0x02;

    /**
     *  Type value for Peer
     */
    public final static byte flagPeerID = 0x03;

    /**
     *  Type value for Pipe
     */
    public final static byte flagPipeID = 0x04;

    /**
     *  Type value for ModuleClass
     */
    public final static byte flagModuleClassID = 0x05;

    /**
     *  Type value for ModuleSpec
     */
    public final static byte flagModuleSpecID = 0x06;

    /**
     *  Location of ID flags within byte array.
     */
    public final static int flagsOffset = IDFormat.IdByteArraySize - IDFormat.flagsSize;

    /**
     *  Our local version of the world Peer Group ID. We need this for cases
     *  where we have to make ids which are in the world peer group. We only
     *  use this ID for those cases and never return this ID.
     */
    public static final PeerGroupID worldPeerGroupID = new PeerGroupID(
                new UUID(0x5961626164616261L, 0x4A78746150325033L)); //YabadabaJXTAP2P!

    /**
     *  Our local version of the net Peer Group ID. We need this for cases
     *  where we have to make ids which are in the net peer group. We only
     *  use this ID for those cases and never return this ID.
     */
    public static final PeerGroupID defaultNetPeerGroupID = new PeerGroupID(
                new UUID(0x5961626164616261L, 0x4E50472050325033L)); //YabadabaNPG P2P!

    /**
     *  This table maps our local private versions of the well known ids to the
     *  globally known version.
     */
    final static Object[] [] wellKnownIDs = {
                                                { net.jxta.peergroup.PeerGroupID.worldPeerGroupID, worldPeerGroupID },
                                                { net.jxta.peergroup.PeerGroupID.defaultNetPeerGroupID, defaultNetPeerGroupID }
                                            };

    /**
     * The instantiator for this ID Format which is used by the IDFactory.
     */
    public static final IDFactory.Instantiator INSTANTIATOR = new Instantiator();

    /**
     *  This class cannot be instantiated.
     */
    protected IDFormat() {}

    /**
     *  Translate from well known ID to our locally encoded versions.
     *
     *  @param input    the id to be translated.
     *  @return the translated ID or the input ID if no translation was needed.
     */
    static ID translateFromWellKnown(ID input) {
        for(int eachWellKnown = 0; eachWellKnown < wellKnownIDs.length; eachWellKnown++) {
            ID aWellKnown = (ID) wellKnownIDs[eachWellKnown][0];

            if(aWellKnown.equals(input))
                return (ID) wellKnownIDs[eachWellKnown][1];
        }

        return input;
    }

    /**
     *  Translate from locally encoded versions to the well known versions.
     *
     *  @param input    the id to be translated.
     *  @return the translated ID or the input ID if no translation was needed.
     */
    static ID translateToWellKnown(ID input) {
        for(int eachWellKnown = 0; eachWellKnown < wellKnownIDs.length; eachWellKnown++) {
            ID aLocalEncoding = (ID) wellKnownIDs[eachWellKnown][1];

            if(aLocalEncoding.equals(input))
                return (ID) wellKnownIDs[eachWellKnown][0];
        }

        return input;
    }


   /**
    *  Public member which returns a URI of the ID.
    *
    *  @param uniqueValue the unique portion of the ID
    *  @return the URI
    */
    protected static URI toURI(String uniqueValue) {
        return URI.create(ID.URIEncodingName + ":" + ID.URNNamespace + ":" + uniqueValue);
    }
}
