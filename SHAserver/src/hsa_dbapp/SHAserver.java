package hsa_dbapp;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;



public class SHAserver {
	
	int activeClients;
	int terminate;
	
	public class ClientHandler implements Runnable {
		BufferedReader reader;
		PrintWriter writer;
		Socket sock;
		
		
		public ClientHandler(Socket clientSocket) {
			try {
				sock = clientSocket;
				InputStreamReader isReader = new InputStreamReader(sock.getInputStream());
				writer = new PrintWriter(sock.getOutputStream());
				reader = new BufferedReader(isReader);
				System.out.println("Connection to client successful");
			} catch (Exception ex) { ex.printStackTrace(); }
			activeClients++;
			System.out.println("Current Clients: " + activeClients);
		}
		
		public void run() {
			try {
				String op = reader.readLine();
				if(op.equals("search"))
				{
					String name = reader.readLine();
					String pw = ""; //reader.readLine();
					String IDIP = getUserIDIP(name,pw);
					if(IDIP == null)
						IDIP = "User not found";
					System.out.println("About to write "+IDIP);
					writer.println(IDIP);
				}
				else if(op.equals("update"))
				{
					String uID = reader.readLine();
					String pw = reader.readLine();
				}
				writer.flush();
			} catch(Exception ex) { ex.printStackTrace(); }
			activeClients--;
			System.out.println("Current Clients: " + activeClients);
		}
	}

	public static void main(String[] args) {
		new SHAserver().go();
	}

	public String getUserIDIP(String userName, String password)
	{
		String url = "jdbc:mysql://localhost:3306/";
		String dbName = "hsa";
		String driver = "com.mysql.jdbc.Driver";
		
		
		String psql = "SELECT DomainID FROM EmailDomains WHERE DomainName=?";
		String psql2 = "SELECT CustomerID, SystemIP FROM Customers_DomainFK WHERE DomainID=? and LocalPart=?";
		ResultSet rs = null;
		
		
		String[] nameDomain = userName.split("[@]");  	//Splits email address into local and domain
		if(nameDomain.length != 2)
			return null;									//Returns out of invalid email syntax

		String uIDIP = null;
		
		try (Connection conn = DriverManager.getConnection(url+dbName,"usersearch","password");
				PreparedStatement pstmt = conn.prepareStatement(psql);
				PreparedStatement pstmt2 = conn.prepareStatement(psql2);){
			
			Class.forName(driver).newInstance();
			
			pstmt.setString(1, nameDomain[1]);
			rs = pstmt.executeQuery();
			
			//if no domain match found exit
			if(!rs.next())
				return null;
			
			//System.out.println("Domain Match: "+rs.getInt(1)+"\n");
				
			pstmt2.setString(1, String.valueOf(rs.getInt(1)));
			pstmt2.setString(2, nameDomain[0]);
			rs = pstmt2.executeQuery();
			
			//if no local part found exit
			if(!rs.next())
				return null;
			
			uIDIP = rs.getInt(1)+"@"+rs.getString(2);
			
			System.out.println("userID: "+rs.getInt(1)+"\n");
			System.out.println("SystemIP: "+rs.getString(2)+"\n");
			
		} catch(Exception e) {
			e.printStackTrace();
			
		}
		return uIDIP;
	}
	
	public void go() {
		terminate = 0;
		JFrame frame = new JFrame("ServerApp");
		JPanel panel = new JPanel();
		JLabel lblCount = new JLabel("Current # of clients: ");
		JLabel lblNum = new JLabel("0");
		panel.add(lblCount);
		panel.add(lblNum);
		frame.getContentPane().add(BorderLayout.CENTER, panel);
		frame.setSize(200,100);
		frame.setVisible(true);
		frame.addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent e) {
				terminate = 1;
			}
		});
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		activeClients = 0;

		
		try(ServerSocket serverSock = new ServerSocket(5000)){
			
			while(true && terminate == 0) {
				Socket clientSocket = serverSock.accept();
				
		
				
				Thread t = new Thread(new ClientHandler(clientSocket));
				t.start();
				System.out.println("Got a Connection");
			} 
		} catch(Exception ex) { ex.printStackTrace(); }
	} // end of go
}
