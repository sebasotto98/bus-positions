/**
 * This program is a server application that receives data from a Publisher and
 * sends requested values from that data to the Subscriber
 *
 * @author  Albernaz de Sotto Mayor Sebastiao Cristo, Konstantakos Michail
 * @since   14/04/2019
 */

package ds.android.ds_project.Interfaces;

import ds.android.ds_project.Classes.Topic;
import ds.android.ds_project.Classes.Value;

import java.net.InetAddress;
import java.util.List;

public interface Broker {

    /**
     * This method returns the topic set of a Broker.
     */
    List<Topic> getTopicList();

    /**
     * This method returns the IP of the Broker.
     */
    InetAddress getIP();

    /**
     * This method returns the port of the Publisher
     * connection.
     */
    int getPubPort();

    /**
     * This method returns the port of the Subscriber
     * connection.
     */
    int getSubPort();

    /**
     * This method returns the port of the Channel
     * connection.
     */
    int getChannelPort();

    /**
     * This method calculates the hashes of the IP + Port
     * of a Broker and the bus line ID.
     */
    void CalculateKeys();

    /**
     * This method distributes the topics to the brokers
     * according to the hash of the IP + Port of each Broker
     * and the hash of the bus line ID and then stores the
     * topic with it's corresponding value in the hashTable
     * of the Broker.
     *
     * @param top This is the topic to be stored.
     */
    Broker hashTopic(Topic top, Value val);

    /**
     * This method sends the value of a requested topic
     * to the connected Broker.
     *
     * @param top This is the requested topic.
     */
    void pull(Topic top);

    /**
     * This method receives all the topics with their
     * corresponding values and passes the topic as an
     * argument to the hashTopic method.
     *
     * @param pub This is the connected Publisher
     */
    Publisher acceptConnection(Publisher pub);

    /**
     * This method receives a topic from the Subscriber
     * and passes it as an argument to the pull method.
     *
     * @param sub This is the connected Subscriber
     */
    Subscriber acceptConnection(Subscriber sub);

    boolean equals(Object o);

}