
import java.rmi.*;

public interface RMIService extends Remote {
    public String sayHello() throws RemoteException;
}
