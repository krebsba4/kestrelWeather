package ketrelweather.app;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import android.support.v4.app.Fragment;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class TextingFragment extends Fragment{
	private Handler handler = new Handler();

	public TextingFragment(){}

	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.texting, container, false);		
		return rootView;
	}
	
	@Override
	public void onStart(){
		super.onStart();
		Button sendButton = (Button)getView().findViewById(R.id.button_send);
		final TextView messageText = (TextView)getView().findViewById(R.id.text_messages);
		final EditText textEntry = (EditText)getView().findViewById(R.id.text_message);
		messageText.setVisibility(View.VISIBLE);
		messageText.setText("");
		textEntry.setEnabled(true);
		textEntry.setText("S");
		
		sendButton.setOnClickListener(new OnClickListener(){
			public void onClick(View view){
				if(MainActivity.outputSocket != null){
					sendMessage(MainActivity.outputSocket, textEntry.getText().toString());
					
					Context context = getActivity();
					CharSequence text = "Message Sent!";
					int duration = Toast.LENGTH_SHORT;

					Toast toast = Toast.makeText(context, text, duration);
					toast.show();
				}
				else{
					Context context = getActivity();
					CharSequence text = "bluetooth output socket not open";
					int duration = Toast.LENGTH_SHORT;

					Toast toast = Toast.makeText(context, text, duration);
					toast.show();
				}
				
				if(MainActivity.inputSocket != null){
					BluetoothSocketListener bsl = new BluetoothSocketListener(MainActivity.inputSocket, handler, messageText);
					Thread messageListener = new Thread(bsl);
					messageListener.start();
				}
				else{
					Context context = getActivity();
					CharSequence text = "bluetooth input socket not open";
					int duration = Toast.LENGTH_SHORT;

					Toast toast = Toast.makeText(context, text, duration);
					toast.show();		
					}
			}
		});

		
	}

	private void sendMessage(BluetoothSocket socket, String msg) {
		OutputStream outStream;
		try {
			outStream = new BufferedOutputStream(socket.getOutputStream());
			//outStream = socket.getOutputStream();
			byte[] byteString = (msg + " ").getBytes();
			//stringAsBytes[byteString.length - 1] = 0;
			outStream.write(byteString);
		} catch (IOException e) {
			Log.d("BLUETOOTH_MESSAGE_SENDER", e.getMessage());
		}
	}


	private class MessagePoster implements Runnable {
		private TextView textView;
		private String message;
		public MessagePoster(TextView textView, String message) {
			this.textView = textView;
			this.message = message;
		}
		public void run() {
			textView.setText(message);
		}
	}

	private class BluetoothSocketListener implements Runnable {
		private BluetoothSocket socket;
		private TextView textView;
		private Handler handler;
		
		public BluetoothSocketListener(BluetoothSocket socket, Handler handler, TextView textView) {
			this.socket = socket;
			this.textView = textView;
			this.handler = handler;
		}
		public void run() {
			int bufferSize = 1024;
			byte[] buffer = new byte[bufferSize];
			try {
				InputStream instream = socket.getInputStream();
				int bytesRead = -1;
				String message = "";
				while (true) {
					message = "";
					bytesRead = instream.read(buffer);
					if (bytesRead != -1) {
						while ((bytesRead==bufferSize)&&(buffer[bufferSize-1] != 0)) {
							message = message + new String(buffer, 0, bytesRead);
							bytesRead = instream.read(buffer);
						}
						message = message + new String(buffer, 0, bytesRead - 1);
						handler.post(new MessagePoster(textView, message));						
						Log.d("MESSAGE_REVIECER", "input stream: " + socket.getInputStream());
					}
				}
			} catch (IOException e) {
				Log.d("BLUETOOTH_MESSAGE_RECIEVER", e.getMessage());
			}
		}
	}
}
