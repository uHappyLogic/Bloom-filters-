package put.cs.idss.pmds.bloomfilter;

import java.math.BigInteger;
import java.util.*;

public class BloomFilter {

	public static void main(String[] args) {

		int n = 10000;
		int range = 100000000;
		double factor = 10; //  = m / n
		int size = (int) Math.round(factor * n); // m

		int k = (int) Math.round(Math.log(factor) / Math.log(2));

		Random random = new Random(0);

		ArrayList<IBloomFilter> filters = new ArrayList<>();

		filters.add(new FastBloomFilter2(size, k));
		filters.add(new FastBloomFilter(size, k));
		filters.add(new BigBloomFilter(size, k));

		HashSet<Integer> set = new HashSet<>(n);

		while(set.size() < n) {
			set.add(random.nextInt(range));
		}

		for (IBloomFilter bloomFilter : filters)
			set.forEach(bloomFilter::add);

		HashMap<String, Results> filtersResults = new HashMap<>();

		for (IBloomFilter filter : filters)
			filtersResults.put(filter.getName(), new Results());

		System.out.println("Elements added to filters");

		for (int i = 0; i < range; i++)
		{
			int key = i;

			for (IBloomFilter filter : filters)
			{
				Boolean containsBF = filter.contains(key);
				Boolean containsHS = set.contains(key);

				if (containsBF && containsHS)
				{
					filtersResults.get(filter.getName()).TP++;
				} else if (!containsBF && !containsHS)
				{
					filtersResults.get(filter.getName()).TN++;
				} else if (!containsBF && containsHS)
				{
					filtersResults.get(filter.getName()).FN++;
				} else if (containsBF && !containsHS)
				{
					filtersResults.get(filter.getName()).FP++;
				}
			}
		}

		for (IBloomFilter filter : filters)
		{
			Results r = filtersResults.get(filter.getName());

			System.out.println("------------------------------------------------");
			System.out.println(String.format("Results for [%s]", filter.getName()));
			System.out.println("TP = " + String.format("%6d", r.TP) + "\tTPR = " + String.format("%1.4f", (double) r.TP/ (double) n));
			System.out.println("TN = " + String.format("%6d", r.TN) + "\tTNR = " + String.format("%1.4f", (double) r.TN/ (double) (range-n)));
			System.out.println("FN = " + String.format("%6d", r.FN) + "\tFNR = " + String.format("%1.4f", (double) r.FN/ (double) (n)));
			System.out.println("FP = " + String.format("%6d", r.FP) + "\tFPR = " + String.format("%1.4f", (double) r.FP/ (double) (range-n)));
			System.out.println("------------------------------------------------");
		}
	}

	private static class Results{
		public int TP = 0;
		public int FP = 0;
		public int TN = 0;
		public int FN = 0;
	}

	private interface IBloomFilter{
		void add(int key);
		Boolean contains(int key);

		String getName();
	}

	private static class BigBloomFilter implements IBloomFilter {

		public BigBloomFilter(int size, int k){
			this.size = size;
			this.k = k;
			bitSet = new BitSet(size);
			coefficients = new ArrayList<>();
			Random random = new Random(0);
			p = BigInteger.valueOf((long) (Math.pow(2, 61) - 1));

			for (int i = 0; i < k; ++i)
			{
				coefficients.add(new BigInteger[k]);

				for (int j = 0; j < k; ++j)
					coefficients.get(i)[j] = BigInteger.valueOf(random.nextInt((int) Math.min((long)Integer.MAX_VALUE, p.longValue())));
		}
		}

		@Override
		public void add(int key){
			for (int i = 0; i < k; ++i){
				bitSet.set(getHashValue(i, key));
			}
		}

		@Override
		public Boolean contains(int key){
			for (int i = 0; i < k; ++i)
				if (!bitSet.get(getHashValue(i, key)))
					return false;

			return true;
		}

		@Override
		public String getName()
		{
			return "BigInteger bloom filter";
		}

		private int getHashValue(int i, int key){

			BigInteger sum = new BigInteger("0");
			BigInteger originalKey = BigInteger.valueOf(key);
			BigInteger actualKey = BigInteger.valueOf(key);

			for (int j = 0; j < k; ++j) {
				sum = sum.add(actualKey.multiply(coefficients.get(i)[j])).mod(p);
				actualKey = actualKey.multiply(originalKey);
			}

			return sum.mod(BigInteger.valueOf(size)).intValue();
		}

		private BitSet bitSet;
		private List<BigInteger[]> coefficients;
		private int size = 0;
		private int k = 0;
		private BigInteger p;
	}

	private static class FastBloomFilter implements IBloomFilter {

		public FastBloomFilter(int size, int k){
			this.size = size;
			this.k = k;
			bitSet = new BitSet(size);
			coefficients = new ArrayList<>();
			Random random = new Random(0);
			p = (long) (Math.pow(2, 61) - 1);

			for (int i = 0; i < k; ++i)
			{
				coefficients.add(new Integer[k]);

				for (int j = 0; j < k; ++j)
					coefficients.get(i)[j] = random.nextInt((int) Math.min((long)Integer.MAX_VALUE, p));
			}
		}

		@Override
		public void add(int key){
			for (int i = 0; i < k; ++i){
				bitSet.set(getHashValue(i, key));
			}
		}

		@Override
		public Boolean contains(int key){
			for (int i = 0; i < k; ++i)
				if (!bitSet.get(getHashValue(i, key)))
					return false;

			return true;
		}

		@Override
		public String getName()
		{
			return "K-size vector hashes bloom filter";
		}

		private int getHashValue(int i, int key){

			long sum = 0;

			for (int j = 0; j < k; ++j) {
				sum = (sum + (long) coefficients.get(i)[j] * (long)key % p);
			}

			return (int) (sum % (long)size);
		}

		private BitSet bitSet;
		private List<Integer[]> coefficients;
		private int size = 0;
		private int k = 0;
		private Long p;
	}

	private static class FastBloomFilter2 implements IBloomFilter {

		public FastBloomFilter2(int size, int k){
			this.size = size;
			this.k = k;
			bitSet = new BitSet(size);
			coefficients = new ArrayList<>();
			Random random = new Random(0);
			p = (long) (Math.pow(2, 61) - 1);

			for (int i = 0; i < k; ++i)
			{
				coefficients.add(new Integer[2]);

				for (int j = 0; j < 2; ++j)
					coefficients.get(i)[j] = Math.max(1, Math.abs(random.nextInt()));
			}
		}

		@Override
		public void add(int key){
			for (int i = 0; i < k; ++i){
				bitSet.set(getHashValue(i, key));
			}
		}

		@Override
		public Boolean contains(int key){
			for (int i = 0; i < k; ++i)
				if (!bitSet.get(getHashValue(i, key)))
					return false;

			return true;
		}

		@Override
		public String getName()
		{
			return "Pair hash bloom filter";
		}

		private int getHashValue(int i, int key){
			int val = (int) (((long)coefficients.get(i)[0] * (long)key + (long)coefficients.get(i)[1]) % size);

			if (val < 0)
				return 2;

			return val;
		}

		private BitSet bitSet;
		private List<Integer[]> coefficients;
		private int size = 0;
		private int k = 0;
		private Long p;
	}
}
