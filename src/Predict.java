import java.io.IOException;
import java.util.*;

import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.util.*;

public class Predict extends Configured implements Tool {

	public static class Map extends MapReduceBase implements
			Mapper<LongWritable, Text, Text, Text> {

		private Perceptron perceptron = new Perceptron();
		JobConf conf = null;

		@Override
		public void configure(JobConf jc) {
			conf = jc;
		}

		public void map(LongWritable key, Text value,
				OutputCollector<Text, Text> output, Reporter reporter)
				throws IOException {
			perceptron.readWeights(conf);

			StringBuilder out = new StringBuilder();
			out.append("|||");

			Sentence sentence = new Sentence(value.toString());
			String predLabel = "";

			for (int i = 0; i < sentence.size(); i++) {

				predLabel = perceptron.predict(Features.getFeatures(sentence
						.getWord(i - 1), sentence.getWord(i), sentence
						.getWord(i + 1), predLabel));

				out.append(" " + predLabel);
			}

			// <in sentence, sequence of labels>
			output.collect(value, new Text(out.toString()));
		}
	}

	public static class Reduce extends MapReduceBase implements
			Reducer<Text, Text, Text, Text> {
		public void reduce(Text key, Iterator<Text> values,
				OutputCollector<Text, Text> output, Reporter reporter)
				throws IOException {
			output.collect(key, values.next());
		}
	}

	public int run(String[] args) throws Exception {
		JobConf conf = new JobConf(getConf(), Predict.class);
		conf.setJobName("predict");

		conf.setOutputKeyClass(Text.class);
		conf.setOutputValueClass(Text.class);

		conf.setMapperClass(Map.class);
		conf.setCombinerClass(Reduce.class);
		conf.setReducerClass(Reduce.class);

		conf.setInputFormat(TextInputFormat.class);
		conf.setOutputFormat(TextOutputFormat.class);

		FileInputFormat.setInputPaths(conf, new Path(args[0]));
		FileOutputFormat.setOutputPath(conf, new Path(args[1]));

		try {// read weight vector
			FileSystem fs = FileSystem.get(conf);
			FileStatus[] status = fs.listStatus(new Path(args[2]));
			for (int j = 0; j < status.length; j++) {
				if (Perceptron.isWeightFile(status[j])) {
					DistributedCache.addCacheFile(status[j].getPath().toUri(),
							conf);
				}
			}
		} catch (Exception e) {
			System.out.println("File not found");
		}

		JobClient.runJob(conf);

		return 0;
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 3) {
			throw new Exception(
					"Wrong number of arguments.\n "
							+ "Usage: Predict <input_folder> <outout_folder> <weight_folder>");
		}

		int res = ToolRunner.run(new Configuration(), new Predict(), args);
		System.exit(res);
	}
}
