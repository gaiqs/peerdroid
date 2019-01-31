import java.io.*;
import java.util.*;
import java.net.URL;
 
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupFactory;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.exception.PeerGroupException;
import net.jxta.discovery.DiscoveryService;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Advertisement;
import net.jxta.document.Element;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredTextDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredDocumentUtils;
import net.jxta.pipe.PipeService;
import net.jxta.pipe.PipeID;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.protocol.PeerGroupAdvertisement;
import net.jxta.protocol.ModuleSpecAdvertisement;
import net.jxta.protocol.ModuleImplAdvertisement;
import net.jxta.id.IDFactory;
import net.jxta.id.ID;
import net.jxta.platform.ModuleClassID;
import net.jxta.platform.ModuleSpecID;

import net.jxta.impl.peergroup.StdPeerGroupParamAdv;

// RestoPeer represents a restaurant that receives auction requests
// for french fries from HungryPeers. RestoPeers offers three sizes of
// french fries (small, large, medium). Each restaurant assignes a
// different price to each size. Each restaurant also offers a special
// offering.
//
// Each resturant is uniquely identified by its brand name.
//

public class RestoPeer {

    private PeerGroup netpg = null;      // The NetPeerGroup
    private PeerGroup restoNet = null;   // The restoNet Peergroup

    private String brand = "Chez JXTA";        // Brand of this restaurant

    // Services within the RestoNet peergroup
    private DiscoveryService disco = null;  // Discovery service
    private PipeService pipes = null;       // Pipe service
    private PipeAdvertisement myAdv = null; // My RestoPeer pipe advertisement

    private int timeout = 3000;          // discovery wait timeout

    // IDs within RestoNet
    private ModuleClassID mcID = IDFactory.newModuleClassID();
    private ModuleSpecID  msID = IDFactory.newModuleSpecID(mcID);
    private static PeerGroupID restoPeerGroupID;

   // main method to start our RestoPeer
    public static void main(String args[]) {
	RestoPeer myapp = new RestoPeer();
	myapp.startJxta();
	System.exit(0);
    }
    
    // Method to start the JXTA platform, join the RestoNet peergroup and
    // advertise the RestoPeer service
    private void startJxta() {
        try {
            //Discover and join (or start) the default peergroup
            netpg = PeerGroupFactory.newNetPeerGroup();
        } catch (PeerGroupException e) {
            //Couldn't initialize; can't continue
            System.out.println("Fatal error : creating the NetPeerGroup");
            System.exit(1);
        }

        // Discover (or create) and join the RestoNet peergroup
        try {
            joinRestoNet();
        } catch (Exception e) {
            System.out.println("Can't join or create RestoNet");
            System.exit(1);
        }

        // Wait while we process requests
        synchronized(RestoPeer.class) {
            try {
                RestoPeer.class.wait();
	    } catch (InterruptedException ie) {
	        System.out.println("Interrupted; exiting");
	    }
        }
    }    

    // Discover (or create) and join the RestoNet peergroup
    private void joinRestoNet() throws Exception {

        int count = 3;   // maximun number of attempts to discover
        System.out.println("Attempting to Discover the RestoNet PeerGroup");

        // Get the discovery service from the NetPeergroup
        DiscoveryService hdisco = netpg.getDiscoveryService();

        Enumeration ae = null;   // Holds the discovered peers

        // Loop until wediscover the RestoNet or
        // until we've exhausted the desired number of attempts
        while (count-- > 0) {
            try {
                // search first in the peer local cache to find
                // the RestoNet peergroup advertisement
                ae = hdisco.getLocalAdvertisements(DiscoveryService.GROUP,
                                          "Name", "RestoNet");

                // If we found the RestoNet advertisement we are done
                if ((ae != null) && ae.hasMoreElements())
                    break;

                // If we did not find it, we send a discovery request
                hdisco.getRemoteAdvertisements(null,
                       DiscoveryService.GROUP, "Name", "RestoNet", 1, null);

                // Sleep to allow time for peers to respond to the
                // discovery request
                try {
                    Thread.sleep(timeout);
                } catch (InterruptedException ie) {}
            } catch (IOException e){
                // Found nothing! Move on
            }
        }

        PeerGroupAdvertisement restoNetAdv = null;

        // Check if we found the RestoNet advertisement.
        // If we didn't, then either
        //       we are the first peer to join or
        //       no other RestoNet peers are up.
        // In either case, we must create the RestoNet peergroup

        if (ae == null || !ae.hasMoreElements()) {
            System.out.println(
                 "Could not find the RestoNet peergroup; creating one");
            try {
		// Create the RestoNetPeerGroup
		restoNet = createRestoPeerGroup();
		
		// Get the PeerGroup Advertisement
		restoNetAdv = netpg.getPeerGroupAdvertisement();
            } catch (Exception e) {
                System.out.println("Error in creating RestoNet Peergroup");
                throw e;
            }
        } else {
            // The RestoNet advertisement was found in the cache;
            // that means we can join the existing RestoNet peergroup

            try {
                restoNetAdv = (PeerGroupAdvertisement) ae.nextElement();
                restoNet = netpg.newGroup(restoNetAdv);
                System.out.println(
                    "Found the RestoNet Peergroup advertisement");
            } catch (Exception e) {
                System.out.println("Error in creating RestoNet PeerGroup from existing adv");
                throw e;
            }
        }

        try {
            // Get the discovery and pipe services for the RestoNet Peergroup
            disco = restoNet.getDiscoveryService();
            pipes = restoNet.getPipeService();
        } catch (Exception e) {
            System.out.println("Error getting services from RestoNet");
            throw e;
        }

        System.out.println("RestoNet Restaurant (" + brand + ") is on-line");
        return;
    }

    // This method is used to create a new instance of
    // the RestoNet peergroup.
    // Peergroups are implemented as modules.
    // Modules are used in JXTA  to load and manage dynamic code on a peer.
    //
    // A peergroup is represented by a set of advertisements: 
    //  1) A peergroup advertisement that advertises the peergroup
    //  2) A module spec advertisement that uniquely
    //         specifies the peergroup (set of peergroup services)
    //  3) A module impl advertisement that describes a
    //         peergroup implementation
    // This method must create all these advertisements
    private PeerGroup createRestoPeerGroup() {

	// Use a unique PeerGroup id as a constant so that the same
	// peergroup ID is used each time the RestoNet Peergroup is created.
	// It is essential that each RestoPeer use the same unique ID
	// so that two peers do not create different IDs for the
	// RestoNet peergroup. 
	//
	// The UUID in the URL constructor was generated via the
	// Shell mkpgrp command that created a new peergroup with a
	// unique peergroup ID.
        try {
            restoPeerGroupID = (PeerGroupID) IDFactory.fromURL(
                  new URL("urn", "",
                          "jxta:uuid-4d6172676572696e204272756e6f202002"));
        } catch (java.net.MalformedURLException e) {
            System.err.println("Can't create restoPeerGroupID: " + e);
            System.exit(1);
        } catch (java.net.UnknownServiceException e) {
            System.err.println("Can't create restoPeerGroupID: " + e);
            System.exit(1);
        }

	// Create a new Module Implementation advertisement that will
	// represent the new RestoPeer peergroup service

	// Assign a new SpecID that uniquely identifies the RestoPeer
	// peergroup service. Ths spec ID must be shared between all
	// instances of the RestoNet peergroup; it again is created via
	// the mkpgrp Shell command.
	ModuleSpecID  msrvID = null;
	try {
	    msrvID = (ModuleSpecID) IDFactory.fromURL(new URL("urn","",
		"jxta:uuid-737D1ED776B043E7A8718B102B62055A614CAC047AD240A8960ABDE6F7847C2306"));
	} catch (java.net.MalformedURLException e) { 
            System.err.println("Can't create restoPeer Spec Id:" + e);
            System.exit(1);
        } catch (java.net.UnknownServiceException e) {
            System.err.println("Can't create restoPeer Spec Id:" + e);
            System.exit(1);
        }        

	// Create a pipe advertisement to be used by Hungry Peers
	// to communicate with the RestoNet peergroup service
	PipeAdvertisement myAdv = (PipeAdvertisement)
	    AdvertisementFactory.newAdvertisement(
	    PipeAdvertisement.getAdvertisementType());
		
	// Again we assign a hardwired ID to the pipe, so everytime a
	// peer recreates the RestoNet peergroup, the same pipe id
	// is used.
	try {
	    myAdv.setPipeID((PipeID) IDFactory.fromURL(new URL("urn","",
              "jxta:uuid-9CCCDF5AD8154D3D87A391210404E59BE4B888209A2241A4A162A10916074A9504")));
	} catch (java.net.MalformedURLException e) {
            System.err.println("Can't create restoPeer PipeID: " + e);
            System.exit(1);
        } catch (java.net.UnknownServiceException e) {
            System.err.println(" Can't create restoPeer PipeID: " + e);
            System.exit(1);
        }        

	// The symbolic name of the pipe is built from
	// the brand name of RestoPeer. So each RestoPeer instance
	// must have a unique name.
	myAdv.setName("RestoNet:RestoPipe:" + brand);
	myAdv.setType(PipeService.UnicastType);

	// Create the module impl advertisement that represents
	// the RestoPeer service.
	// Peergroup services are also represented by modules since a
	// peergroup service needs to be dynamically loaded when the
	// corresponding peergroup is instantiated on the peer.
	// The constructor of the service 
	// module impl takes a set of arguments to describe the service:
	//    msrvID : unique spec ID of the restoNet service
	//    "RestoPeerService" : is the java class implementing the service
	//    "http://jxta.org/tutorial/RestoPeer.jar" : location to find jar file
	//    "sun.com" : provider of the service
	//    a description of the service
        ModuleImplAdvertisement restoImplAdv =
                createServiceModuleImplAdv(msrvID,
		"RestoPeerService", "http://jxta.org/tutorial/RestoPeer.jar",
		"sun.com","RestoPeer Service Module Implementation", myAdv);

        // Create a new peergroup that is a clone of the NetPeergroup
        // but with the added RestoPeer Service.
        return createPeerGroup(netpg, "RestoNet", restoImplAdv);
    }

    // This is a utility method to create a new peergroup
    // implementation. The new peergroup will have all the
    // same services as the parent peergroup; it will have an
    // additional service (the srvImpl).
    // 
    // The method creates the
    // new Peer Group Module Implementation Advertisement and
    // publishes it; creates the associated PeerGroup Adv and
    // publishes it; and instantiates the peergroup
    // using the just created PeerGroup Adv.
    //  
    // parent: parent peergroup the new group is created into
    // groupName : name of the new group
    // srvImpl :  new peergroup service to add to the default set of
    //            NetPeergroup services
    private PeerGroup  createPeerGroup(PeerGroup parent, String groupName, 
				       ModuleImplAdvertisement srvImpl) {

        PeerGroup myPeerGroup = null;
        PeerGroupAdvertisement myPeerGroupAdvertisement = null;
        ModuleImplAdvertisement myModuleImplAdv = null;
        
        // Create the PeerGroup Module Implementation adding the
	// new peergroup service
        myModuleImplAdv = createPeerGroupModuleImplAdv(parent, srvImpl);

        // Publish the new peergroup module implementation in the
        // parent peer group
        DiscoveryService parentDiscovery = parent.getDiscoveryService();

        // Publish advertisement in the parent peergroup with the default
        // lifetime and expiration time for remote publishing
        try {
            parentDiscovery.publish(myModuleImplAdv, DiscoveryService.ADV, 
		PeerGroup.DEFAULT_LIFETIME, PeerGroup.DEFAULT_EXPIRATION);
            parentDiscovery.remotePublish(myModuleImplAdv,
                DiscoveryService.ADV, PeerGroup.DEFAULT_EXPIRATION);
        }
        catch (java.io.IOException e) {
            System.err.println(
                "Cannot publish the new peergroup ModuleImplAdv");
            return null;
        }    

	System.out.println(
                "Published new Peergroup ModulImpl advertisement for : "
                + groupName);

        // Create and publish the new PeerGroup Advertisement
        // corresponding to the new peergroup implementation
        myPeerGroupAdvertisement= createPeerGroupAdvertisement(
				  myModuleImplAdv,groupName);

        // Publish the advertisementin the parent peer group with the
        // default lifetime and expiration time
        try {
            parentDiscovery.publish(myPeerGroupAdvertisement,
                DiscoveryService.GROUP, PeerGroup.DEFAULT_LIFETIME,
                PeerGroup.DEFAULT_EXPIRATION);
            parentDiscovery.remotePublish(myPeerGroupAdvertisement,
                DiscoveryService.GROUP, PeerGroup.DEFAULT_EXPIRATION);
        }
        catch (java.io.IOException e) {
            System.err.println("Cannot create the new PeerGroupAdvertisement");
	    return null;
        }

	System.out.println("Published the new PeerGroup advertisement for :"
	    + groupName);

	try {
	    // Finally, instantiate the new PeerGroup
            myPeerGroup = parent.newGroup(myPeerGroupAdvertisement);
	    System.out.println("Instantiated the new PeerGroup " + groupName);
        } catch (net.jxta.exception.PeerGroupException e) {
            System.err.println("Cannot create the new Peer Group");
            return null;
        }

        return myPeerGroup;
    }

    // This method is a generic method used to create a new
    // module impl advertisement for representing a peergroup
    // service. Each module implementation corresponds to a unique
    // module spec id that uniquely identifies a peergroup service.
    //
    // A peergroup service is represented by:
    //     id: unique module spec id identifying the new peergroup service
    //     code: java class that implement the peergroup service
    //     uri: location where we can download the java class implementing the service
    //     provider: identity of the provider
    //     desc: description of the service.
    //     padv: pipe advertisement to invoke the service
    private  ModuleImplAdvertisement createServiceModuleImplAdv(
               ModuleSpecID id,
	       String       code,
	       String       uri,
	       String       provider,
	       String       desc,
	       PipeAdvertisement padv) {

	// Construct a new module implementation advertisment
        ModuleImplAdvertisement ServiceModuleImplAdv =
            (ModuleImplAdvertisement)
            AdvertisementFactory.newAdvertisement(
                ModuleImplAdvertisement.getAdvertisementType());

	// Set a unique module Spec ID associated with this
	// service implementation
        ServiceModuleImplAdv.setModuleSpecID(id);

	// Set the java class that implements this service
        ServiceModuleImplAdv.setCode(code);
	
	// Set the description of the new service
        ServiceModuleImplAdv.setDescription(desc);

	// Define the compatibility Java JDK1.4 for loading this module
	StructuredTextDocument doc = (StructuredTextDocument)
                StructuredDocumentFactory.newStructuredDocument(
                    new MimeMediaType("text", "xml"), "Comp");
        Element e = doc.createElement("Efmt", "JDK1.4");
        doc.appendChild(e);
        e = doc.createElement("Bind", "V1.0 Ref Impl");
        doc.appendChild(e);
        ServiceModuleImplAdv.setCompat(doc);

	// Set the URL where we can download the code for this module
        ServiceModuleImplAdv.setUri(uri);

	// Set the provider for this module
        ServiceModuleImplAdv.setProvider(provider);

	// Set the pipe advertisement into a param doc to be used 
	// by a peer to connect to the service
	StructuredTextDocument paramDoc = (StructuredTextDocument)
                 StructuredDocumentFactory.newStructuredDocument
                 (new MimeMediaType("text/xml"), "Parm");
        StructuredDocumentUtils.copyElements(paramDoc, paramDoc,	
		 (Element) padv.getDocument(new MimeMediaType("text/xml")));
        ServiceModuleImplAdv.setParam(paramDoc);

	// Return the new module implementation for this service
        return ServiceModuleImplAdv;
    }

    // Create a Module Impl Advertisement that represents a peergroup
    // which is a clone of the NetPeerGroup, but adds a new peergroup
    // service to the peergroup.
    private ModuleImplAdvertisement createPeerGroupModuleImplAdv(
                PeerGroup parent, 
		ModuleImplAdvertisement serviceModuleImplAdv) {

        ModuleImplAdvertisement allPurposePeerGroupImplAdv = null;
        StdPeerGroupParamAdv PeerGroupParamAdv = null;

        try {
	    // Clone the parent (NetPeerGroup) implementation to get
	    // a module implementation
            allPurposePeerGroupImplAdv =
                parent.getAllPurposePeerGroupImplAdvertisement();
        }
        catch (Exception e) {
            System.err.println("Cannot get allPurposePeerGroupImplAdv " + e);
	    return null;
        }
        
	// Get the param field that conatins all the peergroup services
	// associated with the peergroup
        try {
            PeerGroupParamAdv = new StdPeerGroupParamAdv(
			        allPurposePeerGroupImplAdv.getParam());
        }
        catch (PeerGroupException e) {
            System.err.println("Cannot get StdPeerGroupParamAdv " + e);
 	    return null;
        }
	
	// Get the hashtable containaing the list of all the peergroup
	// services from the param advertisements
        Hashtable allPurposePeerGroupServicesHashtable =
            PeerGroupParamAdv.getServices();

	// Add our new peergroup service to the list of peergroup services
	allPurposePeerGroupServicesHashtable.put(mcID, serviceModuleImplAdv);
	
	// Update the PeerGroupModuleImplAdv with our new list
	// of peergroup services
	allPurposePeerGroupImplAdv.setParam((Element) 
	        PeerGroupParamAdv.getDocument(new MimeMediaType("text/xml")));

	// Set the new unique Spec ID that identifies this new peergroup
	// implementation
	allPurposePeerGroupImplAdv.setModuleSpecID(msID);
 
        // We are done creating our new peergroup implementation
        return allPurposePeerGroupImplAdv;
    }

    // This utility method is used to create a PeerGroup advertisement
    // associated with a module peergroup implementation
    private PeerGroupAdvertisement createPeerGroupAdvertisement(
	 ModuleImplAdvertisement implAdv, String groupName) {

	// Create a new peergroup advertisement
        PeerGroupAdvertisement myadv = (PeerGroupAdvertisement) 
	    AdvertisementFactory.newAdvertisement(
	        PeerGroupAdvertisement.getAdvertisementType());
        
        // Instead of creating a new group ID we use the
        // RestoPeer pre-defined peergroup id, so we create the same
        // peergroupId each time the group
	// is created
        myadv.setPeerGroupID(restoPeerGroupID);

	// Assign the pre-defined module spec id that corresponds
	// to our peergroup module implementation
        myadv.setModuleSpecID(implAdv.getModuleSpecID());
	
	// Assign peergroup name
        myadv.setName(groupName);

	// Set the peergroup description
        myadv.setDescription("This is RestoNet Peergroup");
        
        return myadv;
    }
}
