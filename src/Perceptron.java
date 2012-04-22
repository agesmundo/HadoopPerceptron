import java.io.IOException;
import java.util.List;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.OutputCollector;

/**
 * Perceptron
 * 
 * @author Andrea Gesmundo
 * 
 */
public abstract class Perceptron extends LinearModel{

	
	// ////////////////////////////////////////////////////////////
	// Constructors

	
	// ////////////////////////////////////////////////////////////

	public void collectOutput(OutputCollector<Text, DoubleWritable> output)
			throws IOException {
		for (int i = 0; i < id2feat.size(); i++) {
			Feature feat = id2feat.get(i);
			output.collect(new Text(feat.featstr), new DoubleWritable(feat.weight));
		}
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

			update(goldCand,topCand);//TODO
			
		}
		return predicted;
	}

	abstract void update(Candidate promote, Candidate penalize);


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
		Feature newfeat = new Feature(feat);
		id2feat.add(newfeat);
		return id.intValue();
	}

	/**
	 * Upadte a list of feats by para.
	 * 
	 * @param feats
	 *            String id for features
	 * @param para
	 *            Promotion amount.
	 */
	protected void updateFeat(List<String> feats, double para) {

		// Debug
		if (para == 0.0) {
			System.err.println("*** ZERO UPDATING***");
		}

		for (int i = 0; i < feats.size(); i++) {
			String onefeat = feats.get(i);
			int feaid = getAndRegFeatID(onefeat);;
			Feature feat = id2feat.get(feaid);
			feat.weight += para;
		}
	}
}
