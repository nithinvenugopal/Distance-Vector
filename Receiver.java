/*
 * Name: Nithin Venugopal
 * UNCC ID: 800966213
 * 
 */

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

class Receiver extends Thread {
	private int port;
	Hashtable<String, Vector<DistanceVector>> Host = null;
	protected DatagramSocket ReceiverSocket = null; 
	ArrayList<String> neighborslist = null;
	Hashtable<String, Double> ncpair = null;	
	private boolean listening = true;
	private String file;
	
	public Receiver(int port, String file, Hashtable<String, Double> ncpair, ArrayList<String> neighborslist, Hashtable<String, Vector<DistanceVector>> Host) throws IOException {
		super("Receiver");
		this.port = port;
		this.file = file;        
		ReceiverSocket = new DatagramSocket(port);		
		this.ncpair = ncpair;
		this.neighborslist = neighborslist;
		this.Host = Host;		
	}

	public void run() { 
		while(listening) {
			try {
				byte[] bufReceived = new byte[32768];                
				DatagramPacket receiverPacket = new DatagramPacket(bufReceived, bufReceived.length);
				ReceiverSocket.receive(receiverPacket); 
				Hashtable<String, Vector<DistanceVector>> latest = Host;
				String received = new String(receiverPacket.getData(), 0, receiverPacket.getLength());
				String[] hostFile = file.split("\\.dat");
				LinkedList<RTable> receivedList = readReceived(received);				
				String start = hostFile[0];
				for(int i=0; i<receivedList.size(); i++) {
					RTable table = receivedList.get(i);
					if(latest.containsKey(start + "-" + table.getDest())) {
						Vector<DistanceVector> dVector = latest.get(start + "-" + table.getDest());
						int index = neighborslist.indexOf(table.getSource());
						double newCost = ncpair.get(start + "-" + table.getSource()) + table.getCost();
						if(dVector.get(index).getCost() > newCost) {
							dVector.get(index).setCost(newCost);
						}
					}
					else {
						if(start.equals(table.getDest())) {
							continue;
						}
						// new destination
						else {
							Vector<DistanceVector> newVector = new Vector<DistanceVector>();							
							for(int k=0; k<neighborslist.size(); k++) {
								newVector.add(new DistanceVector(start, neighborslist.get(k), table.getDest(), 99999.0));
							}
							int index = neighborslist.indexOf(table.getSource());
							newVector.set(index, new DistanceVector(start, table.getSource(), table.getDest(), ncpair.get(start + "-" + table.getSource()) + table.getCost()));
							Host.put(start + "-" + table.getDest(), newVector);
						}						
					}
				}			

			} 
			catch(IOException e) {
				e.printStackTrace();
			}			
		}
		ReceiverSocket.close();
	}

	private LinkedList<RTable> readReceived(String received) {
		
		//Match the string received and add it to the routelist
		String receivedReg = "(?:([A-Za-z\\s]+path ))([A-Za-z\\d\\.]+) - ([A-Za-z\\d\\.]+):(?:([A-Za-z\\s]+is ))([A-Za-z\\d\\.]+)(?:([A-Za-z\\s]+)+)([\\d.]+)";		
		LinkedList<RTable> routeList = new LinkedList<RTable>();
		String[] lines = received.split("\\n");			
		Pattern RdPattern = Pattern.compile(receivedReg, Pattern.CASE_INSENSITIVE|Pattern.DOTALL);
		for(int i=0; i<lines.length; i++) {			
			String Nodestart = "";
			String Nodeend = "";
			String Nodevia = "";
			String Nodecost = "";
			Matcher PattR = RdPattern.matcher(lines[i]);
			if(PattR.find()) {			
				Nodestart = PattR.group(2);
				Nodeend = PattR.group(3);
				Nodevia = PattR.group(5);
				Nodecost = PattR.group(7);
			}
			routeList.add(new RTable(Nodestart, Nodevia, Nodeend, Double.parseDouble(Nodecost)));			
		}
		return routeList;
	}
}