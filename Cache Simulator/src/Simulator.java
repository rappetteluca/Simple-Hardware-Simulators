import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.util.Scanner;

public class Simulator 
{
	//Definitions
	private static final String PARAMETER_FILE = "parameters.txt";
	private static final String MEMORY_FILE = "accesses.txt";
	private static final String OUTPUT_FILE = "statistics.txt";
	private static final String WRITE_ALLOCATE = "wa";
	private static final String WRITE_NO_ALLOCATE = "wna";
	private static final String WRITE_THROUGH = "wt";
	private static final String WRITE_BACK = "wb";
	private static final char CACHE_READ = 'r';
	private static final char CACHE_WRITE = 'w';
	
	//Parameters
	private int totalCapacity;
	private int numWays;
	private int numSets;
	private int blockSize;
	private int indexBits;
	private int offsetBits;
	private CacheSet[] cacheSets;
	private String allocPolicy;
	private String writePolicy;
	private long offsetMask;
	private long indexMask;
	
	//Statistics
	private int numRHits = 0;
	private int numRMisses = 0;
	private int numWHits = 0;
	private int numWMisses = 0;
	private int numWriteBacks = 0;
	private int numWriteThroughs = 0;
	private int totalAccesses = 0;
	private double hitRate = 0.;
	DecimalFormat hitRateFormat = new DecimalFormat("#.######");

	public static void main(String[] args) 
	{
		new Simulator();
	}
	
	public Simulator()
	{
		openParameterFile();
		openAccessFile();
		writeStatsFile();
	}
	
	private void writeStatsFile() 
	{
		try
		{
			File target = new File(OUTPUT_FILE);
			if (target.exists())
			{
				target.delete();
			}
			hitRate = (double)(numRHits + numWHits) / (double)(totalAccesses);
			OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(target), "UTF-8");
			fw.write("rhits: " + numRHits + "\n");
			fw.write("whits: " + numWHits + "\n");
			fw.write("rmisses: " + numRMisses + "\n");
			fw.write("wmisses: " + numWMisses + "\n");
			fw.write("hrate: " + hitRateFormat.format(hitRate) + "\n");
			fw.write("wb: " + numWriteBacks + "\n");
			fw.write("wt: " + numWriteThroughs + "\n");
			fw.flush();
			fw.close();
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
	}

	private void openParameterFile()
	{
		File target = new File(PARAMETER_FILE);
		if (target.exists())
		{
			try
			{
				Scanner scan = new Scanner(target);
				String input = scan.nextLine();
				numWays = Integer.parseInt(input); //Direct-Mapped = 1, 2 Way = 2.
				
				input = scan.nextLine();
				offsetBits = Integer.parseInt(input);
				blockSize = (int) Math.pow(2.0, offsetBits);
				
				input = scan.nextLine();
				indexBits = Integer.parseInt(input);
				numSets = (int) Math.pow(2.0, indexBits);
				
				allocPolicy = scan.nextLine();
				
				writePolicy = scan.nextLine();
				scan.close();
				
				
				totalCapacity = numSets * blockSize * numWays;
				cacheSets = new CacheSet[numSets];
				for (int i = 0; i < numSets; i++)
				{
					cacheSets[i] = new CacheSet(numWays);
				}
				offsetMask = (0x1L << (offsetBits - 0)) - 1;
				indexMask = (0x1L << (indexBits - 0)) - 1;
				//System.out.printf("Total Capacity: %d\n", totalCapacity);
				//System.out.printf("Blocksize: %d\n", blockSize);
				//System.out.printf("Num sets: %d\n", numSets);
				//System.out.printf("index Bits: %d\n", indexBits);
				//System.out.printf("offset Bits: %d\n", offsetBits);
				//System.out.printf("Allocation Policy: %s\n", allocPolicy);
				//System.out.printf("Write Policy: %s\n", writePolicy);
				
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	private void openAccessFile()
	{
		File target = new File(MEMORY_FILE);
		if (target.exists())
		{
			try
			{
				Scanner scan = new Scanner(target);
				while (scan.hasNext())
				{
					String s = scan.nextLine();
					parseAccess(s);
					totalAccesses++;
				}
				scan.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	private void parseAccess(String s) 
	{
		Scanner scan = new Scanner(s);
		char AccessType = scan.next().charAt(0);
		long addr = scan.nextLong(16);
		scan.close();
		int index = (int) ((addr >>> offsetBits) & indexMask);
		int offset = (int) (addr & offsetMask);
		long tag = addr >>> (offsetBits + indexBits);
		
		//System.out.println(AccessType + " : " + addr + " Index: " + index + " Offset: " + offset + " Tag: " + tag);
		
		CacheBlock blockToFind = cacheSets[index].get(tag);
		if (AccessType == CACHE_READ)
		{
			if (blockToFind != null)
			{
				numRHits++;
			}
			else
			{
				numRMisses++;
				cacheSets[index].allocate(tag, AccessType);
			}
		}
		else if (AccessType == CACHE_WRITE)
		{
			if (writePolicy.equals(WRITE_THROUGH))
			{
				//Every Write is Write Through
				numWriteThroughs++;
			}
			if (blockToFind != null)
			{
				numWHits++;
				blockToFind.isDirty = true;
			}
			else
			{
				numWMisses++;
				if (allocPolicy.equals(WRITE_ALLOCATE))
				{
					cacheSets[index].allocate(tag, AccessType);
				}
				else if (allocPolicy.equals(WRITE_NO_ALLOCATE) && writePolicy.equals(WRITE_BACK))
				{
					//Write-No-Allocate, Miss Occurs.
					//Write instruction goes to main memory
					numWriteThroughs++;
				}
			}
		}
		
	}
	
	
	private class CacheSet
	{
		private CacheBlock[] blocks; //No Data storing so no blockSize needed.
		private int associativity;
		private int nextReplacementIndex = 0;
		
		public CacheSet(int numWays)
		{
			associativity = numWays;
			blocks = new CacheBlock[associativity];
			
			for (int i = 0; i < associativity; i++)
			{
				blocks[i] = new CacheBlock();
			}
		}
		
		public CacheBlock get(long tag)
		{
			for (int i = 0; i < blocks.length; i++)
			{
				if(blocks[i].isValid)
				{
					if(blocks[i].tag == tag)
					{
						return blocks[i];
					}
				}
			}
			
			return null;
		}
		
		public void allocate(long tag, char AccessType)
		{
			if (AccessType == CACHE_READ)
			{
				blocks[nextReplacementIndex].setBlockR(tag);
			}
			else
			{
				blocks[nextReplacementIndex].setBlockW(tag);
			}
			
			nextReplacementIndex = (nextReplacementIndex + 1) % associativity; //FIFO
		}
		
	}
	
	private class CacheBlock
	{
		private boolean isValid;
		private boolean isDirty;
		private long tag;
		
		public CacheBlock()
		{
			isValid = false;
			isDirty = false;
			tag = 0x0L;
		}
		
		public void setBlockR(long tag)
		{
			isValid = true;
			if (isDirty == true)
			{
				if(writePolicy.equals(WRITE_BACK))
				{
					numWriteBacks++;
				}
			}
			isDirty = false;
			this.tag = tag;
		}
		
		public void setBlockW(long tag)
		{
			isValid = true;
			if (isDirty == true)
			{
				if(writePolicy.equals(WRITE_BACK))
				{
					numWriteBacks++;
				}
			}
			isDirty = true;
			this.tag = tag;
		}
		
	}
}
