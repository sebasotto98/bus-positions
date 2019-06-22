package gr.bus_positions.Interfaces;
import gr.bus_positions.Classes.Topic;
import gr.bus_positions.Classes.Value;
import java.net.InetAddress;
/**
 * This program is a client application that reads a dataset with
 * bus information and sends it using push to Broker.
 *
 * @author  Albernaz de Sotto Mayor Sebastiao Cristo, Konstantakos Michail
 * @since   14/04/2019
 */
public interface Publisher {
    /**
     * This method returns the IP of the Publisher.
     */
    InetAddress getIP();

    /**
     * This method returns the port of the Publisher.
     */
    int getPort();

    /**
     * This method receives a list with all the brokers
     * and saves it in a static data structure.
     */
    void getBrokerList();

    /**
     * This method sends a topic with its respective
     * value to the connected Broker.
     *
     * @param top This is the topic to be sent.
     * @param val This is the topic's respective value.
     */
    void push(Topic top, Value val);

    /**
     * This method reads the given dataset and stores it
     * in a corresponding data structure.
     *
     * @param path This is the path where the dataset is located.
     */
    void readData(String path);
}