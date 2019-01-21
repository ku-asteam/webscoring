import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

/**
 * This class computes PRank matrix form  
 * @author masoud
 *
 */
public class PRankMatrixForm_ThreadEdition extends Thread
{
	
	public static File file;
	public static Path writeInfo;
	public static OutputStream out;
	public static BufferedWriter writer;
	static float [][] adjacencyMatrix;//keeps adjacency matrix and then transition matrix
	public static DataPreparing dataPR;
	public static ArrayList<String> papersList;
	public static int totalNoneZero;
	

	public static	String[] gtSets = {"01.txt","02.txt","03.txt","04.txt","05.txt","06.txt","07.txt","08.txt","09.txt","10.txt","11.txt"};
	public static ArrayList<Integer> gtSetMembers = new ArrayList<>(); //Holds the ground truth papers 
	public static Path gtSetPath;
	public static InputStream gtSetIn;
	public static BufferedReader gtSetReader;
	public static int size = 0;
	
	public static float similarity[][];
	public static int iteration = 0;
	public static String type =  "";	
	
	/**
	 * Keep the compressed simRank matrix
	 */
	public static float[] SimRank_val = new float[totalNoneZero];
	public static int[] SimRank_row_idx = new int [totalNoneZero];
	public static int[] SimRank_col_ptr = new int [size+1];

	/**
	 * Keep the compressed rvsSimRank matrix
	 */
	public static float[] rvsSimRank_val = new float[totalNoneZero];
	public static int[] rvsSimRank_row_idx = new int [totalNoneZero];
	public static int[] rvsSimRank_col_ptr = new int [size+1];
	
	/**
	 * indicates the action of the class 1- compute JPrank or 2- write file
	 */
	public static String action = "";

	public static float [][] SimRank_temp;	//Keep the result of Q^T*S for SimRank	
	public static float [][] rvsSimRank_temp;	//Keep the result of Q^T*S for rvsSimRank
	
	public static float alpha = 0;
	public static float cons = 0;
	
	public int	ThreadNo; // indicates the number of the current thread
	public int	ThreadCount; // keeps the total number of threads
	public int  currentIteration = 0; // keeps current iteration to pass to the WtiteInfo() method
	
	public int 	nonZeroSimilarity_perThread = 0; //keeps non zero similarity scores for a thread
	static public int 	nonZeroSimilarity = 0; //keeps total non zero similarity scores for an iteration

	/**
	 * @param requiredAction : indicates the required action must taken by the class: 
	 * 	"do Prank" : computes the similarity
	 *  "write gt"  : writes similarity measures only for nodes in ground truth sets 
	 *  "write all" : writes similarity measures for all nodes
	 */
	public PRankMatrixForm_ThreadEdition(String requiredAction, int	ThreadNo,int ThreadCount, float alpha, float decayFa)
	{
		action = requiredAction;
		this.ThreadCount = ThreadCount;
		this.ThreadNo = ThreadNo;
		this.alpha = alpha;
		this.cons = decayFa;
	}
	
	/**
	 * @param requiredAction : indicates the required action must taken by the class: "do JPrank" or "write file"
	 * @param keeps current iteration to pass to the WtiteInfo() method
	 */
	public PRankMatrixForm_ThreadEdition (String requiredAction, int currentIteration, int nonZeroSimilarity)
	{
		action = requiredAction;
		this.nonZeroSimilarity = nonZeroSimilarity;
		this.currentIteration = currentIteration;
	}

	/**
	 * This method  1) initials the static variable used in all instances of the class;
	 * 				2) puts ground truth set nodes in a list;
	 * 			  	3) compressed adjacency matrix based on both inlinks and outlinks;
	 * @param graph : main graph is based on out-links
	 * @param rvsGraph : reverse graph is based on in-links
	 */
	public static void initialization (String graph, String rvsGraph, int totalIteration, String dataType, boolean weightedGraph)
	{
		dataPR = new DataPreparing();
		papersList = dataPR.papersList;
		size = papersList.size();
		
		//Puts ground truth papers in the set
		for (int gt=0; gt<gtSets.length;gt++)
		{
			try
			{
				gtSetPath = FileSystems.getDefault().getPath("ground_truth/"+gtSets[gt]); 
				gtSetIn = Files.newInputStream(gtSetPath);
				gtSetReader = new BufferedReader(new InputStreamReader(gtSetIn));
				String line = null;				
				while ((line=gtSetReader.readLine())!=null)
					gtSetMembers.add(papersList.indexOf(line.trim()));
				gtSetReader.close();
				gtSetIn.close();											
			}
			catch(Exception ex)
			{
				ex.printStackTrace();
			}
		}

		System.out.println("The ground truth nodes maped in the list!");
		System.out.println();

		//making CSC representation of adjacency matrix for SimRank Part
		adjacencyMatrix = new float [papersList.size()][papersList.size()];
		if (!weightedGraph)
			dataPR.prepareUnweightedAdjacencyMatrix(graph, adjacencyMatrix); // main graph which is based on out-links
		else 
			dataPR.prepareWeightedAdjacencyMatrix(graph, adjacencyMatrix); // main graph which is based on out-links

		dataPR.prepareColNormTransitionMatrix(adjacencyMatrix);
		totalNoneZero = dataPR.getTotalNoneZero();
		SimRank_val = new float[totalNoneZero];
		SimRank_row_idx = new int [totalNoneZero];
		SimRank_col_ptr = new int [size+1];
		CompressedSparseColumn obj = new CompressedSparseColumn();
		obj.CSC(adjacencyMatrix, SimRank_val, SimRank_row_idx, SimRank_col_ptr);
		adjacencyMatrix =null;

		System.out.println("The adjacency matrix for SimRank part is calculated and copressed!");
		System.out.println();

		//making CSC representation of adjacency matrix for rvs-SimRank Part		
		adjacencyMatrix = new float [papersList.size()][papersList.size()];
		if (!weightedGraph)
			dataPR.prepareUnweightedAdjacencyMatrix(rvsGraph, adjacencyMatrix); // reverse graph which is based on in-links
		else 
			dataPR.prepareWeightedAdjacencyMatrix(rvsGraph, adjacencyMatrix); // reverse graph which is based on in-links		

		dataPR.prepareColNormTransitionMatrix(adjacencyMatrix);
		totalNoneZero = dataPR.getTotalNoneZero();
		rvsSimRank_val = new float[totalNoneZero];
		rvsSimRank_row_idx = new int [totalNoneZero];
		rvsSimRank_col_ptr = new int [size+1];
		obj.CSC(adjacencyMatrix, rvsSimRank_val, rvsSimRank_row_idx, rvsSimRank_col_ptr);
		adjacencyMatrix = null;

		System.out.println("The adjacency matrix for rvsSimRank part is calculated and copressed!");
		System.out.println();

		iteration = totalIteration;
		type = dataType;
		
	}
	/**
	 * computes P-Rank by using compressed sparse column (CSC) of transition matrix in multiplication. 
	 * It saves similarity information of ONLY ground truth papers in the file per each iteration.
	 * @param graph: main graph is based on out-links
	 * @param rvsGraph: reverse graph is based on in-links 
	 * @param weightedGraph; for a weighted graph boolean false should be true
	 * @param alpha
	 * @param iteration
	 * @param type
	 */
	public void PrankCSC_ResultOnGT()
	{

		//computes Q^T*S by using CSC form of Q
		for (int i=ThreadNo;i<size;i+=ThreadCount)
			for (int j=0;j<size;j++)
			{
				for (int k=SimRank_col_ptr[i];k<SimRank_col_ptr[i+1];k++)
				{
					SimRank_temp[i][j] = SimRank_temp[i][j] + cons*SimRank_val[k]*similarity[SimRank_row_idx[k]][j]; 
				}

				for (int k=rvsSimRank_col_ptr[i];k<rvsSimRank_col_ptr[i+1];k++)
				{
					rvsSimRank_temp[i][j] = rvsSimRank_temp[i][j] + cons*rvsSimRank_val[k]*similarity[rvsSimRank_row_idx[k]][j]; 
				}
			}

		//computes Temp1*Q by using CSC form of Q
		float SimRanksum = 0.0f;
		float rvsSimRanksum = 0.0f;
		for (int i=0;i<size;i++)
			for (int j=ThreadNo;j<size;j+=ThreadCount)
			{
				for (int k=SimRank_col_ptr[i];k<SimRank_col_ptr[i+1];k++)
				{
					SimRanksum = SimRanksum + SimRank_val[k]*SimRank_temp[j][SimRank_row_idx[k]]; 
				}

				for (int k=rvsSimRank_col_ptr[i];k<rvsSimRank_col_ptr[i+1];k++)
				{
					rvsSimRanksum = rvsSimRanksum + rvsSimRank_val[k]*rvsSimRank_temp[j][rvsSimRank_row_idx[k]]; 
				}

				if(i==j)	//we set the similarity(i,i)=1
					similarity[i][j] = 1.0f;
				else
					similarity[i][j] = alpha*SimRanksum + (1-alpha)*rvsSimRanksum;

				if(similarity[i][j]!=0.0)
					nonZeroSimilarity_perThread++;
				SimRanksum = 0;
				rvsSimRanksum = 0;
			}
	}
	
	/*
	 * Writing the results of an iteration in a file
	 */
	public void PrankCSC_GT_WriteFile()
	{
		try
		{
			file = new File(type+"_PRank_IT_"+currentIteration);
			if( file.exists())
				file.delete();
			writeInfo = FileSystems.getDefault().getPath(file.getPath());
			Files.createFile(writeInfo);
			out = Files.newOutputStream(writeInfo);
			writer = new BufferedWriter(new OutputStreamWriter(out));
			//writing # of none zero similarities in the first line of the file
			writer.write("NoneZeroSimilarity: "+nonZeroSimilarity);
			writer.newLine();											

			for (int i=0;i<gtSetMembers.size();i++)
			{
				for (int j=0;j<size;j++)
				{						
					if (similarity[gtSetMembers.get(i)][j]!=0.0)
					{
						BigDecimal bd = new BigDecimal(similarity[gtSetMembers.get(i)][j]);
						bd = bd.setScale(5,BigDecimal.ROUND_UP);
						writer.write(gtSetMembers.get(i)+","+j+","+bd);
						writer.newLine();							
					}							
				}
			}
			System.out.println("The result of P-Rank with dataset "+type+" on iteration "+currentIteration+" ONLY for papers in ground truth is written in the file!");
			writer.close();
			out.close();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	/*
	 * Writing the results of an iteration in a file
	 */
	public void PrankCSC_WriteFile()
	{
		try
		{
			file = new File(type+"_PRank_IT_"+currentIteration);
			if( file.exists())
				file.delete();
			writeInfo = FileSystems.getDefault().getPath(file.getPath());
			Files.createFile(writeInfo);
			out = Files.newOutputStream(writeInfo);
			writer = new BufferedWriter(new OutputStreamWriter(out));
			//writing # of none zero similarities in the first line of the file
			writer.write("NoneZeroSimilarity: "+nonZeroSimilarity);
			writer.newLine();											
			for (int i=0;i<size;i++)
			{
				for (int j=0;j<size;j++)
				{
					if (similarity[i][j]!=0.0)
					{
						BigDecimal bd = new BigDecimal(similarity[i][j]);
						bd = bd.setScale(5,BigDecimal.ROUND_UP);
						writer.write(i+","+j+","+bd);
						writer.newLine();							
					}							
				}
			}
			System.out.println("The result of P-Rank with dataset "+type+" on iteration "+currentIteration+" is written in the file!");
			writer.close();
			out.close();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}

	public void run()
	{
		if (action.equals("do Prank"))
			PrankCSC_ResultOnGT();
		
		if (action.equals("write file"))
			PrankCSC_GT_WriteFile();		
	}

	public static void main(String argv[]) throws InterruptedException
	{
		/**
		 * NOTICE:		1- Set the number of Iteration		2- Set the Type		3- indicate if the graph are weighted 
		 */
		initialization("unweighted_graph.txt", "rvs_unweighted_graph.txt", 4, "unweighted_DBLP", false);

		for (float alphaPrank=0.5f; alphaPrank<0.6f;alphaPrank+=0.1f)
		{
			System.out.println();
			similarity = new float[size][size];
			for ( int i = 0 ; i < size ; i++ )
				similarity[i][i]=1;
					
			for(int iter=1; iter<=iteration;iter++)
			{  				      	     		
				SimRank_temp = new float[size][size];
				rvsSimRank_temp = new float[size][size];
	
				PRankMatrixForm_ThreadEdition thread1 = new PRankMatrixForm_ThreadEdition("do Prank", 0, 5, alphaPrank, 0.8f);
				PRankMatrixForm_ThreadEdition thread2 = new PRankMatrixForm_ThreadEdition("do Prank", 1, 5, alphaPrank, 0.8f);
				PRankMatrixForm_ThreadEdition thread3 = new PRankMatrixForm_ThreadEdition("do Prank", 2, 5, alphaPrank, 0.8f);
				PRankMatrixForm_ThreadEdition thread4 = new PRankMatrixForm_ThreadEdition("do Prank", 3, 5, alphaPrank, 0.8f);
				PRankMatrixForm_ThreadEdition thread5 = new PRankMatrixForm_ThreadEdition("do Prank", 4, 5, alphaPrank, 0.8f);
	
				thread1.start();
				thread2.start();
				thread3.start();
				thread4.start();
				thread5.start();
				
				thread1.join();
				thread2.join();
				thread3.join();
				thread4.join();
				thread5.join();
				
				int nonZeroSimilarity = thread1.nonZeroSimilarity_perThread +thread2.nonZeroSimilarity_perThread+thread3.nonZeroSimilarity_perThread+thread4.nonZeroSimilarity_perThread+thread5.nonZeroSimilarity_perThread; 
					
				PRankMatrixForm_ThreadEdition thread6 = new PRankMatrixForm_ThreadEdition("write file", iter, nonZeroSimilarity);
				thread6.start();
				thread6.join();
				
				SimRank_temp = null;	 
				rvsSimRank_temp = null;	
			}
		similarity = null;
		}
	}
}
