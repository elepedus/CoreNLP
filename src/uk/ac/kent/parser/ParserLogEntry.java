package uk.ac.kent.parser;

import edu.stanford.nlp.parser.nndep.Configuration;
import edu.stanford.nlp.parser.nndep.DependencyTree;

import java.util.Arrays;
import java.util.List;

/**
 * Created by elepedus on 10/02/2016.
 */
public class ParserLogEntry {
    public List<Integer> features;
    public String transition;
    public String[] stackWords;
    public String[] stackPOS;
    public String[] bufferWords;
    public String[] bufferPOS;
    public List<String> arcs;
    public List<String> partOfSpeechArcs;

    public ParserLogEntry() {

    }

    public ParserLogEntry(Configuration configuration, List<Integer> features, String transition) {
        stackWords = configuration.getStackWords();
        stackPOS = configuration.getStackPOSTags();
        bufferWords = configuration.getBufferWords();
        bufferPOS = configuration.getBufferPOSTags();
        arcs = configuration.getDependencyArcs();
        partOfSpeechArcs = configuration.getPOSDependencyArcs();
        this.features = features;
        this.transition = transition;
    }

    public String[] getTopTwoPOS() {

        String[] posArray = new String[2];
        posArray[0] = stackPOS[stackPOS.length - 2];
        posArray[1] = stackPOS[stackPOS.length - 1];
        return posArray;
    }
}
