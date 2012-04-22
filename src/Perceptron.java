import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.util.StringUtils;

/**
 * Perceptron
 * 
 * @author Andrea Gesmundo
 * 
 */
public class Perceptron {

	// constants
	private final int FEAT_HASH_INIT = 1000;// 1000000;

	// variables
	private Map<String, Integer> feat2id;
	private ArrayList<Feat> id2feat;

	// ////////////////////////////////////////////////////////////
	// Constructors

	public Perceptron() {
		feat2id = new HashMap<String, Integer>(FEAT_HASH_INIT);
		id2feat = new ArrayList<Feat>(FEAT_HASH_INIT);
	}

	// ////////////////////////////////////////////////////////////

	public void readWeights(JobConf conf) {
		try {
			Path[] patternsFiles = DistributedCache.getLocalCacheFiles(conf);
			for (Path patternsFile : patternsFiles) {
				BufferedReader fis = new BufferedReader(new FileReader(
						patternsFile.toString()));
				readWeights(fis);
			}
		} catch (IOException ioe) {
			System.err.println("Caught exception while getting cached files: "
					+ StringUtils.stringifyException(ioe));
		}
	}

	public void readWeights(BufferedReader fis) throws IOException {
		String line = null;
		while ((line = fis.readLine()) != null) {
			String tokens[] = line.split("\t");
			addNewFeature(tokens[0], Double.parseDouble(tokens[1]));
			// NB need to register label if missing
			LabelLib.storeLabel(Features.getLabel(tokens[0]));
		}
	}

	public void collectOutput(OutputCollector<Text, DoubleWritable> output)
			throws IOException {
		for (int i = 0; i < id2feat.size(); i++) {
			Feat feat = id2feat.get(i);
			output.collect(new Text(feat.featstr), new DoubleWritable(feat.weight));
		}
	}

	public String predict(List<String> feats) throws IOException {
		if (LabelLib.getCandidateLabels().size() == 0)
			throw new IOException("no label candidates");

		// rank labels
		List<Candidate> cands = initCands(feats);

		// pick best
		Candidate topCand = pickTop(cands);

		return topCand.label;
	}

	public String train(List<String> feats, String gold) throws IOException {
		String predicted = "";

		// rank labels
		List<Candidate> cands = initCands(feats);

		// pick best
		Candidate topCand = pickTop(cands);
		predicted = topCand.label;

		// if best is wrong
		boolean isCorrect = false;
		if (topCand.label.equals(gold.toString()))
			isCorrect = true;
		if (!isCorrect) {
			// find gold
			Candidate goldCand = null;
			for (int i = 0; i < cands.size(); i++) {
				Candidate cand = cands.get(i);
				if (cand.label.equals(gold.toString())) {
					goldCand = cand;
					break;
				}
			}
			if (goldCand == null)
				throw new IOException("null gold cand");
			// update
			standardUpdate(goldCand.features,topCand.features);
		}
		return predicted;
	}

	void standardUpdate(List<String>promote, List<String>penalize){
		updateFeat(promote, +1);
		updateFeat(penalize, -1);
	}

	private Candidate pickTop(List<Candidate> cands) throws NullPointerException {
		if (cands.size() == 0)
			throw new NullPointerException("\nError:\nThe list of candidates is empty.");
		Candidate topCand = cands.get(0);
		double topOpScore = topCand.getScore();
		for (int i = 1; i < cands.size(); i++) {
			Candidate cand = cands.get(i);
			if (cand.getScore() > topOpScore) {
				topOpScore = cand.getScore();
				topCand = cand;
			}
		}
		return topCand;
	}

	private List<Candidate> initCands(List<String> feats) {
		List<Candidate> cands = new ArrayList<Candidate>();
		for (String label : LabelLib.getCandidateLabels()) {
			Candidate cand = new Candidate(label, feats);
			scoreCand(cand);
			cands.add(cand);
		}
		return cands;
	}

	private void scoreCand(Candidate cand) {
		double score = getScore(cand.getFeatures());
		cand.setScore(score);
	}

	// ////////////////////////////////////////////////////////////

	// private int size() {
	// return id2feat.size();
	// }
	//
	// private void trimToSize() {
	// id2feat.trimToSize();
	// }

	// /**
	// * Set the weights from the training result.
	// *
	// * @param id
	// * The ID of the feature to update.
	// * @param weight
	// * The the weight of the feature.
	// */
	// private void setWeight(int id, double weight) {
	// Feat feat = id2feat.get(id);
	// feat.weight = weight;
	// }

	// /**
	// * Register a set of feature.
	// *
	// * @param feats
	// * A List of name of feature.
	// */
	// private void regFeat(List<String> feats) {
	// for (int i = 0; i < feats.size(); i++) {
	// regFeat(feats.get(i));
	// }
	// }

	/**
	 * Given the string of the feature, 
	 * returns the id.
	 * If the feature is new, the method registers the feature and returns the id. 
	 * 
	 * @param feat
	 *            Name of the feature.
	 * @return The index of the feature.
	 */
	private int getAndRegFeatID(String feat) {
		Integer id = (Integer) feat2id.get(feat);
		// if already in the lib
		if (id != null) {
			return id.intValue();
		}
		// if not in the lib update the HT and the List
		id = new Integer(id2feat.size());
		feat2id.put(feat, id);
		Feat newfeat = new Feat(feat);
		id2feat.add(newfeat);
		return id.intValue();
	}

	private void addNewFeature(String feat, double score) throws IOException {
		Integer id = (Integer) feat2id.get(feat);
		// if already in the lib error
		if (id != null) {
			throw new IOException("feature " + feat + " was already registred");
		}
		// if not in the lib update the HT and the List
		id = new Integer(id2feat.size());
		feat2id.put(feat, id);
		Feat newfeat = new Feat(feat, score);
		id2feat.add(newfeat);
	}

	/**
	 * Get the ID from the feature name.
	 * 
	 * @param featgetFeatID
	 *            The feature name.
	 * @return The feature ID, return -1 if the feature doesn't exist.
	 */
	private int getFeatID(String feat) {
		Integer id = (Integer) feat2id.get(feat);
		if (id != null) {
			return id.intValue();
		}
		return -1;
	}

	// /**
	// * Get weight from the feature name.
	// *
	// * @param feat
	// * The feature name.
	// * @return The feature weight, return 0 if the feature does't exist.
	// */
	// private double getWeight(String feat) {
	// int feaid = getFeatID(feat);
	// if (feaid == -1)
	// return 0;
	// return id2feat.get(feaid).weight;
	// }

	/**
	 * Get the score (sum of weights) of a set of features.
	 * 
	 * @param feats
	 *            A List of feature names.
	 * @return The sum of the weights of the features selected.
	 */
	private double getScore(List<String> feats) {
		double score = 0;
		for (int i = 0; i < feats.size(); i++) {
			int feaid = getFeatID(feats.get(i));
			if (feaid != -1)
				score += id2feat.get(feaid).weight;
		}
		return score;
	}

	/**
	 * Upadte a list of feats by para.
	 * 
	 * @param feats
	 *            String id for features
	 * @param para
	 *            Promotion amount.
	 */
	private void updateFeat(List<String> feats, double para) {

		// Debug
		if (para == 0.0) {
			System.err.println("*** ZERO UPDATING***");
		}

		for (int i = 0; i < feats.size(); i++) {
			String onefeat = feats.get(i);
			int feaid = getAndRegFeatID(onefeat);;
			Feat feat = id2feat.get(feaid);
			feat.weight += para;
		}
	}

	static class Feat {

		/** Name of the feature */
		public String featstr;
		/** Weight of the feature */
		public double weight;

		public Feat(String str) {
			featstr = str;
			weight = 0;
		}

		public Feat(String str, double w) {
			featstr = str;
			weight = w;
		}

	}

	static class Candidate {
		private String label;
		private List<String> features;
		private double score;

		Candidate(String lbl, List<String> feats) {
			features = new ArrayList<String>();
			label = lbl;
			for (int i = 0; i < feats.size(); i++) {
				features.add(label + Features.defaultFeatureSeparator
						+ feats.get(i));
			}
		}

		List<String> getFeatures() {
			return features;
		}

		void setScore(double scr) {
			score = scr;
		}

		double getScore() {
			return score;
		}
	}

}
