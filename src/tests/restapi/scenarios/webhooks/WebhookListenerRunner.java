package tests.restapi.scenarios.webhooks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class WebhookListenerRunner implements Runnable {

	private int port;
    public String content;
    
    public WebhookListenerRunner(int port) {
    	this.port = port;
    	this.content = null;
    }
    
    @Override
    public void run() {
        try {
			listenOnPort(this.port);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    private void listenOnPort(int port) throws IOException {
    	ServerSocket serverSocket = new ServerSocket(port);
    	Socket socket = serverSocket.accept();
    	//read message
    	BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    	String content = br.readLine();
    	System.out.println("webhook recieved! - "+port+ ":"+ content);
    	this.content = content;
    	serverSocket.close();
    }

}
