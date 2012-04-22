import java.util.ArrayList;
import java.util.List;


public class Candidate {

	String label;
	List<String> features;
	double score;

	Candidate(String lbl, List<String> feats) {
		features = new ArrayList<String>();
		label = lbl;
		for (int i = 0; i < feats.size(); i++) {
			features.add(label + Features.defaultFeatureSeparator
					+ feats.get(i));
		}
	}

//	List<String> getFeatures() {
//		return features;
//	}
//
//	void setScore(double scr) {
//		score = scr;
//	}
//
//	double getScore() {
//		return score;
//	}
//	
//	String getLabel(){
//		return label;
//	}
}

