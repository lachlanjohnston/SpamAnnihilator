package sample;

/**
 * Created by lachlan on 06/03/17.
 */
public class Email {
    public String name;
    public double probability;

    public String getName() {
        return name;
    }

    public double getProbability() {
        return probability;
    }

    public Email(String name, double probability) {
        this.name = name;
        this.probability = probability;
    }
}
