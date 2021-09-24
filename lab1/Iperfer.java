/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cs640_lab1;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.ServerSocket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Scanner;
/**
 *
 * @author cmdup
 */
public class Iperfer {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        run_client("",0,0);
        //run_server(0);
        String hostname_ip;
        int port_num;
        int time;
        // TODO code application logic here
        //if(!(args.length == 3 || args.length == 6)) {
         //   System.out.println("Error: Invalid arguments");
        //    System.exit(1);
        //}
        if(args[0].equals("-c")) {
            run_client("",0,0);
            //if ((!args[1].equals("-h")) || (!args[3].equals("-p")) || (!args[5].equals("-t"))) {
            //    System.out.println("Error: Invalid arguments");
            //    System.exit(1);
            //}
            // Check input values
            // Hostname
            //try {
                
            //} catch (Exception ex) {
                
            //}
            // Port
            try {
                port_num = Integer.parseInt(args[4]);
                if((port_num < 1024) || (port_num > 65535)) {
                    System.out.println("Error: port number must be in the range 1024 to 65535");
                    System.exit(1);
                }
            } catch (Exception ex) {
                System.out.println("Error: port number must be a valid number");
                System.exit(1);
            }
            // Time
            // Time is assumed to mean seconds
            try {
                time = Integer.parseInt(args[6]);
                if(time < 0) {
                    System.out.println("Error: time cannot be a negative value");
                    System.exit(1);
                }
            } catch (Exception ex) {
                System.out.println("Error: time must be a valid number");
                System.exit(1);
            }
    
            
        } else if(args[0].equals("-s")) {
            run_server(0);
        } else {
            System.out.println("Error: Invalid arguments");
            System.exit(1);
        }
    }
    
    public static void run_client(String hostname_ip, int port_num, int time) {
        try{
            String test_hostname_ip = "127.0.0.1";
            int test_port_num = 5000;
            int test_time = 3;
            Socket socket = new Socket(test_hostname_ip, test_port_num);
            PrintWriter out = new PrintWriter(socket.getOutputStream(),true);
            String msg = "Hello World!";
            byte barray[] = new byte[1000];
            for(int i = 0; i < 1000; i++) {
                barray[i] = 0;
            }
            //ADD loop 
            
            out.println(msg);
            
            out.close();
            socket.close();
        } catch(Exception ex) {
            
        }
    }
    
    public static void run_server(int port_num) {
        try{
            int test_port_num = 5000;
            ServerSocket server = new ServerSocket(test_port_num);
            Socket socket = server.accept();
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line = in.readLine();
            System.out.println(line);
            in.close();
            socket.close();
            server.close();
        } catch(Exception ex){
            
        }
       
    }
    
}
