package common;

import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Vector;

import planes.Airplane;
import planes.PlanesManager;

import flights.Flight;
import flights.FlightsManager;

import bookings.Booking;

/**
 * Class responsible for searching flights and airplanes that are registered in
 * the system. Includes Flight Search by ID, Airplane search by ID or seats,
 * list flights and list airplanes.
 * 
 */
public class Search {
	private FlightsManager flightsManager;
	private PlanesManager planesManager;

	/**
	 * The Main constructor.
	 */
	public Search(FlightsManager fM, PlanesManager pM) {
		flightsManager = fM;
		planesManager = pM;
	}

	/**
	 * A method to search for a flight by plane.
	 */
	public Flight searchFlightById(int flightId) {
		return flightsManager.searchFlightById(flightId);
	}

	/**
	 * A method to search for a plane.
	 */
	public Airplane searchPlane(int planeId) {
		Iterator<Airplane> it = planesManager.getPlanesList().iterator();

		while (it.hasNext()) {
			Airplane plane = it.next();

			/* If the ID's match, we have found our plane. */
			if (plane.getId() == planeId) {
				return plane;
			}
		}

		/* There was no plane with this ID. */
		return null;
	}

	/**
	 * A method to search for a plane by the number of seats, useful to book
	 * charter flights
	 */
	public Airplane searchPlaneBySeats(int seats) {
		Airplane airplane;
		Iterator<Airplane> it = planesManager.getPlanesList().iterator();

		while (it.hasNext()) {
			airplane = it.next();

			/* If the airplane has enough seats, we have found our plane. */
			if (airplane.getNoSeats() >= seats)
				return airplane;
		}
		return null;
	}

	/**
	 * A method to list all the flights for a given date.
	 */
	public Vector<Flight> listFlightsByDate(GregorianCalendar data) {

		Vector<Flight> finalList = new Vector<Flight>();

		/* Collects the list of all flights and creates an iterator over it. */
		Iterator<Flight> it = flightsManager.getFlightsList().iterator();
		while (it.hasNext()) {
			Flight flight = it.next();

			/* If the two dates match, we add this flight to the final vector. */
			if (flight.getDate().equals(data)) {
				finalList.add(flight);
			}
		}

		/*
		 * If we have elements on the list, we can return the vector. Otherwise,
		 * we simply return null, indicating there were no members.
		 */
		if (finalList.size() != 0) {
			return finalList;
		}
		return null;

	}

	/*
	 * A method to search for a list of bookings in a flight. This method is
	 * likely to be used only by the BackOffice.
	 */
	public Vector<Booking> listBookingsFlight(Flight flight) {

		return null;
	}

}
