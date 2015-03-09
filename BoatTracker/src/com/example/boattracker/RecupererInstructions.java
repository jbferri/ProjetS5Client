package com.example.boattracker;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.os.AsyncTask;
import android.util.Log;

/************************************************************************************
 * Définition d'une AsyncTask qui permet d'envoyer des requetes HTTP dans un 		*
 * thread différent. L'Asynctask RecupererInstructions permet de récuperer 			*
 * dans la BDD du serveur les paramètres de fonctionnement (activation GPS, 		*
 * activation de l'emission sonore, changement de la fréquence d'émission) 			*
 * lorsque le GPS est désactivé. 													*
 ************************************************************************************/
public class RecupererInstructions extends AsyncTask<String, Integer, Double> {

	@Override
	protected Double doInBackground(String... params) {
		demanderInfos(params[0]);
		return null;
	}

	public void demanderInfos(String androidId) {
		InputStream is = null;
		String result = "";

		// Envoi de la commande http
		try {
			HttpClient httpclient = new DefaultHttpClient();
			// HttpPost httppost = new HttpPost("http://172.20.10.3/GSCtuto/EnvoiDonnees.php");
			// HttpPost httppost = new HttpPost("http://172.20.10.8:8888/ProjetS5/Transfert/EnvoiDonnees.php");
			HttpPost httppost = new HttpPost("http://orion-brest.com/TestProjetS5/Transfert/EnvoiDonnees.php");
			List<NameValuePair> donnees = new ArrayList<NameValuePair>();
			donnees.add(new BasicNameValuePair("id", androidId));
			httppost.setEntity(new UrlEncodedFormEntity(donnees));
			HttpResponse response = httpclient.execute(httppost);
			HttpEntity entity = response.getEntity();
			is = entity.getContent();
		} catch (Exception e) {
			Log.e("log_tag", "Erreur lors de la connexion http " + e.toString());
		}

		// Conversion de la requête en string
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(is, "utf8"));
			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
			is.close();
			result = sb.toString();
		} catch (Exception e) {
			Log.e("log_tag","Erreur dans la conversion de result " + e.toString());
		}
		Log.i("BoatTracker", "5) result :" + result);

		// Récupération des résultats de la requete
		if (result.length() > 0) {
			String[] tabReponse = result.split(";");
			MainActivity.freq = tabReponse[0];
			MainActivity.activationEmissionGPS = tabReponse[1];
			MainActivity.activationEmissionSonore = tabReponse[2];
		}
	}
}