package uk.ac.kent;

import edu.stanford.nlp.parser.nndep.Dataset;
import edu.stanford.nlp.scoref.Example;
import org.yaml.snakeyaml.Yaml;
import uk.ac.kent.parser.ParserLogEntry;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by elepedus on 10/02/2016.
 * Takes a parser log and extracts training examples from it
 */
public class ExampleExtractor {
    private List<ParserLogEntry> logEntries;
    private Yaml yaml;
    private Dataset dataset;
    private static List<String> transitions = Arrays.asList("L(ROOT)", "L(NMOD)", "L(DEP)", "L(P)", "L(ADV)", "L(PMOD)", "L(OBJ)", "L(COORD)", "L(VC)", "L(PRD)",
            "L(CONJ)", "L(AMOD)", "L(IM)", "L(OPRD)", "L(NAME)", "L(SUB)", "L(APPO)", "L(PRT)", "L(SUFFIX)", "L(PRN)",
            "L(TITLE)", "R(ROOT)", "R(NMOD)", "R(DEP)", "R(P)", "R(ADV)", "R(PMOD)", "R(OBJ)", "R(COORD)", "R(VC)",
            "R(PRD)", "R(CONJ)", "R(AMOD)", "R(IM)", "R(OPRD)", "R(NAME)", "R(SUB)", "R(APPO)", "R(PRT)", "R(SUFFIX)",
            "R(PRN)", "R(TITLE)", "S");

    public ExampleExtractor() {
        this(new LinkedList<>());
    }

    public ExampleExtractor(LinkedList<ParserLogEntry> logEntries) {
        this.logEntries = logEntries;
        yaml = new Yaml();
        dataset = new Dataset(48, 43);
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        String inputPath = "parseLog.yaml";
        ExampleExtractor extractor = new ExampleExtractor();
        extractor.loadLogEntries(inputPath);
        extractor.extractExamples();
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

    public void extractExamples() {
        for (ParserLogEntry entry : logEntries) {
            dataset.addExample(entry.features, getLabel(entry));
        }
    }

    private List<Integer> getLabel(ParserLogEntry entry) {
        List<Integer> label = new ArrayList<>();
        for (String transition : transitions) {
            label.add(getLabelScore(transition, entry));
        }
        return label;
    }

    private Integer getLabelScore(String candidateTransition, ParserLogEntry entry) {
        if (candidateTransition.equals(entry.transition)) {
            return 1;
        } else if (canApply(candidateTransition, entry)) {
            return 0;
        } else {
            return -1;
        }
    }

    private boolean canApply(String candidateTransition, ParserLogEntry entry) {
        int stackSize = entry.stackWords.length;
        int bufferSize = entry.bufferWords.length;
        // Can only apply a SHIFT transition when the buffer is not empty
        if (candidateTransition.equals("S")) {
            return entry.bufferWords.length > 0;
        }

        // From here on we are only dealing with L/R transitions
        if (candidateTransition.startsWith("L")) {
            // Dependent is not ROOT
            return stackSize > 2;
        } else {
            if (stackSize == 2 && bufferSize == 0 && candidateTransition.equals("R(ROOT)")) {
                return true;
            }
            return stackSize >= 2;
        }
    }

    public Dataset getDataset() {
        return dataset;
    }
}
