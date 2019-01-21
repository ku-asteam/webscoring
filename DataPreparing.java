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
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * @author masoud
 * @Comment This class prepares the data in required right format,
 * 			Extracts the adjacency matrix for an UNWEIGHTED graph from a file
 * 			Extracts the adjacency matrix for a WEIGHTED graph from a file
 * 			Normalize the adjacency matrix on columns
 * 			Normalize the adjacency matrix on rows 
 * 			Extract the list of outlinks for all paper
 * 			Inserts the Link-based similarity results in the database 
 * 
 */

public class DataPreparing
{
	public Path readInfo;
	public InputStream in;
	public BufferedReader reader;	
	public File file;
	public Path writeInfo;
	public OutputStream out;
	public BufferedWriter writer;

	public FileReading fr;
	public ArrayList<String> papersList;
	private int totalNoneZero; //indicates the number of non zero values in the adjacency matrix. Will be used for matrix compression 

	public int getTotalNoneZero()
	{
		return totalNoneZero;
	}

	public DataPreparing()
	{
		fr = new FileReading("linkBasedSimilarity");
		papersList = fr.getPapersList();
	}

	/**
	 * Reads links_id.txt file and make new files (outlinklist_byCode.txt, inlinklist_byCode.txt) based on the code of the papers, 
	 * i.e., the paper assigned from 0 to the size of dataset  
	 */
	public void prepareDataByCode(String graph)
	{
		StringTokenizer stk;		
		File file1;
		Path writeInfo1;
		OutputStream out1;
		BufferedWriter writer1;
		try
		{
			file = new File("outlinklist_byCode.txt");
			if( file.exists())
				file.delete();
			writeInfo = FileSystems.getDefault().getPath(file.getPath());
			Files.createFile(writeInfo);
			out = Files.newOutputStream(writeInfo);
			writer = new BufferedWriter(new OutputStreamWriter(out));

			file1 = new File("inlinklist_byCode.txt");
			if( file1.exists())
				file1.delete();
			writeInfo1 = FileSystems.getDefault().getPath(file1.getPath());
			Files.createFile(writeInfo1);
			out1 = Files.newOutputStream(writeInfo1);
			writer1 = new BufferedWriter(new OutputStreamWriter(out1));

			readInfo = FileSystems.getDefault().getPath(graph);		
			in = Files.newInputStream(readInfo);
			reader = new BufferedReader(new InputStreamReader(in));
			String line1 = null;
			while ((line1=reader.readLine())!=null)
			{
				stk = new StringTokenizer(line1,"\t");	
				String citingDoc;
				String citedDoc;
				citingDoc = stk.nextToken();
				citedDoc  = stk.nextToken();

				if (papersList.indexOf(citingDoc)==-1)
					System.out.println("Citing:"+citingDoc);

				if (papersList.indexOf(citedDoc)==-1)
					System.out.println("Cited:"+citedDoc);

				writer.write(papersList.indexOf(citingDoc)+"\t"+papersList.indexOf(citedDoc));
				writer.newLine();
				writer1.write(papersList.indexOf(citedDoc)+"\t"+papersList.indexOf(citingDoc));
				writer1.newLine();
			}			
			reader.close();
			in.close();
			writer.close();
			out.close();
			writer1.close();
			out1.close();		

			file = new File("nodelist_byCode.txt");
			if( file.exists())
				file.delete();
			writeInfo = FileSystems.getDefault().getPath(file.getPath());
			Files.createFile(writeInfo);
			out = Files.newOutputStream(writeInfo);
			writer = new BufferedWriter(new OutputStreamWriter(out));

			readInfo = FileSystems.getDefault().getPath("dataset-year.txt");		
			in = Files.newInputStream(readInfo);
			reader = new BufferedReader(new InputStreamReader(in));
			String line = null;
			while ((line=reader.readLine())!=null)
			{
				stk = new StringTokenizer(line,"\t");				
				writer.write(Integer.toString(papersList.indexOf(stk.nextToken())));
				writer.newLine();
			}
			reader.close();
			in.close();
			writer.close();
			out.close();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		System.out.println("Outlink list and inlinks list created by code! ^^");
	}

	/**
	 * Reads links_id.txt file and make new files (inlink.txt, outlinks.txt)   
	 */
	public void prepareData(String graph)
	{
		StringTokenizer stk;		
		File file1;
		Path writeInfo1;
		OutputStream out1;
		BufferedWriter writer1;
		try
		{
			file = new File("outlinklist.txt");
			if( file.exists())
				file.delete();
			writeInfo = FileSystems.getDefault().getPath(file.getPath());
			Files.createFile(writeInfo);
			out = Files.newOutputStream(writeInfo);
			writer = new BufferedWriter(new OutputStreamWriter(out));

			file1 = new File("inlinklist.txt");
			if( file1.exists())
				file1.delete();
			writeInfo1 = FileSystems.getDefault().getPath(file1.getPath());
			Files.createFile(writeInfo1);
			out1 = Files.newOutputStream(writeInfo1);
			writer1 = new BufferedWriter(new OutputStreamWriter(out1));

			readInfo = FileSystems.getDefault().getPath(graph);		
			in = Files.newInputStream(readInfo);
			reader = new BufferedReader(new InputStreamReader(in));
			String line1 = null;
			while ((line1=reader.readLine())!=null)
			{
				//In links_id.txt file, first paper is citing one and second one is cited one
				stk = new StringTokenizer(line1,"\t");	
				String citingDoc = stk.nextToken(); 
				String citedDoc = stk.nextToken();
				writer.write(citingDoc+","+citedDoc);
				writer.newLine();
				writer1.write(citedDoc+","+citingDoc);
				writer1.newLine();
			}			
			reader.close();
			in.close();
			writer.close();
			out.close();
			writer1.close();
			out1.close();			
			file = new File("nodelist.txt");
			if( file.exists())
				file.delete();
			writeInfo = FileSystems.getDefault().getPath(file.getPath());
			Files.createFile(writeInfo);
			out = Files.newOutputStream(writeInfo);
			writer = new BufferedWriter(new OutputStreamWriter(out));

			readInfo = FileSystems.getDefault().getPath("dataset-year.txt");		
			in = Files.newInputStream(readInfo);
			reader = new BufferedReader(new InputStreamReader(in));
			String line = null;
			while ((line=reader.readLine())!=null)
			{
				stk = new StringTokenizer(line,"\t");
				writer.write(stk.nextToken());
				writer.newLine();
			}
			reader.close();
			in.close();
			writer.close();
			out.close();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		System.out.println("Outlink list and inlinks list created! ^^");
	}

	/**
	 * Adds self-edges to the directed graph
	 */
	public void makeSelfUnweightedDirectedGraph(String graph)
	{
		try
		{
			file = new File("self_"+graph);
			if( file.exists())
				file.delete();
			writeInfo = FileSystems.getDefault().getPath(file.getPath());
			Files.createFile(writeInfo);
			out = Files.newOutputStream(writeInfo);
			writer = new BufferedWriter(new OutputStreamWriter(out));

			readInfo = FileSystems.getDefault().getPath(graph);		
			in = Files.newInputStream(readInfo);
			reader = new BufferedReader(new InputStreamReader(in));
			String line1 = null;
			while ((line1=reader.readLine())!=null)
			{
				writer.write(line1);
				writer.newLine();
			}			
			reader.close();
			in.close();

			//adding self-edges
			for(int i=0;i<papersList.size();i++)
			{
				writer.write(papersList.get(i)+"\t"+papersList.get(i));
				writer.newLine();
			}

			writer.close();
			out.close();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		System.out.println("The self-edge unweighted directed graph is made! ^^");

	}

	/**
	 * Makes an undirected graph 
	 */
	public void makeUnDirectedGraph(String graph)
	{
		try
		{
			file = new File("undirected_graph.txt");
			if( file.exists())
				file.delete();
			writeInfo = FileSystems.getDefault().getPath(file.getPath());
			Files.createFile(writeInfo);
			out = Files.newOutputStream(writeInfo);
			writer = new BufferedWriter(new OutputStreamWriter(out));

			readInfo = FileSystems.getDefault().getPath(graph);		
			in = Files.newInputStream(readInfo);
			reader = new BufferedReader(new InputStreamReader(in));
			String line1 = null;
			while ((line1=reader.readLine())!=null)
			{

				StringTokenizer st = new StringTokenizer(line1,"\t");
				String node_1 = st.nextToken();
				String node_2 = st.nextToken();				
				writer.write(node_1+"\t"+node_2);
				writer.newLine();
				writer.write(node_2+"\t"+node_1);
				writer.newLine();
			}			
			reader.close();
			in.close();
			writer.close();
			out.close();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		System.out.println("The undirected graph is made! ^^");
	}

	/**
	 * Makes an undirected graph for reachability vector method
	 */
	public void makeUnDirectedGraph_ReachabilityVector(String graph)
	{
		try
		{
			file = new File("input.wlinks");
			if( file.exists())
				file.delete();
			writeInfo = FileSystems.getDefault().getPath(file.getPath());
			Files.createFile(writeInfo);
			out = Files.newOutputStream(writeInfo);
			writer = new BufferedWriter(new OutputStreamWriter(out));

			readInfo = FileSystems.getDefault().getPath(graph);		
			in = Files.newInputStream(readInfo);
			reader = new BufferedReader(new InputStreamReader(in));
			String line1 = null;
			while ((line1=reader.readLine())!=null)
			{

				StringTokenizer st = new StringTokenizer(line1,"\t");
				String node_1 = st.nextToken();
				String node_2 = st.nextToken();				
				writer.write(node_1+"\t"+node_2+"\t"+1);
				writer.newLine();
				writer.write(node_2+"\t"+node_1+"\t"+1);
				writer.newLine();
			}			
			reader.close();
			in.close();
			writer.close();
			out.close();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		System.out.println("The undirected graph is made! ^^");
	}

	/**
	 * Extracts the adjacency matrix for an UNWEIGHTED graph from a file
	 */
	public void prepareUnweightedAdjacencyMatrix(String fileName,float [][] adjecancyMatrix)
	{ 
		StringTokenizer stk;
		String citingDoc=null;
		String citedDoc=null;
		totalNoneZero = 0;

		try
		{
			readInfo = FileSystems.getDefault().getPath(fileName);		
			in = Files.newInputStream(readInfo);
			reader = new BufferedReader(new InputStreamReader(in));
			String line1 = null;
			while ((line1=reader.readLine())!=null)
			{
				stk = new StringTokenizer(line1,"\t");	
				citingDoc = stk.nextToken().trim();
				citedDoc  = stk.nextToken().trim();
				//System.out.println(citingDoc);
				//System.out.println(citedDoc);
				adjecancyMatrix[papersList.indexOf(citingDoc)][papersList.indexOf(citedDoc)] = 1.0f;
				totalNoneZero++;
			}
			reader.close();
			in.close();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}		
	}
	
	/**
	 * Extracts the adjacency matrix for an SELF_EDGE UNWEIGHTED graph from a file
	 * gives value 0.001 to (i,i) in adjacency matrix to decrease the effect of self-edges   
	 */
	public void prepareSelfUnweightedAdjacencyMatrix(String fileName,float [][] adjecancyMatrix, float selfEdgeWeight)
	{ 
		StringTokenizer stk;
		totalNoneZero = 0;
		try
		{
			readInfo = FileSystems.getDefault().getPath(fileName);		
			in = Files.newInputStream(readInfo);
			reader = new BufferedReader(new InputStreamReader(in));
			String line1 = null;
			while ((line1=reader.readLine())!=null)
			{
				stk = new StringTokenizer(line1,"\t");	
				String citingDoc;
				String citedDoc;
				citingDoc = stk.nextToken();
				citedDoc  = stk.nextToken();
				if (citingDoc.equals(citedDoc))
					adjecancyMatrix[papersList.indexOf(citingDoc)][papersList.indexOf(citedDoc)] = selfEdgeWeight;
				else
					adjecancyMatrix[papersList.indexOf(citingDoc)][papersList.indexOf(citedDoc)] = 1.0f;
				totalNoneZero++;
			}
			reader.close();
			in.close();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}		
	}


	/**
	 * Extracts the adjacency matrix for a WEIGHTED graph from a file
	 */
	public void prepareWeightedAdjacencyMatrix(String fileName,float [][] adjecancyMatrix)
	{ 
		StringTokenizer stk;
		totalNoneZero = 0;

		try
		{
			readInfo = FileSystems.getDefault().getPath(fileName);		
			in = Files.newInputStream(readInfo);
			reader = new BufferedReader(new InputStreamReader(in));
			String line1 = null;
			while ((line1=reader.readLine())!=null)
			{
				stk = new StringTokenizer(line1,"\t");	
				String citingDoc;
				String citedDoc;
				String weight;
				citingDoc = stk.nextToken();
				citedDoc  = stk.nextToken();
				weight  = stk.nextToken();
				adjecancyMatrix[papersList.indexOf(citingDoc)][papersList.indexOf(citedDoc)] = Float.valueOf(weight);
				totalNoneZero++;
			}
			reader.close();
			in.close();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}		
	}


	/**
	 * prepares a transition matrix based on the column normalization 
	 */
	public void prepareColNormTransitionMatrix(float [][] adjecancyMatrix)
	{
		float columnSum;
		int size = adjecancyMatrix.length;

		for (int col=0;col<size;col++)
		{
			columnSum = 0.0f;
			for (int row=0;row<size;row++)
				columnSum = columnSum + adjecancyMatrix[row][col];
			for (int row=0;row<size;row++)
			{
				if (adjecancyMatrix[row][col]!=0.0)
					adjecancyMatrix[row][col] = adjecancyMatrix[row][col]/columnSum;
			}
		}		
	}

	/**
	 * prepares a transition matrix based on the row normalization 
	 */
	public void prepareRowNormTransitionMatrix(float [][] adjecancyMatrix)
	{
		float rowSum;
		int size = adjecancyMatrix.length;

		for (int row=0;row<size;row++)
		{
			rowSum = 0.0f;
			for (int col=0;col<size;col++)
				rowSum = rowSum + adjecancyMatrix[row][col];
			for (int col=0;col<size;col++)
			{
				if (adjecancyMatrix[row][col]!=0.0)
					adjecancyMatrix[row][col] = adjecancyMatrix[row][col]/rowSum;
			}
		}		

	}

	/**
	 * Reads link-based similarity result from appropriate files and insert them in database
	 * @param SimilarityFile
	 * @param onlyResultOnGT: TRUE if the similarity files created only for papers in ground truth
	 */
	public void insertData(String SimilarityFile, String tabelName, boolean onlyResultOnGT, int topK)
	{	
		StringTokenizer stk;
		UtilityClass utClass = new UtilityClass();
		utClass.createConenction("JacSim");

		String node_1="0";
		String oldNode_1="0";
		String node_2;
		float simvalue;
		Map<String, Float> nodeSimValue = new HashMap<String, Float>();
		utClass.createTable(tabelName);				
		try
		{
			readInfo = FileSystems.getDefault().getPath(SimilarityFile);		
			in = Files.newInputStream(readInfo);
			reader = new BufferedReader(new InputStreamReader(in));
			String line = null;

			// If the file is created by {simrankCSC_ResultOnGT()} method and only contains the result for papers in ground truth set,the first line should be ignore 
			if (onlyResultOnGT)
				line=reader.readLine();

			while ((line=reader.readLine())!=null)
			{
				stk = new StringTokenizer(line,",");
				node_1 = stk.nextToken();
				node_2 = stk.nextToken();
				simvalue = Float.valueOf(stk.nextToken());
				BigDecimal bd = new BigDecimal(simvalue);
				bd = bd.setScale(5,BigDecimal.ROUND_UP);		
				if(oldNode_1.equals(node_1))
					//Puting all similarity results in Map 
					nodeSimValue.put(papersList.get(Integer.valueOf(node_2)), bd.floatValue());
				else
				{
					//Sorting "querySimValue"
					nodeSimValue = utClass.mapSort(nodeSimValue);
					//Inserting top k nearest neighbor queries to the node in Database
					Object[] queries = nodeSimValue.keySet().toArray();				
					for (int i=0; i<=topK && i<queries.length; i++)
					{
						if (!papersList.get(Integer.valueOf(oldNode_1)).equals(queries[i].toString()))
							utClass.insertData(papersList.get(Integer.valueOf(oldNode_1)), queries[i].toString(), nodeSimValue.get(queries[i]));								
					}
					nodeSimValue.clear();
					oldNode_1 = node_1;
					nodeSimValue.put(papersList.get(Integer.valueOf(node_2)), bd.floatValue());												
				}
			}
			/*
			 * inserting last node related similarity in database
			 */
			nodeSimValue = utClass.mapSort(nodeSimValue);
			Object[] queries = nodeSimValue.keySet().toArray();				
			for (int i=0; i<=topK && i<queries.length; i++) //i<=topK becuase always similarity(a,a)=1 is maximum
			{
				if (!papersList.get(Integer.valueOf(oldNode_1)).equals(queries[i].toString())) // always similarity(a,a)=1 and is maximum, we don't put it in database
					utClass.insertData(papersList.get(Integer.valueOf(oldNode_1)), queries[i].toString(), nodeSimValue.get(queries[i]));						
			}
			reader.close();
			in.close();
			System.out.println(SimilarityFile +" file is written in the database! ^^");
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		utClass.closeConnection();
	}	

	/**
	 * From big files containing all-pairs similarity, only similarity values for papers in ground truth set inserted in database 
	 * @param SimilarityFile
	 * @param tabelName
	 * @param topK
	 */
	public void insertGTData(String SimilarityFile, String tabelName, int topK)
	{	
		StringTokenizer stk;
		UtilityClass utClass = new UtilityClass();
		utClass.createConenction("JacSim");
		String[] gtSets = {"01.txt","02.txt","03.txt","04.txt","05.txt","06.txt","07.txt","08.txt","09.txt","10.txt","11.txt"};
		ArrayList<Integer> gtSetMembers = new ArrayList<>(); //Holds the ground truth papers	
		Path gtSetPath;
		InputStream gtSetIn;
		BufferedReader gtSetReader;

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

		String node_1="0";
		String oldNode_1="0";
		String node_2;
		float simvalue;
		Map<String, Float> nodeSimValue = new HashMap<String, Float>();
		utClass.createTable(tabelName);				
		try
		{
			readInfo = FileSystems.getDefault().getPath(SimilarityFile);		
			in = Files.newInputStream(readInfo);
			reader = new BufferedReader(new InputStreamReader(in));
			String line = null;

			while ((line=reader.readLine())!=null)
			{
				stk = new StringTokenizer(line,",");
				node_1 = stk.nextToken();
				node_2 = stk.nextToken();				
				simvalue = Float.valueOf(stk.nextToken());
				BigDecimal bd = new BigDecimal(simvalue);
				bd = bd.setScale(5,BigDecimal.ROUND_UP);		
				if(oldNode_1.equals(node_1))
				{
					if (gtSetMembers.contains(Integer.valueOf(node_1)))	//Putting similarity results in Map for nodes belong to ground truth set
						nodeSimValue.put(papersList.get(Integer.valueOf(node_2)), bd.floatValue());
				}
				else
				{
					if (!nodeSimValue.isEmpty())
					{
						//Sorting "querySimValue"
						nodeSimValue = utClass.mapSort(nodeSimValue);
						//Inserting top k nearest neighbor queries to the node in Database
						Object[] queries = nodeSimValue.keySet().toArray();				
						for (int i=0; i<=topK && i<queries.length; i++)
						{
							if (!papersList.get(Integer.valueOf(oldNode_1)).equals(queries[i].toString()))
								utClass.insertData(papersList.get(Integer.valueOf(oldNode_1)), queries[i].toString(), nodeSimValue.get(queries[i]));								
						}
						nodeSimValue.clear();
					}
					oldNode_1 = node_1;
					if (gtSetMembers.contains(Integer.valueOf(node_1)))
						nodeSimValue.put(papersList.get(Integer.valueOf(node_2)), bd.floatValue());												
				}
			}
			/*
			 * inserting last node related similarity in database
			 */
			nodeSimValue = utClass.mapSort(nodeSimValue);
			Object[] queries = nodeSimValue.keySet().toArray();				
			for (int i=0; i<=topK && i<queries.length; i++) //i<=topK becuase always similarity(a,a)=1 is maximum
			{
				if (!papersList.get(Integer.valueOf(oldNode_1)).equals(queries[i].toString())) // always similarity(a,a)=1 and is maximum, we don't put it in database
					utClass.insertData(papersList.get(Integer.valueOf(oldNode_1)), queries[i].toString(), nodeSimValue.get(queries[i]));						
			}
			reader.close();
			in.close();
			System.out.println(SimilarityFile +" file is written in the database! ^^");
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		utClass.closeConnection();
	}	

	/**
	 * makes a reverse graph for an un-weighted graph.
	 */
	public void makeUnweightedReverseGraph(String graph)
	{
		try
		{			
			file = new File("rvs_"+graph);
			if( file.exists())
				file.delete();
			writeInfo = FileSystems.getDefault().getPath(file.getPath());
			Files.createFile(writeInfo);
			out = Files.newOutputStream(writeInfo);
			writer = new BufferedWriter(new OutputStreamWriter(out));
						
			readInfo = FileSystems.getDefault().getPath(graph);		
			in = Files.newInputStream(readInfo);
			reader = new BufferedReader(new InputStreamReader(in));
			String line1 = null;
			while ((line1=reader.readLine())!=null)
			{
				StringTokenizer st = new StringTokenizer(line1,"\t");
				String node_1 = st.nextToken();
				String node_2 = st.nextToken();				
				writer.write(node_2+"\t"+node_1);
				writer.newLine();
			}			
			reader.close();
			in.close();
			writer.close();
			out.close();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}	
	}

	
	/**
	 * makes a reverse graph for a weighted graph.
	 */
	public void makeWeightedReverseGraph(String graph)
	{
		try
		{			
			file = new File("rvs_"+graph);
			if( file.exists())
				file.delete();
			writeInfo = FileSystems.getDefault().getPath(file.getPath());
			Files.createFile(writeInfo);
			out = Files.newOutputStream(writeInfo);
			writer = new BufferedWriter(new OutputStreamWriter(out));
						
			readInfo = FileSystems.getDefault().getPath(graph);		
			in = Files.newInputStream(readInfo);
			reader = new BufferedReader(new InputStreamReader(in));
			String line1 = null;
			while ((line1=reader.readLine())!=null)
			{
				StringTokenizer st = new StringTokenizer(line1,"\t");
				String node_1 = st.nextToken();
				String node_2 = st.nextToken();
				String simValue = st.nextToken();
				writer.write(node_2+"\t"+node_1+"\t"+simValue);
				writer.newLine();
			}			
			reader.close();
			in.close();
			writer.close();
			out.close();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}	
	}

	
	public static void main(String[] args)
	{
		DataPreparing lk = new DataPreparing();
		
		
		for (int i=1; i<11; i++)
		{
			lk.insertData("Results_on_TREC-dataset/weighted_Jrvs/weighted_cosine_TREC_Jrvs_C_4_A_9_IT_"+i,"weighted_cosine_TREC_Jrvs_C_4_A_9_IT_"+i, true,30);
		}
		/**
		 * 
		 
		for (int alpha=1; alpha<11;alpha++)
			for (int i=1; i<11;i++)
			{								
				lk.insertData("TREC-Jrvs/unweighted_TREC_Jrvs_C_8/unweighted_TREC_Jrvs_C_8_A_"+alpha+"_IT_"+i,"unweighted_TREC_Jrvs_C_8_A_"+alpha+"_IT_"+i, true,30);
			}
		 */
		
		/**
		 * For inserting JPrank in database
	
		for (int PRankBeta=8; PRankBeta<11;PRankBeta++)
			for (int i=1; i<11;i++)
			{								
				lk.insertData("unweighted_TREC_JPrank_C_4_A_9_B_"+PRankBeta+"_IT_"+i,"unweighted_TREC_JPrank_C_4_A_9_B_"+PRankBeta+"_IT_"+i, true,30);
			}

		 */
		
		
	}
}


