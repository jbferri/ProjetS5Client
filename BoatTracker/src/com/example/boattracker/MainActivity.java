package com.example.boattracker;

import java.text.SimpleDateFormat;
import java.util.Calendar;

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
import android.os.BatteryManager;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

public class MainActivity extends Activity implements LocationListener,
		View.OnClickListener {

	/******************************************
	 * Les Variables *
	 ******************************************/

	// Variables utilisées pour les widgets (aspect graphique de l'application)
	EditText text = null;
	RadioGroup radio = null;
	TextView latitude = null;
	TextView longitude = null;
	TextView emissionGPS = null;
	TextView emissionSonore = null;
	TextView frequenceEmission = null;
	Button boutonDemarrerApp = null;

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

	// Variable utilisée pour l'obtention du niveau de batterie
	Intent batteryStatus = null;

	// Variables utilisées pour la gestion du GPS
	LocationManager objgps = null;;
	Location loc = null;
	public static String activationEmissionGPS = "--";

	// Variables utilisées pour la gestion de l'emission sonore
	BluetoothAdapter mBluetoothAdapter;
	public static String activationEmissionSonore = "--";
	SoundPool soundPool;
	int soundID;
	int streamID;

	// Variables utilisées pour la gestion de la fréquence d'emission
	int frequence;
	public static String freq = "--";

	// Variable utilisée pour le format de la date
	@SuppressLint("SimpleDateFormat")
	SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	/******************************************
	 * Les Méthodes *
	 ******************************************/

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
		boutonDemarrerApp = (Button) findViewById(R.id.btnEnvoyer);
		objgps = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		androidId = Secure.getString(getContentResolver(), Secure.ANDROID_ID);
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
		soundID = soundPool.load(this, R.raw.cornedebrume, 1);
		boutonDemarrerApp.setOnClickListener(this);
	}

	/***********************************************************************************
	 * Redéfinition des méthodes de l'interface OnClickListener. Seule *
	 * onClick() est ici modifiée. *
	 ***********************************************************************************/
	@Override
	public void onClick(View v) {
		/*
		 * Une fois le bouton "Go!" cliqué : 
		 * - on interdit la modification du nom 
		 * - on interdit la réutilisation du bouton "Go!"
		 * - on lance le GPS 
		 */
		text.setKeyListener(null);
		boutonDemarrerApp.setEnabled(false);
		radio.setEnabled(false);
		objgps.requestLocationUpdates(LocationManager.GPS_PROVIDER, frequence, 1, this);			
	}

	/***********************************************************************************
	 * Redéfinition des méthodes de l'interface LocationListener. Seule *
	 * onLocationChanged() est ici modifiée. onLocationChanged() est appellée à *
	 * chaque changement de position GPS *
	 ***********************************************************************************/
	@Override
	public void onLocationChanged(Location location) {
		//On récupère la nouvelle position GPS
		this.loc = location;
		//On appelle les méthodes pour récupérer les infos à envoyer au serveur
		recupererCoordGPS();
		recupererIdentifiant();
		recupererType();
		recupererDate();
		recupererNiveauBatterie();
		/* On envoie la requete http avec toutes les infos et on récupère
		 * dans la réponse http la valeur de la fréquence ainsi que les booléens
		 * pour l'activation GPS et l'activation de l'émission sonore
		 */
		EnvoiRequete Rqt = new EnvoiRequete();
		Rqt.execute(type, nomEntite, lat, lon, date, batterie, precision, androidId);
		/* On appelle les méthodes pour fixer la frequence et activer ou
		 * désactiver les émission sonore et GPS.
		 */
		activerOuDesactiverEmissionGPS();
		activerOuDesactiverEmissionSonore();
		changerFrequence();
		// On affiche les infos sur l'écran du smartphone
		afficherInformations();
		/* Si le GPS a été désactivé, on locationChanged() n'est plus appelée.
		 * Donc il n'y a plus de requête http envoyée via l'AsyncTask EnvoiRequete.
		 * Pour continuer à envoyer des requêtes http et pouvoir par exemple réactiver le GPS
		 * on lance un nouveau thread d'écoute grâce à la méthode ecouteActivationGPS();
		 */
		if (activationEmissionGPS.equals("0")) {
			ecouteActivationGPS();
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
	 * Les méthodes ci-dessous sont invoquées après chaque réponse http reçue *
	 * de la part du serveur. Elles permettent de fixer la fréquence décidée *
	 * par l'administrateur du serveur, d'activer/désactiver les émissions sonores *
	 * et GPS, et d'afficher les informations sur l'écran du smartphone *
	 ***********************************************************************************/
	
	/*
	 * Applique le changement de fréquence d'émission du GPS décidé par le
	 * serveur
	 */
	private void changerFrequence() {
		if (freq != "--") {
			frequence = Integer.valueOf(freq);
		}
	}

	/*
	 * Active ou désactive l'émission sonore et le bluetooth selon les
	 * informations récoltées sur le serveur
	 */
	private void activerOuDesactiverEmissionSonore() {
		if (activationEmissionSonore.equals("0")) {
			mBluetoothAdapter.disable();
			soundPool.stop(streamID);
		}
		if (activationEmissionSonore.equals("1")) {
			mBluetoothAdapter.enable();
			streamID = soundPool.play(soundID, 1f, 1f, 0, -1, 1f);
		}
	}

	/*
	 * Active ou désactive l'émission GPS selon les informations récoltées sur
	 * le serveur
	 */
	private void activerOuDesactiverEmissionGPS() {
		if (activationEmissionGPS.equals("0")) {
			objgps.removeUpdates(this);
		}
		if (activationEmissionGPS.equals("1")) {
			objgps.requestLocationUpdates(LocationManager.GPS_PROVIDER,frequence, 1, this);
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

	/***********************************************************************************
	 * Le méthode ci-dessous est invoquée uniquement lorsque le GPS *
	 * est désactivé par l'administrateur du serveur. Elle permet de lancer *
	 * une nouvelle Asynctask afin de continuer à "écouter" le serveur et ainsi *
	 * réactiver le GPS si le serveur le demande. *
	 ***********************************************************************************/
	public void ecouteActivationGPS(){
		while(true){
			RecupererInstructions Rqtt = new RecupererInstructions();
			//On envoie une requête à la fréquence décidée par le serveur
			try {
				Thread.sleep(frequence);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			/* On envoie la requete http avec juste un id et on récupère
			 * dans la réponse http la valeur de la fréquence ainsi que les booléens
			 * pour l'activation GPS et l'activation de l'émission sonore
			 */
			Rqtt.execute(androidId);
			/* On appelle les méthodes pour fixer la frequence et activer ou
			 * désactiver les émission sonore et GPS et on affiche les infos à l'écran.
			 */
			activerOuDesactiverEmissionGPS();
			activerOuDesactiverEmissionSonore();
			changerFrequence();
			afficherInformations();
			//On sort de la boucle si le GPS a été remis en marche.
			if (activationEmissionGPS.equals("1")) {
				break;
			}
		}
	}
	
	/***********************************************************************************
	 * Les méthodes ci-dessous sont invoquées à chaque changement de position du *
	 * GPS, c'est à dire à chaque fois que la méthode onLocationChanged() est *
	 * appellée *
	 ************************************************************************************/

	// Détermine quel type d'entité envoie les informations au serveur
	private void recupererType() {
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

}
