package common;

import java.rmi.*;

import messages.Feedback;

public interface BackOfficeRemoteInterface extends Remote{
	
	abstract void sendPositiveFeedback(Feedback feedback) throws RemoteException;
	
	abstract void sendNegativeFeedback(Feedback feedback) throws RemoteException;
	
	abstract void registerOperator(String comp, String name, String addr, String phone, String mail,String password, FrontOfficeRemoteInterface f) throws RemoteException;
	
	abstract void loginOperator(String user, String pass, FrontOfficeRemoteInterface f) throws RemoteException;
	
	
}