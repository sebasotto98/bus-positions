package gr.bus_positions.Implementations;
import gr.bus_positions.Classes.Topic;
import gr.bus_positions.Classes.Value;
import gr.bus_positions.Interfaces.Broker;
import gr.bus_positions.Interfaces.Node;
import gr.bus_positions.Interfaces.Publisher;
import gr.bus_positions.Interfaces.Subscriber;
import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import static gr.bus_positions.Channel.CHANNEL_IP;
import static gr.bus_positions.Channel.CHANNEL_PORT;
/**
 * This class is a server application that receives data from a Publisher and
 * sends requested values from that data to a Subscriber.
 *
 * @author  Albernaz de Sotto Mayor Sebastiao Cristo, Konstantakos Michail
 * @since   14/04/2019
 */
public class BrokerImpl implements Broker, Node, Runnable, Serializable, Cloneable {
    public static final long serialVersionUID = -5083142442417609669L;
    public static final int SLEEP_TIME = 200;
    private static final String BROKER_IP = "192.168.1.4";
    private int PORT = 6543;
    private int PUB_PORT = 4321;
    private int SUB_PORT = 5432;
    private transient volatile List<Broker> brokers = new ArrayList<>();
    private List<Subscriber> registeredSubscribers = new ArrayList<>();
    private List<Publisher> registeredPublishers = new ArrayList<>();
    private List<Topic> topicList;
    private transient volatile Hashtable<Topic, Value> dataHashtable = new Hashtable<>();
    private transient ServerSocket pubProviderSocket;
    private transient ServerSocket subProviderSocket;
    private transient ObjectOutputStream subOut;
    private transient ObjectInputStream subIn;
    private transient ObjectOutputStream pubOut;
    private transient ObjectInputStream pubIn;
    private String busLineIDHash;
    private InetAddress IP;
    private volatile int brokerID;
    private long pubConTrId;
    private long subConTrId;
    private Topic topic;
    public List<Broker> getBrokers() {
        return brokers;
    }
    public void setBrokers(List<Broker> brokers) {
        this.brokers = brokers;
    }
    public List<Topic> getTopicList() {
        return topicList;
    }
    public InetAddress getIP() {
        return IP;
    }
    public int getPubPort() {
        return PUB_PORT;
    }
    public int getSubPort() {
        return SUB_PORT;
    }
    public int getChannelPort() {
        return PORT;
    }
    public void init(int c) {
        topicList = new ArrayList<>(dataHashtable.keySet());
        connect();
        ObjectOutputStream out = null;
        try {
            IP = InetAddress.getByName(BROKER_IP);
            Socket requestSocket = new Socket(InetAddress.getByName(CHANNEL_IP), CHANNEL_PORT);
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(requestSocket.getInputStream());
            out.writeUnshared(this);
            brokers = (List<Broker>) in.readUnshared();
        } catch (UnknownHostException e) {
            System.err.println("Could not connect to channel.");
        } catch (IOException e) {
            System.err.println("Could not send broker's object.");
        } catch (ClassNotFoundException e) {
            System.err.println("Could not receive brokers list.");
        }
        this.brokerID = brokers.size();
        Thread subConTr = new Thread(this);
        subConTrId = subConTr.getId();
        subConTr.start();
        Thread pubConTr = new Thread(this);
        pubConTrId = pubConTr.getId();
        pubConTr.start();
        Updater updater = new Updater();
        Thread updaterTr = new Thread(updater);
        updaterTr.start();
        while (true) {
            try {
                out.writeUnshared(brokerID);
                Thread.sleep(1000);
            } catch (IOException e) {
                break;
            } catch (InterruptedException e) {
                break;
            }
        }
    }
    public void connect() {
        try {
            pubProviderSocket = new ServerSocket(PUB_PORT);
            subProviderSocket = new ServerSocket(SUB_PORT);
        } catch (IOException e) {
            System.err.println("Error when attempting to open sockets.");
        }
    }
    public void disconnect() {
        try {
            pubProviderSocket.close();
            subProviderSocket.close();
        } catch (IOException e) {
            System.err.println("Error when attempting to close sockets.");
        }
    }
    public void updateNodes() {
        long currentTrID = Thread.currentThread().getId();
        if (currentTrID == pubConTrId) {
            try {
                pubOut.writeUnshared(getBrokers());
                pubOut.flush();
                pubOut.reset();
            } catch (IOException e) {
                System.err.println("Could not send broker list.");
            }
        } else {
            try {
                subOut.writeUnshared(getBrokers());
                subOut.flush();
                subOut.reset();
            } catch (IOException e) {
                System.err.println("Could not send broker list.");
            }
        }
    }
    public void CalculateKeys() {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            String[] tokens = topic.getTopic().split(",");
            byte[] messageDigest = md.digest(tokens[0].getBytes());
            BigInteger no = new BigInteger(1, messageDigest);
            busLineIDHash = no.toString(16);
            while (busLineIDHash.length() < 32) {
                busLineIDHash = "0" + busLineIDHash;
            }
            String IPPort = IP.toString() + PUB_PORT;
            MessageDigest md2 = MessageDigest.getInstance("SHA-1");
            byte[] messageDigest2 = md2.digest(IPPort.getBytes());
            BigInteger no2 = new BigInteger(1, messageDigest2);
            String IPPortHash = no2.toString(16);
            while (IPPortHash.length() < 32) {
                IPPortHash = "0" + IPPortHash;
            }
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
    public Broker hashTopic(Topic top, Value val) {
        CalculateKeys();
        if (strToInt(busLineIDHash) % brokers.size() == brokerID - 1) {
            for (Topic topic : dataHashtable.keySet()) {
                if (topic.equals(top)) {
                    top = topic;
                    break;
                }
            }
            dataHashtable.put(top, val);
            return this;
        }
        return this;
    }
    private static int strToInt(String str) {
        int i = 0;
        int num = 0;
        while (i < str.length()) {
            num *= 10;
            num += str.charAt(i++) - '0';
        }
        if (num < 0) num = -num;
        return num;
    }
    public void pull(Topic top) {
        for (Topic topic : dataHashtable.keySet()) {
            if (topic.equals(top)) {
                top = topic;
                break;
            }
        }
        try {
            Value lastValue = dataHashtable.get(top);
            subOut.writeUnshared(lastValue);
            while (true) {
                if (!lastValue.getTimestamp().equals(dataHashtable.get(top).getTimestamp())) {
                    lastValue = dataHashtable.get(top);
                    subOut.writeUnshared(lastValue);
                    subOut.flush();
                    subOut.reset();
                }
                try {
                    Thread.sleep(SLEEP_TIME);
                } catch (InterruptedException er) {
                    System.out.println("Thread was interrupted.");
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Subscriber disconnected.");
        }
    }
    public Publisher acceptConnection(Publisher pub) {
        try {
            while (true) {
                topic = (Topic) pubIn.readObject();
                Value value = (Value) pubIn.readObject();
                hashTopic(topic, value);
            }
        } catch (IOException e) {
            System.out.println("Publisher disconnected.");
            return null;
        } catch (ClassNotFoundException e) {
            System.err.println("Publisher sent unexpected object.");
            return null;
        } finally {
            return pub;
        }
    }
    public Subscriber acceptConnection(Subscriber sub) {
        Topic topic;
        try {
            topic = (Topic) subIn.readUnshared();
            if (topic.getTopic().compareTo(String.valueOf(0)) == 0) {
                return sub;
            }
            pull(topic);
        } catch (IOException e) {
            System.err.println("Error when attempting to receive topic from Subscriber.");
        } catch (ClassNotFoundException e) {
            System.err.println("Subscriber sent unexpected object.");
        }
        return sub;
    }
    /**
     * When the method starts it checks which thread called it.
     * <p>
     * If it is a Publisher connection thread then it opens a socket for the connection and if it is
     * the first Broker to connect to a Publisher then it sends the brokers list.
     * <p>
     * Afterwards it clones the current thread so that a new connection may be established between the
     * Broker and another Publisher.
     * <p>
     * Finally, the Publisher acceptConnection method is called in order to receive the data from the
     * connected Publisher.
     * <p>
     * If it is a Subcriber connection thread then it opens a socket for the connection and if it is
     * the first Broker to connect to a Subscriber then it sends the brokers list.
     * <p>
     * Afterwards it clones the current thread so that a new connection may be established between the
     * Broker and another Subscriber.
     * <p>
     * Finally, the Subscriber acceptConnection method is called in order to send the requested topic's
     * value to the connected Subscriber.
     */
    public void run() {
        long currentTrID = Thread.currentThread().getId();
        if (currentTrID == pubConTrId) {
            try {
                Socket pubConnection = pubProviderSocket.accept();
                pubOut = new ObjectOutputStream(pubConnection.getOutputStream());
                pubIn = new ObjectInputStream(pubConnection.getInputStream());
                BrokerImpl copyBroker = null;
                try {
                    copyBroker = (BrokerImpl) this.clone();
                } catch (CloneNotSupportedException e) {
                    System.err.println("Error while trying to make a copy of this Broker.");
                }
                Thread nextPubConTr = new Thread(copyBroker);
                pubConTrId = nextPubConTr.getId();
                copyBroker.pubConTrId = pubConTrId;
                nextPubConTr.start();
                Publisher pub = (Publisher) pubIn.readUnshared();
                registeredPublishers.add(pub);
                registeredPublishers.remove(acceptConnection(pub));
            } catch (IOException e) {
                System.err.println("Error when attempting to send broker list or receive Publisher object.");
            } catch (ClassNotFoundException e) {
                System.err.println("Publisher sent unexpected object.");
            }
        } else if (currentTrID == subConTrId) {
            try {
                Socket subConnection = subProviderSocket.accept();
                subOut = new ObjectOutputStream(subConnection.getOutputStream());
                subIn = new ObjectInputStream(subConnection.getInputStream());
                BrokerImpl copyBroker = null;
                try {
                    copyBroker = (BrokerImpl) this.clone();
                } catch (CloneNotSupportedException e) {
                    System.err.println("Error while trying to make a copy of this Broker.");
                }
                Thread nextSubConTr = new Thread(copyBroker);
                subConTrId = nextSubConTr.getId();
                copyBroker.subConTrId = subConTrId;
                nextSubConTr.start();
                Subscriber sub = (Subscriber) subIn.readUnshared();
                registeredSubscribers.add(sub);
                registeredSubscribers.remove(acceptConnection(sub));
            } catch (IOException e) {
                System.err.println("Error when attempting receive Subscriber object.");
            } catch (ClassNotFoundException e) {
                System.err.println("Subscriber sent unexpected object.");
            }
        }
    }
    /**
     * This class is used in a Thread to update the broker list everytime a new broker
     * connects to the channel and to send its topic list everytime a subscriber requests
     * to find which topics belong to which brokers.
     */
    public class Updater implements Runnable {

        ServerSocket channelProviderSocket;

        Updater() {
            try {
                channelProviderSocket = new ServerSocket(PORT);
            } catch (IOException e) {
                System.err.println("Error while trying open server socket.");
            }
        }

        @Override
        public void run() {
            Socket connection;
            ObjectInputStream in;
            ObjectOutputStream out;
            try {
                connection = channelProviderSocket.accept();
                in = new ObjectInputStream(connection.getInputStream());
                out = new ObjectOutputStream(connection.getOutputStream());
                int cmd = (int) in.readUnshared();
                if (cmd == 0) {
                    brokers = (List<Broker>) in.readUnshared();
                } else if (cmd == 1) {
                    topicList = new ArrayList<>(dataHashtable.keySet());
                    out.writeUnshared(thisBroker());
                } else if (cmd == 2) {
                    brokers = (List<Broker>) in.readUnshared();
                    brokerID = (int) in.readUnshared();
                }
            } catch (IOException e) {
                System.err.println("Could not send broker object.");
            } catch (ClassNotFoundException e) {
                System.err.println("Could not receive brokers list.");
            }
            Thread updaterTr = new Thread(this);
            updaterTr.start();
        }

    }
    private Broker thisBroker() {
        return this;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BrokerImpl broker = (BrokerImpl) o;
        return brokerID == broker.brokerID;
    }
    /**
     * This is the main method that asks the user how many brokers he wants
     * and uses a loop to create that number of brokers, initializing them with
     * an ID which corresponds to the size of the brokers list.
     *
     * @param args Unused
     */
    public static void main(String[] args) {
        BrokerImpl broker;
        broker = new BrokerImpl();
        broker.init(0);
    }
}