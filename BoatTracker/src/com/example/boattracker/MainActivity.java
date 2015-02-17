package com.example.boattracker;

import java.io.IOException;
import java.util.ArrayList;
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
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;


public class MainActivity extends Activity implements  LocationListener {

	EditText text = null;
	LocationManager objgps = null;;
	Location loc = null;
	TextView latitude = null;
	TextView longitude = null;
	String lat = null;
	String lon = null;
	RadioGroup radio = null;
	String type = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		text = (EditText) findViewById(R.id.editText1);
		latitude = (TextView) findViewById(R.id.textView3);
		longitude = (TextView) findViewById(R.id.textView5);
		radio = (RadioGroup) findViewById(R.id.choixType);
		objgps = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		
		objgps.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,1, this);
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
	
	
	@Override
	public void onLocationChanged(Location location) {
		Log.i("BoatTracker", "La position a changé.");
		this.loc = location;
		afficherLocation();
		determinerType();
		if (text.getText().toString().length() > 0){
			Log.i("BoatTracker", "j'ai rentré du texte");
			EnvoiRequete rqt = new EnvoiRequete();
			rqt.execute(text.getText().toString());
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
				envoyerMessage(params[0]);
			return null;
		}

		public void envoyerMessage(String name) {
			
			HttpClient client = new DefaultHttpClient();
			//HttpPost post = new HttpPost("http://10.29.229.220/GSCtuto/reception4.php");
			HttpPost post = new HttpPost("http://10.29.226.210:8888/cartes/reception.php");

			
			try {
				List<NameValuePair> donnees = new ArrayList<NameValuePair>();
				donnees.add(new BasicNameValuePair("type", type));
				donnees.add(new BasicNameValuePair("nom", name));
				donnees.add(new BasicNameValuePair("latitude", lat));
				donnees.add(new BasicNameValuePair("longitude", lon));
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
