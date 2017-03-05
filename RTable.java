import java.io.*;
import java.net.*;
import java.util.*;

class RTable {
	private String source;
	private String dest;
	private String via;
	private double cost;
	
	// constructor	
	public RTable(String s, String v, String d, double cost) {
		source = s;
		via = v;
		dest = d;
		this.cost = cost;
	}

	public String getSource() {
		return source;
	}
	public String getVia() {
		return via;
	}
	public String getDest() {
		return dest;
	}
	public double getCost() {
		return cost;
	}
	public void setCost(double newCost) {
		cost = newCost;
	}
}