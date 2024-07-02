package System.Util;

import java.io.IOException;
import java.net.*;

/**
 * Implements a duplex socket to abstract send and receive.
 * @author Abdalla Abdelhadi
 */
public class DuplexSocket {
    /**
     * The send and receive packets.
     */
    private DatagramPacket sendPacket, receivePacket;

    /**
     * The send and receive sockets.
     */
    private DatagramSocket receiveSocket, sendSocket;

    /**
     * The port to send on.
     */
    private int sendPortNum;

    /**
     * The address to listen on.
     */
    private InetAddress hostAddress;

    /**
     * The middleware logger
     */
    private Logger logger;

    /**
     * Creates a new duplex packet with the specified parameters.
     * @param receivePortNum int, the port to receive on.
     * @param sendPortNum int, the port to send on.
     * @param hostAddress InetAddress, the address to listen on.
     */
    public DuplexSocket (int receivePortNum, int sendPortNum, InetAddress hostAddress, Logger logger){
        try {
            this.sendSocket = new DatagramSocket();
            this.receiveSocket = new DatagramSocket(receivePortNum);
            this.sendPortNum = sendPortNum;
            this.hostAddress = hostAddress;
            this.logger = logger;
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a new duplex packet with the specified parameters.
     * @param receivePortNum int, the port to receive on.
     * @param sendPortNum int, the port to send on.
     * @param hostAddress InetAddress, the address to listen on.
     */
    public DuplexSocket (int receivePortNum, int sendPortNum, InetAddress hostAddress, Logger logger, int timeout){
        this(receivePortNum, sendPortNum, hostAddress, logger);
        try {
            this.receiveSocket.setSoTimeout(timeout);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends the specified serialized message.
     * @param msg byte[], the message to send.
     */
    public void send(byte[] msg){
        this.sendPacket = new DatagramPacket(msg, msg.length, this.hostAddress, this.sendPortNum);

        try {
            this.sendSocket.send(this.sendPacket); //send packet
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Receives a message on the receive socket.
     * @return DatagramPacket, the received packet.
     */
    public DatagramPacket receive() throws SocketTimeoutException {
        byte[] msg = new byte[100];
        this.receivePacket = new DatagramPacket(msg, msg.length);

        try {
            // Block until a datagram is received via sendReceiveSocket.
            this.logger.log("Waiting to receive packet... ");
            this.receiveSocket.receive(this.receivePacket);//receive packet
            System.out.println();
        } catch(IOException e) {
            // If a timeout occurs (only for timed out sockets)
            if(e instanceof SocketTimeoutException) {
                throw new SocketTimeoutException();
            }
            e.printStackTrace();
            System.exit(1);
        }

        return this.receivePacket;
    }
    
    public void close() {
    	this.sendSocket.close();
    	this.receiveSocket.close();
    }
}
