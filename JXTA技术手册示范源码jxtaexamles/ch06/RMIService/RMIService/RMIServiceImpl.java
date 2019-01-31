import java.rmi.*;
import java.rmi.server.*;
import java.io.*;

import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupFactory;
import net.jxta.exception.PeerGroupException;
import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.StructuredTextDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.Element;
import net.jxta.document.MimeMediaType;
import net.jxta.discovery.DiscoveryService;
import net.jxta.protocol.ModuleImplAdvertisement;
import net.jxta.protocol.ModuleClassAdvertisement;
import net.jxta.protocol.ModuleSpecAdvertisement;
import net.jxta.platform.ModuleClassID;
import net.jxta.id.IDFactory;

import jxta.security.util.URLBase64;

public class RMIServiceImpl implements RMIService {

    PeerGroup netpg;
    String stub;

    public RMIServiceImpl() throws RemoteException { }

    public void init() throws Exception {
        try {
            //Discover and join (or start) the default peergroup
            netpg = PeerGroupFactory.newNetPeerGroup();
        } catch (PeerGroupException e) {
            //Couldn't initialize; can't continue
            System.out.println("Fatal error : creating the NetPeerGroup");
            System.exit(1);
        }
        makeStub();
        publishAdv();
    }

    // The stub is how RMI clients call RMI servers. RMI clients normally
    // get the stub from the lookup service via object serialization; in
    // this example, they'll get the stub from the module service
    // advertisement. Note that the serialized object is encoded as an
    // ASCII string so that it can be transmitted through a variety
    // of networks.
    private void makeStub() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(UnicastRemoteObject.exportObject(this));
        oos.close();
        stub = new String(URLBase64.encode(baos.toByteArray()));
    }

    public String sayHello() {
        return "Hello";
    }

    // Publish the advertisement. There's no point in caching this
    // advertisement because the RMI stub is invalid between runs. So we
    // always have to publish a new advertisement.
    public boolean publishAdv() {
        try {
            DiscoveryService disco = netpg.getDiscoveryService();
            ModuleClassAdvertisement mca = (ModuleClassAdvertisement)
                AdvertisementFactory.newAdvertisement(
                ModuleClassAdvertisement.getAdvertisementType());
            mca.setName("JXTAMOD:RMIService:HelloService");
            mca.setDescription("Sample RMI-as-JXTA Service");
            ModuleClassID mcID = IDFactory.newModuleClassID();
            mca.setModuleClassID(mcID);

            disco.publish(mca, DiscoveryService.ADV);
            disco.remotePublish(mca, DiscoveryService.ADV);

            ModuleSpecAdvertisement msa = (ModuleSpecAdvertisement)
                AdvertisementFactory.newAdvertisement(
                ModuleSpecAdvertisement.getAdvertisementType());
            msa.setName("JXTASPEC:RMIService:HelloService");
            msa.setVersion("Version 1.0");
            msa.setCreator("sun.com");
            msa.setSpecURI("http://www.jxta.org/tutorial/RMIService.jar");
            msa.setModuleSpecID(IDFactory.newModuleSpecID(mcID));
            StructuredTextDocument doc = (StructuredTextDocument)
                StructuredDocumentFactory.newStructuredDocument(
                new MimeMediaType("text/xml"), "Parm");
            Element e = doc.createElement("Stub", stub);
            doc.appendChild(e);
            msa.setParam(doc);
            disco.publish(msa, DiscoveryService.ADV);
            disco.remotePublish(msa, DiscoveryService.ADV);
            System.out.println("Created the RMI advertisement");
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static void main(String[] args) throws Exception {
        RMIServiceImpl rsi = new RMIServiceImpl();
        rsi.init();
        System.out.println("RMIService waiting for requests...");
        // If we exit, rsi becomes eligible for GC, which means
        // it could no longer handle requests
        synchronized(rsi) {
            rsi.wait();
        }
    }
}
