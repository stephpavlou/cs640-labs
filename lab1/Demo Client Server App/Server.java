package com.assignment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Java Program for the Server
 */
public class Server {

    /** Port on which this server will run */
    private final int port;

    /** Initialize socket and input stream */
    private ServerSocket    server   = null;
    private Socket          socket   = null;
    private BufferedReader  in       = null;

    public Server(int port) {
        this.port = port;
    }

    /**
     * Start the server
     */
    void start() {
        try {
            // Specify what port is the Server going to run
            server = new ServerSocket(port);
            System.out.println("Server started");

            System.out.println("Waiting for a client ...");

            socket = server.accept();
            System.out.println("Client accepted");

            // Reader object to take input from the client socket
            in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            String line = "";

            // Read the message from client
            while ((line = in.readLine()) != null) {
                System.out.println(line);
            }

            // Close the connection
            System.out.println("Closing connection");
            socket.close();
            in.close();
        } catch(IOException i) {
            System.out.println(i);
            System.exit(1);
        }
    }

    public static void main(String args[]){
        // Specify what port is the Server going to run
        int port = 5000;
        Server server = new Server(port);
        server.start();
    }
}