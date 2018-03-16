import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Scanner;

public class Simulator 
{
	private Register registers[] = new Register[32];
	private static final String INPUTFILE = "trace.txt";
	private static final String OUTPUTFILE = "statistics.txt";
	private static final boolean WINDOWSMODE = true;
	private long lastPC = 0;
	private long curPC = 0;
	private int totalInstructions = 0;
	private int rTypeInstructions = 0;
	private int iTypeInstructions = 0;
	private int jTypeInstructions = 0;
	private int forwardBranches = 0;
	private int backwardBranches = 0;
	private int noBranches = 0;
	private int numLoads = 0;
	private int numStores = 0;
	private boolean prevBranch = false;

	public static void main(String[] args) 
	{
		new Simulator();
	}
	
	public Simulator()
	{
		for (int i = 0; i < registers.length; i++)
		{
			registers[i] = new Register(i);
		}
		openTraceFile();
		writeStatsFile();
	}
	
	private void writeStatsFile() 
	{
		try
		{
			File target = new File(OUTPUTFILE);
			if (target.exists())
			{
				target.delete();
			}
			OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(target), "UTF-8");
			fw.write("insts: " + totalInstructions + (WINDOWSMODE ? "\r\n" : "\n"));
			fw.write("r-type: " + rTypeInstructions + (WINDOWSMODE ? "\r\n" : "\n"));
			fw.write("i-type: " + iTypeInstructions + (WINDOWSMODE ? "\r\n" : "\n"));
			fw.write("j-type: " + jTypeInstructions + (WINDOWSMODE ? "\r\n" : "\n"));
			fw.write("fwd-taken: " + forwardBranches + (WINDOWSMODE ? "\r\n" : "\n"));
			fw.write("bkw-taken: " + backwardBranches + (WINDOWSMODE ? "\r\n" : "\n"));
			fw.write("not-taken: " + noBranches + (WINDOWSMODE ? "\r\n" : "\n"));
			fw.write("loads: " + numLoads + (WINDOWSMODE ? "\r\n" : "\n"));
			fw.write("stores: " + numStores + (WINDOWSMODE ? "\r\n" : "\n"));;
			for (Register r: registers)
			{
				fw.write(r.toString() + (WINDOWSMODE ? "\r\n" : "\n"));
			}
			fw.flush();
			fw.close();
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
	}

	private void openTraceFile()
	{
		File target = new File(INPUTFILE);
		if (target.exists())
		{
			try
			{
				Scanner scan = new Scanner(target);
				while (scan.hasNext())
				{
					parseInstruction(scan.nextLong(16), scan.nextLong(16));
					totalInstructions++;
				}
				scan.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	private void parseInstruction(long counter, long instruction) 
	{
		lastPC = curPC;
		curPC = counter;
		if (prevBranch == true)
		{
			if (curPC == lastPC + 4)
			{
				noBranches++;
			}
			else if (curPC > lastPC + 4)
			{
				forwardBranches++;
			}
			else if (curPC < lastPC + 4)
			{
				backwardBranches++;
			}
			
			prevBranch = false;
		}
		instructionDecode(instruction);
		
	}

	private void instructionDecode(long instruction) 
	{
		long opCode = instruction >>> 26;
		long rt = 0;
		long rd = 0;
		long rs = 0;
		Register r;
		if (opCode == 0)
		{
			rTypeInstructions++;
			long funct = instruction & 0x0000003FL;
			if (funct == 0L || funct == 0x00000002L || funct == 0x00000003L) 
			{
				//SLL & SRL, SRA
				rd = instruction >>> 11;
				rd = rd & 0x0000001FL;
				r = registers[(int) rd];
				r.numWrites++;
				rt = instruction >>> 16;
				rt = rt & 0x0000001FL;
				r = registers[(int) rt];
				r.numReads++;
			}
			else if (funct == 0x00000020L || funct == 0x00000021L || funct == 0x00000024L
					|| funct == 0x00000027L || funct == 0x00000025L || funct == 0x0000002AL 
					|| funct == 0x0000002BL || funct == 0x00000022L || funct == 0x00000023L)
			{
				//ADD, ADDU, AND, NOR, OR, SLT, SLTU, SUB, SUBU
				rs = instruction >>> 21;
				rs = rs & 0x0000001FL;
				r = registers[(int) rs];
				r.numReads++;
				rt = instruction >>> 16;
				rt = rt & 0x0000001FL;
				r = registers[(int) rt];
				r.numReads++;
				rd = instruction >>> 11;
				rd = rd & 0x0000001FL;
				r = registers[(int) rd];
				r.numWrites++;
			}
			else if (funct == 0x00000008L)
			{
				//JR
				rs = instruction >>> 21;
				rs = rs & 0x0000001FL;
				r = registers[(int) rs];
				r.numReads++;
				prevBranch = true;
			}
		}
		else if (opCode == 0x00000002L || opCode == 0x00000003L)
		{
			//J, JAL
			jTypeInstructions++;
			prevBranch = true;
		}
		else
		{
			iTypeInstructions++;
			if (opCode == 0x00000008L || opCode == 0x00000009L || opCode == 0x0000000CL
					|| opCode == 0x0000000DL || opCode == 0x0000000AL || opCode == 0x0000000BL)
			{
				//ADDI, ADDIU, ANDI, ORI, SLTI, SLTIU
				rs = instruction >>> 21;
				rs = rs & 0x0000001FL;
				r = registers[(int) rs];
				r.numReads++;
				rt = instruction >>> 16;
				rt = rt & 0x0000001FL;
				r = registers[(int) rt];
				r.numWrites++;
			}
			else if (opCode == 0x0000004L || opCode == 0x00000005L)
			{
				//BEQ, BNE
				rs = instruction >>> 21;
				rs = rs & 0x0000001FL;
				r = registers[(int) rs];
				r.numReads++;
				rt = instruction >>> 16;
				rt = rt & 0x0000001FL;
				r = registers[(int) rt];
				r.numReads++;
				prevBranch = true;
				
			}
			else if (opCode == 0x0000024L || opCode == 0x00000025L || opCode == 0x00000023L)
			{
				//LBU, LHU, LW
				numLoads++;
				rs = instruction >>> 21;
				rs = rs & 0x0000001FL;
				r = registers[(int) rs];
				r.numReads++;
				rt = instruction >>> 16;
				rt = rt & 0x0000001FL;
				r = registers[(int) rt];
				r.numWrites++;
			}
			else if (opCode == 0x0000000FL)
			{
				//LUI
				rt = instruction >>> 16;
				rt = rt & 0x0000001FL;
				r = registers[(int) rt];
				r.numWrites++;
			}
			else if (opCode == 0x00000028L || opCode == 0x00000029L || opCode == 0x0000002BL)
			{
				//SB, SW, SH
				numStores++;
				rs = instruction >>> 21;
				rs = rs & 0x0000001FL;
				r = registers[(int) rs];
				r.numReads++;
				rt = instruction >>> 16;
				rt = rt & 0x0000001FL;
				r = registers[(int) rt];
				r.numReads++;
			}
		}
		
	}

	private class Register
	{
		private int numReads = 0;
		private int numWrites = 0;
		private int regNum;
		
		public Register(int reg)
		{
			regNum = reg;
		}
		
		@Override
		public String toString()
		{
			return new String("reg-" + regNum + ": " + numReads + " " + numWrites);
		}
	}

}
