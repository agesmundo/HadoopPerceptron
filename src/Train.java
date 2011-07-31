import java.io.IOException;
import java.util.*;

import org.apache.hadoop.fs.*;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.util.*;
import org.apache.hadoop.filecache.DistributedCache;

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

		if (args.length != 3 && args.length != 4) {
			throw new Exception(
					"Wrong number of arguments.\n "
							+ "Usage: Train <number_iterations> <input_dir> <output_prefix> [<weight_vector>]");
		}

		int numIterations = Integer.parseInt(args[0]);
		String inputDir = args[1];
		String outputDirPref = args[2];

		String[] runArgs = new String[2 + (args.length - 3)];
		runArgs[0] = inputDir;
		runArgs[1] = outputDirPref + "_" + 1;
		if (runArgs.length == 3)
			runArgs[2] = args[3];

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
}
