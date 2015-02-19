package com.example.boattracker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;


public class MainActivity extends Activity implements  LocationListener {

	EditText text = null;
	RadioGroup radio = null;
	TextView latitude = null;
	TextView longitude = null;
	
	String identifiant = null;
	String lat = null;
	String lon = null;
	String type = null;
	String batterie = null;

	LocationManager objgps = null;;
	Location loc = null;
	
	Intent batteryStatus = null;
	
	int frequence;
	String freq;
	
	SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
	String date = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		frequence = 1000;
		text = (EditText) findViewById(R.id.editText1);
		latitude = (TextView) findViewById(R.id.textView3);
		longitude = (TextView) findViewById(R.id.textView5);
		radio = (RadioGroup) findViewById(R.id.choixType);
		objgps = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		
		objgps.requestLocationUpdates(LocationManager.GPS_PROVIDER,frequence,1, this);
		
		
	}
	
	

	private void determinerType(){
		if (radio.getCheckedRadioButtonId() == R.id.radio2){
			type = "Bouee";
		} else {
			type = "Bateau";
		}
	}
	
	private void afficherLocation() {
		lat = String.valueOf(loc.getLatitude());
		lon = String.valueOf(loc.getLongitude());
		latitude.setText(lat);
		longitude.setText(lon);
	}
	
	private void recupererDate() {
		date = df.format(Calendar.getInstance().getTime());
	}
	
	private void recupererIdentifiant(){
		identifiant = text.getText().toString();
	}
	
	private void recupererNiveauBatterie() {
		IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		batteryStatus = registerReceiver(null, ifilter);
		int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
		float niveauBat = level / (float)scale * 100.0f;
		batterie = String.valueOf(niveauBat);
	}

	private void changerFrequence(){
		if (freq != null){
			frequence = Integer.valueOf(freq);
			objgps.requestLocationUpdates(LocationManager.GPS_PROVIDER,frequence,1, this);
		}
	}
	@Override
	public void onLocationChanged(Location location) {
		this.loc = location;
		
		afficherLocation();
		recupererIdentifiant();
		determinerType();
		recupererDate();
		recupererNiveauBatterie();
		changerFrequence();
		
		Log.i("BoatTracker", "frequence:" + freq);
		if (identifiant.length() > 0){
			EnvoiRequete Rqt = new EnvoiRequete();
			RecupererInstructions Rqtt = new RecupererInstructions();
			Rqt.execute();
			Rqtt.execute();
		}
		
	}

	@Override
	public void onStatusChanged(String provider, int status,
			Bundle extras) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}
	

	
	// Envoi de la requête HTTP
	private class EnvoiRequete extends AsyncTask<String, Integer, Double>{

		@Override
		protected Double doInBackground(String... params) {
				envoyerMessage();
			return null;
		}

		public void envoyerMessage() {
			
			HttpClient client = new DefaultHttpClient();
			HttpPost post = new HttpPost("http://172.20.10.3/GSCtuto/reception4.php");
			//HttpPost post = new HttpPost("http://10.29.226.210:8888/cartes/reception.php");
			//HttpPost post = new HttpPost("http://orion-brest.com/TestProjetS5/reception1&1.php");

			
			try {
				List<NameValuePair> donnees = new ArrayList<NameValuePair>();
				donnees.add(new BasicNameValuePair("type", type));
				donnees.add(new BasicNameValuePair("nom", identifiant));
				donnees.add(new BasicNameValuePair("latitude", lat));
				donnees.add(new BasicNameValuePair("longitude", lon));
				donnees.add(new BasicNameValuePair("heure", date));
				donnees.add(new BasicNameValuePair("batterie", batterie));
				post.setEntity(new UrlEncodedFormEntity(donnees));
				client.execute(post);
				text.setKeyListener(null);
				
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
			} catch (IOException e) {
				// TODO Auto-generated catch block
			}

			
		}

	}
	
	private class RecupererInstructions extends AsyncTask<String, Integer, Double>{

		@Override
		protected Double doInBackground(String... params) {
			demanderInfos();
			return null;
		}
		
		public void demanderInfos(){
			
			InputStream is = null;
			String result = "";

			// Envoi de la commande http
			try{
				HttpClient httpclient = new DefaultHttpClient();
				HttpPost httppost = new HttpPost("http://172.20.10.3/GSCtuto/envoi.php");
				HttpResponse response = httpclient.execute(httppost);
				HttpEntity entity = response.getEntity();
				is = entity.getContent();
			} catch(Exception e){
				Log.e("log_tag", "Error in http connection " + e.toString());
			}
			
			// Convertion de la requête en string
			try{
				BufferedReader reader = new BufferedReader(new InputStreamReader(is,"iso-8859-1"),8);
				StringBuilder sb = new StringBuilder();
				String line = null;
				while ((line = reader.readLine()) != null) {
					sb.append(line + "\n");
				}
				is.close();
				result=sb.toString();
				}catch(Exception e){
					Log.e("log_tag", "Error converting result " + e.toString());
				}
			
			// Parse les données JSON
			try{
				JSONArray jArray = new JSONArray(result);
				for(int i=0;i<jArray.length();i++){
					JSONObject json_data = jArray.getJSONObject(i);
					// Affichage Frequence
					Log.i("log_tag","freq: "+json_data.getString("FrequenceEmission"));
					// Résultats de la requête
					freq += jArray.getJSONObject(i); 
				}
			}catch(JSONException e){
				Log.e("log_tag", "Error parsing data " + e.toString());
			}
		}
	}
	

	

}
