package test;

import java.io.*;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.*;
import org.apache.http.util.EntityUtils;


public class socket_client {
	@SuppressWarnings("deprecation")
	public static void main(String[] args) throws IOException{
		String host = "http://192.168.56.10/index.html"; //192.168.56.11
		@SuppressWarnings("resource")
		HttpClient client = new DefaultHttpClient();
		HttpGet httpget = new HttpGet(host);
		HttpResponse response = client.execute(httpget);
		System.out.println(response.getStatusLine().toString());
	    HttpEntity entity = response.getEntity();
	    System.out.println();
	    System.out.println(EntityUtils.toString(entity));  
		


	}
}
