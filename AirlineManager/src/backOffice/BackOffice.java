package backOffice;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import java.util.Vector;

import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;

import planes.Airplane;
import planes.PlanesManager;

import bookings.Booking;

import messages.FeedBackManager;
import messages.Message;

import clients.Client;
import clients.ClientsManager;
import clients.Operator;
import clients.OperatorManager;
import common.*;

import com.toedter.calendar.JCalendar;

import flights.Flight;
import flights.FlightsManager;
import flights.RFlight;

/**
 * 
 * BackOffice is responsible for managing the system's content including
 * Airplanes, Flights, Clients, Operators, Statistics and Feedback to clients.
 * It is also responsible for connecting to the Front Office through JAVA RMI.
 * 
 * @author Daniela Fontes
 * @author Ivo Correia
 * @author Miguel Penetra
 * @author Pedro Barbosa
 * @author Ricardo Bernardino
 * 
 * 
 */

public class BackOffice extends UnicastRemoteObject implements
		BackOfficeRemoteInterface {

	private static final long serialVersionUID = 1L;

	public static GregorianCalendar now;

	/* The main panel. */
	private JPanel panel = new JPanel();

	/* The list of managers that the BackOffice will deal with. */
	private FeedBackManager feedBackManager;
	private FlightsManager flightsManager;
	private PlanesManager planesManager;
	private StatisticsManager statisticsManager;
	private OperatorManager operatorManager;
	private ClientsManager clientsManager;
	private ClientsMenu clientsMenu;
	private DestinationsPrices destinationsPrices;

	private Menu menu;
	private FeedBackManagerMenu feedBackManagerMenu;
	private FlightsManagerMenu flightsManagerMenu;
	private PlanesManagerMenu planesManagerMenu;
	private StatisticsManagerMenu statisticsManagerMenu;
	private LoginMenu loginMenu;

	/**
	 * The main constructor.
	 */
	public BackOffice() throws RemoteException {
		super();

		JFrame.setDefaultLookAndFeelDecorated(true);
		JDialog.setDefaultLookAndFeelDecorated(true);

		try {
			UIManager
					.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
		} catch (Exception e) {
			System.out.println("Exiting...");
			System.exit(-1);
		}

		feedBackManager = new FeedBackManager();
		flightsManager = new FlightsManager(feedBackManager);
		planesManager = new PlanesManager();
		operatorManager = new OperatorManager();
		clientsManager = new ClientsManager();
		statisticsManager = new StatisticsManager(feedBackManager,
				flightsManager, planesManager);
		clientsMenu = new ClientsMenu();
		destinationsPrices = new DestinationsPrices();

		menu = new Menu();
		feedBackManagerMenu = new FeedBackManagerMenu();
		flightsManagerMenu = new FlightsManagerMenu();
		planesManagerMenu = new PlanesManagerMenu();
		statisticsManagerMenu = new StatisticsManagerMenu();
		loginMenu = new LoginMenu();

		BackOffice.now = new GregorianCalendar();
	}

	public static void main(String[] args) throws RemoteException {

		BackOffice backOffice = new BackOffice();
		try {
			System.getProperties().put("java.security.policy", "policy.all");
			System.setSecurityManager(new RMISecurityManager());

			Registry r = LocateRegistry.createRegistry(Constants.RMI_PORT);
			r.rebind("AirlineManager", backOffice);
		} catch (RemoteException re) {
			System.out
					.println("There's already another instance running in this port: The system will shutdown. Please restart specifying another port.");
			System.exit(0);
		}

		backOffice.executeGraphics();

	}

	/**
	 * The method to authenticate the administrator.
	 */
	public boolean loginAdmin(String username, String password) {
		String user = Constants.ADMIN_USERNAME;
		String pass = Constants.ADMIN_PASSWORD;
		if (username.equals(user) && password.equals(pass)) {
			return true;
		}
		return false;
	}

	/**
	 * Check if the inserted fields can form a valid date.
	 * 
	 * @return GregorianCalendar or null
	 */
	public GregorianCalendar checkDate(int year, int month, int day, int hour,
			int minute) {

		/* A non-positive year. */
		if (year < 0) {
			return null;
		}
		/* An invalid month. */
		else if (month < 1 || month > 12) {
			return null;
		}
		/* An invalid day. */
		else if (day < 1
				|| (day > 31 && (month == 1 || month == 3 || month == 5
						|| month == 7 || month == 8 || month == 10 || month == 12))
				|| (day > 30 && (month == 4 || month == 6 || month == 9 || month == 11))) {
			return null;
		}
		/* Invalid hour. */
		else if (hour < 0 || hour > 23) {
			return null;
		}
		/* Invalid minute. */
		else if (minute < 0 || minute > 59) {
			return null;
		}

		/* It's February, so we ought to check if we are in a leap year or not. */
		if (month == 2) {
			boolean leapYear;

			/* Verifies whether we are in a leap year or not. */
			if (year % 400 == 0) {
				leapYear = true;
			} else if (year % 100 == 0) {
				leapYear = false;
			} else if (year % 4 == 0) {
				leapYear = true;
			} else {
				leapYear = false;
			}

			/* Acts accordingly to the result collected above. */
			if (leapYear) {
				if (day > 29) {
					return null;
				}
			} else {
				if (day > 28) {
					return null;
				}
			}
		}

		GregorianCalendar now = new GregorianCalendar();
		GregorianCalendar date = new GregorianCalendar(year, month - 1, day,
				hour, minute);
		/* This is an old date. */
		if (now.after(date)) {
			return null;
		}
		return date;
	}

	public void executeGraphics() {

		JFrame f = new JFrame();
		f.setSize(Constants.DIM_H, Constants.DIM_V);
		f.setTitle("Airplane Agency");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		panel.setLayout(null);
		panel.setVisible(true);

		/* Sets the panel that will hold the time display. */
		JPanel timeDisplay = new JPanel();
		timeDisplay.setLayout(null);
		timeDisplay.setBounds(new Rectangle(765, 11, 250, 50));
		timeDisplay.setOpaque(false);
		JLabel time = new JLabel();
		timeDisplay.add(time);
		timeDisplay.setVisible(true);
		panel.add(timeDisplay);
		new ShowTime(time, null);

		/* Sets the panel that will hold the time display. */
		JPanel departuresDisplay = new JPanel();
		departuresDisplay.setLayout(null);
		departuresDisplay.setBounds(new Rectangle(530, 510, 500, 400));
		departuresDisplay.setOpaque(false);
		JLabel departureLabel = new JLabel();

		flightsManager.getFlightsCleaner().setComponents(departuresDisplay,
				departureLabel);
		departuresDisplay.add(departureLabel);
		departuresDisplay.setVisible(false);

		panel.add(departuresDisplay);

		panel.add(loginMenu);
		panel.add(menu);
		panel.add(feedBackManagerMenu);
		panel.add(flightsManagerMenu);
		panel.add(planesManagerMenu);
		panel.add(statisticsManagerMenu);
		panel.add(clientsMenu);

		System.out.println("LOADING THE IMAGES...");
		loginMenu.CreateImage("./src/images/02098_dawndeparture_1280x800.jpg",
				"", 0, 0, 990, 570);
		menu.CreateImage("./src/images/02098_dawndeparture_1280x800.jpg", "",
				0, 0, 990, 570);
		feedBackManagerMenu.CreateImage(
				"./src/images/02098_dawndeparture_1280x800.jpg", "", 0, 0, 990,
				570);
		flightsManagerMenu.CreateImage(
				"./src/images/02098_dawndeparture_1280x800.jpg", "", 0, 0, 990,
				570);
		planesManagerMenu.CreateImage(
				"./src/images/02098_dawndeparture_1280x800.jpg", "", 0, 0, 990,
				570);
		statisticsManagerMenu.CreateImage(
				"./src/images/02098_dawndeparture_1280x800.jpg", "", 0, 0, 990,
				570);
		clientsMenu.CreateImage(
				"./src/images/02098_dawndeparture_1280x800.jpg", "", 0, 0, 990,
				570);

		/* Sets all the windows invisible, except, naturally, the main menu. */
		loginMenu.setVisible(true);
		menu.setVisible(false);
		feedBackManagerMenu.setVisible(false);
		flightsManagerMenu.setVisible(false);
		planesManagerMenu.setVisible(false);
		statisticsManagerMenu.setVisible(false);
		clientsMenu.setVisible(false);

		f.setContentPane(panel);
		f.setVisible(true);

	}

	@SuppressWarnings("serial")
	private class Menu extends Window {
		public Menu() {
			/* Creates the buttons that redirect to each manager window. */
			CreateButton("Airplanes", Color.white, "Manage Airplanes", 15, 60,
					200, 100, 30);
			CreateButton("Flights", Color.white, "Manage Flights", 15, 60, 250,
					100, 30);
			CreateButton("Feedback", Color.white, "Handle Feedback", 15, 60,
					300, 100, 30);
			CreateButton("Statistics", Color.white, "Check Statistics", 15, 60,
					350, 100, 30);
			CreateButton("Clients", Color.white, "List all the operators", 15,
					60, 400, 100, 30);
			CreateButton("Exit", Color.white, "Leave the application", 15, 60,
					500, 100, 30);
		}

		public void mouseReleased(MouseEvent e) {
			if (e.getComponent().getName().equals("Airplanes")) {
				menu.setVisible(false);
				planesManagerMenu.entry();
			} else if (e.getComponent().getName().equals("Flights")) {
				menu.setVisible(false);
				flightsManagerMenu.entry();
			} else if (e.getComponent().getName().equals("Feedback")) {
				menu.setVisible(false);
				feedBackManagerMenu.entry();
			} else if (e.getComponent().getName().equals("Statistics")) {
				menu.setVisible(false);
				statisticsManagerMenu.entry();

			} else if (e.getComponent().getName().equals("Clients")) {
				menu.setVisible(false);
				clientsMenu.entry();

			} else if (e.getComponent().getName().equals("Exit")) {
				/* The user is leaving the application. */
				JOptionPane jp = new JOptionPane("Have a nice day!",
						JOptionPane.INFORMATION_MESSAGE);
				JDialog jd = jp.createDialog("Thank you!");
				jd.setBounds(new Rectangle(340, 200, 320, 120));
				jd.setVisible(true);
				System.exit(0);
			}
		}
	}

	@SuppressWarnings("serial")
	private class FeedBackManagerMenu extends Window {
		JPanel positivePanel;
		JPanel negativePanel;
		JPanel sendPanel;
		JPanel toPanel;
		/* Positive Panel Message Area */
		private JTextArea posMsgArea;
		/* Negative Panel Message Area */
		private JTextArea negMsgArea;
		/* Message to Send Text Area */
		private JTextArea messageToSend;
		/* Message to Send Text Area */
		private JTextArea display;
		/* Message to Send Text Area */
		private JTextArea email;
		/* Message to Send Text Area */
		private JTextArea flight;

		public FeedBackManagerMenu() {
			/* Creates the buttons that redirect to each manager window. */
			CreateButton("Positive Feedback", Color.white,
					"Read positive critics sent by clients", 15, 60, 200, 200,
					30);
			CreateButton("Negative Feedback", Color.white,
					"Read negative messages sent by clients", 15, 60, 250, 200,
					30);
			CreateButton("Send Notifications", Color.white,
					"Notificate clients about a special event", 15, 60, 300,
					200, 30);
			CreateButton("Return", Color.white, "Go back to the main menu", 15,
					60, 500, 100, 30);

			/*
			 * Creates the subpanels that are displayed accordingly to the
			 * user's choice.
			 */
			positivePanel = new JPanel();
			negativePanel = new JPanel();
			sendPanel = new JPanel();
			toPanel = new JPanel();

			/* Defines the subpanels. */
			positivePanel.setLayout(null);
			positivePanel.setBounds(new Rectangle(500, 40, 500, 400));
			positivePanel.add(CreateTitle("Positive Feedback Messages:",
					Color.white, 15, 20, 20, 250, 20));
			positivePanel
					.add(posMsgArea = CreateText(10, 50, 40, 60, 350, 320));
			JScrollPane jpPos = new JScrollPane(posMsgArea);
			positivePanel.add(jpPos);
			jpPos.setBounds(40, 60, 350, 320);
			posMsgArea.enableInputMethods(false);

			negativePanel.setLayout(null);
			negativePanel.setBounds(new Rectangle(500, 40, 500, 400));
			negativePanel.add(CreateTitle("Negative Feedback Messages:",
					Color.white, 15, 20, 20, 250, 20));
			negativePanel
					.add(negMsgArea = CreateText(10, 50, 40, 60, 350, 320));
			JScrollPane jpNeg = new JScrollPane(negMsgArea);
			negativePanel.add(jpNeg);
			jpNeg.setBounds(40, 60, 350, 320);
			negMsgArea.enableInputMethods(false);

			sendPanel.setLayout(null);
			sendPanel.setBounds(new Rectangle(500, 40, 500, 400));
			sendPanel.add(CreateButton("Send To...", Color.white,
					"Select the client", 15, 60, 300, 250, 30));
			sendPanel.add(CreateTitle("Notification:", Color.white, 15, 20, 20,
					100, 20));
			sendPanel.add(messageToSend = CreateText(10, 50, 40, 60, 300, 200));

			toPanel.setLayout(null);
			toPanel.setBounds(new Rectangle(500, 40, 500, 400));
			toPanel.add(CreateButton("Send", Color.white, "Send to client", 15,
					150, 50, 200, 30));
			toPanel.add(CreateTitle("Enter the client's email address:",
					Color.white, 15, 20, 20, 250, 20));
			toPanel.add(email = CreateText(10, 50, 270, 20, 200, 20));
			toPanel.add(CreateButton("Send To All", Color.white, "Send To All",
					15, 150, 150, 200, 30));
			toPanel.add(CreateTitle(
					"Enter the flightID to send to all client's with bookings:",
					Color.white, 15, 20, 100, 350, 40));
			toPanel.add(flight = CreateText(10, 50, 370, 100, 100, 20));
			toPanel.add(CreateButton("Send To Turistic Operators", Color.white,
					"Send To Turistic Operators", 15, 130, 260, 220, 30));
			toPanel.add(display = CreateText(10, 50, 40, 300, 300, 130));
			display.enableInputMethods(false);

			/* Adds the subpanels to the main panel. */
			panel.add(positivePanel);
			panel.add(negativePanel);
			panel.add(sendPanel);
			panel.add(toPanel);

			negativePanel.setVisible(false);
			negativePanel.setOpaque(false);
			sendPanel.setVisible(false);
			sendPanel.setOpaque(false);
			positivePanel.setVisible(false);
			positivePanel.setOpaque(false);
			toPanel.setVisible(false);
			toPanel.setOpaque(false);

		}

		public void entry() {
			setVisible(true);
			/* As default, we have the Buy Plane Menu. */
			positivePanel.setVisible(true);
		}

		public void mouseReleased(MouseEvent e) {
			if (e.getComponent().getName().equals("Positive Feedback")) {
				negativePanel.setVisible(false);
				sendPanel.setVisible(false);
				positivePanel.setVisible(true);
				toPanel.setVisible(false);
				// Positive List Button
				posMsgArea.setText("");

				for (Message f : feedBackManager.getPositiveFeedBackList()) {
					posMsgArea.append(f.getMessageContents()
							+ "\n--------------------------\n");
				}
			} else if (e.getComponent().getName().equals("Negative Feedback")) {
				negativePanel.setVisible(true);
				sendPanel.setVisible(false);
				positivePanel.setVisible(false);
				toPanel.setVisible(false);
				negMsgArea.setText("");

				for (Message f : feedBackManager.getNegativeFeedBackList()) {
					negMsgArea.append(f.getMessageContents()
							+ "\n--------------------------\n");
				}
			} else if (e.getComponent().getName().equals("Send Notifications")) {
				negativePanel.setVisible(false);
				sendPanel.setVisible(true);
				positivePanel.setVisible(false);
				toPanel.setVisible(false);
			} else if (e.getComponent().getName().equals("Return")) {
				negativePanel.setVisible(false);
				sendPanel.setVisible(false);
				positivePanel.setVisible(false);
				toPanel.setVisible(false);
				feedBackManagerMenu.setVisible(false);
				menu.setVisible(true);

			} else if (e.getComponent().getName().equals("Send To...")) {

				negativePanel.setVisible(false);
				sendPanel.setVisible(false);
				positivePanel.setVisible(false);
				toPanel.setVisible(true);

			} else if (e.getComponent().getName()
					.equals("Send To Turistic Operators")) {
				display.setText("");
				feedBackManager.sendNotificationAllOperators(
						operatorManager.getOperatorList(), "Notification",
						messageToSend.getText());
				display.setText("Message sent.");

			} else if (e.getComponent().getName().equals("Send")) {
				boolean status = false;
				display.setText("");
				if (!email.getText().equals(""))
					status = feedBackManager.sendNotificationUser(
							email.getText(), "Notification",
							messageToSend.getText());
				if (status)
					display.setText("Your Notification was sent to "
							+ email.getText() + ".\n");
				else
					display.setText("Error : Could not send the message.");

			} else if (e.getComponent().getName().equals("Send To All")) {
				boolean status = false;
				Flight f = null;
				display.setText("");
				if (!flight.getText().equals("")) {
					f = flightsManager.searchFlightById(Integer.parseInt(flight
							.getText()));
					if (f != null) {
						status = true;
						display.setText("Your Notification was sent to \n");
						for (Booking r : f.getBookings()) {
							if (feedBackManager.sendNotificationUser(r
									.getClient().getEmail(), "Notification",
									messageToSend.getText()))
								display.append(r.getClient().getEmail() + "\n");
						}

					}

				}
				if (!status)
					display.setText("Error : Could not send the message.");

			}
		}
	}

	@SuppressWarnings("serial")
	private class FlightsManagerMenu extends Window implements
			PropertyChangeListener {
		private JPanel schedulePanel;
		private JPanel reschedulePanel;
		private JPanel cancelPanel;
		private JPanel listPanel;
		private JPanel listFinishedPanel;

		/* SCHEDULEPANEL */
		private JTextField idPlaneScheduleField;
		private JTextField hourFieldSchedule;
		private JTextField minuteFieldSchedule;
		private JTextField scheduleDate;
		private JCalendar jCalendarFlight;
		private GregorianCalendar calendar;

		private String menuIdentifier;

		private JComboBox originSchedule;
		private JComboBox destinationSchedule;
		private JTextArea confirmActionSchedule;

		private JComboBox regularSchedule;
		private JComboBox normalSchedule;

		/* RESCHEDULEPANEL */
		private JTextField rescheduleFlightID;
		private JTextField rescheduleDate;
		private JTextField hourFieldReschedule;
		private JTextField minuteFieldReschedule;
		private JTextArea confirmActionReschedule;
		private JCalendar jCalendarReschedule;
		/* CANCELPANEL */
		private JTextField idFlightCancelPanel;
		private JTextArea confirmActionCancel;

		/* LISTPANEL */
		private JTextArea listArea;
		
		/* LISTFINISHEDPANEL */
		private JTextArea listFinishedArea;

		public FlightsManagerMenu() {
			/* Creates the buttons that redirect to each manager window. */
			CreateButton("Schedule Flight", Color.white,
					"Schedule a new flight", 15, 60, 200, 200, 30);
			CreateButton("Reschedule Flight", Color.white,
					"Reshedule a given flight", 15, 60, 250, 200, 30);
			CreateButton("Cancel Flight", Color.white,
					"Cancels an already scheduled flight", 15, 60, 300, 200, 30);
			CreateButton("List Flights", Color.white, "List all the Flights",
					15, 60, 350, 200, 30);
			CreateButton("List Finished Flights", Color.white, "List all finished the Flights",
					15, 60, 400, 200, 30);

			CreateButton("Return", Color.white, "Go back to the main menu", 15,
					60, 500, 100, 30);

			/*
			 * Creates the subpanels that are displayed accordingly to the
			 * user's choice.
			 */
			schedulePanel = new JPanel();
			reschedulePanel = new JPanel();
			cancelPanel = new JPanel();
			listPanel = new JPanel();
			listFinishedPanel = new JPanel();

			/* Defines the subpanels. */
			schedulePanel.setLayout(null);
			schedulePanel.setBounds(new Rectangle(500, 40, 500, 500));
			schedulePanel.add(CreateTitle("Date:", Color.white, 15, 60, 20, 70,
					20));
			schedulePanel
					.add(scheduleDate = CreateBoxText(20, 100, 20, 80, 20));
			calendar = new GregorianCalendar();
			scheduleDate.setText(calendar.get(Calendar.DAY_OF_MONTH) + "/"
					+ (calendar.get(Calendar.MONTH) + 1) + "/"
					+ calendar.get(Calendar.YEAR));
			schedulePanel.add(CreateButton("Schedule Date", Color.white,
					"Choose flight date", 15, 60, 50, 150, 30));
			schedulePanel.add(CreateTitle("TIME:", Color.white, 15, 60, 90, 70,
					20));
			schedulePanel.add(hourFieldSchedule = CreateBoxInt(20, 100, 90, 20,
					20, calendar.get(Calendar.HOUR_OF_DAY)));
			schedulePanel
					.add(CreateTitle("h", Color.white, 15, 125, 90, 70, 20));
			schedulePanel.add(minuteFieldSchedule = CreateBoxInt(20, 140, 90,
					20, 20, (calendar.get(Calendar.MINUTE) + 1) % 60));
			schedulePanel.add(CreateTitle("ID Plane:", Color.white, 15, 60,
					120, 70, 20));
			schedulePanel.add(idPlaneScheduleField = CreateBoxInt(20, 135, 120,
					50, 20, 0));
			schedulePanel.add(CreateTitle("Origin:", Color.white, 15, 60, 150,
					70, 20));
			schedulePanel.add(originSchedule = CreateComboBox(120, 150, 120,
					20, destinationsPrices.getDestinations()));
			schedulePanel.add(CreateTitle("Destination:", Color.white, 15, 60,
					180, 100, 20));
			schedulePanel.add(destinationSchedule = CreateComboBox(150, 180,
					120, 20, destinationsPrices.getDestinations()));
			schedulePanel.add(CreateTitle("Normal Flight:", Color.white, 15,
					60, 210, 100, 20));
			Vector<String> choiceChar = new Vector<String>();
			choiceChar.add("Yes");
			choiceChar.add("No");
			schedulePanel.add(normalSchedule = CreateComboBox(170, 210, 50, 20,
					choiceChar));
			schedulePanel.add(CreateTitle("Regular Flight:", Color.white, 15,
					60, 240, 150, 20));
			Vector<String> choiceReg = new Vector<String>();
			choiceReg.add("Yes");
			choiceReg.add("No");
			schedulePanel.add(regularSchedule = CreateComboBox(170, 240, 50,
					20, choiceReg));
			schedulePanel.add(confirmActionSchedule = CreateText(400, 120, 60,
					270, 400, 120));
			schedulePanel.add(CreateButton("Submit", Color.white,
					"Submit the form", 15, 60, 395, 100, 30));

			reschedulePanel.setLayout(null);
			reschedulePanel.setBounds(new Rectangle(500, 40, 500, 400));
			reschedulePanel.add(CreateTitle("Flight ID:", Color.white, 15, 60,
					20, 70, 20));
			reschedulePanel.add(rescheduleFlightID = CreateBoxInt(20, 120, 20,
					80, 20, 0));
			reschedulePanel.add(CreateTitle("Date:", Color.white, 15, 60, 50,
					70, 20));
			reschedulePanel.add(rescheduleDate = CreateBoxText(20, 100, 50, 80,
					20));
			rescheduleDate.setText(calendar.get(Calendar.DAY_OF_MONTH) + "/"
					+ (calendar.get(Calendar.MONTH) + 1) + "/"
					+ calendar.get(Calendar.YEAR));
			reschedulePanel.add(CreateButton("Reschedule Date", Color.white,
					"Choose flight date", 15, 60, 80, 150, 30));
			reschedulePanel.add(CreateTitle("TIME:", Color.white, 15, 60, 120,
					70, 20));
			reschedulePanel.add(hourFieldReschedule = CreateBoxInt(20, 100,
					120, 20, 20, calendar.get(Calendar.HOUR_OF_DAY)));
			reschedulePanel.add(CreateTitle("h", Color.white, 15, 125, 120, 70,
					20));
			reschedulePanel.add(minuteFieldReschedule = CreateBoxInt(20, 140,
					120, 20, 20, (calendar.get(Calendar.MINUTE) + 1) % 60));
			reschedulePanel.add(confirmActionReschedule = CreateText(400, 180,
					60, 150, 400, 180));
			reschedulePanel.add(CreateButton("Reschedule", Color.white,
					"Reschedule a flight", 15, 60, 350, 200, 30));

			cancelPanel.setLayout(null);
			cancelPanel.setBounds(new Rectangle(500, 40, 500, 400));
			cancelPanel.add(CreateTitle("Flight's ID:", Color.white, 15, 100,
					100, 150, 20));
			cancelPanel.add(idFlightCancelPanel = CreateBoxInt(20, 175, 100,
					50, 20, 0));
			cancelPanel.add(confirmActionCancel = CreateText(400, 150, 60, 150,
					400, 150));
			cancelPanel.add(CreateButton("Submit", Color.white,
					"Submit the form", 15, 60, 330, 100, 30));

			listPanel.setLayout(null);
			listPanel.setBounds(new Rectangle(500, 40, 500, 400));
			listPanel.add(CreateTitle("LIST OF FLIGHTS:", Color.white, 15, 20,
					20, 150, 20));
			listPanel.add(CreateTitle(
					" ID      PLANE        DESTINATION                  TIME",
					Color.white, 15, 20, 40, 400, 20));
			listPanel.add(listArea = CreateText(10, 50, 20, 60, 450, 320));
			JScrollPane jpList = new JScrollPane(listArea);
			listPanel.add(jpList);
			jpList.setBounds(20, 60, 450, 320);
			
			
			listFinishedPanel.setLayout(null);
			listFinishedPanel.setBounds(new Rectangle(500, 40, 500, 400));
			listFinishedPanel.add(CreateTitle("LIST OF FLIGHTS:", Color.white, 15, 20,
					20, 150, 20));
			listFinishedPanel.add(CreateTitle(
					" ID      PLANE        DESTINATION                  TIME",
					Color.white, 15, 20, 40, 400, 20));
			listFinishedPanel.add(listFinishedArea = CreateText(10, 50, 20, 60, 450, 320));
			JScrollPane jpFinishedList = new JScrollPane(listFinishedArea);
			listFinishedPanel.add(jpFinishedList);
			jpFinishedList.setBounds(20, 60, 450, 320);

			/* Adds the subpanels to the main panel. */
			panel.add(schedulePanel);
			panel.add(reschedulePanel);
			panel.add(cancelPanel);
			panel.add(listPanel);
			panel.add(listFinishedPanel);

			reschedulePanel.setVisible(false);
			reschedulePanel.setOpaque(false);
			cancelPanel.setVisible(false);
			cancelPanel.setOpaque(false);
			listPanel.setVisible(false);
			listPanel.setOpaque(false);
			listFinishedPanel.setVisible(false);
			listFinishedPanel.setOpaque(false);
			schedulePanel.setVisible(false);
			schedulePanel.setOpaque(false);
		}

		/*
		 * This function is used when the user enters this menu. We need to set
		 * true the right menu and one of its subpanels.
		 */
		public void entry() {
			setVisible(true);
			/* As default, we have the Buy Plane Menu. */
			schedulePanel.setVisible(true);

			menuIdentifier = "schedulePanel";
		}

		public void mouseReleased(MouseEvent e) {
			String[] dateFields = null;
			int day;
			int month;
			int year;
			int hour;
			int minute;
			int idPlane;

			if (e.getComponent().getName().equals("Schedule Flight")) {
				reschedulePanel.setVisible(false);
				cancelPanel.setVisible(false);
				listPanel.setVisible(false);
				listFinishedPanel.setVisible(false);
				schedulePanel.setVisible(true);

				menuIdentifier = "schedulePanel";
			} else if (e.getComponent().getName().equals("Reschedule Flight")) {
				reschedulePanel.setVisible(true);
				cancelPanel.setVisible(false);
				listPanel.setVisible(false);
				listFinishedPanel.setVisible(false);
				schedulePanel.setVisible(false);

				menuIdentifier = "reschedulePanel";
			} else if (e.getComponent().getName().equals("Cancel Flight")) {
				reschedulePanel.setVisible(false);
				cancelPanel.setVisible(true);
				listPanel.setVisible(false);
				listFinishedPanel.setVisible(false);
				schedulePanel.setVisible(false);

				menuIdentifier = "cancelPanel";
			} else if (e.getComponent().getName().equals("List Flights")) {
				reschedulePanel.setVisible(false);
				cancelPanel.setVisible(false);
				listPanel.setVisible(true);
				listFinishedPanel.setVisible(false);
				schedulePanel.setVisible(false);

				listArea.setText(flightsManager.listFlights());

				menuIdentifier = "listPanel";
			} else if (e.getComponent().getName().equals("List Finished Flights")) {
				reschedulePanel.setVisible(false);
				cancelPanel.setVisible(false);
				listPanel.setVisible(false);
				listFinishedPanel.setVisible(true);
				schedulePanel.setVisible(false);

				listFinishedArea.setText(flightsManager.listFinishedFlights());

				menuIdentifier = "listPanel";
			}else if (e.getComponent().getName().equals("Schedule Date")) {
				JFrame dateFlight = new JFrame("Booking");
				jCalendarFlight = new JCalendar();

				dateFlight.getContentPane().add(jCalendarFlight);
				dateFlight.pack();
				dateFlight.setVisible(true);
				/* Every time the user selects a new date, an event is generated */
				jCalendarFlight.addPropertyChangeListener(this);

			} else if (e.getComponent().getName().equals("Reschedule Date")) {
				JFrame dateFlight = new JFrame("Booking");
				jCalendarReschedule = new JCalendar();
				dateFlight.getContentPane().add(jCalendarReschedule);
				dateFlight.pack();
				dateFlight.setVisible(true);
				/* Every time the user selects a new date, an event is generated */
				jCalendarReschedule.addPropertyChangeListener(this);
			} else if (e.getComponent().getName().equals("Reschedule")) {
				try {
					dateFields = rescheduleDate.getText().split("/");
					day = Integer.parseInt(dateFields[0]);
					month = Integer.parseInt(dateFields[1]);
					year = Integer.parseInt(dateFields[2]);
					hour = Integer.parseInt(hourFieldReschedule.getText());
					minute = Integer.parseInt(minuteFieldReschedule.getText());
					idPlane = Integer.parseInt(rescheduleFlightID.getText());
					Flight flight = flightsManager.searchFlightById(idPlane);
					GregorianCalendar date;
					
					if (flight == null){
						/*If we get here, it means that's there no flight in the normal
						 * list. Are not allowing reschedules of regular flights.
						 */
						confirmActionReschedule.setText("There's no such flight!");
					}
					else if ((date = checkDate(year, month, day, hour, minute)) != null) {
						/* Reschedule the normal flight. */
						flight.setDate(date);
						confirmActionReschedule.setText("Flight rescheduled!");
					} else {
						confirmActionReschedule.setText("Invalid date!");
					}

				} catch (NumberFormatException e1) {
					confirmActionReschedule.setText("Invalid date!");
				}

			} else if (e.getComponent().getName().equals("Submit")) {
				/* We are inside one of the filling forms. */
				if (menuIdentifier.equals("schedulePanel")) {

					try {
						dateFields = scheduleDate.getText().split("/");
						day = Integer.parseInt(dateFields[0]);
						month = Integer.parseInt(dateFields[1]);
						year = Integer.parseInt(dateFields[2]);
						hour = Integer.parseInt(hourFieldSchedule.getText());
						minute = Integer
								.parseInt(minuteFieldSchedule.getText());
						idPlane = Integer.parseInt(idPlaneScheduleField
								.getText());
						Airplane airplane = planesManager.searchPlane(idPlane);

						if (airplane != null) {
							GregorianCalendar date;
							if ((checkDate(year, month, day, hour, minute)) != null
									&& !originSchedule.getSelectedItem()
											.equals("")
									&& !destinationSchedule.getSelectedItem()
											.equals("")) {
								date = new GregorianCalendar(year, month - 1,
										day, hour, minute);
								Flight flight = flightsManager.scheduleFlight(
										airplane, date, originSchedule
												.getSelectedItem().toString(),
										destinationSchedule.getSelectedItem()
												.toString(),
										regularSchedule.getSelectedItem()
												.toString() == "Yes" ? true
												: false,
										normalSchedule.getSelectedItem()
												.toString() == "Yes" ? false
												: true);

								if (flight != null)
									confirmActionSchedule
											.setText("Flight schedule with ID "
													+ flight.getId() + "!");
								else
									confirmActionSchedule
											.setText("There's already a regular flight for this date\nin this plane.");

							} else {
								confirmActionSchedule.setText("Invalid data");
							}

						} else {
							confirmActionSchedule.setText("Plane not found.");
						}
					} catch (NumberFormatException e1) {

						confirmActionSchedule.setText("Invalid integers.");
					}

				} else if (menuIdentifier.equals("cancelPanel")) {

					Flight flight = null;
					int id = -1;
					try {
						id = Integer.parseInt(idFlightCancelPanel.getText());

						flight = flightsManager.searchFlightById(id);

					} catch (Exception e1) {
						confirmActionCancel.setText("Invalid data.");
					}

					if (flight != null && id != -1) {
						flightsManager.cancelFlight(flight);
						confirmActionCancel
								.setText("Flight successfully canceled!");
					} else if (flight == null) {
						/* It wasn't found in the normal flight's list.
						 * Consequently, it may be a regular flight with
						 * no bookings yet and we ought to verify this
						 * possibility.
						 */
						
						RFlight rflight = flightsManager.searchRFlightById(id);
						
						/* Not even in the regular list this plane was found,
						 * so it really doesn't exist at all.
						 */
						if (rflight == null){
							confirmActionCancel.setText("Flight not found.");
						}
						else{
							flightsManager.cancelRegularFlight(rflight);
							confirmActionCancel
								.setText("Regular flight successfully canceled!");
						}
					}
				}
			} else if (e.getComponent().getName().equals("Return")) {
				reschedulePanel.setVisible(false);
				cancelPanel.setVisible(false);
				listPanel.setVisible(false);
				schedulePanel.setVisible(false);
				listFinishedPanel.setVisible(false);
				flightsManagerMenu.setVisible(false);
				menu.setVisible(true);
			}
		}

		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			Object source = evt.getSource();
			Calendar cal;
			if (source == jCalendarFlight) {
				cal = jCalendarFlight.getCalendar();
				calendar = new GregorianCalendar(cal.get(Calendar.YEAR),
						cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
				scheduleDate.setText(calendar.get(Calendar.DAY_OF_MONTH) + "/"
						+ (calendar.get(Calendar.MONTH) + 1) + "/"
						+ calendar.get(Calendar.YEAR));
			} else if (source == jCalendarReschedule) {
				cal = jCalendarReschedule.getCalendar();
				calendar = new GregorianCalendar(cal.get(Calendar.YEAR),
						cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
				rescheduleDate.setText(calendar.get(Calendar.DAY_OF_MONTH)
						+ "/" + (calendar.get(Calendar.MONTH) + 1) + "/"
						+ calendar.get(Calendar.YEAR));
			}

		}
	}

	@SuppressWarnings("serial")
	private class PlanesManagerMenu extends Window {
		private JPanel buyPanel;
		private JPanel sellPanel;
		private JPanel listPanel;
		private JPanel findPanel;

		/* So we can know in which the user is at the moment. */
		private String menuIdentifier;

		/* BUYPANEL VARIABLES */
		private JTextField companyField;
		private JTextField modelField;
		private JTextField noSeatsField;
		private JTextArea buyArea;

		/* SELLPANEL VARIABLES */
		private JTextField idSellField;
		private JTextArea sellArea;

		/* LISTPANEL VARIABLES */
		private JTextArea listArea;

		/* FINDPANEL VARIABLES */
		private JTextField idSearchField;
		private JTextArea findArea;

		public PlanesManagerMenu() {
			/* Creates the buttons that redirect to each manager window. */
			CreateButton("Buy Plane", Color.white,
					"Adds a new plane to the fleet", 15, 60, 200, 150, 30);
			CreateButton("Sell Plane", Color.white,
					"Removes a plane from the fleet", 15, 60, 250, 150, 30);
			CreateButton("List Planes", Color.white,
					"Removes a plane from the fleet", 15, 60, 300, 150, 30);
			CreateButton("Find Plane", Color.white,
					"List all the planes from the fleet", 15, 60, 350, 150, 30);
			CreateButton("Return", Color.white, "Go back to the main menu", 15,
					60, 500, 100, 30);

			/*
			 * Creates the subpanels that are displayed accordingly to the
			 * user's choice.
			 */
			buyPanel = new JPanel();
			sellPanel = new JPanel();
			listPanel = new JPanel();
			findPanel = new JPanel();

			/* Defines the subpanels. */
			buyPanel.setLayout(null);
			buyPanel.setBounds(new Rectangle(500, 40, 500, 400));
			buyPanel.add(CreateTitle("No. Seats:", Color.white, 15, 100, 100,
					150, 20));
			buyPanel.add(noSeatsField = CreateBoxInt(20, 175, 100, 50, 20, 0));
			buyPanel.add(CreateTitle("Company:", Color.white, 15, 100, 130, 70,
					20));
			buyPanel.add(companyField = CreateBoxText(20, 175, 130, 110, 20));
			buyPanel.add(CreateTitle("Model:", Color.white, 15, 100, 160, 70,
					20));
			buyPanel.add(modelField = CreateBoxText(20, 175, 160, 110, 20));
			buyPanel.add(buyArea = CreateText(10, 10, 60, 200, 320, 100));
			buyPanel.add(CreateButton("Submit", Color.white, "Submit the form",
					15, 60, 330, 100, 30));

			sellPanel.setLayout(null);
			sellPanel.setBounds(new Rectangle(500, 40, 500, 400));
			sellPanel.add(CreateTitle("Plane's ID:", Color.white, 15, 90, 100,
					150, 20));
			sellPanel.add(idSellField = CreateBoxInt(20, 175, 100, 50, 20, 0));
			sellPanel.add(sellArea = CreateText(10, 10, 60, 140, 320, 140));
			sellPanel.add(CreateButton("Submit", Color.white,
					"Submit the form", 15, 60, 330, 100, 30));

			listPanel.setLayout(null);
			listPanel.setBounds(new Rectangle(500, 40, 500, 400));
			listPanel.add(CreateTitle("LIST OF FLIGHTS:", Color.white, 15, 20,
					20, 150, 20));
			listPanel.add(CreateTitle(
					"     ID    SEATS        COMPANY            MODEL",
					Color.white, 15, 20, 40, 400, 20));
			listPanel.add(listArea = CreateText(10, 50, 40, 60, 350, 280));
			JScrollPane jpList = new JScrollPane(listArea);
			listPanel.add(jpList);
			jpList.setBounds(40, 60, 350, 280);

			findPanel.setLayout(null);
			findPanel.setBounds(new Rectangle(500, 40, 500, 400));
			findPanel.add(CreateTitle("Plane's ID:", Color.white, 15, 90, 70,
					150, 20));
			findPanel.add(idSearchField = CreateBoxInt(20, 175, 70, 50, 20, 0));
			findPanel.add(findArea = CreateText(10, 50, 60, 100, 350, 150));
			findPanel.add(CreateButton("Search Plane", Color.white,
					"Search for an Airplane", 15, 100, 300, 200, 30));

			/* Adds the subpanels to the main panel. */
			panel.add(buyPanel);
			panel.add(sellPanel);
			panel.add(listPanel);
			panel.add(findPanel);

			buyPanel.setVisible(false);
			buyPanel.setOpaque(false);
			sellPanel.setVisible(false);
			sellPanel.setOpaque(false);
			listPanel.setVisible(false);
			listPanel.setOpaque(false);
			findPanel.setVisible(false);
			findPanel.setOpaque(false);

		}

		/*
		 * This function is used when the user enters this menu. We need to set
		 * true the right menu and one of its subpanels.
		 */
		public void entry() {
			setVisible(true);
			/* As default, we have the Buy Plane Menu. */
			buyPanel.setVisible(true);
			menuIdentifier = "buyPanel";
		}

		public void mouseReleased(MouseEvent e) {
			if (e.getComponent().getName().equals("Buy Plane")) {
				buyPanel.setVisible(true);
				sellPanel.setVisible(false);
				listPanel.setVisible(false);
				findPanel.setVisible(false);

				buyArea.setText("");
				menuIdentifier = "buyPanel";
			} else if (e.getComponent().getName().equals("Sell Plane")) {
				buyPanel.setVisible(false);
				sellPanel.setVisible(true);
				listPanel.setVisible(false);
				findPanel.setVisible(false);

				sellArea.setText("");
				menuIdentifier = "sellPanel";
			} else if (e.getComponent().getName().equals("List Planes")) {
				buyPanel.setVisible(false);
				sellPanel.setVisible(false);
				listPanel.setVisible(true);
				findPanel.setVisible(false);

				listArea.setText("");
				Vector<Airplane> list = planesManager.getPlanesList();

				for (int i = 0; i < list.size(); i++) {
					Airplane airplane = list.get(i);
					listArea.append(airplane.getId() + "              "
							+ airplane.getNoSeats() + "\t         "
							+ airplane.getCompany() + "\t                   "
							+ airplane.getModel() + "\n");
				}

				menuIdentifier = "listPanel";
			} else if (e.getComponent().getName().equals("Find Plane")) {
				buyPanel.setVisible(false);
				sellPanel.setVisible(false);
				listPanel.setVisible(false);
				findPanel.setVisible(true);

				findArea.setText("");
				menuIdentifier = "findPanel";
			} else if (e.getComponent().getName().equals("Search Plane")) {
				Airplane airplane = null;
				try {
					int flightID = Integer.parseInt(idSearchField.getText());
					airplane = planesManager.searchPlane(flightID);
					if (airplane != null)
						findArea.setText(airplane.toString());
					else
						findArea.setText("Plane with id " + flightID
								+ " does not exist.");

				} catch (NumberFormatException e1) {
					findArea.setText("Invalid flight id.");
				}
			} else if ((e.getComponent().getName().equals("Submit"))) {
				/* We are inside one of the filling forms. */
				if (menuIdentifier.equals("buyPanel")) {

					Airplane airplane = null;
					try {
						int noSeats = Integer.parseInt(noSeatsField.getText());
						String company = companyField.getText();
						String model = modelField.getText();

						if (!company.equals("") && !model.equals("")
								&& noSeats > 0) {
							airplane = new Airplane(noSeats, company, model, now);
						} else {
							buyArea.setText("Invalid data.");
						}
					} catch (Exception e1) {
						buyArea.setText("Invalid data.");
					}

					if (airplane != null) {
						planesManager.addPlane(airplane);
						buyArea.setText("Plane successfully added to the fleet, with ID "
								+ airplane.getId() + "!");
					}
				} else if (menuIdentifier.equals("sellPanel")) {
					Airplane airplane = null;
					int id = -1;
					try {
						id = Integer.parseInt(idSellField.getText());

						airplane = planesManager.searchPlane(id);

					} catch (Exception e1) {
						sellArea.setText("Invalid data.");
					}

					if (airplane != null && id != -1) {
						Vector<Flight> associatedFlights = airplane
								.getFlights();

						int counter = 0;
						while (associatedFlights.size() > 0) {
							Flight flight = associatedFlights.get(0);
							associatedFlights.remove(0);
							flightsManager.cancelFlight(flight);
							counter++;
						}

						planesManager.removePlane(airplane);

						sellArea.setText("Plane successfully remove from the fleet,\nwith "
								+ counter + " flight(s) cancelled!");
					} else if (airplane == null) {
						sellArea.setText("Plane not found.");
					}
				}
			} else if (e.getComponent().getName().equals("Return")) {
				buyPanel.setVisible(false);
				sellPanel.setVisible(false);
				listPanel.setVisible(false);
				findPanel.setVisible(false);

				planesManagerMenu.setVisible(false);
				menu.setVisible(true);
			}
		}
	}

	@SuppressWarnings("serial")
	private class StatisticsManagerMenu extends Window implements
			PropertyChangeListener {
		private JPanel statsPanel;
		private JTextArea statArea;
		private JCalendar jCalendarBegin;
		private JCalendar jCalendarEnd;
		private GregorianCalendar calendarBegin;
		private JTextField dateBegin;
		private GregorianCalendar calendarEnd;
		private JTextField dateEnd;

		public StatisticsManagerMenu() {
			/* Creates the buttons that redirect to each manager window. */
			CreateButton("Return", Color.white, "Go back to the main menu", 15,
					60, 500, 100, 30);

			/* Creates the subpanels */
			statsPanel = new JPanel();
			statsPanel.setLayout(null);
			statsPanel.setBounds(new Rectangle(500, 40, 500, 400));
			statsPanel.add(CreateTitle("Satistics:", Color.white, 15, 100, 100,
					70, 20));
			statsPanel.add(statArea = CreateText(10, 50, 40, 60, 350, 250));
			statArea.setEditable(false);
			statsPanel.add(CreateButton("Submit", Color.white,
					"Submit the form", 15, 250, 360, 100, 30));
			statsPanel.setOpaque(false);

			add(dateBegin = CreateBoxText(20, 100, 20, 80, 20));
			calendarBegin = new GregorianCalendar();
			dateBegin.setText(calendarBegin.get(Calendar.DAY_OF_MONTH) + "/"
					+ calendarBegin.get((Calendar.MONTH) + 1) + "/"
					+ calendarBegin.get(Calendar.YEAR));
			dateBegin.setEditable(false);
			add(CreateButton("Begin Date", Color.white,
					"Choose the begining of the statistical count", 15, 60, 50,
					150, 30));

			add(dateEnd = CreateBoxText(20, 100, 90, 80, 20));
			calendarEnd = new GregorianCalendar();
			dateEnd.setText(calendarEnd.get(Calendar.DAY_OF_MONTH) + "/"
					+ calendarEnd.get((Calendar.MONTH) + 1) + "/"
					+ calendarEnd.get(Calendar.YEAR));
			dateEnd.setEditable(false);
			add(CreateButton("End Date", Color.white,
					"Choose the begining of the statistical count", 15, 60,
					120, 150, 30));

			calendarEnd = null;
			calendarBegin = null;
			dateBegin.setText("00/00/00");
			dateEnd.setText("00/00/00");
			add(CreateButton("Reset Dates", Color.white,
					"Set the statistical count global", 15, 60, 200, 150, 30));

			this.add(statsPanel);

		}

		/*
		 * This function is used when the user enters this menu. We need to set
		 * true the right menu and one of its subpanels.
		 */
		public void entry() {
			setVisible(true);
			statsPanel.setVisible(true);

		}

		public void mouseReleased(MouseEvent e) {
			if (e.getComponent().getName().equals("Statistics 1")) {

			} else if (e.getComponent().getName().equals("Submit")) {
				if (calendarBegin == null || calendarEnd == null) {
					statArea.setText("Global Statistics\n"
							+ statisticsManager.generate(null, null));

					return;
				}
				if (calendarEnd.after(calendarBegin)) {
					statArea.setText(statisticsManager.generate(calendarBegin,
							calendarEnd));
				} else {
					statArea.setText("The end date is set before the begin one. FAIL");
				}
			} else if (e.getComponent().getName().equals("Reset Dates")) {
				calendarEnd = null;
				calendarBegin = null;
				dateBegin.setText("00/00/00");
				dateEnd.setText("00/00/00");

			} else if (e.getComponent().getName().equals("Return")) {
				statisticsManagerMenu.setVisible(false);
				menu.setVisible(true);
			} else if (e.getComponent().getName().equals("Begin Date")) {
				JFrame date = new JFrame("Booking");
				jCalendarBegin = new JCalendar();

				date.getContentPane().add(jCalendarBegin);
				date.pack();
				date.setVisible(true);
				jCalendarBegin.addPropertyChangeListener(this);
			} else if (e.getComponent().getName().equals("End Date")) {
				JFrame date = new JFrame("Booking");
				jCalendarEnd = new JCalendar();

				date.getContentPane().add(jCalendarEnd);
				date.pack();
				date.setVisible(true);
				jCalendarEnd.addPropertyChangeListener(this);
			}
		}

		/* Every time the user selects a new date, an event is generated */
		public void propertyChange(PropertyChangeEvent evt) {
			Object source = evt.getSource();
			Calendar cal;
			if (source == jCalendarEnd) {
				cal = jCalendarEnd.getCalendar();
				calendarEnd = new GregorianCalendar(cal.get(Calendar.YEAR),
						cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
				dateEnd.setText(calendarEnd.get(Calendar.DAY_OF_MONTH) + "/"
						+ calendarEnd.get(Calendar.MONTH) + "/"
						+ calendarEnd.get(Calendar.YEAR));

			} else {
				cal = jCalendarBegin.getCalendar();
				calendarBegin = new GregorianCalendar(cal.get(Calendar.YEAR),
						cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
				dateBegin.setText(calendarBegin.get(Calendar.DAY_OF_MONTH)
						+ "/" + calendarBegin.get(Calendar.MONTH) + "/"
						+ calendarBegin.get(Calendar.YEAR));
			}

		}
	}

	@SuppressWarnings("serial")
	private class ClientsMenu extends Window {

		/* LIST OPERATORS */
		private JPanel operatorPanel;
		private JPanel clientPanel;
		private JPanel removePanel;
		private JTextArea operatorArea;
		private JTextArea clientArea;

		/* REMOVE OPERATOR */
		private JTextField nameField;
		private JTextArea removeArea;

		public ClientsMenu() {
			/* Creates the buttons that redirect to each manager window. */
			CreateButton("List Operators", Color.white, "List the Operators",
					15, 60, 200, 150, 30);
			CreateButton("List Clients", Color.white, "Remove one operator",
					15, 60, 250, 150, 30);
			CreateButton("Remove Operator", Color.white, "Remove one operator",
					15, 60, 300, 150, 30);
			CreateButton("Return", Color.white, "Go back to the main menu", 15,
					60, 500, 100, 30);

			/* Creates the subpanels */
			clientPanel = new JPanel();
			clientPanel.setLayout(null);
			clientPanel.setBounds(new Rectangle(500, 40, 500, 400));
			clientPanel.add(CreateTitle("Clients:", Color.white, 15, 60, 40,
					70, 20));
			clientPanel.add(CreateTitle("Name", Color.white, 15, 40, 80, 100,
					20));
			clientPanel.add(CreateTitle("Email", Color.white, 15, 150, 80, 100,
					20));
			clientPanel.add(CreateTitle("Phone Contact", Color.white, 15, 260,
					80, 150, 20));
			clientPanel.add(clientArea = CreateText(10, 50, 40, 120, 350, 260));
			JScrollPane jpOperator = new JScrollPane(clientArea);
			clientPanel.add(jpOperator);
			jpOperator.setBounds(new Rectangle(40, 100, 350, 260));
			clientArea.setEditable(false);
			clientPanel.setOpaque(false);

			operatorPanel = new JPanel();
			operatorPanel.setLayout(null);
			operatorPanel.setBounds(new Rectangle(500, 40, 500, 400));
			operatorPanel.add(CreateTitle("Operators:", Color.white, 15, 60,
					40, 100, 20));
			operatorPanel.add(CreateTitle("Name", Color.white, 15, 40, 80, 100,
					20));
			operatorPanel.add(CreateTitle("Company", Color.white, 15, 150, 80,
					100, 20));
			operatorPanel.add(CreateTitle("Phone Contact", Color.white, 15,
					260, 80, 150, 20));
			operatorPanel.add(operatorArea = CreateText(10, 50, 40, 120, 350,
					260));
			JScrollPane jpOperator2 = new JScrollPane(operatorArea);
			operatorPanel.add(jpOperator2);
			jpOperator2.setBounds(new Rectangle(40, 100, 350, 260));
			operatorArea.setEditable(false);
			operatorPanel.setOpaque(false);

			removePanel = new JPanel();
			removePanel.setLayout(null);
			removePanel.setBounds(new Rectangle(500, 40, 500, 400));
			removePanel.add(CreateTitle("Operator's name:", Color.white, 15,
					90, 100, 150, 20));
			removePanel.add(nameField = CreateBoxText(20, 220, 100, 150, 20));
			removePanel.add(removeArea = CreateText(10, 10, 60, 140, 320, 140));
			JScrollPane jpRemove = new JScrollPane(removeArea);
			removePanel.add(jpRemove);
			jpRemove.setBounds(new Rectangle(60, 140, 320, 140));
			removePanel.add(CreateButton("Remove", Color.white,
					"Remove Operator", 15, 60, 300, 100, 30));

			this.add(operatorPanel);
			this.add(clientPanel);
			this.add(removePanel);
			operatorPanel.setVisible(false);
			operatorPanel.setOpaque(false);
			clientPanel.setVisible(false);
			clientPanel.setOpaque(false);
			removePanel.setVisible(false);
			removePanel.setOpaque(false);

		}

		/*
		 * This function is used when the user enters this menu. We need to set
		 * true the right menu and one of its sub panels.
		 */
		public void entry() {
			setVisible(true);
			operatorPanel.setVisible(true);

		}

		public void mouseReleased(MouseEvent e) {

			if (e.getComponent().getName().equals("List Operators")) {
				operatorPanel.setVisible(true);
				clientPanel.setVisible(false);
				removePanel.setVisible(false);

				Vector<Operator> operador = operatorManager.getOperatorList();
				Iterator<Operator> it = operador.iterator();
				operatorArea.setText("");
				while (it.hasNext()) {
					operatorArea.append(it.next().toString() + "\n\n");
				}
			} else if (e.getComponent().getName().equals("List Clients")) {
				operatorPanel.setVisible(false);
				clientPanel.setVisible(true);
				removePanel.setVisible(false);

				clientArea.setText(clientsManager.listClients());

			} else if (e.getComponent().getName().equals("Remove Operator")) {
				operatorPanel.setVisible(false);
				clientPanel.setVisible(false);
				removePanel.setVisible(true);
			} else if (e.getComponent().getName().equals("Remove")) {
				String operator = nameField.getText();
				Operator op = operatorManager.searchOperator(operator);
				operatorManager.removeOperator(op);
				removeArea.setText("Operator with name " + operator
						+ " removed from the system");
			} else if (e.getComponent().getName().equals("Return")) {
				operatorPanel.setVisible(false);
				clientPanel.setVisible(false);
				clientsMenu.setVisible(false);
				menu.setVisible(true);
			}
		}
	}

	@SuppressWarnings("serial")
	private class LoginMenu extends Window {
		private JTextField usernameField, passwordField;

		/* The constructor. */
		public LoginMenu() {

			/* The fields to insert the username and password. */
			CreateTitle("Username: ", Color.white, 15, 90, 200, 90, 20);
			usernameField = CreateBoxText(20, 175, 200, 90, 20);
			CreateTitle("Password: ", Color.white, 15, 90, 230, 90, 20);
			passwordField = CreateBoxPassword(20, 175, 230, 90, 20);

			usernameField.setText(Constants.ADMIN_USERNAME);
			passwordField.setText(Constants.ADMIN_PASSWORD);

			/* The buttons. */
			CreateButton("Login", Color.white, "Login in the system", 15, 60,
					400, 100, 30);
			CreateButton("Exit", Color.white, "Leave the application", 15, 60,
					450, 100, 30);
		}

		/* The option buttons that can be selected by the user. */
		public void mouseReleased(MouseEvent e) {
			if (e.getComponent().getName().equals("Login")) {
				/* The login has been completed successfully. */
				if (loginAdmin(usernameField.getText(), passwordField.getText())) {
					loginMenu.setVisible(false);
					menu.setVisible(true);
				}
				/* The user failed the authentication. */
				else {
					JOptionPane jp = new JOptionPane(
							"Sorry, but the login is incorrect.",
							JOptionPane.INFORMATION_MESSAGE);
					JDialog jd = jp.createDialog("Incorrect information");
					jd.setBounds(new Rectangle(340, 200, 320, 120));
					jd.setVisible(true);
				}
			} else if (e.getComponent().getName().equals("Exit")) {
				/* The user is leaving the application. */
				JOptionPane jp = new JOptionPane("Have a nice day!",
						JOptionPane.INFORMATION_MESSAGE);
				JDialog jd = jp.createDialog("Thank you!");
				jd.setBounds(new Rectangle(340, 200, 320, 120));
				jd.setVisible(true);
				System.exit(0);
			}
		}
	}

	/**
	 * Inserts negative feedback provided by a client app in the appropriate
	 * structure in the FeedbackManager
	 * 
	 * @see common.BackOfficeRemoteInterface#sendNegativeFeedback(messages.Feedback)
	 */
	@Override
	public void sendNegativeFeedback(Message feedback) throws RemoteException {
		System.out.println(" " + feedback.getMessageContents());

		feedBackManager.insertNegativeFeedback(feedback);

	}

	/**
	 * Inserts positive feedback provided by a client app in the appropriate
	 * structure in the FeedbackManager
	 * 
	 * @see common.BackOfficeRemoteInterface#sendNegativeFeedback(messages.Feedback)
	 */
	@Override
	public void sendPositiveFeedback(Message feedback) throws RemoteException {
		System.out.println(" " + feedback.getMessageContents());
		feedBackManager.insertPositiveFeedback(feedback);

	}

	/**
	 * Check if username is already in use and register operator
	 * 
	 * @return String with the result of the operation
	 */
	@Override
	public String registerOperator(String comp, String name, String addr,
			String phone, String mail, String password) throws RemoteException {
		return operatorManager.registerOperator(comp, name, addr, phone, mail,
				password);
	}

	/**
	 * Check if the username and password are correct
	 * 
	 * @return String with the result of the operation
	 */
	@Override
	public String loginOperator(String user, String pass)
			throws RemoteException {
		return operatorManager.loginOperator(user, pass);

	}

	@Override
	public Vector<String> getDestinations() throws RemoteException {
		return destinationsPrices.getDestinations();
	}

	@Override
	public double getPrice(String orig, String dest) throws RemoteException {

		return destinationsPrices.getPrice(orig, dest);
	}

	/**
	 * Method used to create a new booking for a specific client. It is invoked
	 * remotely by the Front Office.
	 * 
	 * @return String with the result of the operation
	 */
	@Override
	public String scheduleBooking(int idFlight, String name, String address,
			String phone, String mail, int seats, boolean isOperator,
			int bookingNumber) throws RemoteException {

		Flight flight = flightsManager.searchFlightById(idFlight);
		FileOutputStream fos = null;
		DataOutputStream dos = null;
		FileInputStream fis = null;
		DataInputStream dis = null;

		File file = new File("idbooking.bin");

		try {
			fis = new FileInputStream(file);
			dis = new DataInputStream(fis);
			bookingNumber = dis.readInt();
			fis.close();
		} catch (IOException e) {
			bookingNumber = 0;
		}

		

		/* First, we need to check if there's such a flight. */
		if (flight == null) {

			/*
			 * If it isn't in the normal list, than we have to check if there is
			 * a flight on the regular list.
			 */

			RFlight rflight = flightsManager.searchRFlightById(idFlight);

			/* If it is still null, than there is no such flight in any list. */
			if (rflight == null) {
				return "Innexistent flight";
			}
			else if (rflight.isCharter() && !isOperator){
				return "Charter";
			}
			

			/*
			 * If we ever get here, it means that we have found a regular flight
			 * with this id. Consequently, we have to schedule a flight with
			 * this information on the normal list, as we got null in the
			 * previous if, meaning we don't have this flight in the normal
			 * list.
			 * If it's a charter flight and it's a regular client scheduling it,
			 * we undo the operation.
			 */

			int weekDay = rflight.getWeekDay();
			GregorianCalendar data = new GregorianCalendar();

			/*
			 * We can be in that specific day of the week. However, if we past
			 * the schedule, this day of the week is no longer valid and
			 * consequently, we will have to increment one day.
			 */
			if (data.get(Calendar.DAY_OF_WEEK) == weekDay) {
				/* We have past the flight hour, so it can only be next week. */
				if (data.get(Calendar.HOUR_OF_DAY) > rflight.getHour()) {
					data.add(Calendar.DAY_OF_MONTH, 7);
				}
				/* It's in this hour, so we need to check the minutes. */
				else if (data.get(Calendar.HOUR_OF_DAY) == rflight.getHour()) {
					/* We have past the time. Only next week. */
					if (data.get(Calendar.MINUTE) >= rflight.getMinute()) {
						data.add(Calendar.DAY_OF_MONTH, 7);
					}
					/* Else, we are still in time to schedule the flight. */
				}
				/* Else, we are still in time to schedule the flight. */
			}
			
			while (data.get(Calendar.DAY_OF_WEEK) != weekDay) {
				data.add(Calendar.DAY_OF_MONTH, 1);
			}

			data.set(Calendar.HOUR_OF_DAY, rflight.getHour());
			data.set(Calendar.MINUTE, rflight.getMinute());
			data.set(Calendar.SECOND, 0);

			flight = flightsManager.reScheduleRFlight(rflight.getPlane(),
					rflight.getIdFlight(), data, rflight.getOrigin(),
					rflight.getDestination(), rflight.isCharter());

		}
		/* Second, we need to check if we still have space in this flight. */
		else if ((new GregorianCalendar()).after(flight.getDate())) {
			return "Over";
		}
		/* Third, we only accept operators booking charter flights. */
		else if (flight.isCharter() && !isOperator) {
			return "Charter";
		}

		/*
		 * We have to make sure several people aren't scheduling at the same
		 * time for the same flight.
		 */
		synchronized (flight.lock) {
			/*
			 * Difference between the number of seats for this booking and the
			 * seats available in the flight.
			 */
			int diff = flight.getEmptySeats() - seats;

			if (diff < 0) {
				return "InsufficientSeats " + flight.getEmptySeats();
			}

			Client client = new Client(name, address, phone, mail);
			Booking booking = new Booking(flight.getId(), seats, client,
					bookingNumber, getPrice(flight.getOrigin(),
							flight.getDestination()));
			clientsManager.putClient(client, booking);
			flightsManager.addBookingFlight(flight, booking);

		}
		try {
			fos = new FileOutputStream(file);

			dos = new DataOutputStream(fos);
			dos.writeInt((bookingNumber + 1));
			fos.close();

		} catch (IOException e) {

		}
		return "Scheduled "+bookingNumber;
	}

	/**
	 * Method to send the flight list to the Front Office through the JAVA RMI
	 * connection.
	 * 
	 * @return String with the list of flights
	 */
	@Override
	public String listFlights() throws RemoteException {
		return flightsManager.listFlights();
	}

	/**
	 * Find a flight in a specific date, origin and destination.
	 * 
	 * @return String with the result of the operation
	 */
	@Override
	public String findFlights(int year, int month, int day, String origin,
			String destination) throws RemoteException {
		return flightsManager
				.findFlights(year, month, day, origin, destination);
	}

	/**
	 * Client invokes this method to cancel a booking made previously by
	 * removing the booking from the flight manifest.
	 * 
	 * @return String with the result of the operation(Success or not)
	 */
	@Override
	public String cancelBooking(int idFlight, int idBooking)
			throws RemoteException {

		Flight flight = flightsManager.searchFlightById(idFlight);

		/* First, we need to check if there's such a flight. */
		if (flight == null) {
			return "Innexistent flight";
		}

		Booking booking = flight.findBookingById(idBooking);

		/* Second, we need to check if we still have space in this flight. */
		if (booking == null) {
			return "Innexistent booking";
		}

		/*
		 * We have to make sure several people aren't scheduling at the same
		 * time for the same flight.
		 */
		synchronized (flight.lock) {
			flightsManager.removeBookingFlight(flight, booking);
			// int seats = booking.getNoSeats();
			// flight.removeBooking(booking);

			// flight.decreaseOccupied(seats);

		}

		return "Cancelled";
	}

	/**
	 * Method invoked by the Operator to charter a new flight.
	 * 
	 * @return String with the result of the operation(Success or not)
	 */
	@Override
	public String scheduleCharter(GregorianCalendar date, String origin,
			String destination, int seats) throws RemoteException {
		/* First, we need to check if there's an airplane with enough seats. */
		Airplane plane = planesManager.searchPlaneBySeats(seats);

		if (plane != null) {
			/*
			 * If it exists we need to create a new flight associated with that
			 * airplane.
			 */
			Flight flight = flightsManager.scheduleFlight(plane, date, origin,
					destination, false, true);
			if (flight != null)
				return "Charter was booked successfully";
		}

		/* If there is not airplane available, return error message. */
		return "It wasn't possible to book the charter, no planes available";
	}

	/**
	 * Get information about a specific booking.
	 * 
	 * @return String with the booking info
	 */
	@Override
	public String getBookingInfo(int idFlight, int idBooking)
			throws RemoteException {
		/* First, we need to check if there's such a flight. */
		Flight flight = flightsManager.searchFlightById(idFlight);

		if (flight == null) {
			return "There's no such flight";
		}
		/* If it exists we try to get the asked booking. */
		Booking booking = flight.findBookingById(idBooking);
		if (booking != null)
			return booking.toString();
		else
			return "That booking is not associated with Flight " + idFlight;
	}

	/**
	 * Modify information about a specific booking.
	 * 
	 * @return String with the result of the operation(Success or not)
	 */
	@Override
	public String modifyBooking(int idFlight, int idBooking, int idNewFlight,
			boolean isOperator, int bookingNumber) throws RemoteException {
		String name, address, phone, email;
		int seats;

		Flight flight = flightsManager.searchFlightById(idFlight);

		/* First, we need to check if there's such a flight. */
		if (flight == null) {
			return "Innexistent flight";
		}

		Booking booking = flight.findBookingById(idBooking);

		/* Second, we need to check if we still have space in this flight. */
		if (booking == null) {
			return "Innexistent booking";
		}

		name = booking.getClient().getName();
		address = booking.getClient().getAddress();
		phone = booking.getClient().getPhoneContact();
		email = booking.getClient().getEmail();
		seats = booking.getNoSeats();

		String answer = scheduleBooking(idNewFlight, name, address, phone,
				email, seats, isOperator, bookingNumber);

		if (!answer.equals("Scheduled")) {
			return answer + "\nYour booking with ID " + idBooking
					+ " to flight " + idFlight + " still exists";
		}

		/*
		 * We have to make sure several people aren't scheduling at the same
		 * time for the same flight.
		 */
		synchronized (flight.lock) {
			flightsManager.removeBookingFlight(flight, booking);
			// flight.removeBooking(booking);
			// flight.decreaseOccupied(seats);
		}

		return "Booking scheduled, with booking number " + bookingNumber
				+ " and flight number " + idNewFlight + ".";
	}

	/**
	 * Get the price for a specific booking.
	 * 
	 * @return Array with two Doubles with the result of the operation(Success
	 *         or not)
	 */
	@Override
	public Double[] bookingPrice(int idFlight, String clientEmail)
			throws RemoteException {
		Flight flight = flightsManager.searchFlightById(idFlight);
		Double[] info = new Double[2];
		info[0] = destinationsPrices.getPrice(flight.getOrigin(),
				flight.getDestination());
		info[1] = clientsManager.searchClient(clientEmail).getKilometers();

		return info;
	}

	/**
	 * Method used to decrease user miles after using discount.
	 */
	@Override
	public void updateMiles(Double miles, String mail) throws RemoteException {
		clientsManager.searchClient(mail).setKilometers(miles);
	}

}

class DestinationsPrices {
	private double[][] table = new double[22][22];
	private Vector<String> destinations;

	public DestinationsPrices() {
		destinations = new Vector<String>();
		destinations.add("Lisbon");
		destinations.add("Porto");
		destinations.add("Paris");
		destinations.add("Milan");
		destinations.add("Rome");
		destinations.add("Amsterdam");
		destinations.add("Madrid");
		destinations.add("Barcelona");
		destinations.add("Berlin");
		destinations.add("London");

		table[0][1] = table[1][0] = 27.5;
		table[0][2] = table[2][0] = 145.0;
		table[0][3] = table[3][0] = 167.8;
		table[0][4] = table[4][0] = 186.8;
		table[0][5] = table[5][0] = 185.9;
		table[0][6] = table[6][0] = 50.0;
		table[0][7] = table[7][0] = 99.4;
		table[0][8] = table[8][0] = 230.6;
		table[0][9] = table[9][0] = 158.5;
		/* * * * * * * * * * * * * * * */
		table[1][2] = table[2][1] = 121.1;
		table[1][3] = table[3][1] = 151.3;
		table[1][4] = table[4][1] = 176.5;
		table[1][5] = table[5][1] = 160.9;
		table[1][6] = table[6][1] = 42.1;
		table[1][7] = table[7][1] = 89.5;
		table[1][8] = table[8][1] = 208.0;
		table[1][9] = table[9][1] = 132.1;
		/* * * * * * * * * * * * * * * */
		table[2][3] = table[3][2] = 63.8;
		table[2][4] = table[4][2] = 115.1;
		table[2][5] = table[5][2] = 42.8;
		table[2][6] = table[6][2] = 104.9;
		table[2][7] = table[7][2] = 83.9;
		table[2][8] = table[8][2] = 87.4;
		table[2][9] = table[9][2] = 34.2;
		/* * * * * * * * * * * * * * * */
		table[3][4] = table[4][3] = 48.6;
		table[3][5] = table[5][3] = 82.7;
		table[3][6] = table[6][3] = 118.5;
		table[3][7] = table[7][3] = 73.4;
		table[3][8] = table[8][3] = 84.0;
		table[3][9] = table[9][3] = 95.6;
		/* * * * * * * * * * * * * * * */
		table[4][5] = table[5][4] = 130.3;
		table[4][6] = table[6][4] = 137.3;
		table[4][7] = table[7][4] = 87.6;
		table[4][8] = table[8][4] = 118.5;
		table[4][9] = table[9][4] = 144.1;
		/* * * * * * * * * * * * * * * */
		table[5][6] = table[6][5] = 147.6;
		table[5][7] = table[7][5] = 124.6;
		table[5][8] = table[8][5] = 57.3;
		table[5][9] = table[9][5] = 35.4;
		/* * * * * * * * * * * * * * * */
		table[6][7] = table[7][6] = 49.7;
		table[6][8] = table[8][6] = 186.3;
		table[6][9] = table[9][6] = 120.6;
		/* * * * * * * * * * * * * * * */
		table[7][8] = table[8][7] = 150.7;
		table[7][9] = table[9][7] = 114.6;
		/* * * * * * * * * * * * * * * */
		table[8][9] = table[9][8] = 92.6;

	}

	public Vector<String> getDestinations() {
		return destinations;
	}

	public double getPrice(String orig, String dest) {
		int dep, arriv;

		dep = getNumber(orig);
		arriv = getNumber(dest);

		if (dep != arriv && dep != -1 && arriv != -1)
			return table[dep][arriv];

		return 0;
	}

	public int getNumber(String city) {

		if (city.equals("Lisbon")) {
			return 0;
		} else if (city.equals("Porto")) {
			return 1;
		} else if (city.equals("Paris")) {
			return 2;
		} else if (city.equals("Milan")) {
			return 3;
		} else if (city.equals("Rome")) {
			return 4;
		} else if (city.equals("Amsterdam")) {
			return 5;
		} else if (city.equals("Madrid")) {
			return 6;
		} else if (city.equals("Barcelona")) {
			return 7;
		} else if (city.equals("Berlin")) {
			return 8;
		} else if (city.equals("London")) {
			return 9;
		}

		return -1;
	}
}
