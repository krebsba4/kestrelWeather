package ketrelweather.app;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

public class BluetoothFragment extends Fragment{
		private static BluetoothAdapter bluetooth;
		private UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
		private static int DISCOVERY_REQUEST = 1;
		private ArrayList<BluetoothDevice> foundDevices;
		private ArrayAdapter<BluetoothDevice> aa;
		private ListView list;

		public BluetoothFragment(){}
		
		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.bluetooth, container, false);		
			return rootView;
		}
		
		private void setUp(){
			configureBluetooth();		
			setupListenButton();
			setupSearchButton();	
			setupListView();
		}
		
		@Override
		public void onStart(){
			super.onStart();
			setUp();
		}
		
		private void configureBluetooth(){
			bluetooth = BluetoothAdapter.getDefaultAdapter();
		}

		private void setupListenButton(){
			Button listenButton = (Button)this.getActivity().findViewById(R.id.button_listen);
			listenButton.setOnClickListener(new OnClickListener(){
				public void onClick(View view){
					Intent disc;
					disc = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
					startActivityForResult(disc, DISCOVERY_REQUEST);
				}
			});
		}

		@Override
		public void onActivityResult(int requestCode, int resultCode, Intent data) {
			if (requestCode == DISCOVERY_REQUEST) {
				boolean isDiscoverable = resultCode > 0;
				if (isDiscoverable) {
					String name = "bluetoothserver";
					try {			
						final BluetoothServerSocket btserver = bluetooth.listenUsingRfcommWithServiceRecord(name, UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"));
						AsyncTask<Integer, Void, BluetoothSocket> acceptThread = new AsyncTask<Integer, Void, BluetoothSocket>() {

							@Override
							protected void onPostExecute(BluetoothSocket result) {
								if (result != null){
									Context context = getActivity();
									CharSequence text = "Bluetooth Server Connected!";
									int duration = Toast.LENGTH_SHORT;

									Toast toast = Toast.makeText(context, text, duration);
									toast.show();
									//switchUI();
								}						
							}

							@Override
							protected BluetoothSocket doInBackground(Integer... params) {
								try {
									Log.d("BLUETOOTH_SERVER", "attempting to open input socket with btserver: " + btserver.toString());
									MainActivity.inputSocket = btserver.accept();
									Log.d("BLUETOOTH_SERVER", "input socket open" + MainActivity.inputSocket.toString());
									return MainActivity.inputSocket;		
								} catch (IOException e) {
									Log.d("BLUETOOTH_SERVER", e.getMessage());
								}

								return null;
							}
						};
						acceptThread.execute(resultCode);

					} catch (IOException e) {
						Log.d("BLUETOOTH_SERVER", e.getMessage());
					}
				}
			} 
		}

		private void setupSearchButton(){
			Button searchButton = (Button)this.getActivity().findViewById(R.id.button_search);
			searchButton.setOnClickListener(new OnClickListener() {
				public void onClick(View view) {
					getActivity().registerReceiver(discoveryResult, new IntentFilter(BluetoothDevice.ACTION_FOUND));
					if (!bluetooth.isDiscovering()) {
						foundDevices.clear();
						bluetooth.startDiscovery();
					}
				}
			});
		}

		private void setupListView(){
			foundDevices = new ArrayList<BluetoothDevice>();
			aa = new ArrayAdapter<BluetoothDevice>(this.getActivity(), android.R.layout.simple_list_item_1, foundDevices);
			list = (ListView)this.getActivity().findViewById(R.id.list_discovered);
			list.setAdapter(aa);

			list.setOnItemClickListener(new OnItemClickListener() {
				public void onItemClick(AdapterView<?> arg0, View view, int index, long arg3) {
					
					Log.d("LIST_VIEW", " found device: " + foundDevices.get(index));
					foundDevices.get(index).fetchUuidsWithSdp();
					//Object []bluetoothArray = bluetoothSet.toArray();
					ParcelUuid [] uuid = ((BluetoothDevice)foundDevices.get(index)).getUuids();
					for(int i = 0; i < uuid.length; i++){
						Log.d("get uuid", "uuid's: " + uuid[0].toString());
					}
					
					AsyncTask<Integer, Void, Void> connectTask = new AsyncTask<Integer, Void, Void>() {

						@Override
						protected void onPostExecute(Void result) {
							
							Context context = getActivity();
							CharSequence text = "Bluetooth Client Connected!";
							int duration = Toast.LENGTH_SHORT;

							Toast toast = Toast.makeText(context, text, duration);
							toast.show();
									
							//switchUI();
						}
						@Override
						protected Void doInBackground(Integer... params) {
							try {
								BluetoothDevice device = foundDevices.get(params[0]);
								Log.d("BLUETOOTH_CLIENT", "attempting to open output socket");
								MainActivity.outputSocket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"));
								MainActivity.outputSocket.connect();
								Log.d("BLUETOOTH", "output socket open");
							} catch (IOException e) {
								Log.d("BLUETOOTH_CLIENT", e.getMessage());
							}
							return null;
						}
					};
					connectTask.execute(index);
				}
			});
		}


		BroadcastReceiver discoveryResult = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				BluetoothDevice remoteDevice;
				remoteDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if (bluetooth.getBondedDevices().contains(remoteDevice)) {
					foundDevices.add(remoteDevice);
					aa.notifyDataSetChanged();
				}
			}
		};


		/*private void switchUI() {
			final TextView messageText = (TextView)getView().findViewById(R.id.text_messages);
			final EditText textEntry = (EditText)getView().findViewById(R.id.text_message);
			messageText.setVisibility(View.VISIBLE);
			messageText.setText("testMessage");
			list.setVisibility(View.GONE);
			textEntry.setEnabled(true);
			textEntry.setText("S");
			textEntry.setOnKeyListener(new OnKeyListener() {
				public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
					if ((keyEvent.getAction() == KeyEvent.ACTION_DOWN) &&
							(keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_BACK)) {
						sendMessage(socket, textEntry.getText().toString());
						textEntry.setText("messageSent");
						return true;
					}
					return false;
				}
			});

			BluetoothSocketListener bsl = new BluetoothSocketListener(socket, handler, messageText);
			Thread messageListener = new Thread(bsl);
			messageListener.start();
		}*/
	}