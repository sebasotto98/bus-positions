package ds.android.ds_project;

import ds.android.ds_project.Interfaces.Broker;
import ds.android.ds_project.Interfaces.Publisher;
import ds.android.ds_project.Interfaces.Subscriber;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * This program is a server application that receives data from a Publisher and
 * sends requested values from that data to the Subscriber
 *
 * @author  Albernaz de Sotto Mayor Sebastiao Cristo, Konstantakos Michail
 * @since   14/04/2019
 */

public class Channel implements Runnable, Cloneable {

    public static final String CHANNEL_IP = "192.168.1.4";
    public static final int CHANNEL_PORT = 7654;

    private static int pubCount = 0;
    private static ServerSocket server;

    private static volatile List<Broker> brokers = new ArrayList<>();
    private static List<Publisher> publishers = new ArrayList<>();
    private static List<Subscriber> subscribers = new ArrayList<>();

    private Channel() {
        try {
            server = new ServerSocket(CHANNEL_PORT);
        } catch (IOException e) {
            System.err.println("Error while trying to open server socket.");
        }
    }

    /**
     * The channel waits for a connection and receives the object of a node.
     *
     * If the object is a Broker, the channel updates the broker list, sends it to the connected
     * broker and then it sends the list to every other broker connected on the channel.
     *
     * If the object is a Publisher, the channel updates the publisher list, sends the broker list
     * to the connected publsher and informs every other publisher that a new publisher connected.
     *
     * If the object is a Subscriber, the channel updates the subscriber list and the broker list and sends
     * the updated broker list to every connected subscriber.
     */
    @Override
    public void run() {
        Socket connection;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        try {
            connection = server.accept();
            out = new ObjectOutputStream(connection.getOutputStream());
            in = new ObjectInputStream(connection.getInputStream());
        } catch (IOException e) {
            System.err.println("Error while trying to initiate connection with a node.");
        }
        Channel copyChannel = null;
        try {
            copyChannel = (Channel) this.clone();
        } catch (CloneNotSupportedException e) {
            System.err.println("Error while trying to make a copy of this Channel.");
        }
        Thread thread = new Thread(copyChannel);
        thread.start();
        Object object = null;
        try {
            object = in.readUnshared();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error while trying to receive object.");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.err.println("Unknown object received.");
        }
        if (object instanceof Broker) {
            brokers.add((Broker)object);
            try {
                out.writeUnshared(brokers);
            } catch (IOException e) {
                System.err.println("Error while trying to send brokers list.");
            }
            for (int i = 0; i < brokers.size() - 1; i++) {
                Socket requestSocket;
                try {
                    requestSocket = new Socket(brokers.get(i).getIP(), brokers.get(i).getChannelPort());
                    out = new ObjectOutputStream(requestSocket.getOutputStream());
                    out.writeUnshared(0);
                    out.writeUnshared(brokers);
                } catch (IOException e) {
                    System.err.println("Error while trying to initiate connection with broker node.");
                }
            }
            System.out.println("Broker " + brokers.size() + " connected.");
            int brokerID = -1;
            while (true) {
                try {
                    brokerID = (int)in.readUnshared();
                } catch (IOException e) {
                    System.err.println("Broker disconnected.");
                    brokers.remove(brokerID - 1);
                    for (int i = 0; i < brokers.size(); i++) {
                        Socket requestSocket;
                        try {
                            requestSocket = new Socket(brokers.get(i).getIP(), brokers.get(i).getChannelPort());
                            out = new ObjectOutputStream(requestSocket.getOutputStream());
                            out.writeUnshared(2);
                            out.writeUnshared(brokers);
                            out.writeUnshared(i+1);
                        } catch (IOException er) {
                            System.err.println("Error while trying to initiate connection with broker node.");
                        }
                    }
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException er) {
                        System.out.println("Thread was interrupted.");
                        break;
                    }
                    ArrayList<Broker> newBrokers = new ArrayList<>();
                    for (int i = 0; i < brokers.size(); i++) {
                        Socket requestSocket;
                        try {
                            requestSocket = new Socket(brokers.get(i).getIP(), brokers.get(i).getChannelPort());
                            ObjectOutputStream outBro = new ObjectOutputStream(requestSocket.getOutputStream());
                            in = new ObjectInputStream(requestSocket.getInputStream());
                            outBro.writeUnshared(1);
                            newBrokers.add((Broker) in.readUnshared());
                        } catch (IOException er) {
                            System.err.println("Error while trying to send command number.");
                        } catch (ClassNotFoundException er) {
                            System.err.println("Error while trying to receive broker object.");
                        }
                    }
                    brokers = newBrokers;
                    List<Subscriber> subs = new ArrayList<>(subscribers);
                    for (Subscriber sub : subs) {
                        Socket requestSocket;
                        try {
                            requestSocket = new Socket(sub.getIP(), sub.getPort());
                            out = new ObjectOutputStream(requestSocket.getOutputStream());
                            out.writeUnshared(brokers);
                        } catch (IOException er) {
                            er.printStackTrace();
                            subscribers.remove(sub);
                        }
                    }
                    break;
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                    break;
                }
            }
        } else if (object instanceof Publisher) {
            publishers.add((Publisher)object);
            try {
                out.writeUnshared(++pubCount);
                out.writeUnshared(brokers);
            } catch (IOException e) {
                System.err.println("Error while trying to send info to publisher node.");
            }
            for (int i = 0; i < publishers.size() - 1; i++) {
                Socket requestSocket;
                try {
                    requestSocket = new Socket(publishers.get(i).getIP(), publishers.get(i).getPort());
                    out = new ObjectOutputStream(requestSocket.getOutputStream());
                    out.writeUnshared(pubCount);
                } catch (IOException e) {
                    System.err.println("Error while trying to send info to publisher node.");
                }
            }
            System.out.println("Publisher " + pubCount + " connected.");
        } else if (object instanceof Subscriber) {
            subscribers.add((Subscriber) object);
            ArrayList<Broker> newBrokers = new ArrayList<>();
            for (int i = 0; i < brokers.size(); i++) {
                Socket requestSocket;
                try {
                    requestSocket = new Socket(brokers.get(i).getIP(), brokers.get(i).getChannelPort());
                    ObjectOutputStream outBro = new ObjectOutputStream(requestSocket.getOutputStream());
                    in = new ObjectInputStream(requestSocket.getInputStream());
                    outBro.writeUnshared(1);
                    newBrokers.add((Broker) in.readUnshared());
                } catch (IOException e) {
                    System.err.println("Error while trying to send command number.");
                } catch (ClassNotFoundException e) {
                    System.err.println("Error while trying to receive broker object.");
                }
            }
            brokers = newBrokers;
            try {
                out.writeUnshared(brokers);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        Channel channel = new Channel();
        Thread thread = new Thread(channel);
        thread.start();
    }

}