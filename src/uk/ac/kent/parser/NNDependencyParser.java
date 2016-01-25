package uk.ac.kent.parser;

import edu.stanford.nlp.parser.nndep.Configuration;
import edu.stanford.nlp.parser.nndep.DependencyParser;
import edu.stanford.nlp.parser.nndep.DependencyTree;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.StringUtils;

import java.util.Properties;

/**
 * Created by elepedus on 23/01/2016.
 * Overloads Stanford's neural network dependency parser to allow partial parsing
 */
public class NNDependencyParser extends DependencyParser {

    public NNDependencyParser(Properties properties) {
        super(properties);
    }

    protected DependencyTree predictInner(CoreMap sentence) {
        labelIDs.put("UNKNOWN", Integer.MAX_VALUE);

        int numTrans = system.numTransitions();

        Configuration c = system.initialConfiguration(sentence);
        while (!system.isTerminal(c)) {
            double[] scores = classifier.computeScores(getFeatureArray(c));

            double optScore = Double.NEGATIVE_INFINITY;
            String optTrans = null;

            for (int j = 0; j < numTrans; ++j) {
                if (scores[j] > optScore && system.canApply(c, system.transitions.get(j))) {
                    optScore = scores[j];
                    optTrans = system.transitions.get(j);
                }
            }
            // Allow partial parsing
            if (optTrans == null){
                optTrans = "R(UNKNOWN)";
            }
            system.apply(c, optTrans);
        }
        return c.tree;
    }
    /**
     * A main program for training, testing and using the parser.
     *
     * <p>
     * You can use this program to train new parsers from treebank data,
     * evaluate on test treebank data, or parse raw text input.
     *
     * <p>
     * Sample usages:
     * <ul>
     *   <li>
     *     <strong>Train a parser with CoNLL treebank data:</strong>
     *     <code>java edu.stanford.nlp.parser.nndep.DependencyParser -trainFile trainPath -devFile devPath -embedFile wordEmbeddingFile -embeddingSize wordEmbeddingDimensionality -model modelOutputFile.txt.gz</code>
     *   </li>
     *   <li>
     *     <strong>Parse raw text from a file:</strong>
     *     <code>java edu.stanford.nlp.parser.nndep.DependencyParser -model modelOutputFile.txt.gz -textFile rawTextToParse -outFile dependenciesOutputFile.txt</code>
     *   </li>
     *   <li>
     *     <strong>Parse raw text from standard input, writing to standard output:</strong>
     *     <code>java edu.stanford.nlp.parser.nndep.DependencyParser -model modelOutputFile.txt.gz -textFile - -outFile -</code>
     *   </li>
     * </ul>
     *
     * <p>
     * See below for more information on all of these training / test options and more.
     *
     * <p>
     * Input / output options:
     * <table>
     *   <tr><th>Option</th><th>Required for training</th><th>Required for testing / parsing</th><th>Description</th></tr>
     *   <tr><td><tt>&#8209;devFile</tt></td><td>Optional</td><td>No</td><td>Path to a development-set treebank in <a href="http://ilk.uvt.nl/conll/#dataformat">CoNLL-X format</a>. If provided, the </td></tr>
     *   <tr><td><tt>&#8209;embedFile</tt></td><td>Optional (highly recommended!)</td><td>No</td><td>A word embedding file, containing distributed representations of English words. Each line of the provided file should contain a single word followed by the elements of the corresponding word embedding (space-delimited). It is not absolutely necessary that all words in the treebank be covered by this embedding file, though the parser's performance will generally improve if you are able to provide better embeddings for more words.</td></tr>
     *   <tr><td><tt>&#8209;model</tt></td><td>Yes</td><td>Yes</td><td>Path to a model file. If the path ends in <tt>.gz</tt>, the model will be read as a Gzipped model file. During training, we write to this path; at test time we read a pre-trained model from this path.</td></tr>
     *   <tr><td><tt>&#8209;textFile</tt></td><td>No</td><td>Yes (or <tt>testFile</tt>)</td><td>Path to a plaintext file containing sentences to be parsed.</td></tr>
     *   <tr><td><tt>&#8209;testFile</tt></td><td>No</td><td>Yes (or <tt>textFile</tt>)</td><td>Path to a test-set treebank in <a href="http://ilk.uvt.nl/conll/#dataformat">CoNLL-X format</a> for final evaluation of the parser.</td></tr>
     *   <tr><td><tt>&#8209;trainFile</tt></td><td>Yes</td><td>No</td><td>Path to a training treebank in <a href="http://ilk.uvt.nl/conll/#dataformat">CoNLL-X format</a></td></tr>
     * </table>
     *
     * Training options:
     * <table>
     *   <tr><th>Option</th><th>Default</th><th>Description</th></tr>
     *   <tr><td><tt>&#8209;adaAlpha</tt></td><td>0.01</td><td>Global learning rate for AdaGrad training</td></tr>
     *   <tr><td><tt>&#8209;adaEps</tt></td><td>1e-6</td><td>Epsilon value added to the denominator of AdaGrad update expression for numerical stability</td></tr>
     *   <tr><td><tt>&#8209;batchSize</tt></td><td>10000</td><td>Size of mini-batch used for training</td></tr>
     *   <tr><td><tt>&#8209;clearGradientsPerIter</tt></td><td>0</td><td>Clear AdaGrad gradient histories every <em>n</em> iterations. If zero, no gradient clearing is performed.</td></tr>
     *   <tr><td><tt>&#8209;dropProb</tt></td><td>0.5</td><td>Dropout probability. For each training example we randomly choose some amount of units to disable in the neural network classifier. This parameter controls the proportion of units "dropped out."</td></tr>
     *   <tr><td><tt>&#8209;embeddingSize</tt></td><td>50</td><td>Dimensionality of word embeddings provided</td></tr>
     *   <tr><td><tt>&#8209;evalPerIter</tt></td><td>100</td><td>Run full UAS (unlabeled attachment score) evaluation every time we finish this number of iterations. (Only valid if a development treebank is provided with <tt>&#8209;devFile</tt>.)</td></tr>
     *   <tr><td><tt>&#8209;hiddenSize</tt></td><td>200</td><td>Dimensionality of hidden layer in neural network classifier</td></tr>
     *   <tr><td><tt>&#8209;initRange</tt></td><td>0.01</td><td>Bounds of range within which weight matrix elements should be initialized. Each element is drawn from a uniform distribution over the range <tt>[-initRange, initRange]</tt>.</td></tr>
     *   <tr><td><tt>&#8209;maxIter</tt></td><td>20000</td><td>Number of training iterations to complete before stopping and saving the final model.</td></tr>
     *   <tr><td><tt>&#8209;numPreComputed</tt></td><td>100000</td><td>The parser pre-computes hidden-layer unit activations for particular inputs words at both training and testing time in order to speed up feedforward computation in the neural network. This parameter determines how many words for which we should compute hidden-layer activations.</td></tr>
     *   <tr><td><tt>&#8209;regParameter</tt></td><td>1e-8</td><td>Regularization parameter for training</td></tr>
     *   <tr><td><tt>&#8209;saveIntermediate</tt></td><td><tt>true</tt></td><td>If <tt>true</tt>, continually save the model version which gets the highest UAS value on the dev set. (Only valid if a development treebank is provided with <tt>&#8209;devFile</tt>.)</td></tr>
     *   <tr><td><tt>&#8209;trainingThreads</tt></td><td>1</td><td>Number of threads to use during training. Note that depending on training batch size, it may be unwise to simply choose the maximum amount of threads for your machine. On our 16-core test machines: a batch size of 10,000 runs fastest with around 6 threads; a batch size of 100,000 runs best with around 10 threads.</td></tr>
     *   <tr><td><tt>&#8209;wordCutOff</tt></td><td>1</td><td>The parser can optionally ignore rare words by simply choosing an arbitrary "unknown" feature representation for words that appear with frequency less than <em>n</em> in the corpus. This <em>n</em> is controlled by the <tt>wordCutOff</tt> parameter.</td></tr>
     * </table>
     *
     * Runtime parsing options:
     * <table>
     *   <tr><th>Option</th><th>Default</th><th>Description</th></tr>
     *   <tr><td><tt>&#8209;escaper</tt></td><td>N/A</td><td>Only applicable for testing with <tt>-textFile</tt>. If provided, use this word-escaper when parsing raw sentences. (Should be a fully-qualified class name like <tt>edu.stanford.nlp.trees.international.arabic.ATBEscaper</tt>.)</td></tr>
     *   <tr><td><tt>&#8209;numPreComputed</tt></td><td>100000</td><td>The parser pre-computes hidden-layer unit activations for particular inputs words at both training and testing time in order to speed up feedforward computation in the neural network. This parameter determines how many words for which we should compute hidden-layer activations.</td></tr>
     *   <tr><td><tt>&#8209;sentenceDelimiter</tt></td><td>N/A</td><td>Only applicable for testing with <tt>-textFile</tt>.  If provided, assume that the given <tt>textFile</tt> has already been sentence-split, and that sentences are separated by this delimiter.</td></tr>
     *   <tr><td><tt>&#8209;tagger.model</tt></td><td>edu/stanford/nlp/models/pos-tagger/english-left3words/english-left3words-distsim.tagger</td><td>Only applicable for testing with <tt>-textFile</tt>. Path to a part-of-speech tagger to use to pre-tag the raw sentences before parsing.</td></tr>
     * </table>
     */
    public static void main(String[] args) {
        Properties props = StringUtils.argsToProperties(args, numArgs);
        DependencyParser parser = new NNDependencyParser(props);
        run(props, parser);
    }
}
