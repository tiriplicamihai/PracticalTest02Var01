package ro.pub.cs.systems.pdsd.practicaltest02var01;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;



class Utilities {

	public static BufferedReader getReader(Socket socket) throws IOException {
		return new BufferedReader(new InputStreamReader(socket.getInputStream()));
	}
	
	public static PrintWriter getWriter(Socket socket) throws IOException {
		return new PrintWriter(socket.getOutputStream(), true);
	}

}

public class PracticalTest02Var01Main extends Activity {

	Button startButton, allButton, tempButton, humButton;
	EditText portEdit;
	TextView infoText;
	
	ServerThread serverThread;
	int port;
	
	class Meteo {

		String temperature;
		String humidity;
	}
	
	
	class StartServerButtonListener implements Button.OnClickListener {

		@Override
		public void onClick(View v) {
			Log.d("CLICK", "STARTING SERVER");
			port = Integer.parseInt(portEdit.getText().toString());
			
			serverThread = new ServerThread();
			serverThread.startServer(port);
		}
		
	}
	
	class SendButtonListener implements Button.OnClickListener {

		@Override
		public void onClick(View v) {
			Button b = (Button) v;
			infoText.setText("");
			
			ClientThread clientThread = new ClientThread();
			clientThread.startClient(b.getText().toString());
		}
		
	}
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_practical_test02_var01_main);
        startButton = (Button) this.findViewById(R.id.start);
        allButton = (Button) this.findViewById(R.id.all);
        tempButton = (Button) this.findViewById(R.id.temperature);
        humButton =  (Button) this.findViewById(R.id.humidity);
        portEdit = (EditText) this.findViewById(R.id.port);
        infoText = (TextView) this.findViewById(R.id.info);
        
        startButton.setOnClickListener(new StartServerButtonListener());
        allButton.setOnClickListener(new SendButtonListener());
        tempButton.setOnClickListener(new SendButtonListener());
        humButton.setOnClickListener(new SendButtonListener());
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.practical_test02_var01_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onDestroy() {
      if (serverThread != null) {
        serverThread.stopServer();
      }
      super.onDestroy();
    }
    
 private class ClientThread extends Thread {
		
		private Socket socket = null;
		private String info = "";
		
		public void startClient(String info) {
			this.info = info;
			start();
		}
		
		@Override
		public void run() {
			try {				
				Socket socket = new Socket("127.0.0.1", port);
				
				PrintWriter writer = Utilities.getWriter(socket);
				
				writer.println(info);
				writer.flush();
				
				BufferedReader reader = Utilities.getReader(socket);
				
				while(true) {
					final String line = reader.readLine();
					if (line.equals("end"))
						break;
					
					infoText.post(new Runnable() {

						@Override
						public void run() {
							String text = infoText.getText().toString();
							infoText.setText(text + "\n" + line);
						}
						
					});
				}
				
				socket.close();
			} catch (Exception exception) {
					exception.printStackTrace();
			}			
		}
	}
    
    private class ServerThread extends Thread {
		
		private boolean isRunning;
		int port;
		private ServerSocket serverSocket;
		private Meteo cache;
		
		public void startServer(int port) {
			this.port = port;
			isRunning = true;
			cache = null;
			start();
		}
		
		public void stopServer() {
			isRunning = false;
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						if (serverSocket != null) {
							serverSocket.close();
						}
					} catch(IOException ioException) {
							ioException.printStackTrace();
						}
				}
			}).start();
		}
		
		@Override
		public void run() {
			Meteo meteo = null;
			try {
				serverSocket = new ServerSocket(port);
				Log.d("SERVER", "Server is running");
				if (cache != null)
					meteo = cache;
				else {
					cache = new Meteo();
					HttpClient httpClient = new DefaultHttpClient();
				
					String url = "http://api.openweathermap.org/data/2.5/weather?q=Bucharest,ro";
				
					HttpGet httpGet = new HttpGet(url);
					ResponseHandler handler = new BasicResponseHandler();
				
					String content = httpClient.execute(httpGet, handler);
					
					JSONObject obj = new JSONObject(content);
					
					cache.temperature = obj.getJSONObject("main").getString("temp");
					cache.humidity = obj.getJSONObject("main").getString("humidity");
					meteo = cache; 
				}
				
			while (isRunning) {
				Socket socket = serverSocket.accept();
				
				BufferedReader reader = Utilities.getReader(socket);
				
				String option = reader.readLine();
				
				Log.d("OPTION", option);
				
				PrintWriter writer = Utilities.getWriter(socket);
				if (option.compareTo("All") == 0) {
					writer.println(meteo.temperature);
					writer.println(meteo.humidity);
				}
		
				if (option.compareTo("Temperature") == 0) {
					writer.println(meteo.temperature);
				}
				if (option.compareTo("Humidity") == 0) {
					writer.println(meteo.humidity);
				}
				writer.println("end");
				
				socket.close();
			}
		} catch (Exception ioException) {
				ioException.printStackTrace();
		}
		}
	}
}
