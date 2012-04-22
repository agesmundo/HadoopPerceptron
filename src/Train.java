import java.io.IOException;
import java.util.*;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.mapred.FileAlreadyExistsException;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.util.*;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Train extends Configured implements Tool {

	//Usage
	static final String USAGE = "Train -i <input_folder> -o <output_folder> [options]";
	
	//Keys to find HadoopPerceptron options in the configuration
	static final String K_HAS_INPUT_PARAMS="HP.has.input.params"; //TODO? useless?
	static final String K_PARAMETERS_FOLDER="HP.parameters.folder";
	static final String K_INPUT_FOLDER="HP.input.folder";
	static final String K_OUTPUT_FOLDER="HP.output.folder";
	static final String K_N_MAP="HP.number.map.tasks";
	static final String K_N_REDUCE="HP.number.reduce.tasks";
	
	//Defaul values for options
	static final String D_N="1";
	static final String D_n="1";
	
	static Options options=initOptions();
	private static Options initOptions(){
		Options options = new Options();

		OptionBuilder.hasArg(false);
		OptionBuilder.withDescription("Display usage.");
		options.addOption(OptionBuilder.create("help"));

		OptionBuilder.withArgName("input_folder");
		OptionBuilder.hasArg(true);
		OptionBuilder.withDescription("Folder in the hadoop dfs containing the training corpus.");
		OptionBuilder.isRequired(true);
		options.addOption(OptionBuilder.create("i"));

		OptionBuilder.withArgName("output_folder_prefix");
		OptionBuilder.hasArg(true);
		OptionBuilder.withDescription("Prefix of the name for the folder in the hadoop dfs where the model parameters are going to be saved.");
		OptionBuilder.isRequired(true);
		options.addOption(OptionBuilder.create("o"));

		/*		
		OptionBuilder.withArgName("integer");
	    OptionBuilder.hasArg(true);
	    OptionBuilder.withDescription("Perceptron training iteration per node. default value is "+D_n+".");
	    OptionBuilder.withType(Integer.class);
	    options.addOption(OptionBuilder.create("n"));
		 */

		OptionBuilder.withArgName("integer");
		OptionBuilder.hasArg(true);
		OptionBuilder.withDescription("Number of Parameter Mixing iterations. default value is "+D_N+".");
		OptionBuilder.withType(Integer.class);
		options.addOption(OptionBuilder.create("N"));

		OptionBuilder.withArgName("parameters_folder");
		OptionBuilder.hasArg(true);
		OptionBuilder.withDescription("Folder in the hadoop dfs containing the parameters used to initialize the model.");
		options.addOption(OptionBuilder.create("p"));
		
		OptionBuilder.withArgName("integer");
		OptionBuilder.hasArg(true);
		OptionBuilder.withDescription("Set recommended number of map tasks.");
		OptionBuilder.withType(Integer.class);
		options.addOption(OptionBuilder.create("M"));

		OptionBuilder.withArgName("integer");
		OptionBuilder.hasArg(true);
		OptionBuilder.withDescription("Set recommended number of reduce tasks.");
		OptionBuilder.withType(Integer.class);
		options.addOption(OptionBuilder.create("R"));
		
		return options;
	}
	
	public static class Map extends MapReduceBase implements
	Mapper<LongWritable, Text, Text, DoubleWritable> {

		private Perceptron perceptron = new Perceptron();
		JobConf conf = null;

		@Override
		public void configure(JobConf jc) {
			conf = jc;
		}

		public void map(LongWritable key, Text value,
				OutputCollector<Text, DoubleWritable> output, Reporter reporter)
						throws IOException {
			if (conf.getBoolean(K_HAS_INPUT_PARAMS, false))
				perceptron.readWeights(conf);

			Sentence sentence = new Sentence(value.toString());
			String prevPredLabel = "";

			for (int i = 0; i < sentence.size(); i++) {

				prevPredLabel = perceptron.train(Features.getFeatures(sentence
						.getWord(i - 1), sentence.getWord(i), sentence
						.getWord(i + 1), prevPredLabel), sentence
						.getGoldLabel(i));
			}

			// <feats id, score>
			perceptron.collectOutput(output);
		}
	}

	public static class Reduce extends MapReduceBase implements
	Reducer<Text, DoubleWritable, Text, DoubleWritable> {
		JobConf conf = null;

		@Override
		public void configure(JobConf jc) {
			conf = jc;
		}

		public void reduce(Text key, Iterator<DoubleWritable> values,
				OutputCollector<Text, DoubleWritable> output, Reporter reporter)
						throws IOException {
			// sum items with same key and divide by number of clusters
			double sum = 0;
			while (values.hasNext()) {
				sum += values.next().get();
			}
			DoubleWritable weight = new DoubleWritable(sum	/ conf.getNumMapTasks()); //TODO? test division, should not be there
			output.collect(key, weight);
		}
	}

	public int run(String[] args) throws Exception {

		try{
			JobConf conf = new JobConf(getConf(), Train.class);
			conf.setJobName("train");

			conf.setOutputKeyClass(Text.class);
			conf.setOutputValueClass(DoubleWritable.class);

			conf.setMapperClass(Map.class);
			conf.setCombinerClass(Reduce.class);
			conf.setReducerClass(Reduce.class);

			conf.setInputFormat(TextInputFormat.class);
			conf.setOutputFormat(TextOutputFormat.class);

			FileInputFormat.setInputPaths(conf, new Path(conf.get(K_INPUT_FOLDER)));
			FileOutputFormat.setOutputPath(conf, new Path(conf.get(K_OUTPUT_FOLDER)));

			int nMap=conf.getInt(K_N_MAP,-1);
			if (nMap>0){
				conf.setNumMapTasks(nMap);
			}
			int nRed=conf.getInt(K_N_REDUCE,-1);
			if (nRed>0){
				conf.setNumReduceTasks(nRed);
			}
			
			String paramFolder=conf.get(K_PARAMETERS_FOLDER);
			if (paramFolder!=null) {// init params are specified
				if(DistributedCacheUtils.loadParametersFolder(paramFolder, conf)==1)return 1;
				conf.setBoolean(K_HAS_INPUT_PARAMS, true);
			}

			JobClient.runJob(conf);

			return 0;
		}catch (FileAlreadyExistsException e){
			System.err.println("\nError:\n"+e.getMessage()+"\n");
			return 1;
		}
	}


	public static void main(String[] args) throws Exception {
		if(Arrays.asList(args).contains("-help")){
			new HelpFormatter().printHelp( USAGE, options );
			System.exit(0);
		}
		try{
			CommandLine cmd = new PosixParser().parse(options, args);

			int	numIterations= Integer.parseInt(cmd.getOptionValue("N",D_N));
			String inputDir = cmd.getOptionValue("i");
			String outputDirPref = cmd.getOptionValue("o");

			Configuration invariantConf= new Configuration();
			if (cmd.hasOption( "M" )) invariantConf.set(K_N_MAP,cmd.getOptionValue("M"));
			if (cmd.hasOption( "R" )) invariantConf.set(K_N_REDUCE,cmd.getOptionValue("R"));
			if (cmd.hasOption( "p" )) invariantConf.set(K_PARAMETERS_FOLDER, cmd.getOptionValue("p")); //this is going to be overwritten for iterations different from the first
			invariantConf.set(K_INPUT_FOLDER, inputDir);
			
			Configuration conf;
			for (int i = 0; i < numIterations; i++) {
				conf= new Configuration(invariantConf);

				conf.set(K_OUTPUT_FOLDER, outputDirPref+"_"+(i+1));
				if (i>0)conf.set(K_PARAMETERS_FOLDER, outputDirPref+"_"+i);

				System.out.println("\n====================\nPARAMETER MIXING ITERATION: " + (i + 1));
				if( ToolRunner.run(conf, new Train(), new String[0]) ==1) System.exit(1);//arguments are passed via the configuration
			}
			System.exit(0);
		}

		catch( ParseException e ) {
			System.err.println("\nError while parsing command line:\n"+e.getMessage()+"\n");
			new HelpFormatter().printHelp( USAGE, options );
		}
		catch( NumberFormatException e ) {
			System.err.println("\nError while parsing command line:\n"+e.getMessage()+"\n");
			new HelpFormatter().printHelp( USAGE, options );
		}
	}

}
