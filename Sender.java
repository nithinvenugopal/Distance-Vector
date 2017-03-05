/*
 * Name: Nithin Venugopal
 * UNCC ID: 800966213
 * 
 */

import java.io.*;
import java.net.*;
import java.util.*;

class Sender extends Thread {
	private String filename;
	private DatagramSocket SenderSocket = null;	
	private int sequence = 0;
	Hashtable<String, Double> ncpair = null;
	ArrayList<String> neighborslist = null;
	ArrayList<String> ports = new ArrayList<String>();
	Hashtable<String, Vector<DistanceVector>> Host = null;

	public Sender(String filename, Hashtable<String, Double> ncpair, ArrayList<String> neighborslist, Hashtable<String, Vector<DistanceVector>> Host) throws IOException {
		super("Sender");
		this.filename = filename;	
		SenderSocket = new DatagramSocket();		
		this.ncpair = ncpair;
		this.neighborslist = neighborslist;
		this.Host = Host;
	}

	public void run() { 
		// Run in loop continously
		while(true) {						
			sequence++;
			System.out.println("\n Output number " + sequence + "\n");			

			// Send routing information			
			try {							   
				DatagramPacket clientPacket = null;
				byte[] bufSent = new byte[32768];
				String dString = "";
				//Only for the first interval
				if(sequence == 1) {
					Hostinfo(filename);
					dString = RouteTable(Host);					
					for(int n=0; n<neighborslist.size(); n++) {
						bufSent = dString.getBytes();
						InetAddress address = InetAddress.getByName(neighborslist.get(n));
						String hostport = ports.get(n);
						int hport = Integer.parseInt(hostport);
						//Construct packet and send
						clientPacket = new DatagramPacket(bufSent, bufSent.length, address, hport);						
						SenderSocket.send(clientPacket);
					}
					String simpleString = Print(dString);
					System.out.println(simpleString);
				}

				//For later intervals
				else if(sequence > 1) {
					boolean isChanged = false;
					LinkedList<String> newPair = filechange(filename);
					for(int i=0; i<newPair.size(); i++) {
						String[] pairCost = newPair.get(i).split(" ");
						if(ncpair.get(pairCost[0]) != Double.parseDouble(pairCost[1])) {
							isChanged = true;
							ncpair.remove(pairCost[0]);
							ncpair.put(pairCost[0], Double.parseDouble(pairCost[1]));							
						}
					}

					// If the data is send, then keep reading and send the data again
					if(isChanged) {
						neighborslist = new ArrayList<String>();
						Hostinfo(filename);
						dString = RouteTable(Host);						
						for(int n=0; n<neighborslist.size(); n++) {
							bufSent = dString.getBytes();
							InetAddress address = InetAddress.getByName(neighborslist.get(n));
							String hostport = ports.get(n);
							int hport = Integer.parseInt(hostport);							
							clientPacket = new DatagramPacket(bufSent, bufSent.length, address, hport);
							SenderSocket.send(clientPacket);
						}
						String simpleString = Print(dString);
						System.out.println(simpleString);
						//
						isChanged = false;
					}
					else {				
						dString = RouteTable(Host);						
						for(int n=0; n<neighborslist.size(); n++) {
							bufSent = dString.getBytes();
							InetAddress address = InetAddress.getByName(neighborslist.get(n));
							String hostport = ports.get(n);
							int hport = Integer.parseInt(hostport);							
							clientPacket = new DatagramPacket(bufSent, bufSent.length, address, hport);
							SenderSocket.send(clientPacket);
						}						
						String simpleString = Print(dString);
						System.out.println(simpleString);
					}
				}
				// sleep for fifteen seconds
				try {
					Thread.sleep(15000);
				} 
				catch(InterruptedException e) { 
					System.out.println(e);
				}
			}
			catch(IOException e) {
				e.printStackTrace();
				System.out.println(e);
			}			
		}		
	}

//Check if the file has changed
	private LinkedList<String> filechange(String filename) throws IOException {		
		FileInputStream FStream = null;
		InputStreamReader InpStream = null;
		BufferedReader BR = null;
		LinkedList<String> LVal = null;
		try {
			FStream = new FileInputStream(filename);
			InpStream = new InputStreamReader(FStream);
			BR = new BufferedReader(InpStream);
			LVal = new LinkedList<String>();
			String Line;
			while((Line = BR.readLine()) != null) {				
				String[] hostFile = filename.split("\\.dat");
				String[] part = Line.split(" ");
				if(part.length == 3) {					
					LVal.add(hostFile[0] + "-" + part[0] + " " + Double.parseDouble(part[1]));
				}
			}
		}
		catch(IOException e) {
			System.err.println(e);
		}		
		BR.close();
		InpStream.close();
		FStream.close();	
		return LVal;
	}	



	private void Hostinfo(String filename) throws IOException {		
		FileInputStream FStream = null;
		InputStreamReader InpStream = null;
		BufferedReader BR = null;
		try {
			FStream = new FileInputStream(filename);
			InpStream = new InputStreamReader(FStream);
			BR = new BufferedReader(InpStream);
			String Line;
			while((Line = BR.readLine()) != null) {				
				String[] hostFile = filename.split("\\.dat");
				String[] part = Line.split(" ");
				if(part.length == 3) {					
					neighborslist.add(part[0]);
					ports.add(part[2]);
					ncpair.put(hostFile[0] + "-" + part[0], Double.parseDouble(part[1]));					
				}
			}
			FStream = new FileInputStream(filename);
			InpStream = new InputStreamReader(FStream);
			BR = new BufferedReader(InpStream);
			while((Line = BR.readLine()) != null) {
				String[] hostFile = filename.split("\\.dat");
				String[] part = Line.split(" ");
				Vector<DistanceVector> dvLine = new Vector<DistanceVector>();
				if(part.length == 3) {				
					Iterator<String> itr = neighborslist.iterator();
					while (itr.hasNext()) {
						String element = itr.next();
						if(element.equals(part[0])) {
							dvLine.add(new DistanceVector(hostFile[0], element, part[0], Double.parseDouble(part[1])));
						}
						else {							
							dvLine.add(new DistanceVector(hostFile[0], element, part[0], 99999.0));
						}											
					}

					Host.put(hostFile[0] + "-" + part[0], dvLine);
				}				
			}
		}		
		catch(IOException e) {
			System.err.println(e);
		}            
		BR.close();
		InpStream.close();
		FStream.close();			        
	}	

	private String RouteTable(Hashtable<String, Vector<DistanceVector>> dvList) {
		Double min = Double.MAX_VALUE;
		String LVal = "";		
		for(Vector<DistanceVector> v : dvList.values()) {
			String source = "";
			String via = "";
			String dest = "";
			double[] costs = new double[v.size()];			
			for(int j=0; j<v.size(); j++) {
				costs[j] = v.get(j).getCost();				
			}
			source = v.get(Mini(costs)).getSource();
			dest = v.get(Mini(costs)).getDest();
			via = v.get(Mini(costs)).getVia();
			min = v.get(Mini(costs)).getCost();

			LVal += "shortest path " + source + " - " + dest + ": the next hop is " + via + " and the cost is " + min + "\n";
		}

		return LVal;			
	}

	private int Mini(double[] d) {
		double min = Double.MAX_VALUE;
		for(int i=0; i<d.length; i++) {
			if(min > d[i]) {
				min = d[i];
			}
		}

		for(int i=0; i<d.length; i++) {
			if(min == d[i]) {
				return i;
			}
		}
		return -1;
	}


	private String Print(String input) {
		String LVal = "";		
		String[] lines = input.split("\\n");
		for(int i=0; i<lines.length; i++) {
			String[] array = lines[i].split(" ");
			String[] start = array[2].split("\\.");
			String[] dest = array[4].split("\\.");
			String[] via = array[9].split("\\.");
			LVal += "shortest path " + start[0] + " - " + dest[0] + ": the next hop is " + via[0] + " and the cost is " + array[14] + "\n";			

		}

		return LVal;			
	}


}