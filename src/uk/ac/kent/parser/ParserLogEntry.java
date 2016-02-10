package uk.ac.kent.parser;

import edu.stanford.nlp.parser.nndep.Configuration;
import edu.stanford.nlp.parser.nndep.DependencyTree;

import java.util.List;

/**
 * Created by elepedus on 10/02/2016.
 */
public class ParserLogEntry {
    public int[] features;
    public String transition;
    public String[] stackWords;
    public String[] stackPOS;
    public String[] bufferWords;
    public String[] bufferPOS;
    public List<String> arcs;
    public List<String> partOfSpeechArcs;

    public ParserLogEntry() {

    }

    public ParserLogEntry(Configuration configuration, int[] featureArray, String transition) {
        stackWords = configuration.getStackWords();
        stackPOS = configuration.getStackPOSTags();
        bufferWords = configuration.getBufferWords();
        bufferPOS = configuration.getBufferPOSTags();
        arcs = configuration.getDependencyArcs();
        partOfSpeechArcs = configuration.getPOSDependencyArcs();
        features = featureArray;
        this.transition = transition;
    }

}
