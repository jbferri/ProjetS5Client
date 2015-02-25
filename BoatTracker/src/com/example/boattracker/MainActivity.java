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
import android.provider.Settings.Secure;
import android.util.Log;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

public class MainActivity extends Activity implements LocationListener {

	// Variables utilisées pour les widgets (aspect graphique de l'application)
	EditText text = null;
	RadioGroup radio = null;
	TextView latitude = null;
	TextView longitude = null;
	TextView emissionGPS = null;
	TextView emissionSonore = null;
	TextView frequenceEmission = null;

	/*
	 * Variables utilisées pour stocker les valeurs qui vont être envoyées sur
	 * le serveur via une requête HTTP. (uniquement des String)
	 */
	String nomEntite = null;
	String androidId = null;
	String lat = null;
	String lon = null;
	String precision = null;
	String type = null;
	String batterie = null;
	String date = null;

	// Variables utilisées pour la gestion du GPS
	LocationManager objgps = null;;
	Location loc = null;

	// Variable utilisée pour l'obtention du niveau de batterie
	Intent batteryStatus = null;

	/*
	 * Variables utilisées pour récupérer les modifications de paramètres
	 * décidées au niveau du serveur
	 */
	int frequence;
	String freq = "--";
	String activationEmissionGPS = "--";
	String activationEmissionSonore = "--";

	// Variable utilisée pour le format de la date
	SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	// Méthode appelée au lancement de l'application
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
		emissionSonore = (TextView) findViewById(R.id.textView14);
		frequenceEmission = (TextView) findViewById(R.id.textView12);
		objgps = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		objgps.requestLocationUpdates(LocationManager.GPS_PROVIDER, frequence,
				1, this);
		androidId = Secure.getString(getContentResolver(), Secure.ANDROID_ID);
	}

	
	/*
	 * Les méthodes ci dessous sont invoquées à chaque changement de position du
	 * GPS, c'est à dire à chaque fois que la méthode onLocationChanged() est
	 * appellée
	 */

	// Détermine quel type d'entité envoie les informations au serveur
	private void determinerType() {
		if (radio.getCheckedRadioButtonId() == R.id.radio2) {
			type = "Bouee";
		} else {
			type = "Bateau";
		}
	}

	// Récupère les coordonnées GPS et la précision GPS
	private void recupererCoordGPS() {
		lat = String.valueOf(loc.getLatitude());
		lon = String.valueOf(loc.getLongitude());
		float a = loc.getAccuracy();
		int b = (int) a;
		precision = String.valueOf(b);
	}

	// Récupère la dare et l'heure
	private void recupererDate() {
		date = df.format(Calendar.getInstance().getTime());
	}

	// Récupère le nom de l'entité
	private void recupererIdentifiant() {
		nomEntite = text.getText().toString();
	}

	// Récupère le niveau de batterie
	private void recupererNiveauBatterie() {
		IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		batteryStatus = registerReceiver(null, ifilter);
		int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
		float niveauBat = level / (float) scale * 100.0f;
		batterie = String.valueOf(niveauBat);
	}

	/*
	 * Applique le changement de fréquence d'émission du GPS décidé par le
	 * serveur
	 */
	private void changerFrequence() {
		if (freq != "--") {
			frequence = Integer.valueOf(freq);
			objgps.requestLocationUpdates(LocationManager.GPS_PROVIDER,
					frequence, 1, this);
		}
	}
	
	/* 
	 * Active ou désactive l'émission sonore selon les informations
	 * récoltées sur le serveur 
	 */
	private void activerOuDesactiverEmissionSonore() {
		if (activationEmissionSonore.equals("0")){
			Log.i("BoatTracker", "normalement c'est desactivee!");
			activationEmissionSonore = "Désactivée";
		}
		if (activationEmissionSonore.equals("1")){
			activationEmissionSonore = "Activée";
		}
	}

	/* 
	 * Active ou désactive l'émission GPS selon les informations
	 * récoltées sur le serveur 
	 */
	private void activerOuDesactiverEmissionGPS() {
		if (activationEmissionGPS.equals("0")){
			activationEmissionGPS = "Désactivée";
		}
		if (activationEmissionGPS.equals("1")){
			Log.i("BoatTracker", "GPS ok");
			activationEmissionGPS = "Activée";
		}
	}

	// Afficher les informations sur l'écran du smartphone
	private void afficherInformations() {
		latitude.setText(lat);
		longitude.setText(lon);
		frequenceEmission.setText(freq);
		emissionGPS.setText(activationEmissionGPS);
		emissionSonore.setText(activationEmissionSonore);
	}

	
	/*
	 * Redéfinition des méthodes de l'interface LocationListener. Seule
	 * onLocationChanged() est ici modifiée.
	 */

	@Override
	public void onLocationChanged(Location location) {
		this.loc = location;

		recupererCoordGPS();
		recupererIdentifiant();
		determinerType();
		recupererDate();
		recupererNiveauBatterie();
		changerFrequence();
		activerOuDesactiverEmissionSonore();
		activerOuDesactiverEmissionGPS();
		afficherInformations();

		if (nomEntite.length() > 0) {
			EnvoiRequete Rqt = new EnvoiRequete();
			RecupererInstructions Rqtt = new RecupererInstructions();
			Rqt.execute();
			Rqtt.execute();
		}

	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
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
	 * Définition d'AsyncTasks qui permettent d'envoyer des requetes HTTP
	 */

	// Envoi des données au serveur
	private class EnvoiRequete extends AsyncTask<String, Integer, Double> {

		@Override
		protected Double doInBackground(String... params) {
			envoyerMessage();
			return null;
		}

		public void envoyerMessage() {

			HttpClient client = new DefaultHttpClient();
			HttpPost post = new HttpPost("http://172.20.10.3/GSCtuto/ReceptionDonnees.php");
			// HttpPost post = new HttpPost("http://10.29.226.210:8888/cartes/reception.php");
			// HttpPost post = new HttpPost("http://orion-brest.com/TestProjetS5/reception1&1.php");

			try {
				List<NameValuePair> donnees = new ArrayList<NameValuePair>();
				donnees.add(new BasicNameValuePair("type", type));
				donnees.add(new BasicNameValuePair("nom", nomEntite));
				donnees.add(new BasicNameValuePair("latitude", lat));
				donnees.add(new BasicNameValuePair("longitude", lon));
				donnees.add(new BasicNameValuePair("heure", date));
				donnees.add(new BasicNameValuePair("batterie", batterie));
				donnees.add(new BasicNameValuePair("precision", precision));
				donnees.add(new BasicNameValuePair("id", androidId));
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

	// Récupération des informations auprès du serveur
	private class RecupererInstructions extends
			AsyncTask<String, Integer, Double> {

		@Override
		protected Double doInBackground(String... params) {
			demanderInfos();
			return null;
		}

		public void demanderInfos() {

			InputStream is = null;
			String result = "";

			// Envoi de la commande http
			try {
				HttpClient httpclient = new DefaultHttpClient();
				HttpPost httppost = new HttpPost("http://172.20.10.3/GSCtuto/EnvoiDonnees.php");
				List<NameValuePair> donnees = new ArrayList<NameValuePair>();
				donnees.add(new BasicNameValuePair("nom", nomEntite));
				donnees.add(new BasicNameValuePair("id", androidId));
				donnees.add(new BasicNameValuePair("type", type));
				httppost.setEntity(new UrlEncodedFormEntity(donnees));
				HttpResponse response = httpclient.execute(httppost);
				HttpEntity entity = response.getEntity();
				is = entity.getContent();
			} catch (Exception e) {
				Log.e("log_tag", "Error in http connection " + e.toString());
			}

			// Conversion de la requête en string
			try {
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(is, "utf8"));
				StringBuilder sb = new StringBuilder();
				String line = null;
				while ((line = reader.readLine()) != null) {
					sb.append(line + "\n");
				}
				is.close();
				result = sb.toString();
			} catch (Exception e) {
				Log.e("log_tag", "Error converting result " + e.toString());
			}
			Log.i("BoatTracker", "result :" + result);

			// Récupération des résultats de la requete
			String[] tabReponse = result.split(";");
			freq = tabReponse[0];
			activationEmissionGPS = tabReponse[1];
			activationEmissionSonore = tabReponse[2];

		}
	}
}
