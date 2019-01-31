import java.io.*;
import java.net.*;
import java.util.*;

import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupFactory;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.exception.PeerGroupException;
import net.jxta.discovery.DiscoveryService;
import net.jxta.id.IDFactory;
import net.jxta.pipe.PipeService;
import net.jxta.protocol.PeerGroupAdvertisement;
import net.jxta.protocol.ModuleImplAdvertisement;


// RestoPeer represents a restaurant that receives auction requests
// for food from HungryPeers. The RestoPeer will discover and join
// the RestoNet and publish itself as a provider for HungryPeers

public class RestoPeer {

    private PeerGroup netpg = null;     // The NetPeerGroup
    private PeerGroup restoNet = null;  // The RestoNet Peergroup

    private String brand = "Chez JXTA";  // Brand of my restaurants
    private int timeout = 3000;          // Timeout; can be adjusted

    // Services within the RestoNet peergroup
    private DiscoveryService disco = null;  // Discovery service
    private PipeService pipes = null;       // Pipe service

    static String groupURL = "jxta:uuid-4d6172676572696e204272756e6f202002"; 

    public static void main(String args[]) {
        RestoPeer myapp = new RestoPeer();
        myapp.startJxta();
        System.exit(0);
    }

    // start the JXTA application
    private void startJxta() {
        try {
            // Discover and join (or start) the default peergroup
            netpg = PeerGroupFactory.newNetPeerGroup();
        } catch (PeerGroupException e) {
            // Couldn't initialize; can't continue
            System.out.println("Fatal error : creating the net PeerGroup");
            System.exit(1);
        }

        // Discover (or create) and join the RestoNet peergroup
        try {
            joinRestoNet();
        } catch (Exception e) {
            System.out.println("Can't join or create RestoNet");
            System.exit(1);
        }

        // Wait for HungryPeers
        System.out.println("Waiting for HungryPeers");
        while (true) {
            // In later examples, HungryPeer requests are processed here
        }
    }

    // Discover (or crete) and join the RestoNet peergroup
    private void joinRestoNet() throws Exception {

        int count = 3;   // maximun number of attempts to discover
        System.out.println("Attempting to Discover the RestoNet PeerGroup");

        // Get the discovery service from the NetPeergroup
        DiscoveryService hdisco = netpg.getDiscoveryService();

        Enumeration ae = null;  // Holds the discovered peers

        // Loop until we discover the RestoNet or
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
            } catch (IOException e) {
                // Found nothing! Move on.
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

                // Create a new, all-purpose peergroup.
                ModuleImplAdvertisement implAdv =
                    netpg.getAllPurposePeerGroupImplAdvertisement();
                restoNet = netpg.newGroup(
                                mkGroupID(),      // Assign new group ID
                                implAdv,          // The implem. adv
                                "RestoNet",       // Name of peergroup
                                "RestoNet, Inc.");// Description of peergroup

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
                     "Found the RestoNet Peergroup advertisement; joined existing group");
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

    private PeerGroupID mkGroupID() throws Exception {
        return (PeerGroupID) IDFactory.fromURL(
             new URL("urn", "", groupURL));
    }
}
