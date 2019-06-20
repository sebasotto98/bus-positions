package ds.android.ds_project.Implementations;

import ds.android.ds_project.Classes.Bus;
import ds.android.ds_project.Classes.Topic;
import ds.android.ds_project.Classes.Value;
import ds.android.ds_project.Interfaces.Broker;
import ds.android.ds_project.Interfaces.Node;
import ds.android.ds_project.Interfaces.Publisher;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import static ds.android.ds_project.Channel.CHANNEL_IP;
import static ds.android.ds_project.Channel.CHANNEL_PORT;
import static ds.android.ds_project.Implementations.BrokerImpl.SLEEP_TIME;

public class PublisherImpl implements Publisher, Node, Runnable, Serializable, Cloneable {

    public static final long serialVersionUID = -6209161616299602640L;
    private static final String PUBLISHER_IP = "192.168.1.4";
    private int PORT = 8765;

    private int pubCount;
    private int pubID;
    private InetAddress IP;
    private int port;
    private boolean connection = true;
    private boolean brokerDisconnected = false;
    private transient Socket requestSocket = null;
    private transient ObjectOutputStream out = null;
    private transient ObjectInputStream in = null;
    private List<Broker> brokers = null;
    private List<ArrayList<String>> busLineData = new ArrayList<>();
    private List<List<String>> busPositionsData = new ArrayList<>();
    private List<List<String>> routeCodesData = new ArrayList<>();


    @Override
    public InetAddress getIP() {
        try {
            return InetAddress.getByName(PUBLISHER_IP);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    @Override
    public int getPort() {
        return PORT;
    }

    public void getBrokerList() {
        try {
            setBrokers((List<Broker>) in.readUnshared());
        } catch (IOException e) {
            System.err.println("Error when attempting to send request for Broker List");
        } catch (ClassNotFoundException e) {
            System.err.println("Error when attempting to get Broker List.");
        }
    }

    public void push(Topic top, Value val) {
        try {
            out.writeObject(top);
            out.writeObject(val);
            out.flush();
            out.reset();
            System.out.println("Push successful.");
        } catch (IOException e) {
            System.err.println("Broker disconnected.");
            brokerDisconnected = true;
        }
    }

//    public void notifyFailure(Broker bro) {
//
//    }

    public void readData(String path) {
        int counter = 0;
        File f = null;
        BufferedReader br = null;
        String line;

        try {
            f = new File(path);
        } catch (NullPointerException e) {
            System.err.println("File not found.");
        }

        try {
            br = new BufferedReader(new FileReader(f));
        } catch (FileNotFoundException e) {
            System.err.println("Error opening file!");
        }

        try {
            while ((line = br.readLine()) != null) {
                if (path.contains("busLines")) {
                    busLineData.add(new ArrayList<String>());
                } else if (path.contains("busPositions")) {
                    busPositionsData.add(new ArrayList<String>());
                } else {
                    routeCodesData.add(new ArrayList<String>());
                }
                String[] tokens = line.split(",");
                for (String str : tokens) {
                    if (path.contains("busLines")) {
                        busLineData.get(counter).add(str);
                    } else if (path.contains("busPositions")) {
                        busPositionsData.get(counter).add(str);
                    } else {
                        routeCodesData.get(counter).add(str);
                    }

                }
                counter++;
            }
        } catch (IOException e) {
            System.err.println("Error reading line " + counter + 1 + ".");
        }

        try {
            br.close();
        } catch (IOException e) {
            System.err.println("Error closing file.");
        }
    }

    public void init(int port) {
        try {
            IP = InetAddress.getByName(CHANNEL_IP);
            this.port = port;
            connect();
            out.writeUnshared(this);
            out.flush();
            out.reset();
            pubCount = (int)in.readUnshared();
            getBrokerList();
        } catch (UnknownHostException e) {
            System.err.println("Could not connect to channel.");
        } catch (IOException e) {
            System.err.println("Could not send publiher's object.");
        } catch (ClassNotFoundException e) {
            System.err.println("Could not receive publisher number.");
        }
        pubID = pubCount;
        updateNodes();
        if (brokers.size() > 0) {
            IP = brokers.get(0).getIP();
        } else {
            connection = false;
            System.out.println("No brokers connected on the channel.");
            return;
        }
        Updater updater = new Updater();
        Thread updaterTr = new Thread(updater);
        updaterTr.start();
        this.port = brokers.get(0).getPubPort();
        connect();
        for (int i = 1; i < brokers.size(); i++) {
            PublisherImpl copyPub = null;
            try {
                copyPub = (PublisherImpl) this.clone();
            } catch (CloneNotSupportedException e) {
                System.err.println("Error while trying to make a copy of this Publisher.");
            }
            copyPub.IP = brokers.get(i).getIP();
            copyPub.port = brokers.get(i).getPubPort();
            copyPub.connect();
            Thread pubTr = new Thread(copyPub);
            pubTr.start();
        }
    }

    public void connect() {
        try {
            requestSocket = new Socket(IP, port);
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            in = new ObjectInputStream(requestSocket.getInputStream());
        } catch (UnknownHostException unknownHost) {
            System.err.println("You are trying to connect to an unknown host!");
        } catch (IOException ioException) {
            System.err.println("Can not connect to host!");
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
        readData("DS_Project/Dataset/busLines.txt");
        readData("DS_Project/Dataset/busPositions.txt");
        readData("DS_Project/Dataset/RouteCodes.txt");
    }

    public List<Broker> getBrokers() {
        return brokers;
    }

    public void setBrokers(List<Broker> brokers) {
        this.brokers = brokers;
    }

    /**
     * When the thread starts, it sends an object of itself to the broker.
     *
     * Then it calculates the last iteration of the 2nd loop as follows:
     * -It splits the the number of bus lines by the number of Publishers we have.
     * (e.g. If we have 20 bus lines and 3 publishers, the output of 20/3 is 6)
     * -Then it multiplies the output by the ID of the Publisher that belongs to this thread.
     * (The ID starts from 1, so continuing the above example, the 1st Publisher's last
     * iteration will be 6 and the 2nd will be 12. The 3rd is explained bellow)
     * -If the Publisher is the last available Publisher (ID equals the total number of Publishers),
     * and also if the above division was not absolute, then the last iteration will be the number of
     * total bus lines we have.
     * (So the 3rd Publisher instead of having a last iteration of 18 will have 20)
     *
     * After this is done, a loop begins from the start of the bus positions data to the end.
     * It saves the line's number, the route's code and the vehicle's ID.
     * The 2nd loop starts beginning from the above equation BUT instead of multiplying by the ID
     * of the Publisher like it did in the 2nd step above, it multiplies it by the ID - 1.
     * This means that the 1st Publisher will always start from 0, the 2nd one will start from where
     * the 1st one finished (because ID - 1 will be 2 - 1 so the output is the same as the last iteration
     * of the 1st Publisher) and the 2rd one will start from where the 2nd one finished.
     *
     * Inside the 2nd loop it checks if the position's line number matches the ID of this Publisher's specific
     * iterations. If it does, then it stores the line's ID and the line's name. Otherwise, this Publisher is
     * not responsible for this line number and it moves to the next bus position.
     *
     * The 3rd loop iterates through the data of the route's code until the route code matches the position's one
     * and it stores the route's type and the route's info.
     *
     * Lastly it creates a Bus object with all of the stored information,
     * a Value object with the bus, the latitude, the longitude and the timestamp of this position
     * and a Topic with the bus line's ID.
     * It then pushes the objects to the connected broker.
     *
     * The sleep is used to emulate that a Publisher is a sensor, otherwise it would end in seconds.
     */
    public void run() {
        if (!connection) {
            return;
        }
        try {
            out.writeUnshared(this);
            out.flush();
            out.reset();
        } catch (IOException e) {
            System.err.println("Error while sending Publisher object.");
        }
        int last = (busLineData.size() / pubCount) * pubID;
        if ((pubID == pubCount) && (busLineData.size() % pubCount != 0)) {
            last = busLineData.size();
        }
        outerloop:
        for (int i = 0; i < busPositionsData.size(); i++) {
            String lineNumber;
            String routeCode;
            String vehicleID;
            String lineName = null;
            String busLineID = null;
            String info = null;
            String routeType = null;
            lineNumber = busPositionsData.get(i).get(0);
            routeCode = busPositionsData.get(i).get(1);
            vehicleID = busPositionsData.get(i).get(2);
            for (int j = (busLineData.size() / pubCount) * (pubID - 1); j < last; j++) {
                if (lineNumber.compareTo(busLineData.get(j).get(0)) == 0) {
                    busLineID = busLineData.get(j).get(1);
                    lineName = busLineData.get(j).get(2);
                    break;
                } else if (j == last - 1) {
                    continue outerloop;
                }
            }
            for (int j = 0; j < routeCodesData.size(); j++) {
                if (routeCode.compareTo(routeCodesData.get(j).get(0)) == 0) {
                    routeType = routeCodesData.get(j).get(2);
                    info = routeCodesData.get(j).get(3);
                    break;
                }
            }
            Bus newBus = new Bus(lineNumber, routeCode, vehicleID, lineName, busLineID, info, routeType);
            Value newValue = new Value(newBus, Double.parseDouble(busPositionsData.get(i).get(3)), Double.parseDouble(busPositionsData.get(i).get(4)), busPositionsData.get(i).get(5));
            Topic newTopic = new Topic(busLineID + "," + routeType);
            push(newTopic, newValue);
            if (brokerDisconnected) {
                break;
            }
            try {
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException e) {
                System.out.println("Thread was interrupted.");
                break;
            }
        }
    }

    /**
     * This class is used in a Thread to update the publisher every time
     * a new publisher connects to the channel.
     */
    public class Updater implements Runnable {

        ServerSocket channelProviderSocket;

        Updater() {
            try {
                channelProviderSocket = new ServerSocket(PORT);
            } catch (IOException e) {
                System.err.println("Error while trying to open server socket.");
            }
        }

        @Override
        public void run() {
            Socket connection;
            ObjectInputStream in;
            try {
                connection = channelProviderSocket.accept();
                in = new ObjectInputStream(connection.getInputStream());
                pubCount = (int)in.readUnshared();
            } catch (IOException e) {
                System.err.println("Could not connect with channel.");
            } catch (ClassNotFoundException e) {
                System.err.println("Could not receive publisher number.");
            }
            Thread updaterTr = new Thread(this);
            updaterTr.start();
        }

    }

    /**
     * This is the main method that asks the user how many publishers he wants
     * and uses a loop to create that number of publishers, initializing them with
     * the port 4321 which corresponds to the Broker with ID 1.
     *
     * @param args Unused
     */
    public static void main(String[] args) {
        PublisherImpl pub = new PublisherImpl();
        pub.init(CHANNEL_PORT);
        Thread pubTr = new Thread(pub);
        pubTr.start();
    }
}