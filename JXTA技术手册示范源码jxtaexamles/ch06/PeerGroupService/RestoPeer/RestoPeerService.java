import java.io.*;
import java.util.*;
import java.net.URL;

import net.jxta.service.Service;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupFactory;
import net.jxta.exception.PeerGroupException;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.Advertisement;
import net.jxta.document.Element;
import net.jxta.document.TextElement;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredTextDocument;
import net.jxta.discovery.DiscoveryService;
import net.jxta.pipe.PipeService;
import net.jxta.pipe.InputPipe;
import net.jxta.pipe.OutputPipe;
import net.jxta.pipe.PipeID;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.protocol.PeerGroupAdvertisement;
import net.jxta.protocol.ModuleImplAdvertisement;
import net.jxta.endpoint.Message;
import net.jxta.id.IDFactory;
import net.jxta.id.ID;

// The class RestoPeerService is an example of a PeerGroup Service.
// This service is registered as a RestoNet Peergroup service.
// This service implements the RestoPeer service for Chez JXTA.
public class RestoPeerService implements Service {


    // Service Module Spec Index ID
    public static String Module_Spec_ID =
        "jxta:uuid-737D1ED776B043E7A8718B102B62055A05";

    private String brand = "Chez JXTA";         // Brand of this restaurant
    private String specials = "large ($3.00)";  // Current restaurant Special

    private PeerGroup restoNet = null;   // The RestoNet Peergroup
    private PipeService pipes = null;    // Pipe service in the RestoNet

    private ModuleImplAdvertisement srvImpl = null;
    private PipeAdvertisement myAdv = null;  // My pipe advertisement
    private InputPipe pipeIn = null;         // Input pipe that we listening
                                             // to for requests
    private int rtimeout = 8000;             // Resolver pipe timeout

    // Service objects are not manipulated directly to protect usage
    //of the service. A Service interface is returned to access the service
    public Service getInterface() {
        return this;
    }

    // Returns the module impl advertisement for this service.
    public Advertisement getImplAdvertisement() {
        return srvImpl;
    }

    // Called when the peergroup initializes the service
    public void init(PeerGroup group, ID assignedID, Advertisement impl) {
        // Save the RestoNet pointer, the pipe and the RestoPeer Service
        restoNet = group;
        srvImpl = (ModuleImplAdvertisement) impl;
        System.out.println("Initialization RestoPeer Special Service: " +
                           srvImpl.getModuleSpecID());

        // Extract the service pipe advertisement from the service
        // module impl Advertisement. The client MUST use the same pipe
        // advertisement to talk to the service. When the client
        // discovers the peergroup advertisement, it will extract the
        // pipe advertisement to create the connecting pipe by getting
        // the module impl advertisement associated with the RestoPeer
        // service.
        System.out.println("Extract the pipe advertisement from the Service");
        PipeAdvertisement pipeadv = null;

        try {
            // Extract the pipe adv from the Module impl param section
            myAdv = (PipeAdvertisement)
                AdvertisementFactory.newAdvertisement((TextElement)
                      srvImpl.getParam().getChildren().nextElement());
        } catch (Exception e) {
            System.out.println("failed to read/parse pipe advertisement");
            e.printStackTrace();
            System.exit(-1);
        }

        // Start the RestoPeer service loop thread to respond to Hungry peers
        // fries requests. Start a new thread for processing all the
        // requests
        HandleFriesRequest peerRequest = new HandleFriesRequest();
        peerRequest.start();
    }

    // Called when the service is started
    public int startApp (String args[]) {
        // nothing to do here
        return 0;
    }

    // Called just before the service is stopped
    public void stopApp() {
    }

    // Thread to handle fries auction requests from HungryPeers.
    // The method waits for HungryPeer requests pipe messages to arrive.
    // Incoming requests contain a pipe advertisement to respond to the
    // HungryPeers requester and a fries size.
    // The method generates a bid offer for the request, opens an output
    // pipe to the HungryPeer requester and send the response.
    private class HandleFriesRequest extends Thread {

        InputStream ip = null;               // Input Stream of message
        PipeAdvertisement hungryPipe = null; // HungryPeer Requester pipe
        StructuredDocument request = null;   // Request document
        StructuredDocument bid = null;       // Bid response
        MimeMediaType mimeType = new MimeMediaType("text", "xml");
        Element el = null;                   // Element in document
        String name = null;                  // Name of the sender
        String size = null;                  // Fries size Requested
        OutputPipe pipeOut = null;           // Output pipe to respond to
                                             // HungryPeer requester

        public void run() {
            // Need to check that the Pipe peergroup service of
            // RestoNet is initialized. Peergroup services are loaded
            // dynamically. Here we just wait until the pipe service
            // is initialized so we can create a new pipe.
            try {
                while (pipes == null) {
                    Thread.sleep(2000);
                    pipes = restoNet.getPipeService();
                }

                // Create the input pipe to receive incoming request
                if (!createRestoPipe()) {
                    System.out.println(
                        "Aborting due to failure to create RestoPeer pipe");
                    return;
                }
            } catch (Exception ex) {
                    System.out.println(
                           "Exception while creating RestoPeer pipe");
                    return;
            }

            System.out.println("RestoNet Restaurant (" + brand +
                           ") waiting for HungryPeer requests");

            // Loop waiting for HungryPeer Requests
            while (true) {
                Message msg = null;          // incoming pipe message
                try {
                    // Block until a message arrive on the RestoPeer pipe
                    msg = pipeIn.waitForMessage();

                    // If message is null, discard message
                    if (msg == null) {
                        if (Thread.interrupted()) {
                            // We have been asked to stop
                            System.out.println("Abort: RestoPeer interrupted");
                            return;
                        }
                    }

                    // We received a message; extract the request
                    try {
                        // Extract the HungryPipe pipe information
                        // to reply to the sender
                        ip = msg.getElement("HungryPeerPipe").getStream();

                        // Construct the associated pipe advertisement
                        // via the AdvertisementFactory
                        hungryPipe = (PipeAdvertisement)
                        AdvertisementFactory.newAdvertisement(mimeType, ip);

                        // Extract the sender name and fries size requested
                        // building a StructuredDocument
                        ip = msg.getElement("Request").getStream();
                        request = StructuredDocumentFactory.
                                    newStructuredDocument(mimeType, ip);

                        // Extract the fields from the structured Document
                        Enumeration enum = request.getChildren();

                        // Loop over all the elements of the document
                        while (enum.hasMoreElements()) {
                            el = (Element) enum.nextElement();
                            String attr = (String) el.getKey();
                            String value = (String) el.getValue();

                            // Extract the HungryPeer Requester Name
                            if (attr.equals("Name")) {
                                name = value;
                                continue;
                            }

                            // Extract the Fries  size requested
                            if (attr.equals("Fries")) {
                                size = value;
                                continue;
                            }
                        }
                    } catch (Exception e) {
                        // Broken Content
                        continue;
                    }

                    System.out.println("Received Request from HungryPeer "
                               + name + " for " + size + " Fries.");

                    // The auction request is valid. We can
                    // create the output pipe to send the response bid to
                    // the HungryPeer requester
                    try {
                        System.out.println(
                           "Attempting to create Output Pipe to HungryPeer " +
                            name);

                        // Create an output pipe connection to the HungryPeer
                        // requester
                        pipeOut = pipes.createOutputPipe(hungryPipe, rtimeout);
                        // Check if we have a pipe
                        if (pipeOut == null) {
                            // Cannot conect the pipe
                            System.out.println(
                                "Could not find HungryPeer pipe");
                            continue;
                        }
                    } catch (Exception e) {
                        // Pipe creation exception
                        System.out.println(
                            "HungryPeer may not be listening anymore");
                        continue;
                    }

                    // We have a pipe connection to the HungryPeer. Now
                    // create the bid response document
                    try {
                        bid = StructuredDocumentFactory.newStructuredDocument(
                                     mimeType, "RestoNet:Bid");

                        // Set the Bid values (Brand, price, special)
                        // in the response document
                        el = bid.createElement("Brand", brand);
                        bid.appendChild(el);
                        el = bid.createElement("Price", ""+ friesPrice(size));
                        bid.appendChild(el);
                        el = bid.createElement("Specials", specials);
                        bid.appendChild(el);

                        // Create a new pipe message
                        msg = pipes.createMessage();

                        // Add the Bid offer to the message
                        msg.addElement(msg.newMessageElement(
                                   "Bid", mimeType, bid.getStream()));

                        pipeOut.send(msg);
                        pipeOut.close();

                    } catch (Exception ex) {
                        System.out.println(
                            "Error sending bid offer to HungryPeer " + name);
                    }

                    System.out.println("Sent Bid Offer to HungryPeer ("
                        + name  + ") Fries price = "  + friesPrice(size) +
                        ", special = " + specials);
                } catch (Exception e) {
                    System.out.println("Abort RestoPeer interrupted");
                    return;
                }
            }
        }
    }

    // Determine the price of the French fries depending on the size
    private String friesPrice(String size) {
        if (size.equals("small"))
              return "$1.50";
        if (size.equals("medium"))
              return "2.50";
        if (size.equals("large"))
              return "3.00";
        return "error";
    }

    // Create the input endpoint
    private boolean createRestoPipe() {
        try {
            // Create my input pipe to listen for hungry peers
            // requests
            pipeIn = pipes.createInputPipe(myAdv);
        } catch (Exception e) {
            System.out.println(
                "Could not initialize the Restaurant pipe" + e.getMessage());
            return false;
        }
        return true;
    }
}
