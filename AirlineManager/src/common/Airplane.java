package common;

import java.util.Vector;

public class Airplane {
	/* The total amount of seats and the number of occupied seats.
	 * This last parameter may be replaced by finding the size of
	 * the list of clients in this flight.
	 */
	
	//TODO: Um voo pode ter mais do que um flight, logo, estas vari�veis n�o podem ficar associadas ao avi�o.
	private int noSeats, occupiedSeats;
	/* The flight associated with this airplane. */
	private Vector <Flight> flights;
	private String company, model;
	private int id;
	
	//TODO: This is temporary!
	public static int idCreator = 0;
	
	/* The constructor. */
	public Airplane(int number, String company, String model){
		noSeats = number;
		this.company = company;
		this.model = model;
		this.flights = new Vector <Flight>();
		
		id = idCreator++;
	}

	public void associateFlight(Flight flight){
		flights.add(flight);
	}
	
	public void removeFlight (Flight flight){
		flights.remove(flight);
	}
	
	/* Getters and setters. */
	public int getNoSeats() {
		return noSeats;
	}

	public int getOccupiedSeats() {
		return occupiedSeats;
	}

	public Vector <Flight> getFlights() {
		return flights;
	}

	public int getId() {
		return id;
	}
	
	public String getCompany() {
		return company;
	}

	public String getModel() {
		return model;
	}

	public void setNoSeats(int noSeats) {
		this.noSeats = noSeats;
	}

	public void setOccupiedSeats(int occupiedSeats) {
		this.occupiedSeats = occupiedSeats;
	}

	public void setFlights(Vector <Flight> flights) {
		this.flights = flights;
	}

	public void setId(int id) {
		this.id = id;
	}
	
	public void setCompany(String company) {
		this.company = company;
	}

	public void setModel(String model) {
		this.model = model;
	}
	
}