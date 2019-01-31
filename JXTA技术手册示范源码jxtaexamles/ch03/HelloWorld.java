import java.io.StringWriter;

import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupFactory;
import net.jxta.exception.PeerGroupException;
import net.jxta.document.Advertisement;
import net.jxta.document.StructuredTextDocument;
import net.jxta.document.MimeMediaType;
import net.jxta.discovery.DiscoveryService;
import net.jxta.pipe.PipeService;
import net.jxta.resolver.ResolverService;
import net.jxta.membership.MembershipService;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.protocol.PeerGroupAdvertisement;

public class HelloWorld {

    static PeerGroup group = null;

    DiscoveryService disco;
    ResolverService resolv;
    PipeService pipe;
    MembershipService member;
    PeerGroupAdvertisement pgadv;

    public static void main(String args[]) {
        HelloWorld myapp = new HelloWorld();
        myapp.startJxta();
        System.exit(0);
    }

    private void startJxta() {

        try {
            // create and start the default jxta NetPeerGroup
            group = PeerGroupFactory.newNetPeerGroup();
        } catch (PeerGroupException e) {
            // could not instantiate the group; print the stack and exit
            System.out.println("Fatal error : creating the net PeerGroup");
            System.exit(1);
        }

        System.out.println("Started Hello World");

        // get the PeerGroup Advertisement                 
        pgadv = group.getPeerGroupAdvertisement();
        
        try {
            // Print out the PeerGroup Advertisement Document
            // To do so, we must convert it to a text document with
            // whatever MIME type we want. First, we'll print it as
            // a plain text document.
            StructuredTextDocument doc = (StructuredTextDocument)
                       pgadv.getDocument(new MimeMediaType("text/plain"));
            
            // Now that we have a plain text document
            // we can print the document via the StructuredTextDocument
            // senddtoWriter method
            StringWriter out = new StringWriter();
            doc.sendToWriter(out);
            System.out.println(out.toString());
            out.close();

            // For comparison, now we'll do the same thing but print it
            // out as an XML document.
            StructuredTextDocument adoc = (StructuredTextDocument)
                      pgadv.getDocument(new MimeMediaType("text/xml"));
            StringWriter aout = new StringWriter();
            adoc.sendToWriter(aout);
            System.out.println(aout.toString());
            aout.close();        
            
            // Now we'll access the PeerGroup service
            // and get various information from it
            PeerGroupID pgid = group.getPeerGroupID();
            System.out.println ("pgid= " + pgid);
            
            PeerID pid = group.getPeerID();
            System.out.println("pid= " + pid);
            
            String name = group.getPeerName();
            System.out.println("peer name=" + name);

            // Get the core services. We don't use the core services
            // in this example, but this is how you retrieve them if
            // you need them.
            disco = group.getDiscoveryService();
            pipe = group.getPipeService();
            member = group.getMembershipService();
            resolv = group.getResolverService();
            
            System.out.println("All done");

        } catch (Exception ex) {
             ex.printStackTrace();
        }
    }
}
