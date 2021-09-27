import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.ServerSocket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Scanner;
/**
 *  Authors: Stephanos Pavlou, Chris Dupont, Param Gandhi
 * 
 * It is assumed that the flags are case sensitive
 * 
 *  Class Iperfer:
 *  This class is used to make a simple client or server to measure packets
 *  of 1000 bytes of "0" and the rate of transfer between them. main does 
 *  checking of the inputs to make sure they are valid before running either
 *  as a client or a server.
 */


public class Iperfer {

    public static void main(String[] args) {
        String hostname_ip = "";
        int port_num = 0;
        int time = 0;
        
        if(!(args.length == 3 || args.length == 7)) {
            System.out.println("Error: Invalid arguments");
            System.exit(1);
        }
        if(args[0].equals("-c")) {
            // This checks that the -h, -p, and -t flags are in the right location and are formatted correctly
            if ((!args[1].equals("-h")) || (!args[3].equals("-p")) || (!args[5].equals("-t"))) {
                System.out.println("Error: Invalid arguments");
                System.exit(1);
            }
            // Check input values
            // Hostname
            try {
                hostname_ip = args[2];
                // This tests that the hostname/ip is no greater than 253 characters
                if(hostname_ip.length() > 253) {
                    System.out.println("Error: hostname must be at most 253 ASCII characters");
                    System.exit(1);
                }
                // This tests that there are only valid characters in the ip/hostname
                String acceptable_chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.-";
                for(int i = 0; i < hostname_ip.length(); i++) {
                    boolean good = false;
                    for(int j = 0; j < acceptable_chars.length();j++) {
                        if(hostname_ip.charAt(i) == acceptable_chars.charAt(j)){
                            good = true;
                            break;
                        }
                    }
                    if(!good) {
                        System.out.println("Error: hostname must only contain valid characters");
                        System.exit(1);
                    }
                }
                
                if(hostname_ip.contains(".")) {
                    String[] labels = hostname_ip.split("\\.");
                    for(int i = 0; i < labels.length; i++) {
                        int label_length = labels[i].length();
                        // This tests that each label is at least 1 char long and at most 63 char long
                        if((label_length < 1) || (label_length > 63)) {
                            System.out.println("Error: label name must contain 1 to 63 characters");
                            System.exit(1);
                        }
                        // This tests that each label does not start or end with a '-'
                        if((labels[i].charAt(0) == '-') || (labels[i].charAt(labels[i].length()-1) == '-')) {
                            System.out.println("Error: labels cannot begin or end with a hyphen");
                            System.exit(1);
                        }
                    }
                } else {
                    System.out.println("Error: Hostname must have multiple "
                            + "labels delimited by periods");
                    System.exit(1);
                }
            } catch (Exception ex) {
                System.out.println("Error: Invalid arguments");
                System.exit(1);
            }
            // Port
            // This tests that the port number at least 1024 and at most 65535
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
            // This tests that the time argument is a valid non-negative integer
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
            run_client(hostname_ip, port_num, time);
        } else if(args[0].equals("-s")) {
            // This checks that the -p flag is in the correct location with the right format
            if (!args[1].equals("-p")) {
                System.out.println("Error: Invalid arguments");
                System.exit(1);
            }
            // Port
            // This tests that the port number at least 1024 and at most 65535
            try {
                port_num = Integer.parseInt(args[2]);
                if((port_num < 1024) || (port_num > 65535)) {
                    System.out.println("Error: port number must be in the range 1024 to 65535");
                    System.exit(1);
                }
            } catch (Exception ex) {
                System.out.println("Error: port number must be a valid number");
                System.exit(1);
            }
            run_server(port_num);
        } else {
            System.out.println("Error: Invalid arguments");
            System.exit(1);
        }
    }
    
    public static void run_client(String hostname_ip, int port_num, int time) {
        try{
            //System.out.printf("hostname_ip: "+ hostname_ip +", port_num: %d, time: %d", port_num, time);
            long num_packets = 0;
            double data_rate;
            Socket socket = new Socket(hostname_ip, port_num);
            //System.out.println("Client has connected");
            PrintWriter out = new PrintWriter(socket.getOutputStream(),true);
            // This creates an array of 1000 bytes, all set to 0
            byte byte_array[] = new byte[1000];
            for(int i = 0; i < 1000; i++) {
                byte_array[i] = 0;
            }
            // This converts the array of bytes into a string that can be 
            // passed to the socket
            String msg = new String(byte_array);
            //System.out.println("msg: " + msg);
            //System.out.printf("msg.length(): %d\n", msg.length());
            
            // This captures the time that the client should stop transmitting
            long target_time = System.currentTimeMillis() + (1000 * time);
            while(System.currentTimeMillis() < target_time) {
                // This keeps track of the number of 1000 byte chunks sent
                num_packets++;
                out.println(msg);
            }
            
            out.close();
            socket.close();
            
            // This calculates the rate of the communication in Mbps
            double bits_sent = num_packets*8000;
            //System.out.printf("bits_sent: %f", bits_sent);
            double megabits_sent = bits_sent/1000000;
            //System.out.printf("megabits sent: %f", megabits_sent);
            data_rate = megabits_sent/time;
            //data_rate = ((num_packets*8000)/1000000)/((double)time);
            System.out.printf("sent=%d KB rate =%.3f Mbps\n", num_packets, data_rate);
        } catch(Exception ex) {
            System.out.println(ex);
        }
    }
    
    public static void run_server(int port_num) {
        try{
            ServerSocket server = new ServerSocket(port_num);
            Socket socket = server.accept();
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            long num_bytes = 0;
            long start_time = -1;
            long end_time = 0;
            String line = "";
            byte[] data;
            while((line = in.readLine()) != null) {
                // This is used to capture the start time of the transaction
                if(start_time == -1) {
                    start_time = System.currentTimeMillis();
                }
                data = line.getBytes();
                // This keeps track of the number of bytes received
                num_bytes += data.length;
                //System.out.println("received: " + line);
                //System.out.printf("Length of byte array received: %d", data.length);
            }
            
            // This calculates the bytes received and the speed of the
            // transaction Mbps
            end_time = System.currentTimeMillis();
            long elapsed_time = end_time - start_time;
            double elapsed_time_seconds = elapsed_time / 1000;
            //System.out.printf("Elapsed time seconds: %f", elapsed_time_seconds);
            double bits_received = num_bytes * 8;
            //System.out.printf("bits_received: %f", bits_received);
            double megabits_received = bits_received / 1000000;
            //System.out.printf("megabits_received: %f", megabits_received);
            double rate_received = megabits_received/elapsed_time_seconds;
            
            in.close();
            socket.close();
            server.close();
            System.out.printf("received=%d KB rate =%.3f Mbps\n", (num_bytes/1000), rate_received);
        } catch(Exception ex){
            System.out.println(ex);
        }
       
    }
    
}
