package gr.bus_positions.Implementations;
import java.io.Serializable;
import java.net.InetAddress;
import gr.bus_positions.Classes.Topic;
import gr.bus_positions.Interfaces.Broker;
import gr.bus_positions.Interfaces.Subscriber;
/**
 * This class is a client application that asks the user which bus line he is
 * interested in and then communicates with the brokers to find the topic and
 * return its value to the user.
 *
 * @author  Albernaz de Sotto Mayor Sebastiao Cristo, Konstantakos Michail
 * @since   14/04/2019
 */
public class SubscriberImpl implements Subscriber, Serializable {
    public static final long serialVersionUID = 931651160190181219L;
    private InetAddress IP;
    private int port;

    public SubscriberImpl(InetAddress IP, int port) {
        this.IP = IP;
        this.port = port;
    }

    @Override
    public InetAddress getIP() {
        return IP;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public void register(Broker bro, Topic top) {
    }

    @Override
    public void disconnect(Broker bro, Topic top) {
    }

    @Override
    public void getValue(String request) {
    }
}