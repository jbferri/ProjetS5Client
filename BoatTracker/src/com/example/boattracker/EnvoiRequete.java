package com.example.boattracker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
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

import android.os.AsyncTask;
import android.util.Log;

/************************************************************************************
 * Définition d'une AsyncTask qui permet d'envoyer des requetes HTTP dans 			*
 * un thread différent. L'Asynctask EnvoiRequete permet de transmettre 				*
 * des infos au serveur (coordonnées GPS, niveau batterie,...) lorsque le GPS 		*
 * est actif et que onLocationChanged() est appellée régulièrement. Elle permet 	*
 * aussi de récuperer dans la BDD du serveur certains paramètres de fonctionnement 	*
 * (activation GPS, activation de l'emission sonore, changement de la fréquence 	*
 * d'émission). 																	*
 ************************************************************************************/
public class EnvoiRequete extends AsyncTask<String, Integer, Double> {

	@Override
	protected Double doInBackground(String... params) {
		envoyerMessage(params[0], params[1], params[2], params[3], params[4], params[5], params[6], params[7]);
		return null;
	}

	public void envoyerMessage(String type, String nomEntite, String lat, String lon, String date, String batterie, String precision, String androidId) {
		InputStream is = null;
		String result = "";

		// Envoi requête http et récupération de la réponse
		try {
			HttpClient client = new DefaultHttpClient();
			// HttpPost post = new HttpPost("http://172.20.10.3/GSCtuto/ReceptionDonnees.php");
			// HttpPost post = new HttpPost("http://172.20.10.8:8888/ProjetS5/Transfert/ReceptionDonnees.php");
			HttpPost post = new HttpPost("http://orion-brest.com/TestProjetS5/Transfert/ReceptionDonnees.php");
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
			HttpResponse response = client.execute(post);
			HttpEntity entity = response.getEntity();
			is = entity.getContent();
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
		} catch (IOException e) {
			// TODO Auto-generated catch block
		}

		// Conversion de la réponse en string
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
			Log.e("log_tag",
					"Erreur dans la conversion de result " + e.toString());
		}

		// Récupération des résultats de la requete
		if (result.length() > 0) {
			String[] tabReponse = result.split(";");
			MainActivity.freq = tabReponse[0];
			MainActivity.activationEmissionGPS = tabReponse[1];
			MainActivity.activationEmissionSonore = tabReponse[2];
		}
	}

}