import java.io.IOException;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

public class TP6B {
	
	/*****************************************Mapper 1******************************************/
	
	
	public static class TP6Mapper extends Mapper<Object, Text, NullWritable, Text> {

		private SortedMap<String, String> TopKMap = new TreeMap<String, String>();

		public static boolean isNumeric(String str) {
			try {
				int d = Integer.parseInt(str);
			} catch (NumberFormatException nfe) {
				return false;
			}
			return true;
		}

		public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
			String[] data = value.toString().split(",");
			if ((!data[4].isEmpty()) && (isNumeric(data[4]))) {

				String data1 = TopKMap.get(data[2]);

				if (data1 != null) {
					String[] data2 = data1.toString().split(",");
					// la ville existe déja dans la SortedMap
					if (Integer.parseInt(data2[0]) < Integer.parseInt(data[4]))
						// la population de la nouvelle ville est superieure
						// à la population de la ville existante dans la SortedMap
						TopKMap.replace(data[2], data[4].concat(",").concat(data[2]));

				} else {
					// la ville n'existe pas dans la SortedMap
					TopKMap.put(data[2], data[4].concat(",").concat(data[2]));
				}

			}
		}
		
		  @Override
		  public void cleanup(Context context) throws IOException, InterruptedException{
			for(String nameCity : TopKMap.values()){
				context.write(NullWritable.get(), new Text(nameCity));
			}
		  }

	}

  /*******************************************Reducer 1*************************************/
  
  
	public static class TP6Reducer extends Reducer<NullWritable, Text, NullWritable, Text> {

		public void reduce(NullWritable key, Iterable<Text> values, Context context)
				throws IOException, InterruptedException {
			for (Text value : values) {
				
				context.write(key, new Text(value));

			}
		}
  
  /*******************************************Mapper 2*************************************/
  
  public static class TP6Mapper2
  extends Mapper<Object, Text, NullWritable, Text>{
	 public int k = 0;
	 private SortedMap<Integer, String> TopKMap = new TreeMap<Integer, String>();
	 @Override
	 public void setup(Context context){
		k = 10;  
	 }
 
	public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
		String[] data = value.toString().split(",");
	
			TopKMap.put(Integer.parseInt(data[0]), data[1]);

				if (TopKMap.size() > k) {
					TopKMap.remove(TopKMap.firstKey());
				}
	}
 
 @Override
 public void cleanup(Context context) throws IOException, InterruptedException{
	for(String nameCity : TopKMap.values()){
		context.write(NullWritable.get(), new Text(nameCity));
	}
 }
}
  
  
  /*******************************************Reducer 2*************************************/
  
  public static class TP6Reducer2
  extends Reducer<NullWritable,Text,NullWritable,Text> {
 public int k = 0;
 private SortedMap<Integer, String> TopKMap = new TreeMap<Integer, String>();
 @Override
 public void setup(Context context){
		k = 10;  
	  }
 
public void reduce(NullWritable key, Iterable<Text> values,
                  Context context
                  ) throws IOException, InterruptedException {
		for (Text value : values) {
			String[] data = value.toString().split(",");
			TopKMap.put(Integer.parseInt(data[0]), value.toString());
			if (TopKMap.size() > k)
				TopKMap.remove(TopKMap.firstKey());
		}
		for (String c : TopKMap.values()) {
			context.write(key, new Text(c));
		}

}
}
  
  /******************************************* Combiner *************************************/
  
	public static class TP6Combiner extends Reducer<NullWritable, Text, NullWritable, Text> {
		public int k = 0;
		public SortedMap<Integer, String> combinerTopKCities = new TreeMap<Integer, String>();

		@Override
		public void setup(Context context) {
			k = 10;
		}

		public void reduce(NullWritable key, Iterable<Text> values, Context context)
				throws IOException, InterruptedException {
			for (Text cityData : values) {
				String[] data = cityData.toString().split(",");
				combinerTopKCities.put(Integer.parseInt(data[0]), cityData.toString());
				if (combinerTopKCities.size() > k) {
					combinerTopKCities.remove(combinerTopKCities.firstKey());
				}
			}
			for (String cityInfo : combinerTopKCities.values()) {
				context.write(key, new Text(cityInfo));

			}

		}
	}
  
  public static void main(String[] args) throws Exception {
    Configuration conf = new Configuration();
    Path out = new Path(args[1]);
    Job job = Job.getInstance(conf, "TP6");
    job.setNumReduceTasks(1);
    job.setJarByClass(TP6.class);
    job.setMapperClass(TP6Mapper.class);
    job.setMapOutputKeyClass(NullWritable.class);
    job.setMapOutputValueClass(Text.class);
    
    job.setCombinerClass(TP6Combiner.class);
    
    job.setReducerClass(TP6Reducer.class);
    job.setOutputKeyClass(NullWritable.class);
    job.setOutputValueClass(Text.class);
    job.setOutputFormatClass(FileOutputFormat.class);
    job.setInputFormatClass(TextInputFormat.class);
    FileInputFormat.addInputPath(job, new Path(args[0]));
    FileOutputFormat.setOutputPath(job, new Path(out, "out1"));
    //System.exit(job.waitForCompletion(true) ? 0 : 1);
    if (!job.waitForCompletion(true)) {
    	  System.exit(1);
    	}
    
		Job job2 = Job.getInstance(conf, "TP6");
		job2.setNumReduceTasks(1);
		job2.setJarByClass(TP6.class);
		job2.setMapperClass(TP6Mapper2.class);
		job2.setMapOutputKeyClass(NullWritable.class);
		job2.setMapOutputValueClass(Text.class);

		job2.setCombinerClass(TP6Combiner.class);

		job2.setReducerClass(TP6Reducer2.class);
		job2.setOutputKeyClass(NullWritable.class);
		job2.setOutputValueClass(Text.class);
		job2.setOutputFormatClass(TextOutputFormat.class);
		job2.setInputFormatClass(TextInputFormat.class);
		FileInputFormat.addInputPath(job2, new Path(out, "out1"));
		FileOutputFormat.setOutputPath(job2, new Path(out, "out1"));
		if (!job2.waitForCompletion(true)) {
			  System.exit(1);
			}

  }}
}