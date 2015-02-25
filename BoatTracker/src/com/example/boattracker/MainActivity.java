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


public class MainActivity extends Activity implements LocationListener {

	// Variables utilis�es pour les widgets (aspect graphique de l'application)
	EditText text = null;
	RadioGroup radio = null;
	TextView latitude = null;
	TextView longitude = null;
	TextView emissionGPS = null;
	TextView frequenceEmission = null;
	
	/* Variables utilis�es pour stocker les valeurs qui vont �tre envoy�es
	 * sur le serveur via une requ�te HTTP. (uniquement des String)
	 */
	String identifiant = null;
	String lat = null;
	String lon = null;
	String type = null;
	String batterie = null;
	String date = null;

	// Variables utilis�es pour la gestion du GPS
	LocationManager objgps = null;;
	Location loc = null;
	
	// Variable utilis�e pour l'obtention du niveau de batterie
	Intent batteryStatus = null;
	
	/* Variables utilis�es pour red�finir la fr�quence d'�mission du GPS
	 * apr�s modification de sa valeur au niveau du serveur 
	 */
	int frequence;
	String freq;
	String activationEmission;
	
	// Variable utilis�e pour le format de la date
	SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	
	
	
	
	
	// M�thode appel�e au lancement de l'application
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		frequence = 1000;
		text = (EditText) findViewById(R.id.editText1);
		latitude = (TextView) findViewById(R.id.textView3);
		longitude = (TextView) findViewById(R.id.textView5);
		radio = (RadioGroup) findViewById(R.id.choixType);
		emissionGPS = (TextView) findViewById(R.id.textView10);
		frequenceEmission = (TextView) findViewById(R.id.textView12);
		objgps = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		objgps.requestLocationUpdates(LocationManager.GPS_PROVIDER,frequence,1, this);
	}
	
	
	/* 
	 * Les m�thodes ci dessous sont invoqu�es � chaque changement de position du GPS,
	 * c'est � dire � chaque fois que la m�thode onLocationChanged() est appell�e
	 * 
	 */
	
	// D�termine quel type d'entit� envoie les informations au serveur
	private void determinerType(){
		if (radio.getCheckedRadioButtonId() == R.id.radio2){
			type = "Bouee";
		} else {
			type = "Bateau";
		}
	}
	
	// Affiche les coordonn�es GPS � l'�cran
	private void afficherLocation() {
		lat = String.valueOf(loc.getLatitude());
		lon = String.valueOf(loc.getLongitude());
		latitude.setText(lat);
		longitude.setText(lon);
	}
	
	// R�cup�re la dare et l'heure
	private void recupererDate() {
		date = df.format(Calendar.getInstance().getTime());
	}
	
	// R�cup�re le nom de l'entit�
	private void recupererIdentifiant(){
		identifiant = text.getText().toString();
	}
	
	// R�cup�re le niveau de batterie
	private void recupererNiveauBatterie() {
		IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		batteryStatus = registerReceiver(null, ifilter);
		int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
		float niveauBat = level / (float)scale * 100.0f;
		batterie = String.valueOf(niveauBat);
	}

	/* Applique le changement de fr�quence d'�mission 
	 * du GPS d�cid� par le serveur 
	 */
	private void changerFrequence(){
		if (freq != null){
			Log.i("BoatTracker", "youhou!");
			frequence = Integer.valueOf(freq);
			frequenceEmission.setText(freq);
			emissionGPS.setText("Activ�e");
			Log.i("BoatTracker", "frequence:" + freq);
			freq = null;
			objgps.requestLocationUpdates(LocationManager.GPS_PROVIDER,frequence,1, this);
		}
	}


	/* 
	 * Red�finition des m�thodes de l'interface LocationListener. Seule 
	 * onLocationChanged() est ici modifi�e.
	 * 
	 */
	
	@Override
	public void onLocationChanged(Location location) {
		this.loc = location;
		
		afficherLocation();
		recupererIdentifiant();
		determinerType();
		recupererDate();
		recupererNiveauBatterie();
		changerFrequence();
		
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
	
	
	
	
	/*
	 * D�finition d'AsyncTasks qui permettent d'envoyer des requetes HTTP
	 * 
	 */
	
	// Envoi des donn�es au serveur
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
	
	// R�cup�ration des informations aupr�s du serveur
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
				List<NameValuePair> donnees = new ArrayList<NameValuePair>();
				donnees.add(new BasicNameValuePair("nom", identifiant));
				httppost.setEntity(new UrlEncodedFormEntity(donnees));
				HttpResponse response = httpclient.execute(httppost);
				HttpEntity entity = response.getEntity();
				is = entity.getContent();
			} catch(Exception e){
				Log.e("log_tag", "Error in http connection " + e.toString());
			}
			
			// Conversion de la requ�te en string
			try{
				BufferedReader reader = new BufferedReader(new InputStreamReader(is,"utf8"));
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
			Log.i("BoatTracker", "result :" + result);
			
			// R�cup�ration des r�sultats de la requete
			String[] tabReponse = result.split(";");
			freq = tabReponse[0];
			activationEmission = tabReponse[1];
			
			
			
		}
	}
	

	

}
