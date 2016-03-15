package uk.ac.kent;

import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.util.Heap;
import org.yaml.snakeyaml.Yaml;
import sun.awt.image.ImageWatched;
import uk.ac.kent.parser.ParserLogEntry;

import javax.swing.plaf.synth.SynthTextAreaUI;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by elepedus on 18/02/2016.
 */
@SuppressWarnings("Duplicates")
public class ParserLogAnalyser {
    private LinkedList<ParserLogEntry> logEntries;
    private Yaml yaml;
    private LinkedList<Bigram> bigrams;
    private int threshold;

    public ParserLogAnalyser() {
        logEntries = new LinkedList<ParserLogEntry>();
        yaml = new Yaml();
    }

    public static void main(String[] args) {
        String inputPath = "training/experiment2/parseLog.yaml";
        ParserLogAnalyser analyser = new ParserLogAnalyser();
        analyser.loadLogEntries(inputPath);
        analyser.bigrams = analyser.extractPOSBigramFrequencies();
        analyser.threshold = analyser.bigrams.stream().mapToInt(x -> x.frequency).sum() / 100000;
        writePOSBigramHistogram(analyser.bigrams, "training/experiment2/POSBigramHistogram.txt");
        analyser.modifyParseDecisions();
        analyser.saveExamplesToFile("training/experiment2/trainingExamples.yaml");

    }

    private void modifyParseDecisions() {
        for (ParserLogEntry entry : logEntries) {
            modifyParseDecision(entry);
        }
    }

    private void modifyParseDecision(ParserLogEntry entry) {
        if (entry.stackPOS.length > 1) {
            String[] partsOfSpeech = entry.getTopTwoPOS();
            String first = partsOfSpeech[0];
            String second = partsOfSpeech[1];
            System.out.print(entry.transition + " -> ");
            entry.transition = getArc(first, second);
            System.out.println(entry.transition);
        }
    }

    private String getArc(String posA, String posB) {
        int rFrequency = getArcFrequency(posA, posB);
        int lFrequency = getArcFrequency(posB, posA);
        int sFrequency = bigrams.stream().filter(x -> x.frequency <  threshold).mapToInt(x -> x.frequency).sum();

        if (sFrequency > Integer.max(rFrequency, lFrequency)) {
            return "S";
        } else if (rFrequency > lFrequency) {
            return "R(PARSED)";
        } else {
            return "L(PARSED)";
        }
    }

    private TreeMap<String, Float> getArcProbabilities(String posA, String posB) {
        int rFrequency = getArcFrequency(posA, posB);
        int lFrequency = getArcFrequency(posB, posA);
        float total = rFrequency + lFrequency;
        TreeMap<String, Float> probabilities = new TreeMap<>();
        probabilities.put("R(PARSED)", rFrequency / total);
        probabilities.put("L(PARSED)", lFrequency / total);
        return probabilities;
    }

    private int getArcFrequency(String from, String to) {
        Optional result = bigrams.stream()
                .filter(x -> x.exactMatch(from, to))
                .findFirst();
        if (result.isPresent()) {
            Bigram b = (Bigram) result.get();
            return b.getFrequency();
        } else return 0;
    }


    public void saveExamplesToFile(String outputPath) {
        try {
            PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(outputPath, true)));
            for (ParserLogEntry entry : logEntries) {
                writer.println("---");
                yaml.dump(entry, writer);
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        String corpus = IOUtils.slurpFileNoExceptions("POSTaggedSherlock");
        HashMap<String, Bigram> bigrams = CorpusAnalyser.getStringBigramHashMap(corpus);
        for (ParserLogEntry logEntry : logEntries) {
            for (int i = 0; i < logEntry.stackPOS.length - 1; i++) {
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
