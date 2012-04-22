import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.util.StringUtils;

/**
 * Linear Model
 * 
 * @author Andrea Gesmundo
 * 
 */
public class LinearModel {

	// constants
	protected final int FEAT_HASH_INIT = 1000;// 1000000;

	// variables
	protected Map<String, Integer> feat2id;
	protected List<Feature> id2feat;
	

	// ////////////////////////////////////////////////////////////
	// Constructors

	public LinearModel() {
		feat2id = new HashMap<String, Integer>(FEAT_HASH_INIT);
		id2feat = new ArrayList<Feature>(FEAT_HASH_INIT);
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

	public String predict(List<String> feats) throws IOException {
		if (LabelLib.getCandidateLabels().size() == 0)
			throw new IOException("no label candidates");

		// rank labels
		List<Candidate> cands = initCands(feats);

		// pick best
		Candidate topCand = pickTop(cands);

		return topCand.label;
	}

	protected Candidate pickTop(List<Candidate> cands) throws NullPointerException {
		if (cands.size() == 0)
			throw new NullPointerException("\nError:\nThe list of candidates is empty.");
		Candidate topCand = cands.get(0);
		double topOpScore = topCand.score;
		for (int i = 1; i < cands.size(); i++) {
			Candidate cand = cands.get(i);
			if (cand.score > topOpScore) {
				topOpScore = cand.score;
				topCand = cand;
			}
		}
		return topCand;
	}

	protected List<Candidate> initCands(List<String> feats) {
		List<Candidate> cands = new ArrayList<Candidate>();
		for (String label : LabelLib.getCandidateLabels()) {
			Candidate cand = new Candidate(label, feats);
			scoreCand(cand);
			cands.add(cand);
		}
		return cands;
	}

	protected void scoreCand(Candidate cand) {
		cand.score=getScore(cand.features);
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
		Feature newfeat = new Feature(feat, score);
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

}
