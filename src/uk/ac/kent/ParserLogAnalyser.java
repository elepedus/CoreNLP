package uk.ac.kent;

import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.util.Heap;
import edu.stanford.nlp.util.StringUtils;
import org.yaml.snakeyaml.Yaml;
import sun.awt.image.ImageWatched;
import uk.ac.kent.parser.ParserLogEntry;

import javax.swing.plaf.synth.SynthTextAreaUI;
import java.io.*;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by elepedus on 18/02/2016.
 */
@SuppressWarnings("Duplicates")
public class ParserLogAnalyser {
    LinkedList<ParserLogEntry> logEntries;
    private Yaml yaml;
    LinkedList<Bigram> bigrams;
    private int threshold;

    /**
     * Explicitly specifies the number of arguments expected with
     * particular command line options.
     */
    protected static final Map<String, Integer> numArgs = new HashMap<>();

    static {
        numArgs.put("path", 1);
    }

    public ParserLogAnalyser() {
        logEntries = new LinkedList<>();
        yaml = new Yaml();
    }

    public static void main(String[] args) {
        Properties props = StringUtils.argsToProperties(args, numArgs);
        String path = props.getProperty("path");
        String inputPath = path + "/parseLog.yaml";
        ParserLogAnalyser analyser = new ParserLogAnalyser();
        analyser.loadLogEntries(inputPath);
        analyser.bigrams = analyser.extractPOSBigramFrequencies(path + "/posPatterns.txt");
        analyser.threshold = analyser.bigrams.stream().mapToInt(x -> x.frequency).sum() / 100000;
        writePOSBigramHistogram(analyser.bigrams, path + "/POSBigramHistogram.txt");
        analyser.modifyParseDecisions();
        analyser.saveExamplesToFile(path + "/trainingExamples.yaml");

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
        int lFrequency = getArcFrequency(posA, posB);
        int rFrequency = getArcFrequency(posB, posA);
        int sFrequency = bigrams.stream().filter(x -> x.frequency < threshold).mapToInt(x -> x.frequency).sum();

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

    public Bigram getTopBigram() {
        Optional<Bigram> max = bigrams.stream()
                .filter(x -> !x.getFirst().equals("-ROOT-")) // we're only interested in bigrams involving non-root POS
                .max(Comparator.comparing(x -> x.frequency));
        if (max.isPresent()) {
            return max.get();
        }
        return null;
    }

    public LinkedList<ParserLogEntry> getMatchingEntries(Bigram bigram) {
        return logEntries.stream().filter(x -> {
            if (x.stackPOS.length > 1) {
                String[] pos = x.getTopTwoPOS();
                return bigram.exactMatch(pos[0], pos[1]) && !x.transition.contains("PARSED");
            } else return false;
        }).collect(Collectors.toCollection(LinkedList::new));
    }

    public void updateTransitions(LinkedList<ParserLogEntry> entries, String newTransition) {
        entries.parallelStream().forEach(x ->{
            x.transition = newTransition;
        });
    }


    public LinkedList<Bigram> extractPOSBigramFrequencies() {
        return extractPOSBigramFrequencies(null);
    }

    public LinkedList<Bigram> extractPOSBigramFrequencies(String posTaggedCorpus) {
        HashMap<String, Bigram> bigrams;
        if (posTaggedCorpus != null) {
            String corpus = IOUtils.slurpFileNoExceptions(posTaggedCorpus);
            bigrams = CorpusAnalyser.getStringBigramHashMap(corpus);
        } else {
            bigrams = new HashMap<>();
        }
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
