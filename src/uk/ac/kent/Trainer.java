package uk.ac.kent;

import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.io.RuntimeIOException;
import edu.stanford.nlp.parser.nndep.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Properties;

/**
 * Created by elepedus on 25/01/2016.
 * Trains a dependency parser model
 *
 * The duplication in this file makes me sick, but I haven't got time to
 * refactor CoreNLP Dependency Parser :(
 */
public class Trainer {
    Config config;
    ExampleExtractor exampleExtractor;
    Dataset trainingSet;
    Classifier classifier;
    private ArrayList<String> knownWords;
    private ArrayList<String> knownPos;
    private ArrayList<String> knownLabels;
    private ArrayList<Integer> preComputed;

    /**
     * Parse examples from a modified log file
     * @param args
     */
    public static void main(String[] args) {
        Trainer trainer = new Trainer("training/experiment2/trainingExamples.yaml", "training/experiment2/modelOutputFile.txt.gz");
    }

    public Trainer(String parseLogPath, String modelPath) {
        config = new Config(new Properties());
        config.maxIter = 1000;
        exampleExtractor = new ExampleExtractor();
        exampleExtractor.loadLogEntries(parseLogPath);
        exampleExtractor.extractExamples();
        trainingSet = exampleExtractor.getDataset();
        loadPreTrainedModel(modelPath);
        trainModel(modelPath);
    }

    public void loadPreTrainedModel(String modelPath) {

        knownWords = new ArrayList<String>();
        knownPos = new ArrayList<String>();
        knownLabels = new ArrayList<String>();

        try {
            BufferedReader input = IOUtils.readerFromString(modelPath);
            String s;
            s = input.readLine();
            int nDict = Integer.parseInt(s.substring(s.indexOf('=') + 1));
            s = input.readLine();
            int nPOS = Integer.parseInt(s.substring(s.indexOf('=') + 1));
            s = input.readLine();
            int nLabel = Integer.parseInt(s.substring(s.indexOf('=') + 1));
            s = input.readLine();
            int eSize = Integer.parseInt(s.substring(s.indexOf('=') + 1));
            s = input.readLine();
            int hSize = Integer.parseInt(s.substring(s.indexOf('=') + 1));
            s = input.readLine();
            int nTokens = Integer.parseInt(s.substring(s.indexOf('=') + 1));
            s = input.readLine();
            int nPreComputed = Integer.parseInt(s.substring(s.indexOf('=') + 1));


            double[][] E = new double[nDict + nPOS + nLabel][eSize];
            String[] splits;
            int index = 0;
            for (int k = 0; k < nDict; ++k) {
                s = input.readLine();
                splits = s.split(" ");
                knownWords.add(splits[0]);
                for (int i = 0; i < eSize; ++i)
                    E[index][i] = Double.parseDouble(splits[i + 1]);
                index = index + 1;
            }
            for (int k = 0; k < nPOS; ++k) {
                s = input.readLine();
                splits = s.split(" ");
                knownPos.add(splits[0]);
                for (int i = 0; i < eSize; ++i)
                    E[index][i] = Double.parseDouble(splits[i + 1]);
                index = index + 1;
            }
            for (int k = 0; k < nLabel; ++k) {
                s = input.readLine();
                splits = s.split(" ");
                knownLabels.add(splits[0]);
                for (int i = 0; i < eSize; ++i)
                    E[index][i] = Double.parseDouble(splits[i + 1]);
                index = index + 1;
            }

            double[][] W1 = new double[hSize][eSize * nTokens];
            for (int j = 0; j < W1[0].length; ++j) {
                s = input.readLine();
                splits = s.split(" ");
                for (int i = 0; i < W1.length; ++i)
                    W1[i][j] = Double.parseDouble(splits[i]);
            }

            double[] b1 = new double[hSize];
            s = input.readLine();
            splits = s.split(" ");
            for (int i = 0; i < b1.length; ++i)
                b1[i] = Double.parseDouble(splits[i]);

            double[][] W2 = new double[nLabel * 2 - 1][hSize];
            for (int j = 0; j < W2[0].length; ++j) {
                s = input.readLine();
                splits = s.split(" ");
                for (int i = 0; i < W2.length; ++i)
                    W2[i][j] = Double.parseDouble(splits[i]);
            }

            preComputed = new ArrayList<Integer>();
            while (preComputed.size() < nPreComputed) {
                s = input.readLine();
                splits = s.split(" ");
                for (String split : splits) {
                    preComputed.add(Integer.parseInt(split));
                }
            }
            input.close();

            classifier = new Classifier(config, trainingSet, E, W1, b1, W2, preComputed);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void trainModel(String modelPath) {
        for (int iter = 0; iter < config.maxIter; ++iter) {
            System.err.println("##### Iteration " + iter);

            Classifier.Cost cost = classifier.computeCostFunction(config.batchSize, config.regParameter, config.dropProb);
            System.err.println("Cost = " + cost.getCost() + ", Correct(%) = " + cost.getPercentCorrect());
            classifier.takeAdaGradientStep(cost, config.adaAlpha, config.adaEps);

            // Clear gradients
            if (config.clearGradientsPerIter > 0 && iter % config.clearGradientsPerIter == 0) {
                System.err.println("Clearing gradient histories..");
                classifier.clearGradientHistories();
            }
        }

        classifier.finalizeTraining();
        writeModelFile(modelPath);

    }

    public void writeModelFile(String modelFile) {
        try {
            double[][] W1 = classifier.getW1();
            double[] b1 = classifier.getb1();
            double[][] W2 = classifier.getW2();
            double[][] E = classifier.getE();

            Writer output = IOUtils.getPrintWriter(modelFile);

            output.write("dict=" + knownWords.size() + "\n");
            output.write("pos=" + knownPos.size() + "\n");
            output.write("label=" + knownLabels.size() + "\n");
            output.write("embeddingSize=" + E[0].length + "\n");
            output.write("hiddenSize=" + b1.length + "\n");
            output.write("numTokens=" + (W1[0].length / E[0].length) + "\n");
            output.write("preComputed=" + preComputed.size() + "\n");

            int index = 0;

            // First write word / POS / label embeddings
            for (String word : knownWords) {
                output.write(word);
                for (int k = 0; k < E[index].length; ++k)
                    output.write(" " + E[index][k]);
                output.write("\n");
                index = index + 1;
            }
            for (String pos : knownPos) {
                output.write(pos);
                for (int k = 0; k < E[index].length; ++k)
                    output.write(" " + E[index][k]);
                output.write("\n");
                index = index + 1;
            }
            for (String label : knownLabels) {
                output.write(label);
                for (int k = 0; k < E[index].length; ++k)
                    output.write(" " + E[index][k]);
                output.write("\n");
                index = index + 1;
            }

            // Now write classifier weights
            for (int j = 0; j < W1[0].length; ++j)
                for (int i = 0; i < W1.length; ++i) {
                    output.write("" + W1[i][j]);
                    if (i == W1.length - 1)
                        output.write("\n");
                    else
                        output.write(" ");
                }
            for (int i = 0; i < b1.length; ++i) {
                output.write("" + b1[i]);
                if (i == b1.length - 1)
                    output.write("\n");
                else
                    output.write(" ");
            }
            for (int j = 0; j < W2[0].length; ++j)
                for (int i = 0; i < W2.length; ++i) {
                    output.write("" + W2[i][j]);
                    if (i == W2.length - 1)
                        output.write("\n");
                    else
                        output.write(" ");
                }

            // Finish with pre-computation info
            for (int i = 0; i < preComputed.size(); ++i) {
                output.write("" + preComputed.get(i));
                if ((i + 1) % 100 == 0 || i == preComputed.size() - 1)
                    output.write("\n");
                else
                    output.write(" ");
            }

            output.close();
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }
}
