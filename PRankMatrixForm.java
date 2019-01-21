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
 * This class computes PRank based on matrix form S=C.(Q^T*S*Q)VI(n,n)  
 * @author masoud
 *
 */
public class PRankMatrixForm
{
	public File file;
	public Path writeInfo;
	public OutputStream out;
	public BufferedWriter writer;
	float [][] adjacencyMatrix;//keeps adjacency matrix and then transition matrix
	public DataPreparing dataPR;
	public ArrayList<String> papersList;
	public int totalNoneZero;

	public PRankMatrixForm()
	{
		dataPR = new DataPreparing();
		papersList = dataPR.papersList;
	}

	/**
	 * computes P-Rank by using compressed sparse column (CSC) of transition matrix in multiplication 
	 * saves information in file per each iteration 
	 * @param graph: main graph is based on out-links
	 * @param rvsGraph: reverse graph is based on in-links
	 * @param weightedGraph; for a weighted graph, false boolean should set as true
	 * @param alpha
	 * @param iteration
	 * @param type
	 */
	public void PrankCSC(String graph, String rvsGraph, boolean weightedGraph, float alpha, int iteration, String type)
	{
		totalNoneZero = dataPR.getTotalNoneZero();

		float cons = 0.8f;
		int size = papersList.size();

		//making CSC representation of adjacency matrix for SimRank Part
		adjacencyMatrix = new float [papersList.size()][papersList.size()];
		if (!weightedGraph)
			dataPR.prepareUnweightedAdjacencyMatrix(graph, adjacencyMatrix); // main graph which is based on out-links
		else 
			dataPR.prepareWeightedAdjacencyMatrix(graph, adjacencyMatrix); // main graph which is based on out-links

		dataPR.prepareColNormTransitionMatrix(adjacencyMatrix);
		totalNoneZero = dataPR.getTotalNoneZero();

		//compressed matrix form for SimRank
		float[] SimRank_val = new float[totalNoneZero];
		int[] SimRank_row_idx = new int [totalNoneZero];
		int[] SimRank_col_ptr = new int [size+1];
		CompressedSparseColumn obj = new CompressedSparseColumn();
		obj.CSC(adjacencyMatrix, SimRank_val, SimRank_row_idx, SimRank_col_ptr);



		//making CSC representation of adjacency matrix for rvs-SimRank Part		
		for (int i=0; i<papersList.size();i++)
			for (int j=0; j<papersList.size();j++)
				adjacencyMatrix[i][j] = 0;

		if (!weightedGraph)
			dataPR.prepareUnweightedAdjacencyMatrix(rvsGraph, adjacencyMatrix); // reverse graph which is based on in-links
		else 
			dataPR.prepareWeightedAdjacencyMatrix(rvsGraph, adjacencyMatrix); // reverse graph which is based on in-links		

		dataPR.prepareColNormTransitionMatrix(adjacencyMatrix);
		totalNoneZero = dataPR.getTotalNoneZero();

		//compressed matrix form for rvs-SimRank
		float[] rvsSimRank_val = new float[totalNoneZero];
		int[] rvsSimRank_row_idx = new int [totalNoneZero];
		int[] rvsSimRank_col_ptr = new int [size+1];
		obj.CSC(adjacencyMatrix, rvsSimRank_val, rvsSimRank_row_idx, rvsSimRank_col_ptr);

		adjacencyMatrix = null;		

		float similarity[][] = new float[size][size];
		for ( int i = 0 ; i < size ; i++ )
			similarity[i][i]=1;

		for(int iter=1; iter<=iteration;iter++)
		{	      		      
			float SimRank_temp[][] = new float[size][size];	//Keep the result of Q^T*S for SimRank
			float rvsSimRank_temp[][] = new float[size][size];	//Keep the result of Q^T*S for rvsSimRank

			//computes Q^T*S by using CSC form of Q
			for (int i=0;i<size;i++)
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
				for (int j=0;j<size;j++)
				{
					for (int k=SimRank_col_ptr[i];k<SimRank_col_ptr[i+1];k++)
					{
						SimRanksum = SimRanksum + SimRank_val[k]*SimRank_temp[j][SimRank_row_idx[k]]; 
					}

					for (int k=rvsSimRank_col_ptr[i];k<rvsSimRank_col_ptr[i+1];k++)
					{
						rvsSimRanksum = rvsSimRanksum + rvsSimRank_val[k]*rvsSimRank_temp[j][rvsSimRank_row_idx[k]]; 
					}

					similarity[i][j] = alpha*SimRanksum + (1-alpha)*rvsSimRanksum;
					SimRanksum = 0;
					rvsSimRanksum = 0;
				}

			//we set the similarity(i,i)=1
			for (int i=0;i<size;i++)
			{
				similarity[i][i]=1.0f;
			}
			try
			{
				file = new File(type+"_PRank_IT_"+iter);
				if( file.exists())
					file.delete();
				writeInfo = FileSystems.getDefault().getPath(file.getPath());
				Files.createFile(writeInfo);
				out = Files.newOutputStream(writeInfo);
				writer = new BufferedWriter(new OutputStreamWriter(out));
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
				System.out.println("The result of P-Rank with dataset "+type+" on iteration "+iter+" is written in the file!");
				writer.close();
				out.close();
			}
			catch(Exception ex)
			{
				ex.printStackTrace();
			}
		}
	}	
	
	/**
	 * Computes the required time to execute P-Rank by using compressed sparse column (CSC) of transition matrix in multiplication  
	 * @param graph: main graph is based on out-links
	 * @param rvsGraph: reverse graph is based on in-links
	 * @param weightedGraph
	 * @param alpha
	 * @param totalIteration
	 * @param type
	 */
	public void PrankCSC_Timer(String graph, String rvsGraph, float alpha, int totalIteration, String type)
	{
		float cons = 0.8f;
		int size = papersList.size();
		totalNoneZero = dataPR.getTotalNoneZero();

		long startTime = 0;	// also captures the memory allocation time for first iteration;
		long stopTime = 0;
		long elapsedTimeCurrentIT = 0;
		long elapsedTimeLastIT = 0;
		long elapsedTimeAdjacencyMatrix = 0;	// captures the time for preparing the adjacency matrices and saving them a compressed matrix;

		try
		{
			file = new File("TIMER_"+type+"_PRank");					
			if( file.exists())
				file.delete();
			writeInfo = FileSystems.getDefault().getPath(file.getPath());
			Files.createFile(writeInfo);
			out = Files.newOutputStream(writeInfo);
			writer = new BufferedWriter(new OutputStreamWriter(out));
			writer.write("Performance Information for P-Rank Matrix Form (In Minutes):");
			writer.newLine();											
			writer.newLine();											

			startTime = System.currentTimeMillis();
			/*
			 * 1- making adjacency matrix for SimRank Part,
			 * 2- normalizing it on columns and making the appropriate transition matrix
			 * 3- Transforming the transition matrix to Compress Sparse Column form   
			 */
			adjacencyMatrix = new float [papersList.size()][papersList.size()];
			dataPR.prepareUnweightedAdjacencyMatrix(graph, adjacencyMatrix); // main graph which is based on out-links
			dataPR.prepareColNormTransitionMatrix(adjacencyMatrix);
			totalNoneZero = dataPR.getTotalNoneZero();
			//compressed matrix form for SimRank
			float[] SimRank_val = new float[totalNoneZero];
			int[] SimRank_row_idx = new int [totalNoneZero];
			int[] SimRank_col_ptr = new int [size+1];
			CompressedSparseColumn obj = new CompressedSparseColumn();
			obj.CSC(adjacencyMatrix, SimRank_val, SimRank_row_idx, SimRank_col_ptr);

			/*
			 * 1- making adjacency matrix for rvs-SimRank Part,
			 * 2- normalizing it on columns and making the appropriate transition matrix
			 * 3- Transforming the transition matrix to Compress Sparse Column form   
			 */
			for (int i=0; i<papersList.size();i++)
				for (int j=0; j<papersList.size();j++)
					adjacencyMatrix[i][j] = 0;
			dataPR.prepareUnweightedAdjacencyMatrix(rvsGraph, adjacencyMatrix); // reverse graph which is based on in-links
			dataPR.prepareColNormTransitionMatrix(adjacencyMatrix);
			totalNoneZero = dataPR.getTotalNoneZero();
			float[] rvsSimRank_val = new float[totalNoneZero];
			int[] rvsSimRank_row_idx = new int [totalNoneZero];
			int[] rvsSimRank_col_ptr = new int [size+1];
			obj.CSC(adjacencyMatrix, rvsSimRank_val, rvsSimRank_row_idx, rvsSimRank_col_ptr);
			adjacencyMatrix = null;		

			stopTime = System.currentTimeMillis();
			elapsedTimeAdjacencyMatrix = stopTime - startTime;
			writer.write("The Execution Time for creating adjacency matrices and their compressing is: "+(elapsedTimeAdjacencyMatrix*0.001)/(float)60); 
			writer.newLine();
			writer.newLine();
			System.out.println("Execution Time for creating adjacency matrices and their compressign is: "+(elapsedTimeAdjacencyMatrix*0.001)/(float)60);
			elapsedTimeLastIT = elapsedTimeLastIT + elapsedTimeAdjacencyMatrix;	// creating adjacency matrix time is added to the time of first iteration

			
			float similarity[][] = new float[size][size];
			for ( int i = 0 ; i < size ; i++ )
				similarity[i][i]=1;

			startTime = System.currentTimeMillis();
			for(int iteration=1; iteration<=totalIteration;iteration++)
			{	      		      
				float SimRank_temp[][] = new float[size][size];	//Keep the result of Q^T*S for SimRank
				float rvsSimRank_temp[][] = new float[size][size];	//Keep the result of Q^T*S for rvsSimRank

				//computes Q^T*S by using CSC form of Q
				for (int i=0;i<size;i++)
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
					for (int j=0;j<size;j++)
					{
						for (int k=SimRank_col_ptr[i];k<SimRank_col_ptr[i+1];k++)
						{
							SimRanksum = SimRanksum + SimRank_val[k]*SimRank_temp[j][SimRank_row_idx[k]]; 
						}

						for (int k=rvsSimRank_col_ptr[i];k<rvsSimRank_col_ptr[i+1];k++)
						{
							rvsSimRanksum = rvsSimRanksum + rvsSimRank_val[k]*rvsSimRank_temp[j][rvsSimRank_row_idx[k]]; 
						}

						similarity[i][j] = alpha*SimRanksum + (1-alpha)*rvsSimRanksum;
						SimRanksum = 0;
						rvsSimRanksum = 0;
					}

				//we set the similarity(i,i)=1
				for (int i=0;i<size;i++)
				{
					similarity[i][i]=1.0f;
				}
				stopTime = System.currentTimeMillis();
				elapsedTimeCurrentIT = stopTime - startTime;			    
				writer.write("The Execution Time for Iteration "+iteration+" is: "+((elapsedTimeCurrentIT+elapsedTimeLastIT)*0.001)/(float)60); 
				writer.newLine();
				System.out.println("Execution Time for P-Rank Matrix Form With Dataset "+type+" in Iteration "+iteration+" is: "+((elapsedTimeCurrentIT+elapsedTimeLastIT)*0.001)/(float)60);
				elapsedTimeLastIT = elapsedTimeLastIT + elapsedTimeCurrentIT;
				startTime = System.currentTimeMillis();	
			}
			writer.close();
			out.close();
			similarity = null;
		}
		catch(Exception ex)
		{
			ex.printStackTrace();	
		}
	}

	/**
	 * computes P-Rank by using compressed sparse column (CSC) of transition matrix in multiplication. 
	 * It saves similarity information of ONLY ground truth papers in the file per each iteration.
	 * It is more faster than {PrankCSC(float [][] transitionMatrix, int iteration, String type)} method.
	 * @param graph: main graph is based on out-links
	 * @param rvsGraph: reverse graph is based on in-links 
	 * @param weightedGraph; for a weighted graph boolean false should be true
	 * @param alpha
	 * @param iteration
	 * @param type
	 */
	public void PrankCSC_ResultOnGT(String graph, String rvsGraph, boolean weightedGraph, float alpha, int iteration, String type)
	{
		String[] gtSets = {"01.txt","02.txt","03.txt","04.txt","05.txt","06.txt","07.txt","08.txt","09.txt","10.txt","11.txt"};
		ArrayList<Integer> gtSetMembers = new ArrayList<>(); //Holds the ground truth papers 
		Path gtSetPath;
		InputStream gtSetIn;
		BufferedReader gtSetReader;
		float cons = 0.8f;
		int size = papersList.size();

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

		//making CSC representation of adjacency matrix for SimRank Part
		adjacencyMatrix = new float [papersList.size()][papersList.size()];
		if (!weightedGraph)
			dataPR.prepareUnweightedAdjacencyMatrix(graph, adjacencyMatrix); // main graph which is based on out-links
		else 
			dataPR.prepareWeightedAdjacencyMatrix(graph, adjacencyMatrix); // main graph which is based on out-links

		dataPR.prepareColNormTransitionMatrix(adjacencyMatrix);
		totalNoneZero = dataPR.getTotalNoneZero();

		//compressed matrix form for SimRank
		float[] SimRank_val = new float[totalNoneZero];
		int[] SimRank_row_idx = new int [totalNoneZero];
		int[] SimRank_col_ptr = new int [size+1];

		CompressedSparseColumn obj = new CompressedSparseColumn();
		obj.CSC(adjacencyMatrix, SimRank_val, SimRank_row_idx, SimRank_col_ptr);

		//making CSC representation of adjacency matrix for rvs-SimRank Part		
		for (int i=0; i<papersList.size();i++)
			for (int j=0; j<papersList.size();j++)
				adjacencyMatrix[i][j] = 0;

		if (!weightedGraph)
			dataPR.prepareUnweightedAdjacencyMatrix(rvsGraph, adjacencyMatrix); // reverse graph which is based on in-links
		else 
			dataPR.prepareWeightedAdjacencyMatrix(rvsGraph, adjacencyMatrix); // reverse graph which is based on in-links		

		dataPR.prepareColNormTransitionMatrix(adjacencyMatrix);
		totalNoneZero = dataPR.getTotalNoneZero();

		//compressed matrix form for rvs-SimRank
		float[] rvsSimRank_val = new float[totalNoneZero];
		int[] rvsSimRank_row_idx = new int [totalNoneZero];
		int[] rvsSimRank_col_ptr = new int [size+1];

		obj.CSC(adjacencyMatrix, rvsSimRank_val, rvsSimRank_row_idx, rvsSimRank_col_ptr);

		adjacencyMatrix = null;

		float similarity[][] = new float[size][size];
		for ( int i = 0 ; i < size ; i++ )
			similarity[i][i]=1;

		for(int iter=1; iter<=iteration;iter++)
		{	      
			int nonZeroSimilarity = 0; //keeps the # of none zero similarity values in each iteration 				      	     
			float SimRank_temp[][] = new float[size][size];	//Keep the result of Q^T*S for SimRank
			float rvsSimRank_temp[][] = new float[size][size];	//Keep the result of Q^T*S for rvsSimRank

			//computes Q^T*S by using CSC form of Q
			for (int i=0;i<size;i++)
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
				for (int j=0;j<size;j++)
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
						nonZeroSimilarity++;
					SimRanksum = 0;
					rvsSimRanksum = 0;
				}

			try
			{
				file = new File(type+"_PRank_IT_"+iter);
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
				System.out.println("The result of P-Rank with dataset "+type+" on iteration "+iter+" ONLY for papers in ground truth is written in the file!");
				writer.close();
				out.close();
			}
			catch(Exception ex)
			{
				ex.printStackTrace();
			}
		}
	}


	public static void main(String argv[])
	{
		PRankMatrixForm obj = new PRankMatrixForm();

		//*** P-Rank for UNWEIGHTED and directed graph
		// for a weighted graph boolean false should be true
		//obj.PrankCSC_ResultOnGT("weighted_cosine_graph.txt","rvs_weighted_cosine_graph.txt",true,0.5f,10,"weighted_cosine");		
		//obj.PrankCSC_ResultOnGT("weighted_extended_graph.txt","rvs_weighted_extended_graph.txt",true,0.5f,10,"weighted_extended");
		//obj.PrankCSC_Timer("unweighted_graph.txt", "rvs_unweighted_graph.txt", 0.5, 10, "unweighted");
		obj.PrankCSC_ResultOnGT("unweighted_graph.txt", "rvs_unweighted_graph.txt", false, 0.5f, 5, "unweighted");


	}
}
