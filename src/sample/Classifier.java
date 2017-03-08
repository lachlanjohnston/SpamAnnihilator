package sample;

import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Lachlan Johnston
 *
 * This class implements a naive bayesian classifier, using the bag of words model
 * to classify emails as either legitimate (ham), or spam emails.
 */
public class Classifier implements Runnable {

    interface ClassifierListener {
        void onCompleted();
        void print(String text);
    }

    static private ObservableList<Email> emails = FXCollections.observableArrayList();
    private ClassifierListener listener = null;
    private File dir = null;

    Classifier(File dir) {
        this.dir = dir;
    }

    public ObservableList<Email> getEmails() {
        return emails;
    }

    public File getDir() {
        return dir;
    }

    void setListener(ClassifierListener listener) {
        this.listener = listener;
    }

    @Override
    public void run() {
        classify();
    }

    /**
     * This method preforms the classification of emails.
     */
    private void classify() {
        try {
            listener.print("Starting Classifier!\n");

            List<String> docMap = new ArrayList<String>();
            HashMap<String, Float> globalHamMap = new HashMap<String, Float>();
            HashMap<String, Float> globalSpamMap = new HashMap<String, Float>();

            List<File> filesHam = Files.walk(Paths.get(dir + "/train/ham"))
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .collect(Collectors.toList());

            List<File> filesSpam = Files.walk(Paths.get(dir + "/train/spam"))
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .collect(Collectors.toList());

            int hamFiles = filesHam.size();
            int spamFiles = filesSpam.size();

            for (File file : filesHam) {
                Scanner s = new Scanner(file);

                while (s.hasNext()) {
                    String word = s.next();
                    word.toLowerCase();

                    if (!docMap.contains(word)) {
                        docMap.add(word);
                    }
                }

                for (String word : docMap) {
                    if (globalHamMap.containsKey(word)) {
                        Float val = globalHamMap.get(word);
                        globalHamMap.put(word, val+1f);
                    } else {
                        globalHamMap.put(word, 1f);
                    }
                }

                docMap.clear();
            }

            listener.print("Ham training files loaded: " + hamFiles
                            + "\nSpam files loaded: " + spamFiles
                            + "\nBeginning classification!\n");

            for (File file : filesSpam) {
                Scanner s = new Scanner(file);

                while (s.hasNext()) {
                    String word = s.next();
                    word.toLowerCase();

                    if (!docMap.contains(word)) {
                        docMap.add(word);
                    }
                }

                for (String word : docMap) {
                    if (globalSpamMap.containsKey(word)) {
                        Float val = globalSpamMap.get(word);
                        globalSpamMap.put(word, val+1f);
                    } else {
                        globalSpamMap.put(word, 1f);
                    }
                }

                docMap.clear();
            }

            HashMap<String, Float> probs = new HashMap<String, Float>();
            Set<Map.Entry<String, Float>> ham = globalHamMap.entrySet();

            for (Map.Entry<String, Float> e : ham) {
                float probwh = e.getValue() / hamFiles;
                float probws = 0;
                if (globalSpamMap.containsKey(e.getKey()))
                    probws = globalSpamMap.get(e.getKey()) / spamFiles;

                float probspam = probws / (probws + probwh);
                probs.put(e.getKey(), probspam);
            }

            Set<Map.Entry<String, Float>> spam = globalSpamMap.entrySet();

            for (Map.Entry<String, Float> e : spam) {
                if (!probs.containsKey(e.getKey())) {
                    float probws = e.getValue() / spamFiles;
                    float probwh = 0;

                    float probspam = probws / (probws + probwh);
                    probs.put(e.getKey(), Math.abs(probspam));
                }
            }

            Set<Map.Entry<String, Float>> stuff = probs.entrySet();

            // for (Map.Entry<String, Float> e : stuff)
            // 	System.out.println(e.getKey() + " " + e.getValue());

            globalSpamMap.clear();
            globalHamMap.clear();

            listener.print("Classifying spam!\n");

            List<File> filesTest = Files.walk(Paths.get(dir + "/test/spam"))
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .collect(Collectors.toList());

            int isSpam = 0;
            int isHam = 0;
            int totalFiles = filesTest.size();

            for (File file : filesTest) {
                Scanner s = new Scanner(file);
                double eta = 0f;

                while(s.hasNext()){
                    String word = s.next();
                    word.toLowerCase();

                    if(probs.containsKey(word)) {
                        double prob = (double) probs.get(word);
                        if (prob > 0 && prob < 1)
                            eta += Math.log(1d - prob) - Math.log(prob);
                    }
                }

                double probSpam = 1d / (1d + Math.pow(Math.E, eta));
                emails.add(new Email(file.getName(), probSpam));
                // System.out.println(file.getName() + " " + probSpam);
                if (probSpam < .5) isHam++;
                else isSpam++;
            }

            double percentageCorrect = (double) isSpam / filesTest.size();
            listener.print("Correct: " + isSpam + "\nIncorrect: " +
                    isHam + "\nPercentage: " + percentageCorrect + "\n");

            listener.print("Classifying ham!\n");

            filesTest = Files.walk(Paths.get(dir + "/test/ham"))
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .collect(Collectors.toList());

            int correct = isSpam;
            isSpam = 0;
            isHam = 0;
            totalFiles += filesTest.size();

            for (File file : filesTest) {
                Scanner s = new Scanner(file);
                double eta = 0f;

                while(s.hasNext()){
                    String word = s.next();
                    word.toLowerCase();

                    if(probs.containsKey(word)) {
                        double prob = (double) probs.get(word);
                        if (prob > 0 && prob < 1)
                            eta += Math.log(1d - prob) - Math.log(prob);
                    }
                }

                double probSpam = 1d / (1d + Math.pow(Math.E, eta));
                emails.add(new Email(file.getName(), probSpam));
                // System.out.println(file.getName() + " " + probSpam);
                if (probSpam < .5) isHam++;
                else isSpam++;
            }

            correct += isHam;

            percentageCorrect = (double) isHam / filesTest.size();
            listener.print("Correct: " + isHam + "\nIncorrect: " +
                    isSpam + "\nPercentage: " + percentageCorrect
                    + "\nTotal percentage classified correctly: " + (double) correct/(totalFiles));



            listener.onCompleted();

        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
