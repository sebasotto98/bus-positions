package gr.bus_positions.Interfaces;
import java.util.List;
public interface Node {
    /**
     * This method initializes a node.
     *
     * @param c This is the port number for Publisher and Subscriber
     *          and the ID for Broker.
     */
    void init(int c);

    /**
     * This method is used to connect to communicate with the Channel
     * and get any necessary information.
     */
    void connect();

    /**
     * This method is used to disconnect from a Broker for Publisher and Subscriber
     * and to close the sockets for Broker.
     */
    void disconnect();

    /**
     * This method helps update node's values.
     */
    void updateNodes();

    /**
     * This is a property of the brokers list that returns it.
     *
     * @return Returns the list.
     */
    List<Broker> getBrokers();

    /**
     * This is a property of the brokers list that changes its value.
     *
     * @param brokers This is the desired value of the list.
     */
    void setBrokers(List<Broker> brokers);
}