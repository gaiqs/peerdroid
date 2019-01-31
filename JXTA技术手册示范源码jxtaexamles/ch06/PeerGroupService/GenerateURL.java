import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupFactory;
import net.jxta.pipe.PipeID;
import net.jxta.id.IDFactory;

public class GenerateURL {

    public static void main(String[] args) throws Exception {
        PeerGroup pg = PeerGroupFactory.newNetPeerGroup();
        PipeID id = IDFactory.newPipeID(pg.getPeerGroupID());
        System.out.println(id);
    }
}
