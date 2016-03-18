package uk.ac.kent;

import edu.stanford.nlp.pipeline.StanfordCoreNLPServer;
import edu.stanford.nlp.util.StringUtils;
import uk.ac.kent.parser.NNDependencyParser;
import uk.ac.kent.parser.ParserLogEntry;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Created by elepedus on 16/03/2016.
 */
public class ParserFlow {
    public static final int PORT = 9000;
    private final String corpusPath;
    private String parseLogPath;
    private String histogramPath;
    private String examplesOutputPath;
    private final String testPath;
    private String outputPath;
    private String modelPath;
    private int currentIteration;
    private boolean done;
    private int maxIterations = Integer.MAX_VALUE;
    private StanfordCoreNLPServer server;
    private HashMap<String, Integer> passHistory;
    private int currentPassLimit = 1;
    private int maxPasses = 1;

    /**
     * Explicitly specifies the number of arguments expected with
     * particular command line options.
     */
    protected static final Map<String, Integer> numArgs = new HashMap<>();

    static {
        numArgs.put("path", 1);
        numArgs.put("corpus", 1);
    }

    public static void main(String[] args) {
        Properties properties = StringUtils.argsToProperties(args, numArgs);
        ParserFlow flow = new ParserFlow(properties.getProperty("path"));
    }

    public ParserFlow(String path) {
        modelPath = path + "/model.txt.gz";
        corpusPath = path + "/corpus.txt";
        testPath = "training/train.dep";
        currentIteration = 0;
        passHistory = new HashMap<>();
        done = false;
        ensureModel(modelPath);
        startServer();

        while (!done) {
            System.out.println("Starting iteration " + currentIteration);
            String currentPath = path + "/iteration" + currentIteration;
            new File(currentPath).mkdir();
            parseLogPath = currentPath + "/parseLog.yaml";
            histogramPath = currentPath + "/bigrams.txt";
            examplesOutputPath = currentPath + "/trainingExamples.yaml";
            outputPath = currentPath + "/parseOutput.txt";
            System.out.println("Parsing corpus...");
            parseCorpus(corpusPath, modelPath, parseLogPath, outputPath);
            System.out.println("Processing Parse Log...");
            processParseLog(parseLogPath, histogramPath, examplesOutputPath);
            train(modelPath, examplesOutputPath);
            test(modelPath, testPath);
            cleanUp(parseLogPath);
            restartServer();
            currentIteration++;
            if (currentIteration == maxIterations) done = true;
        }
    }

    private void restartServer() {
        try {
            server.serverExecutor.shutdown();
            server.serverExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            System.err.println("Tasks Interrupted.");
        } finally {
            server.serverExecutor.shutdownNow();
        }
        server.server.stop(0);
        server = null;
        startServer();
    }

    private void cleanUp(String parseLogPath) {
        Path path = new File(parseLogPath).toPath();
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void test(String modelPath, String testPath) {
        Properties parserProps = new Properties();
        parserProps.setProperty("model", modelPath);
        parserProps.setProperty("testFile", testPath);
        NNDependencyParser parser = new NNDependencyParser(parserProps);
        NNDependencyParser.run(parserProps, parser);
    }

    private void train(String modelPath, String examplesOutputPath) {
        Trainer trainer = new Trainer(examplesOutputPath, modelPath);
    }

    private void processParseLog(String parseLogPath, String histogramPath, String examplesOutputPath) {
        ParserLogAnalyser analyser = new ParserLogAnalyser();
        analyser.loadLogEntries(parseLogPath);
        analyser.bigrams = analyser.extractPOSBigramFrequencies();
        LinkedList<ParserLogEntry> relevantEntries;
        Bigram topBigram;
        String key = "";
        do {
            topBigram = analyser.getTopBigram();
            key = topBigram.getFirst() + topBigram.getSecond();
            int currentCount = passHistory.getOrDefault(key, 0);
            passHistory.put(key, currentCount + 1);
            if (currentCount > currentPassLimit) {
                analyser.bigrams.remove(topBigram);
            }
        } while (passHistory.get(key) > currentPassLimit);

        do {
            relevantEntries = analyser.getMatchingEntries(topBigram);
            if (relevantEntries.size() == 0) {
                analyser.bigrams.remove(topBigram);
                if (analyser.bigrams.size() == 0) {
                    done = true;
                    return;
                }
                topBigram = analyser.getTopBigram();
            }
        } while (relevantEntries.size() == 0);
        ParserLogAnalyser.writePOSBigramHistogram(analyser.bigrams, histogramPath);
        System.out.println("Top Bigram: " + topBigram.getFirst() + " " + topBigram.getSecond() + " Entries found: " + relevantEntries.size());
        analyser.updateTransitions(relevantEntries, "L(PARSED)");
//        analyser.logEntries = relevantEntries;
        analyser.saveExamplesToFile(examplesOutputPath);
    }


    private static void parseCorpus(String corpusPath, String modelPath, String parseLogPath, String outputPath) {
        Properties parserProps = new Properties();
        parserProps.setProperty("model", modelPath);
        parserProps.setProperty("textFile", corpusPath);
        if (outputPath != null) {
            parserProps.setProperty("outFile", outputPath);
        }
        parserProps.setProperty("logOutputPath", parseLogPath);
        NNDependencyParser parser = new NNDependencyParser(parserProps);
        NNDependencyParser.run(parserProps, parser);
    }

    private static void ensureModel(String modelPath) {
        File model = new File(modelPath);
        if (model.exists()) {
            return;
        } else {
            Properties parserProps = new Properties();
            parserProps.setProperty("trainFile", "training/train.dep");
            parserProps.setProperty("devFile", "training/dev.dep");
            parserProps.setProperty("embedFile", "training/glove.6B.50d.txt");
            parserProps.setProperty("embeddingSize", "50");
            parserProps.setProperty("model", modelPath);
            NNDependencyParser parser = new NNDependencyParser(parserProps);
            parser.config.maxIter = 100;
            NNDependencyParser.run(parserProps, parser);
        }
    }

    private void startServer() {
        try {
            server = new StanfordCoreNLPServer(PORT);
            server.defaultProps.setProperty("depparse.model", modelPath);
            server.run();
            System.out.println("Server running");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
