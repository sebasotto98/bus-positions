package gr.bus_positions_android;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import gr.bus_positions.Classes.Topic;
import gr.bus_positions.Classes.Value;
import gr.bus_positions.Interfaces.Broker;
import gr.bus_positions.Interfaces.Node;
import gr.bus_positions.Interfaces.Subscriber;
import static gr.bus_positions.Channel.CHANNEL_IP;
import static gr.bus_positions.Channel.CHANNEL_PORT;
import static gr.bus_positions.Implementations.BrokerImpl.SLEEP_TIME;
public class SubscriberImpl implements Subscriber, Node, Serializable {
    public static final long serialVersionUID = -931651160190181219L;
    private static final String SUBSCRIBER_IP = "10.0.2.15";
    private int PORT = 9876;
    private transient Socket requestSocket = null;
    private transient ObjectOutputStream out = null;
    private transient ObjectInputStream in = null;
    private int port;
    private InetAddress IP;
    private transient MapsActivity.ReadValues view;
    private Subscriber sub = new gr.bus_positions.Implementations.SubscriberImpl(getIP(), getPort());
    private List<Broker> brokers;
    private List<ArrayList<Topic>> brokerKeys;
    private List<Topic> topics = new ArrayList<>();
    private List<Broker> subscribedBrokers = new ArrayList<>();

    SubscriberImpl(MapsActivity.ReadValues view) {
        this.view = view;
    }

    @Override
    public InetAddress getIP() {
        try {
            return InetAddress.getByName(SUBSCRIBER_IP);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    @Override
    public int getPort() {
        return PORT;
    }

    public void register(Broker bro, Topic top) {
        if (!topics.contains(top)) {
            subscribedBrokers.add(bro);
            topics.add(top);
        }
        IP = bro.getIP();
        port = bro.getSubPort();
        connect();
        try {
            out.writeUnshared(sub);
            out.writeUnshared(top);
        } catch (IOException exception) {
            System.err.println("Error when attempting to send request for topic.");
        }
    }

    public void disconnect(Broker bro, Topic top) {
        if (topics.contains(top)) {
            subscribedBrokers.remove(bro);
            topics.remove(top);
        }
        disconnect();
    }

    public void init(int c) {
        this.port = c;
        try {
            IP = InetAddress.getByName(CHANNEL_IP);
        } catch (UnknownHostException e) {
            System.err.println("Couldn't get channel's IP.");
        }
        connect();
    }

    public void connect() {
        try {
            requestSocket = new Socket(IP, port);
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            in = new ObjectInputStream(requestSocket.getInputStream());
        } catch (UnknownHostException unknownHost) {
            System.err.println("You are trying to connect to an unknown host!");
        } catch (IOException ioException) {
            System.err.println("Cannot connect to host!");
        }
    }

    public void disconnect() {
        try {
            in.close();
            out.close();
            requestSocket.close();
        } catch (IOException ioException) {
            System.err.println("Error when attempting to disconnect from Broker.");
        }
    }

    public void updateNodes() {
        brokerKeys = new ArrayList<>();
        for (int i = 0; i < brokers.size(); i++) {
            ArrayList<Topic> newArrayList = new ArrayList<>(brokers.get(i).getTopicList());
            brokerKeys.add(newArrayList);
        }
    }

    public List<Broker> getBrokers() {
        return brokers;
    }

    public void setBrokers(List<Broker> brokers) {
        this.brokers = brokers;
    }

    /**
     * When the thread starts, it initializes the node with the portnum 5432
     * which belongs to the broker with ID 1 and connect with it,
     * calls the updateNode() to get the brokers list and
     * disconnect from broker with ID 1.
     * <p>
     * Then it asks in a loop the user to give the bus line he is interested in.
     * If the number is 0, the program exits.
     * Otherwise, it searches in the brokers list for the broker that is responsible for
     * this topic and registers it.
     * <p>
     * Lastly it receives the value of the topic, disconnects from the broker
     * and visualizes it if it is valid.
     */
    public void getValue(String request) {
        this.init(CHANNEL_PORT);
        try {
            out.writeUnshared(sub);
            brokers = (List<Broker>) in.readUnshared();
        } catch (IOException e) {
            System.err.println("Error while trying to send subscriber object.");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        updateNodes();
        Broker registeredBroker = null;
        Value value;
        do {
            value = null;
            if (registeredBroker == null) {
                outerLoop:
                for (int i = 0; i < brokers.size(); i++) {
                    for (Topic topic : brokerKeys.get(i)) {
                        if (topic.getTopic().equals(request)) {
                            registeredBroker = brokers.get(i);
                            register(registeredBroker, topic);
                            try {
                                value = (Value) in.readUnshared();
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (ClassNotFoundException e) {
                                e.printStackTrace();
                            }
                            break outerLoop;
                        }
                    }
                }
            } else {
                try {
                    value = (Value) in.readUnshared();
                } catch (IOException e) {
                    registeredBroker = null;
                    Socket connection;
                    ObjectInputStream in;
                    try {
                        ServerSocket channelProviderSocket = new ServerSocket(PORT);
                        connection = channelProviderSocket.accept();
                        in = new ObjectInputStream(connection.getInputStream());
                        brokers = (List<Broker>) in.readUnshared();
                        updateNodes();
                    } catch (IOException er) {
                        System.err.println("Could not connect with channel.");
                    } catch (ClassNotFoundException er) {
                        System.err.println("Could not receive brokers list.");
                    }
                    continue;
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
            if (value == null) {
                System.out.println("There is no data for your request: " + request);
                try {
                    Thread.sleep(SLEEP_TIME);
                } catch (InterruptedException e) {
                    System.out.println("Thread was interrupted.");
                    break;
                }
            } else {
                view.visualizeData(value);
            }
        } while (true);
    }
}