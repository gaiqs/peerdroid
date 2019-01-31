import java.util.*;
import java.io.*;

import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupFactory;
import net.jxta.exception.PeerGroupException;
import net.jxta.document.Advertisement;
import net.jxta.document.StructuredTextDocument;
import net.jxta.document.MimeMediaType;
import net.jxta.document.TextElement;
import net.jxta.discovery.DiscoveryService;
import net.jxta.protocol.ModuleSpecAdvertisement;

import jxta.security.util.URLBase64;

public class RMIClientPeer {
    static Enumeration peers;
    static PeerGroup netpg;

    static int timeout = 10000;
    static int count = 3;

    public static void main(String[] args) throws Exception {
        try {
            netpg = PeerGroupFactory.newNetPeerGroup();
        } catch (PeerGroupException pge) {
            // Couldn't initialize; can't continue
            System.out.println("Fatal error : creating the NetPeerGroup");
            System.exit(-1);
        }
        if (!discoverRMIPeers()) {
            System.out.println("Can't find RMI peers");
            System.exit(-1);
        }
        callPeers();
        System.exit(0);
    }

    private static boolean discoverRMIPeers() {
        DiscoveryService disco = netpg.getDiscoveryService();
        disco.getRemoteAdvertisements(null,
                        DiscoveryService.ADV,
                        "Name", "JXTASPEC:RMIService:HelloService", 5, null);
        try {
            Thread.sleep(timeout);
        } catch (InterruptedException ie) {}

        System.out.println("Looking for RMI Service peers...");
        while (count-- > 0) {
            try {
                peers = disco.getLocalAdvertisements(DiscoveryService.ADV,
                        "Name", "JXTASPEC:RMIService:HelloService");
                if (peers != null && peers.hasMoreElements())
                    break;
                disco.getRemoteAdvertisements(null,
                               DiscoveryService.ADV,
                               "Name", "JXTASPEC:RMIService:HelloService", 5,
				null);
                try {
                    Thread.sleep(timeout);
                } catch (InterruptedException ie) {}
            } catch (Exception e) {
                // Try again
            }
        }

        if (peers == null || !peers.hasMoreElements())
            return false;
        return true;
    }

    private static void callPeers() {
        while (peers.hasMoreElements()) {
            try {
                Object o = peers.nextElement();
                ModuleSpecAdvertisement msa = (ModuleSpecAdvertisement) o;
                StructuredTextDocument doc = (StructuredTextDocument)
                    msa.getParam();
                if (doc == null) {
                    // No params
                    System.out.println("RMIService adv. has no params; ignoring");
                    continue;
                }
                Enumeration elements = doc.getChildren();
                String stub = null;
                while (elements.hasMoreElements()) {
                    TextElement te = (TextElement) elements.nextElement();
                    String elementName = te.getName();
                    if (elementName.equals("Stub")) {
                        stub = te.getTextValue();
                        break;
                    }
                }
                if (stub == null) {
                    System.out.println("Didn't find a stub parameter; ignoring");
                    continue;
                }
                byte[] enc = stub.getBytes();
                ByteArrayInputStream bais =
                    new ByteArrayInputStream(URLBase64.decode(enc,
                                                              0, enc.length));
                ObjectInputStream ois = new ObjectInputStream(bais);
                RMIService rs = (RMIService) ois.readObject();
                System.out.println("Remote service says " + rs.sayHello());
            } catch (Exception e) {
                System.out.println("Couldn't talk to peer -- " + e);
                e.printStackTrace();
            }
        }
    }
}
