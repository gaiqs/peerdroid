import java.io.*;
import java.util.Enumeration;
import java.util.Vector;

import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupFactory;
import net.jxta.exception.PeerGroupException;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Advertisement;
import net.jxta.discovery.DiscoveryService;
import net.jxta.pipe.PipeService;
import net.jxta.protocol.PeerGroupAdvertisement;

// HungryPeer tries to find a restaurant to
// get a good meal. It finds all RestoPeers in
// the RestoNet peergroup and (in later examples)
// requests bids from them.

public class HungryPeer {

    private PeerGroup netpg = null;     // NetPeergroup
    private PeerGroup restoNet = null;  // Resto Peergroup
    private int timeout = 3000;         // Timeout; can be adjusted

    // Services within the RestoNet Peergroup
    private DiscoveryService disco;     // Discovery Service
    private PipeService pipes;          // Pipe Service

    // Vector of discovered RestoPeers
    private Vector restaurantAdvs = new Vector();

    public static void main(String args[]) {
        HungryPeer myapp = new HungryPeer();
        myapp.startJxta();
        System.exit(0);
    }

    // Start the JXTA application
    private void startJxta() {
        try {
            // Discover (or create) and join the default jxta NetPeerGroup
            netpg = PeerGroupFactory.newNetPeerGroup();
        } catch (PeerGroupException e) {
            // Couldn't initialize; can't continue
            System.out.println("Fatal error : creating the net PeerGroup");
            System.exit(1);
        }

        // Discover and join the RestoNet Peergroup
        // HungryPeers never create the RestoNet peergroup
        try {
            if (!joinRestoNet()) {
                System.out.println("Sorry could not find the RestoNet Peergroup");
                System.exit(2);
            }
        } catch (Exception e) {
            System.out.println("Can't join RestoNet group");
            System.exit(1);
        }
    }

    // This method is used to discover the RestoNet Peergroup.
    // If found the peer will join the peergroup
    private boolean joinRestoNet() {

        int count = 3; // maximum number of attempts to discover

        System.out.println("Attempting to discover the RestoNet Peergroup");

        // Get the Discovery service handle from the NetPeerGroup
        DiscoveryService hdisco = netpg.getDiscoveryService();

        // All discovered RestoNet Peers
        Enumeration ae = null;

        // Loop until we find the "RestoNet" Peergroup advertisement
        // or we've exhausted the desired number of attempts
        while (count-- > 0) {
            try {
                // Check if we have the advertisement in the local
                // peer cache
                ae = hdisco.getLocalAdvertisements(DiscoveryService.GROUP,
                                            "Name", "RestoNet");

                // If we found the RestoNet advertisement, we are done
                if ((ae != null) && ae.hasMoreElements())
                    break;

                // The RestoNet advertisement is not in the local
                // cache . Send a discovery request to search for it.
                hdisco.getRemoteAdvertisements(null,
                       DiscoveryService.GROUP, "Name", "RestoNet", 1, null);

                // Wait to give peers a chance to respond
                try {
                    Thread.sleep(timeout);
                } catch (InterruptedException ie) {}
            } catch (IOException e) {
                // Found nothing! Move on.
            }
        }

        // Check if we found the RestoNet advertisement
        if (ae == null || !ae.hasMoreElements()) {
            return false;
        }

        System.out.println("Found the RestoNet PeerGroup Advertisement");
        // Get the advertisement
        PeerGroupAdvertisement adv =
            (PeerGroupAdvertisement) ae.nextElement();

        try {
            // Call the PeerGroup Factory to instantiate a new
            // peergroup instance
            restoNet = netpg.newGroup(adv);
        } catch (Exception e) {
          System.out.println("Could not create RestoPeerGroup");
          return false;
        }

        try {
            // Call the PeerGroup Factory to instantiate a new
            // peergroup instance
            restoNet = netpg.newGroup(adv);

            // Get the Discovery and Pipe services to
            // be used within the RestoNet Peergroup
            disco = restoNet.getDiscoveryService();
            pipes = restoNet.getPipeService();
        } catch (Exception e) {
          System.out.println("Could not create RestoPeerGroup");
          return false;
        }

        System.out.println("The HungryPeer joined the restoNet PeerGroup");
        return true;
    }
}
