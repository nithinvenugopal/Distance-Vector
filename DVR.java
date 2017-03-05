/*
 * Name: Nithin Venugopal
 * UNCC ID: 800966213
 * 
 */

import java.io.*;
import java.util.*;

public class DVR {

	public static void main(String[] args) throws IOException {
		int port = 49000;
		String filename = "";
		Hashtable<String, Double> ncpair = new Hashtable<String, Double>();
		ArrayList<String> neighborslist = new ArrayList<String>();
		Hashtable<String, Vector<DistanceVector>> Host = new Hashtable<String, Vector<DistanceVector>>();		

		port = Integer.parseInt(args[0]);
		filename = args[1];	
		if(port <= 1024) {
			System.out.println("Port numbers above 1024 is preferred.");
			System.exit(-1);
		}			

		//Call receiver to listen on particular port
		new Receiver(port, filename, ncpair, neighborslist, Host).start();

		//Call sender to send to particular port
		new Sender(filename, ncpair, neighborslist, Host).start();
	}
}
