import java.io.IOException;
import java.util.*;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.util.*;

public class Evaluate extends Configured implements Tool {

	public static class Map extends MapReduceBase implements
			Mapper<LongWritable, Text, Text, IntWritable> {

		private final static IntWritable one = new IntWritable(1);

		private Perceptron perceptron = new Perceptron();
		JobConf conf = null;

		@Override
		public void configure(JobConf jc) {
			conf = jc;
		}

		public void map(LongWritable key, Text value,
				OutputCollector<Text, IntWritable> output, Reporter reporter)
				throws IOException {
			perceptron.readWeights(conf);

			Sentence sentence = new Sentence(value.toString());
			String predLabel = "";
			for (int i = 0; i < sentence.size(); i++) {

				String goldLabel = sentence.getGoldLabel(i);

				predLabel = perceptron.predict(Features.getFeatures(sentence
						.getWord(i - 1), sentence.getWord(i), sentence
						.getWord(i + 1), predLabel));

				// <[true,false],count>
				output.collect(new Text("" + predLabel.equals(goldLabel)), one);

			}
		}
	}

	public static class Reduce extends MapReduceBase implements
			Reducer<Text, IntWritable, Text, IntWritable> {
		public void reduce(Text key, Iterator<IntWritable> values,
				OutputCollector<Text, IntWritable> output, Reporter reporter)
				throws IOException {

			int sum = 0;
			while (values.hasNext()) {

				sum += values.next().get();

			}

			output.collect(key, new IntWritable(sum));
		}
	}

	public int run(String[] args) throws Exception {
		JobConf conf = new JobConf(getConf(), Evaluate.class);
		conf.setJobName("evaluate");

		conf.setOutputKeyClass(Text.class);
		conf.setOutputValueClass(IntWritable.class);

		conf.setMapperClass(Map.class);
		conf.setCombinerClass(Reduce.class);
		conf.setReducerClass(Reduce.class);

		conf.setInputFormat(TextInputFormat.class);
		conf.setOutputFormat(TextOutputFormat.class);

		FileInputFormat.setInputPaths(conf, new Path(args[0]));
		FileOutputFormat.setOutputPath(conf, new Path(args[1]));

		if(DistributedCacheUtils.loadParametersFolder(args[2], conf)==1)return 1;

		JobClient.runJob(conf);

		return 0;
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 3) {
			throw new Exception(
					"Wrong number of arguments.\n "
							+ "Usage: Evaluate <input_folder> <outout_folder> <weight_folder>");
		}

		int res = ToolRunner.run(new Configuration(), new Evaluate(), args);
		System.exit(res);
	}
}
