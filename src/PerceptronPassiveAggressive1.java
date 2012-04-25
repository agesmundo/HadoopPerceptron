import java.io.IOException;


public class PerceptronPassiveAggressive1 extends Perceptron{
	
	double C=1;//slak variable
	
	void update(Candidate promote, Candidate penalize){
		double margin = 1;//TODO move this
		double loss = penalize.score-promote.score +margin;
		double normSquare=promote.features.size()+penalize.features.size();//this holds for binary features
		double alpha = loss/normSquare;
		if(alpha>C)alpha=C;
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
