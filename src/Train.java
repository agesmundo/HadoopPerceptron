import java.io.IOException;
import java.util.*;


import org.apache.hadoop.fs.*;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.util.*;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Train extends Configured implements Tool {

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
			if (conf.getBoolean("has.input.weight", false))
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
			DoubleWritable weight = new DoubleWritable(sum
					/ conf.getNumMapTasks());
			output.collect(key, weight);
		}
	}

	public int run(String[] args) throws Exception {
		JobConf conf = new JobConf(getConf(), Train.class);
		conf.setJobName("train");

		conf.setOutputKeyClass(Text.class);
		conf.setOutputValueClass(DoubleWritable.class);

		conf.setMapperClass(Map.class);
		conf.setCombinerClass(Reduce.class);
		conf.setReducerClass(Reduce.class);

		conf.setInputFormat(TextInputFormat.class);
		conf.setOutputFormat(TextOutputFormat.class);

		// conf.setNumMapTasks(100);

		FileInputFormat.setInputPaths(conf, new Path(args[0]));
		FileOutputFormat.setOutputPath(conf, new Path(args[1]));

		if (args.length > 2) {// in weight is specified
			try {
				FileSystem fs = FileSystem.get(conf);
				FileStatus[] status = fs.listStatus(new Path(args[2]));
				for (int j = 0; j < status.length; j++) {
					if (Perceptron.isWeightFile(status[j])) {
						DistributedCache.addCacheFile(status[j].getPath()
								.toUri(), conf);
					}
				}
				conf.setBoolean("has.input.weight", true);
			} catch (Exception e) {
				System.out.println("File not found");
			}
		}

		JobClient.runJob(conf);

		return 0;
	}

	public static void main(String[] args) throws Exception {

		//build command line parser
		Options options = new Options();

		OptionBuilder.withArgName("input_folder");
		OptionBuilder.hasArg(true);
		OptionBuilder.withDescription("Folder in the hadoop dfs containing the training corpus.");
		OptionBuilder.isRequired(true);
		options.addOption(OptionBuilder.create("i"));

		OptionBuilder.withArgName("output_folder");
		OptionBuilder.hasArg(true);
		OptionBuilder.withDescription("Folder in the hadoop dfs where the model parameters are going to be saved.");
		OptionBuilder.isRequired(true);
		options.addOption(OptionBuilder.create("o"));

		/*		
		final Integer NDefault=new Integer(1);
		OptionBuilder.withArgName("integer");
	    OptionBuilder.hasArg(true);
	    OptionBuilder.withDescription("Perceptron training iteration per node. default value is "+nDefault+".");
	    OptionBuilder.withType(Integer.class);
	    options.addOption(OptionBuilder.create("n"));
		 */

		final Integer NDefault=new Integer(1);//TODO check how to set defaults
		OptionBuilder.withArgName("integer");
		OptionBuilder.hasArg(true);
		OptionBuilder.withDescription("Number of Parameter Mixing iterations. default value is "+NDefault+".");
		OptionBuilder.withType(Integer.class);
		options.addOption(OptionBuilder.create("N"));

		OptionBuilder.withArgName("parameters_folder");
		OptionBuilder.hasArg(true);
		OptionBuilder.withDescription("Folder in the hadoop dfs containing the parameters used to initialize the model.");
		options.addOption(OptionBuilder.create("w"));

		//		if (args.length != 3 && args.length != 4) {
		//			throw new Exception(
		//					"Wrong number of arguments.\n "
		//							+ "Usage: Train <number_iterations> <input_dir> <output_prefix> [<weight_vector>]");
		//		}

		try{
			CommandLineParser parser = new PosixParser();
			CommandLine cmd = parser.parse( options, args);

//			int numIterations=NDefault;
//			if( cmd.hasOption( "N" ) ) {
			int	numIterations= Integer.parseInt(cmd.getOptionValue("N",""+NDefault));
//			}
			String inputDir = cmd.getOptionValue("i");
			String outputDirPref = cmd.getOptionValue("o");

			String[] runArgs = new String[2];
			if (cmd.hasOption( "w" )){
				runArgs = new String[3];
				runArgs[2] = cmd.getOptionValue("w");
			}
			runArgs[0] = inputDir;
			runArgs[1] = outputDirPref + "_" + 1;

			System.out.println("########### Runing iteration: 1");
			int res = ToolRunner.run(new Configuration(), new Train(), runArgs);

			for (int i = 1; i < numIterations; i++) {
				runArgs = new String[3];
				runArgs[0] = inputDir;
				runArgs[1] = outputDirPref + "_" + (i + 1);
				runArgs[2] = outputDirPref + "_" + i;

				System.out.println("########### Runing iteration: " + (i + 1));
				res = ToolRunner.run(new Configuration(), new Train(), runArgs);
			}
			System.exit(res);
		}	    
		catch( ParseException e ) {
			System.err.println("\nError while parsing command line\n"+e.getMessage()+"\n");
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "Train -i <input_folder> -o <output_folder> [options]", options );
		}
		catch( NumberFormatException e ) {
			System.err.println(e.getMessage()+"\n");
//			System.err.println("An integer para\n");
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "Train -i <input_folder> -o <output_folder> [options]", options );
		}

	}
}
