package gr.bus_positions.Implementations;

import java.io.Serializable;
import java.net.InetAddress;

import gr.bus_positions.Classes.Topic;
import gr.bus_positions.Interfaces.Broker;
import gr.bus_positions.Interfaces.Subscriber;

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