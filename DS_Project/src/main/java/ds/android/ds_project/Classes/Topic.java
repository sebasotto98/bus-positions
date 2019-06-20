package ds.android.ds_project.Classes;

import java.io.Serializable;

/**
 * The Topic class is used to store the line's ID
 *
 * @author  Albernaz de Sotto Mayor Sebastiao Cristo, Konstantakos Michail
 * @since   14/04/2019
 */

public class Topic implements Serializable {

    public static final long serialVersionUID= 7544965582174783715L;
    private String topic;

    public Topic(String topic) {
        this.topic = topic;
    }

    public String getTopic() {
        return topic;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else  if (!(o instanceof Topic)) {
            return false;
        }
        Topic top = (Topic) o;
        return topic.compareTo(top.getTopic()) == 0;
    }

}
