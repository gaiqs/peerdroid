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
 * $Id: PeerGroupAdvertisement.java,v 1.2 2005/05/24 02:39:57 hamada Exp $
 */

package net.jxta.protocol;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import net.jxta.document.Element;
import net.jxta.document.ExtendableAdvertisement;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredDocumentUtils;
import net.jxta.id.ID;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.platform.ModuleSpecID;

/**
 * Describes a peer group and references additional information required
 * for instantiating it.
 *
 * The PeerGroup method newGroup performs the task of instantiating a
 * PeerGroup given its advertisement (provided the required subsequent
 * documents can actually be found).
 *
 * @see net.jxta.platform.ModuleSpecID
 * @see net.jxta.protocol.ModuleImplAdvertisement
 * @see net.jxta.peergroup.PeerGroup
 */

public abstract class PeerGroupAdvertisement extends ExtendableAdvertisement
    implements Cloneable {
    public static final String nameTag = "Name";
    public static final String gidTag = "GID";
    public static final String descTag = "Desc";
    public static final String msidTag = "MSID";
    public static final String svcTag = "Svc";
    public static final String mcidTag = "MCID";
    public static final String paramTag = "Parm";
    protected static final String [] fields = {nameTag, gidTag, descTag};
    
    private PeerGroupID gid = null;
    private ModuleSpecID specId = null;
    private String name = null;
    private String description = null;
    protected Map indexMap = new HashMap();

    // A table of structured documents to be interpreted by each service.
    // For cost and convenience reasons the elements in this table are
    // not cloned when the table is set or returned. For safe operation these
    // elements should be immutable, but we're helpless if they are not.
    private Hashtable serviceParams = new Hashtable();

    /**
     *  Returns the identifying type of this Advertisement.
     *
     * @return String the type of advertisement
     **/
    public static String getAdvertisementType() {
        return "jxta:PGA" ;
    }
    
    /**
     * {@inheritDoc}
     **/
    public final String getBaseAdvType() {
        return getAdvertisementType();
    }
    
    /**
     * Clone this PeerGroupAdvertisement
     *
     * @return Object an object of class PeerGroupAdvertisement that is a deep-enough
     * copy of this one.
     */
    public Object clone() {

        // All members are either immutable or never modified nor allowed to
        // be modified: all accessors return clones.
        try {
            return super.clone();
        } catch (CloneNotSupportedException impossible) { ; }
        return null;
    }

    /**
     * returns the name of the group.
     * @return String name of the group.
     *
     */

    public String getName() {
        return name;
    }

    /**
     * sets the name of the group.
     * @param name name of the group.
     *
     */

    public void setName(String name) {
        this.name = name;
        if (name != null) {
            indexMap.put(nameTag, name);
        } else {
            indexMap.remove(nameTag);
        }
    }

    // IDs are know to be immutable but that could change.
    // Accessors clone() them for safety; their clone method costs nothing.

    /**
     * Returns the id of the group spec that this uses.
     * @return ID the spec id
     *
     */

    public ModuleSpecID getModuleSpecID() {
        return specId;
    }

    /**
     * Sets the id of the group spec that this peer group uses.
     
     * @param sid The id of the spec
     *
     */

    public void setModuleSpecID(ModuleSpecID sid) {
        this.specId = sid;
    }

    /**
     * Returns the id of the group.
     *
     * @return ID the group id
     *
     */

    public PeerGroupID getPeerGroupID() {
        return gid;
    }

    /**
     * Sets the id of the group.
     
     * @param gid The id of this group.
     *
     */

    public void setPeerGroupID(PeerGroupID gid) {
        this.gid = gid;
        if (gid != null) {
            indexMap.put(gidTag, gid.toString());
        } else {
            indexMap.remove(gidTag);
        }
    }

    /**
     * Returns a unique ID for indexing purposes.
     * We use the id of the group as a plain ID.
     *
     * @return ID a unique id for that advertisement.
     *
     */

    public ID getID() {
        return gid;
    }

    /** 
     * returns the description
     * @return String the description
     * 
     */

    public String getDescription() {
        return description;
    }
 
    /** 
     * sets the description
     * @param description the description
     * 
     */

    public void setDescription(String description) {
        this.description = description;
        if (description != null) {
            indexMap.put(descTag, description);
        } else {
            indexMap.remove(descTag);
        }
    }
    
    /**
     * sets the sets of parameters for all services.
     * This method first makes a deep copy, in order to protect the active
     * information from uncontrolled sharing.  This quite an expensive
     * operation. If only a few of the parameters need to be
     * added, it is wise to use putServiceParam() instead.
     *
     * @param params The whole set of parameters.
     *
     */
    public void setServiceParams(Hashtable params) {
        if (params == null) {
            serviceParams = new Hashtable();
            return;
        }

        Hashtable copy = new Hashtable();

        Enumeration keys = params.keys();
        while (keys.hasMoreElements()) {
            ID key = (ID) keys.nextElement();
            Element e = (Element) params.get(key);
            Element newDoc = StructuredDocumentUtils.copyAsDocument(e);
            copy.put(key, newDoc);
        }

        serviceParams = copy;
    }

    /**
     * Returns the sets of parameters for all services.
     
     * <p/>This method returns a deep copy, in order to protect the real
     * information from uncontrolled sharing while keeping it shared as
     * long as it is safe. This quite an expensive operation. If only a few
     * parameters need to be accessed, it is wise to use getServiceParam()
     * instead.
     *
     * @return all of the parameters.
     *
     */
    public Hashtable getServiceParams() {
        Hashtable copy = new Hashtable();

        Enumeration keys = serviceParams.keys();
        while (keys.hasMoreElements()) {
            ID key = (ID) keys.nextElement();
            Element e = (Element) serviceParams.get(key);
            Element newDoc = StructuredDocumentUtils.copyAsDocument(e);
            copy.put(key, newDoc);
        }
        return copy;
    }

    /**
     * Puts a service parameter in the service parameters table
     * under the given key. The key is of a subclass of ID; usually a
     * ModuleClassID. This method makes a deep copy of the given element
     * into an independent document.
     *
     * @param key The key.
     * @param param The parameter, as an element. What is stored is a copy as
     * a standalone StructuredDocument which type is the element's name.
     *
     */
    public void putServiceParam(ID key, Element param) {
        if (param == null) {
            serviceParams.remove(key);
            return;
        }
        Element newDoc = StructuredDocumentUtils.copyAsDocument(param);
        serviceParams.put(key, newDoc);
    }

    /**
     * Returns the parameter element that matches the given key from the
     * service parameters table. The key is of a subclass of ID; usually a
     * ModuleClassID.
     *
     * @param key The key.
     *
     * @return StructuredDocument The matching parameter document or null if
     * none matched. The document type id "Param".
     *
     */
    public StructuredDocument getServiceParam(ID key) {
        Element param = (Element) serviceParams.get(key);
        if (param == null) return null;

        StructuredDocument newDoc =
            StructuredDocumentUtils.copyAsDocument(param);
        return newDoc;
    }

    /**
     * Removes and returns the parameter element that matches the given key
     * from the service parameters table. The key is of a subclass of ID;
     * usually a ModuleClassID.
     *
     * @param key The key.
     *
     * @return Element the removed parameter element or null if not found.
     * This is actually a StructureDocument of type "Param".
     *
     */
    public StructuredDocument removeServiceParam(ID key) {
        Element param = (Element) serviceParams.remove(key);
        if (param == null) return null;

        // It sound silly to clone it, but remember that we could be sharing
        // this element with a clone of ours, so we have the duty to still
        // protect it.

        StructuredDocument newDoc =
            StructuredDocumentUtils.copyAsDocument(param);
        return newDoc;
    }
    /**
     *  {@inheritDoc}
     */
    public final Map getIndexMap() {
        return  Collections.unmodifiableMap(indexMap);
    }
}
