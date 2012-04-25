import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class PerceptronPassiveAggressive extends Perceptron{
	
	void update(Candidate promote, Candidate penalize){
		List<Double> xxx = new ArrayList<Double>();xxx.get(100);
		double margin = 1;//TODO move this
		double loss = penalize.score-promote.score +margin;
		double normSquare=promote.features.size()+penalize.features.size();//this holds for binary features
		double alpha = loss/normSquare;
		updateFeat(promote.features, +1*alpha);
		updateFeat(penalize.features, -1*alpha);

		//debug
		try{
			scoreCand(promote);
			scoreCand(penalize);
			if(penalize.score>promote.score+margin-0.01)throw new IOException("PA TEST FAIL:\npenalize: "+penalize.score+" ;promote: "+promote.score);
		}
		catch(IOException e){
			System.err.println("\n"+e.getMessage()+"\n");
		}

	}

}
