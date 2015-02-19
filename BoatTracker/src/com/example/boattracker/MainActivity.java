package com.example.boattracker;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

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
	
	Intent batteryStatus;
	
	SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
	String date = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		text = (EditText) findViewById(R.id.editText1);
		latitude = (TextView) findViewById(R.id.textView3);
		longitude = (TextView) findViewById(R.id.textView5);
		radio = (RadioGroup) findViewById(R.id.choixType);
		objgps = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		
		objgps.requestLocationUpdates(LocationManager.GPS_PROVIDER,2000,1, this);
		
		IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		batteryStatus = registerReceiver(null, ifilter);
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
		int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
		float niveauBat = level / (float)scale * 100.0f;
		batterie = String.valueOf(niveauBat);
	}

	
	@Override
	public void onLocationChanged(Location location) {
		Log.i("BoatTracker", "La position a chang�.");
		this.loc = location;
		afficherLocation();
		recupererIdentifiant();
		determinerType();
		recupererDate();
		recupererNiveauBatterie();
		Log.i("BoatTracker", "niveau de batterie:" + batterie);
		if (identifiant.length() > 0){
			Log.i("BoatTracker", "j'ai rentr� du texte");
			EnvoiRequete rqt = new EnvoiRequete();
			rqt.execute();
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
	

	
	// Envoi de la requ�te HTTP
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
	
	
	

	

}
