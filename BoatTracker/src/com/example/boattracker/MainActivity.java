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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings.Secure;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

public class MainActivity extends Activity implements LocationListener, View.OnClickListener {

	/******************************************
	 * 		Les Variables					  *
	 ******************************************/
	
	// Variables utilis�es pour les widgets (aspect graphique de l'application)
	EditText text = null;
	RadioGroup radio = null;
	TextView latitude = null;
	TextView longitude = null;
	TextView emissionGPS = null;
	TextView emissionSonore = null;
	TextView frequenceEmission = null;
	Button boutonDemarrerApp = null;

	/*
	 * Variables utilis�es pour stocker les valeurs qui vont �tre envoy�es sur
	 * le serveur via une requ�te HTTP. (uniquement des String)
	 */
	String nomEntite = null;
	String androidId = null;
	String lat = null;
	String lon = null;
	String precision = null;
	String type = null;
	String batterie = null;
	String date = null;
	
	// Variable utilis�e pour l'obtention du niveau de batterie
	Intent batteryStatus = null;

	// Variables utilis�es pour la gestion du GPS
	LocationManager objgps = null;;
	Location loc = null;
	String activationEmissionGPS = "--";

	// Variables utilis�es pour la gestion de l'emission sonore
	BluetoothAdapter mBluetoothAdapter;
	String activationEmissionSonore = "--";
	SoundPool soundPool;
	int soundID;
	int streamID;

	// Variables utilis�es pour la gestion de la fr�quence d'emission
	int frequence;
	String freq = "--";

	// Variable utilis�e pour le format de la date
	@SuppressLint("SimpleDateFormat")
	SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	
	/******************************************
	 * 		Les M�thodes					  *
	 ******************************************/
	
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
		emissionSonore = (TextView) findViewById(R.id.textView14);
		frequenceEmission = (TextView) findViewById(R.id.textView12);
		boutonDemarrerApp = (Button) findViewById(R.id.btnEnvoyer);
		objgps = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		androidId = Secure.getString(getContentResolver(), Secure.ANDROID_ID);
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
		soundID = soundPool.load(this, R.raw.cornedebrume, 1);
		boutonDemarrerApp.setOnClickListener(this);	
	}

	/***********************************************************************************
	 * 		Red�finition des m�thodes de l'interface OnClickListener. Seule			   *
	 *  	onClick() est ici modifi�e.												   *
	 ***********************************************************************************/
	@Override
	public void onClick(View v) {
		/* 
		 * Une fois le bouton "Go!" cliqu� : 
		 * - on interdit la modification du nom
		 * - on interdit la r�utilisation des boutons
		 * - on lance le GPS
		 * - on lance un d�compte : toutes les 5s pendant 10h on envoie
		 * une requ�te pour r�cuperer les instructions du serveur concernant
		 * l'�mission GPS, l'�mission sonore et la fr�quence du GPS. 
		 */
		text.setKeyListener(null);
		boutonDemarrerApp.setEnabled(false);
		radio.setEnabled(false);
		objgps.requestLocationUpdates(LocationManager.GPS_PROVIDER, frequence,1, this);
		
		new CountDownTimer(36000000,5000) {

			@Override
			public void onTick(long millisUntilFinished) {
				recupererIdentifiant();
				recupererType();
				if (nomEntite.length() > 0) {
					RecupererInstructions Rqtt = new RecupererInstructions();
					Rqtt.execute();
					activerOuDesactiverEmissionGPS();
					activerOuDesactiverEmissionSonore();
					changerFrequence();
					afficherInformations();
				}				
			}

			@Override
			public void onFinish() {
				// TODO Auto-generated method stub
				
			}
			
		}.start();
			
	}
	
	/***********************************************************************************
     *	 Les m�thodes ci-dessous sont invoqu�es de mani�re r�currente gr�ce au         *
	 *	 d�compte du CoutndownTimer. Elles sont appel�es toutes les 5s lorsque         *
	 *    que la m�thode onTick() est appell�e.										   *					   *
	 ***********************************************************************************/
	
	/*
	 * Applique le changement de fr�quence d'�mission du GPS d�cid� par le
	 * serveur
	 */
	private void changerFrequence() {
		if (freq != "--") {
			frequence = Integer.valueOf(freq);
		}
	}
	
	/* 
	 * Active ou d�sactive l'�mission sonore et le bluetooth selon les informations
	 * r�colt�es sur le serveur 
	 */
	private void activerOuDesactiverEmissionSonore() {
		if (activationEmissionSonore.equals("0")){
			activationEmissionSonore = "D�sactiv�e";
			mBluetoothAdapter.disable();
			soundPool.stop(streamID);
		}
		if (activationEmissionSonore.equals("1")){
			activationEmissionSonore = "Activ�e";
			mBluetoothAdapter.enable();
			streamID = soundPool.play(soundID, 1f, 1f, 0, -1, 1f);
		}
	}

	/* 
	 * Active ou d�sactive l'�mission GPS selon les informations
	 * r�colt�es sur le serveur 
	 */
	private void activerOuDesactiverEmissionGPS() {
		if (activationEmissionGPS.equals("0")){
			activationEmissionGPS = "D�sactiv�e";
			objgps.removeUpdates(this);
		}
		if (activationEmissionGPS.equals("1")){
			activationEmissionGPS = "Activ�e";
			objgps.requestLocationUpdates(LocationManager.GPS_PROVIDER,frequence, 1, this);
		}
	}
	
	
	/***********************************************************************************
	 * 		Red�finition des m�thodes de l'interface LocationListener. Seule		   *
	 *  	onLocationChanged() est ici modifi�e.									   *
	 ***********************************************************************************/
	@Override
	public void onLocationChanged(Location location) {
		this.loc = location;
		recupererCoordGPS();
		recupererIdentifiant();
		recupererType();
		recupererDate();
		recupererNiveauBatterie();
		afficherInformations();

		if (nomEntite.length() > 0) {
			EnvoiRequete Rqt = new EnvoiRequete();
			Rqt.execute();
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
	
	/***********************************************************************************
	 *	 Les m�thodes ci-dessous sont invoqu�es � chaque changement de position du     *
	 *	 GPS, c'est � dire � chaque fois que la m�thode onLocationChanged() est		   *
	 *	 appell�e																	   *
	 ************************************************************************************/

	// D�termine quel type d'entit� envoie les informations au serveur
	private void recupererType() {
		if (radio.getCheckedRadioButtonId() == R.id.radio2) {
			type = "Bouee";
		} else {
			type = "Bateau";
		}
	}

	// R�cup�re les coordonn�es GPS et la pr�cision GPS
	private void recupererCoordGPS() {
		lat = String.valueOf(loc.getLatitude());
		lon = String.valueOf(loc.getLongitude());
		float a = loc.getAccuracy();
		int b = (int) a;
		precision = String.valueOf(b);
	}

	// R�cup�re la dare et l'heure
	private void recupererDate() {
		date = df.format(Calendar.getInstance().getTime());
	}

	// R�cup�re le nom de l'entit�
	private void recupererIdentifiant() {
		nomEntite = text.getText().toString();
	}

	// R�cup�re le niveau de batterie
	private void recupererNiveauBatterie() {
		IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		batteryStatus = registerReceiver(null, ifilter);
		int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
		float niveauBat = level / (float) scale * 100.0f;
		batterie = String.valueOf(niveauBat);
	}

	// Afficher les informations sur l'�cran du smartphone
	private void afficherInformations() {
		latitude.setText(lat);
		longitude.setText(lon);
		frequenceEmission.setText(freq);
		emissionGPS.setText(activationEmissionGPS);
		emissionSonore.setText(activationEmissionSonore);
	}

	
	
	/***********************************************************************************
	 *	 D�finition d'AsyncTasks qui permettent d'envoyer des requetes HTTP	:	       *
	 *	  - une Asynctask EnvoiRequete pour transmettre les infos au serveur 		   *
	 *	    (coordonn�es GPS, niveau batterie,... 									   *
	 *    - une Asynctask RecupererInstructions pour r�cuperer dans la BDD du 		   *
	 *      serveur certains parmam�tres de fonctionnement (activation GPS,            *
	 *      activation de l'emission sonore, changement de la fr�quence d'�mission)	   *
	 ************************************************************************************/

	// Envoi des donn�es au serveur
	private class EnvoiRequete extends AsyncTask<String, Integer, Double> {

		@Override
		protected Double doInBackground(String... params) {
			envoyerMessage();
			return null;
		}

		public void envoyerMessage() {
			HttpClient client = new DefaultHttpClient();
			//HttpPost post = new HttpPost("http://172.20.10.3/GSCtuto/ReceptionDonnees.php");
			//HttpPost post = new HttpPost("http://172.20.10.8:8888/ProjetS5/Transfert/ReceptionDonnees.php");
			HttpPost post = new HttpPost("http://orion-brest.com/TestProjetS5/Transfert/ReceptionDonnees.php");
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
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
			} catch (IOException e) {
				// TODO Auto-generated catch block
			}
		}

	}

	// R�cup�ration des informations aupr�s du serveur
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
				//HttpPost httppost = new HttpPost("http://172.20.10.3/GSCtuto/EnvoiDonnees.php");
				//HttpPost httppost = new HttpPost("http://172.20.10.8:8888/ProjetS5/Transfert/EnvoiDonnees.php");
				HttpPost httppost = new HttpPost("http://orion-brest.com/TestProjetS5/Transfert/EnvoiDonnees.php");
				List<NameValuePair> donnees = new ArrayList<NameValuePair>();
				donnees.add(new BasicNameValuePair("nom", nomEntite));
				donnees.add(new BasicNameValuePair("id", androidId));
				donnees.add(new BasicNameValuePair("type", type));
				httppost.setEntity(new UrlEncodedFormEntity(donnees));
				HttpResponse response = httpclient.execute(httppost);
				HttpEntity entity = response.getEntity();
				is = entity.getContent();
			} catch (Exception e) {
				Log.e("log_tag", "Erreur lors de la connexion http " + e.toString());
			}

			// Conversion de la requ�te en string
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
				Log.e("log_tag", "Erreur dans la conversion de result " + e.toString());
			}
			Log.i("BoatTracker", "5) result :" + result);

			// R�cup�ration des r�sultats de la requete
			if (result.length() >0){
				String[] tabReponse = result.split(";");
				freq = tabReponse[0];
				activationEmissionGPS = tabReponse[1];
				activationEmissionSonore = tabReponse[2];
			}
		}
	}
}
