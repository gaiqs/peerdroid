package peerdroid.sample.dialog;

import peerdroid.sample.PeerDroidSample;
import peerdroid.sample.R;
import android.app.Dialog;
import android.widget.EditText;

/**
 *  P2P Streaming Player for Google Android
 *  
 *  Android Dialog Windows used to show network information like
 *  connection speed or type.
 *
 * @author   Marco Picone 
 * @created  01/09/2008
 */
public class NetworkInfoDialog extends Dialog {
   
	private PeerDroidSample activity;

	/**
	 * Class constructor
	 * @param activity
	 */
	public NetworkInfoDialog( PeerDroidSample activity) {
       super(activity);
       this.activity = activity;
    }

	/**
	 * Method call when the Dialog Window start
	 */
    protected void onStart() {
        super.onStart();
        setContentView(R.layout.network_info);
        getWindow().setFlags(4, 4);
        setTitle("Network Info");     
    }
  
}
