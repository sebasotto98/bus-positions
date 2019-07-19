package gr.bus_positions.Interfaces;
import gr.bus_positions.Topic;
import java.net.InetAddress;
public interface Subscriber {
    /**
     * This method returns the IP of the Publisher.
     */
    InetAddress getIP();

    /**
     * This method returns the port of the Publisher.
     */
    int getPort();

    /**
     * This method registers the broker that is responsible for the desired topic,
     * connects to it and sends it the desired topic.
     *
     * @param bro This is the broker that has the desired topic.
     * @param top This is the desired topic.
     */
    void register(Broker bro, Topic top);

    /**
     * This method unregisters the broker that had the desired topic
     * when the value is received and disconnect from it.
     *
     * @param bro This is the broker that sent the desired topic.
     * @param top This is the received topic.
     */
    void disconnect(Broker bro, Topic top);

    /**
     * This method returns the value of the requested topic.
     *
     * @param request This is the topic the user is interested in.
     */
    void getValue(String request);
}