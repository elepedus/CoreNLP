package uk.ac.kent;

import edu.stanford.nlp.io.IOUtils;
import org.yaml.snakeyaml.Yaml;
import sun.awt.image.ImageWatched;
import uk.ac.kent.parser.ParserLogEntry;

import java.io.*;
import java.util.*;

/**
 * Created by elepedus on 18/02/2016.
 */
public class ParserLogAnalyser {
    private List<ParserLogEntry> logEntries;
    private Yaml yaml;

    public ParserLogAnalyser() {
        logEntries = new LinkedList<ParserLogEntry>();
        yaml = new Yaml();
    }

    public static void main(String[] args) {
        String inputPath = "parseLog.yaml";
        ParserLogAnalyser analyser = new ParserLogAnalyser();
        analyser.loadLogEntries(inputPath);
        LinkedList<Bigram> bigrams = analyser.extractPOSBigramFrequencies();
        analyser.writePOSBigramHistogram(bigrams, "POSBigramHistogram.txt");

    }

    public void loadLogEntries(String inputPath) {
        try {
            Reader reader = new FileReader(inputPath);
            for (Object o : yaml.loadAll(reader)) {
                ParserLogEntry entry = (ParserLogEntry) o;
                logEntries.add(entry);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public LinkedList<Bigram> extractPOSBigramFrequencies() {
        HashMap<String, Bigram> bigrams = new HashMap<>();
        for (ParserLogEntry logEntry : logEntries) {
            for (int i = 0; i < logEntry.stackPOS.length - 2; i++) {
                String firstTag = logEntry.stackPOS[i];
                String secondTag = logEntry.stackPOS[i + 1];
                String hashKey = firstTag + secondTag;
                if (bigrams.containsKey(hashKey)) {
                    bigrams.get(hashKey).incrementfrequency();
                } else {
                    bigrams.put(hashKey, new Bigram(firstTag, secondTag));
                }
            }
        }
        return new LinkedList<>(bigrams.values());
    }

    public static void writePOSBigramHistogram(LinkedList<Bigram> bigrams, String outputPath) {
        LinkedList<String> lines = new LinkedList<>();
        bigrams.sort(null);
        for (Bigram bigram : bigrams) {
            lines.push(bigram.getFrequency() + " " + bigram.getFirst() + " " + bigram.getSecond());
        }
        writeLineCollectionToFile(lines, outputPath);
    }

    public static void writeLineCollectionToFile(Collection<String> lines, String outputPath) {
        try {
            PrintWriter writer = IOUtils.getPrintWriter(outputPath);
            writer.print(String.join("\n", lines));
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
