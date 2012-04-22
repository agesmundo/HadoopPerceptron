import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.OutputCollector;


public class PerceptronAveraged extends Perceptron{

	private int upCount=0;//TODO try to set this static to see if changes something of the result, it shouldn't
	private List<Double> id2cumulative=new ArrayList<Double>();

	void update(Candidate promote, Candidate penalize){

		upCount++;
		updateFeat(promote.features, +1);
		updateFeat(penalize.features, -1);
	}

	protected void updateFeat(List<String> feats, double para) {

		for (int i = 0; i < feats.size(); i++) {
			String onefeat = feats.get(i);
			int feaid = getAndRegFeatID(onefeat);;
			Feature feat = id2feat.get(feaid);
			feat.weight += para;
			Double avgWeight = getCumulativeFromId(feaid);
			avgWeight+=para*upCount;
			id2cumulative.set(feaid, avgWeight);
		}
	}

	public void collectOutput(OutputCollector<Text, DoubleWritable> output)
			throws IOException {
		for (int i = 0; i < id2feat.size(); i++) {
			Feature feat = id2feat.get(i);
			Double avgWeight= getCumulativeFromId(i);
			output.collect(new Text(feat.featstr), new DoubleWritable(feat.weight-(avgWeight/(upCount+1))));
		}
	}

	private Double getCumulativeFromId(int id){
		while(! (id<id2cumulative.size()))id2cumulative.add(new Double(0));
		return id2cumulative.get(id);
	}
}
