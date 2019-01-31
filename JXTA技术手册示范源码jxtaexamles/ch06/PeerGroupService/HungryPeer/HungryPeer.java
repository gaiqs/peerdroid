import java.io.*;
import java.util.Enumeration;
import java.util.Vector;
import java.net.URL;

import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupFactory;
import net.jxta.exception.PeerGroupException;
import net.jxta.exception.ServiceNotFoundException;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Advertisement;
import net.jxta.document.StructuredDocument;
import net.jxta.document.Element;
import net.jxta.document.TextElement;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.MimeMediaType;
import net.jxta.discovery.DiscoveryService;
import net.jxta.pipe.PipeService;
import net.jxta.pipe.InputPipe;
import net.jxta.pipe.PipeID;
import net.jxta.pipe.OutputPipe;
import net.jxta.endpoint.Message;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.protocol.PeerGroupAdvertisement;
import net.jxta.protocol.ModuleSpecAdvertisement;
import net.jxta.protocol.ModuleImplAdvertisement;
import net.jxta.id.IDFactory;
import net.jxta.id.ID;
import net.jxta.platform.ModuleClassID;
import net.jxta.platform.ModuleSpecID;

// The HungryPeer is joining the RestoNet PeerGroup and searching
// for RestoPeers. The HungryPeer is establishing a pipe connection
// with all the RestoPeers that it has discovered. The Hungry peer
// sends auction requests for French fries to RestoPeers and
// then wait for auction bids from RestoPeers

public class HungryPeer {

    private PeerGroup netpg = null;     // NetPeergroup
    private PeerGroup restoNet = null;  // Resto Peergroup
    private DiscoveryService disco;            // Discovery Service
    private PipeService pipes;                 // Pipe Service
    private PipeAdvertisement myAdv;    // Hungry peer pipe advertisement
    private InputPipe myPipe;           // Input pipe to talk to hungry peer
    private MimeMediaType mimeType = new MimeMediaType("text", "xml"); // mime-type
    private int timeout = 3000;         // Discovery timeout
    private int rtimeout = 30000;       // Pipe Resolver Timeout
    private PeerGroupAdvertisement adv; //RestoNet Peergroup advertisement
    private Vector restoPeerAdvs = new Vector(); //  RestoPeers adv found
    private Vector restoPeerPipes = new Vector(); //  PestoPeers Pipe connection
    private String myIdentity = "Bill Joy";  // Identity of this HungryPeer
    private String friesRequest ="medium";   // Fries Auction request

    // Hungry Peer Main method to start our HungryPeer
    public static void main(String args[]) {
        HungryPeer myapp = new HungryPeer();
        myapp.startJxta();
        System.exit(0);
    }

    public void HungryPeer() { }

    // method to start the JXTA platform and our
    // HungryPeer
    private void startJxta() {

        try {
            // create, and Start the default jxta NetPeerGroup
            netpg = PeerGroupFactory.newNetPeerGroup();

        } catch (PeerGroupException e) {
            // could not instanciate the NetPeerGroup
            System.out.println("Fatal error : creating the NetPeerGroup");
            System.exit(1);
        }

        // Discover and Join the RestoNet Peergroup
        joinRestoNet();

        //Set our HungryPeer communication pipe so RestoPeers
        //can talk to us
        if (!setHungryPeerPipe()) {
            System.out.println(
               "Aborting due to failure to create our HungryPeer pipe");
            return;
        }

        //Attempt to locate RestoPeers in RestoNet
        discoverRestoServices();

        // Connect to RestoPeers that have been discovered
        connectToRestoPeers();

        // I am hungry send an auction request for French Fries
        // to the RestoPeers I have connected to
        sendFriesAuctionRequests();

        //Process incoming bids from RestoPeers
        receiveFriesBids();

    }

    // This method is used to attempt to discover the RestoNet
    // Peergroup in the current NetPeerGroup. If found the peer join
    // the RestoNet peergroup
    private boolean joinRestoNet() {

        int count = 3; // number of tries to search for the peergroup

        System.out.println("Attempting to discover the RestoNet Peergroup");

        // get the Discovery service handle from the NetPeerGroup pointer
        DiscoveryService hdisco = netpg.getDiscoveryService();

        Enumeration ae = null;

        // loop until we found the "RestoNet" Peergroup advertisement
            while (count-->0) {
            try {

                // Check if we have the advertisement in the local
                // peer cache
                ae = hdisco.getLocalAdvertisements(DiscoveryService.GROUP
                                                   , "Name"
                                                   , "RestoNet");

                // check if we got the advertisement, if we got
                // it we can exit the loop. Not necessary to go
                // further
                if ((ae != null) && ae.hasMoreElements()) break;

                // The "RestoNet" advertisement is not in the local
                // cache send a discovery request to search for the advertisement
                hdisco.getRemoteAdvertisements(null, DiscoveryService.GROUP,
		     "Name", "RestoNet", 1, null);

                // wait to give a chance to the discovery
                try {
                    Thread.sleep(timeout);
                } catch (Exception e){
                }
            } catch (IOException e){
                // found nothing!  move on
            }
        }

        // check if we found the RestoNet advertisement
        if (ae == null || !ae.hasMoreElements()) {
            System.out.println("Sorry could not find the RestoNet Peergroup");
            return false;
        }

        System.out.println("Found the RestoNet PeerGroup Advertisement");
        // Get the advertisement
        PeerGroupAdvertisement adv =
            (PeerGroupAdvertisement) ae.nextElement();

        try {

            // Call the PeerGroup Factory to instantiate a new
            // peergroup instance on that peer
            restoNet = netpg.newGroup(adv);

            // get the Discovery and Pipe Service to
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

    // Create the HungryPeer Communication pipe to receive bid responses
    // from RestoPeers. The advertisement of this pipe is sent as part
    // of the auction request for RestoPeers to respond.
    private boolean setHungryPeerPipe() {
        try {

            // get the Discovery and Pipe Service to
            // be used within the RestoNet Peergroup
            disco = restoNet.getDiscoveryService();
            pipes = restoNet.getPipeService();

            // Create a pipe advertisement for our hungry peer. This
            // pipe will be used within the RestoNet peergroup for other
            // peers to talk to our hungry peer
            myAdv = (PipeAdvertisement)
                AdvertisementFactory.newAdvertisement(
                PipeAdvertisement.getAdvertisementType() );

            // Initialize the advertisement with unique peer information
            // So we can communicate
            myAdv.setPipeID(IDFactory.newPipeID(restoNet.getPeerGroupID() ) );
            myAdv.setName("restoNet:HungryPipe:" + myIdentity);
            myAdv.setType(PipeService.UnicastType);

            // Create the input pipe
            myPipe = pipes.createInputPipe(myAdv);

        } catch (Exception e) {
            System.out.println("Could not create the HungryPeer pipe");
            return false;
        }

        return true;
    }

    // Discover RestoPeers that have joined RestoNet. We discover RestoPeer
    // via their published Service advertisement
    private void discoverRestoServices() {

        int found = 0;                  // Count of RestoPeer found
        RestoPeerService restoSrv = null;  // RestoPeer Service Advertisement

        System.out.println("Locating RestoPeers in the RestoNet Peergroup");

        try {
            // extract the RestoPeer service from the RestoNet peergroup
            // advertisement since it is now part of the RestoNet Peergroup
            ID  msrvID = null;
            PipeAdvertisement restoPipe = null;

            // get the Spec ID for the RestoPeerService to lookup the service
            // in the Peergroug handle
            try {
                msrvID = IDFactory.fromURL(new URL("urn", "",
                         RestoPeerService.Module_Spec_ID));

            } catch (java.net.MalformedURLException e) {
            } catch (java.net.UnknownServiceException e) {
                System.err.println(" Can't create restoPeer PipeID: UnknownServiceException ") ;
                System.exit(1);
            }

            // lookup for the RestoPeer peergroup service
            // we have to wait to make sure the service was completly initialized
            // before we can lookup the service
            while (restoSrv == null) {
                try {
                    Thread.sleep(2000);
                    restoSrv = (RestoPeerService) restoNet.lookupService(msrvID);

                } catch (Exception ex) { // This is ok
                }
            }

            // Extract the pipe advertisement from the RestoPeer service module
            // implementation advertisement. Get the ModuleImpl advertisement
            // associated with the service
            ModuleImplAdvertisement restoImpl = (ModuleImplAdvertisement)
                restoSrv.getImplAdvertisement();

            // extract the pipe from the param section of the Module Implementation
            restoPipe = (PipeAdvertisement)
                AdvertisementFactory.newAdvertisement((TextElement)
                      restoImpl.getParam().getChildren().nextElement());

            // save the pipe
            restoPeerAdvs.addElement(restoPipe);
            ++found;

        } catch (Exception ex) {
            System.out.println("Failure to connect to RestoPeer service pipe");
            ex.printStackTrace();
        }
        // Completed RestoPeer Discovery
        System.out.println("Found " + found + " RestoPeers Service");
    }

    // Method to connect and open output pipes to all the
    // RestoPeers that we have discovered. Each RestoPeer is
    // identified by its unique RestoPeer pipe advertisement.
    private void connectToRestoPeers() {

        // Enumerate all the RestoPeer pipe advertisments we have discovered
        // and attempt to connect a pipe which each of them
        for (Enumeration en = restoPeerAdvs.elements(); en.hasMoreElements();) {

            PipeAdvertisement padv = (PipeAdvertisement) en.nextElement();

            try {

                System.out.println("Attempt to connect to discovered RestoPeer");

                // Create an output Pipe connection to the RestoPeer
                OutputPipe pipeOut = pipes.createOutputPipe(padv, rtimeout);

                // Check if we have a connected Pipe
                if (pipeOut == null) { // Failed go to next RestoPeer
                    System.out.println("Failure to connect to RestoPeer Pipe:" +
                                       padv.getName());
                    continue;
                }

                // Save the output Pipe in RestoPeers connected structure
                restoPeerPipes.addElement(pipeOut);

                System.out.println("Connected pipe to " + padv.getName());

            } catch (Exception e) { // Error during connection go to next RestoPeer
                 System.out.println("RestoPeer may not be there anymore:" +
                                    padv.getName());
                 continue;
            }
        }
    }

    // Send an auction request for French Fries to all the RestoPeer
    // pipes we have successfully connected
    private void sendFriesAuctionRequests() {

        // Enumerate all the RestoPeer pipe connections we have successfully
        // connected with
        for (Enumeration en = restoPeerPipes.elements(); en.hasMoreElements();) {
            OutputPipe op = (OutputPipe) en.nextElement();
            try {

                // Construct the Request document
                StructuredDocument request  =
                    StructuredDocumentFactory.newStructuredDocument(mimeType,
                    "RestoNet:Request");

                // Fill up the Fries auction request argument
                Element re;
                re = request.createElement("Name", myIdentity);
                request.appendChild(re);
                re = request.createElement("Fries", friesRequest);
                request.appendChild(re);

                // create the pipe message to send
                Message msg = pipes.createMessage();

                // fill the first message element which is the HungryPeer
                // pipe advertisement return address. We need this
                // so RestoPeers can respond to us
                msg.addElement(msg.newMessageElement("HungryPeerPipe", mimeType,
                    myAdv.getDocument(mimeType).getStream()));

                // fill the second message element
                // the fries request. Insert the document
                //in the message
                msg.addElement(msg.newMessageElement("Request", mimeType,
                     request.getStream()));

                // send the auction message to the RestoPeer connected
                // pipe
                op.send(msg);

                System.out.println("Sent Fries Auction Request (" + friesRequest +
                                   ") to connected peers");

            } catch (Exception ex) { // Error sending auction request
                System.out.println("Failed to send auction request to RestoPeer");
            }
        }
    }

    // Receive bid requests from RestoPeers on the
    // HungryPeer listening pipe
    private void receiveFriesBids() {

        // Continue until we got all answers
        while (true) {

            Message msg = null;      // pipe message received
            String price = null;     // Fries price bid
            String brand = null;     // RestoPeer name which offers the bid
            String specials = null;  // specials offer bid
            InputStream ip = null;   // input stream to read message element
            StructuredDocument bid = null; //Bid document received

            try {

                // Wait for a bid message to arrive from a RestoPeer
                // Will block until a message arrive
                msg = myPipe.waitForMessage();

                // Check if the message is valid
                if (msg == null) {
                    if (Thread.interrupted()) {// interupted
                        // We have been asked to stop
                        System.out.println("Abort Receiving bid loop interrupted");
                        myPipe.close(); // Close the Pipe
                        return;
                    }
                }

            } catch (Exception ex) { // error in receiving message
                myPipe.close();
                System.out.println("Abort Receiving Error receiving bids");
                return;
            }

            // We got a message from a RestoPeer. Let's
            // extract and display infomation about the bid received
            try {

                // Extract the Bid document from the message
                ip = msg.getElement("Bid").getStream();
                bid = StructuredDocumentFactory.newStructuredDocument(mimeType,
                                                                        ip);

                // Parse the document to extract bid information
                Enumeration enum = bid.getChildren();
                while (enum.hasMoreElements()) {
                    Element element = (Element) enum.nextElement();
                    String attr = (String) element.getKey();
                    String value = (String) element.getValue();
                    if (attr.equals("Price")) {
                        price = value;
                        continue;
                    }
                    if (attr.equals("Brand")) {
                        brand = value;
                        continue;
                    }
                    if (attr.equals("Specials")) {
                        specials = value;
                        continue;
                    }
                }

                // We got a valid bid. Let's print it
                System.out.println("Received Fries Bid from RestoPeers (" +
                                   brand + ") at a Price ($" + price +
                                   ") \nRestoPeers Special (" + specials + ")");

            } catch (Exception e) { // Error extracting bid from the message
                System.out.println("Error extracting bid from the message");
                continue; // broken content;
            }
        }
    }
}
