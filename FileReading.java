import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * @author masoud
 * @Comment This class reads different existing files and extracts some information such as dataset papers, dataset words, paper's 
 *			words and their related TF and TF/IDF and paper's length.   
 * @INPUT_DATA 1- word-list.txt 2-paper-list.txt 3-files in "score" folder 4-files in "orginaltermscore" folder 
 */
public class FileReading 
{
	public Path readInfo;
	public InputStream in;
	public BufferedReader reader;
	
	public Path tfList;
	public Path freqList;
	public Path lengthList;
	
	public OutputStream tfListOut;
	public OutputStream freqListOut;
	public OutputStream lengthListOut;
	
	public BufferedWriter tfListWriter;
	public BufferedWriter freqListWriter;
	public BufferedWriter lengthListWriter;

	/**
	 * This map contains the code of the nodes and their inlinks LIST;
	 * Probably, there are some nodes without inlinks and as a result without an entry in the map 	
	 */
	public Map<Integer, ArrayList<Integer>> inlinksMap;

	/**
	 * This map contains the weight of inliks to a node as a list;
	 * the order of weights is the same as the order of nodes in inlinksMap.	
	 */
	public Map<Integer, ArrayList<Float>> inlinksWeightMap;

	/**
	 * This map contains the code of the nodes and their outlinks LIST;
	 * Probably, there are some nodes without outlinks and as a result without an entry in the map 	
	 */
	public Map<Integer, ArrayList<Integer>> outlinksMap;

	/**
	 * This map contains the weight of outliks to a node as a list;
	 * the order of weights is the same as the order of nodes in outlinksMap.	
	 */
	public Map<Integer, ArrayList<Float>> outlinksWeightMap;

	
	/**
	 * All of the words in dataset
	 */
	private ArrayList<String> wordsList;
	
	/**
	 * All of the papers in dataset, this list contains the paper index and their real codes, for example index 0 is paper number 1 with 
	 * actual code 137197
	 */	
	private ArrayList<String> papersList;
	
	/**
	 * All of the papers years in dataset, this list contains the paper index and their publication year, for example index 0 is paper number 1 with 
	 * actual code 137197
	 */	
	private ArrayList<Integer> papersYear;
	
	/**
	 * All of the keywords of a special paper	
	 */
	private ArrayList<String> paperKeywords;
	
	/**
	 * All of the words tfidf of a special paper	
	 */
	private ArrayList<Float> paperTFIDF;
	
	/**
	 * An initial Map of all of the papers in dataset
	 * here paper objects just contain keyword list, tf value of keywords,tf/idf value of keywords and paper length 	
	 */
	private Map<Integer, Paper> papersMap;
	
	/**
	 * contains document frequency of all words in dataset
	 */
	private int[] wordsDocumentFreq;
	
	/**
	 * returns a Map of all papers in dataset
	 * @return Map<Integer, Paper>, 
	 */
	public Map<Integer, Paper> getPapersMap() 
	{			
		parsingPapersToMap();
		return papersMap;
	}

	public void setPapersMap(Map<Integer, Paper> papersMap) 
	{
		this.papersMap = papersMap;
	}
	
	/**
	 * returns an array contains document frequency of all dataset words
	 * @return int[]
	 */
	public int[] getWordsDocumentFreq() 
	{
		return wordsDocumentFreq;
	}

	public void setWordsDocumentFreq(int[] documentFreq) 
	{
		this.wordsDocumentFreq = documentFreq;
	}

	/**
	 * returns list of keywords of a special paper, at first should call "makePaperKeywordsAndTFIDF(int paperIndex)" method
	 * @return ArrayList<String>
	 */
	public ArrayList<String> getPaperKeywords()
	{
		return paperKeywords;
	}

	/**
	 * returns list of keywords tf/idf values of a special paper, at first should call "makePaperKeywordsAndTFIDF(int paperIndex)" method
	 * @return ArrayList<Float>
	 */
	public ArrayList<Float> getPaperTFIDF()
	{
		return paperTFIDF;
	}
	

	/**
	 * returns list of all words in dataset
	 * * @return ArrayList<Float>
	 */
	public ArrayList<String> getWordsList()
	{
		return wordsList;
	}

	/**
	 * returns list of all papers in dataset 
	 * @return ArrayList<String>
	 */
	public ArrayList<String> getPapersList()
	{
		return papersList;
	}

	public FileReading() 
	{
		//by making an object instance wordsList and papersList updated
		makeAllPapers();
		makeAllWords();			
	}

	/*
	 * for link-based similarity we dont need to the list of the words
	 */
	public FileReading(String linkBasedSimilarity) 
	{
		//by making an object instance papersList updated
		makeAllPapers();		
	}
	
	/**
	 *	reading dataset words list from file
	 */
	public void makeAllWords()
	{
		wordsList = new ArrayList<>();
		readInfo = FileSystems.getDefault().getPath("words-list.txt");		
		
		try
		{
			in = Files.newInputStream(readInfo);
			reader = new BufferedReader(new InputStreamReader(in));
			String line = null;
			while ((line=reader.readLine())!=null)
				wordsList.add(line.trim());			
			reader.close();
			in.close();
		}
		catch (IOException ex)
		{
			System.out.print(ex);
			
		}
	}

	/**
	 *	reading dataset papers list from file and extracting the papers code and their publication years
	 */	
	public void makeAllPapers()
	{
		StringTokenizer stk;
		papersList = new ArrayList<>();
		papersYear = new ArrayList<>();
		readInfo = FileSystems.getDefault().getPath("dataset-year.txt");	
		
		try
		{
			in = Files.newInputStream(readInfo);
			reader = new BufferedReader(new InputStreamReader(in));
			String line = null;
			while ((line=reader.readLine())!=null)
			{
				stk = new StringTokenizer(line,"\t");
				papersList.add(stk.nextToken().trim());
				papersYear.add(Integer.parseInt(stk.nextToken()));				
			}
			reader.close();
			in.close();
		}
		catch (IOException ex)
		{
			System.out.print(ex);		
		}
	}
	
	
	/**
	 * extract all of the keywords of a paper and their related TF/IDF values
	 */
	public void makePaperKeywordsAndTFIDF(int paperIndex)
	{
		String paperCode;
		String line;
		Path paperInfo;
		InputStream in;
		BufferedReader reader;
		StringTokenizer stk;
		Object[] arrayOfLines;

		
		paperCode = papersList.get(paperIndex);		
		//in "Score" folder there are a text file for every paper contains its keywords and their tf/idf valuse
		paperInfo = FileSystems.getDefault().getPath("LETOR-data/score/"+paperCode);
		paperKeywords = new ArrayList<>();
		paperTFIDF = new ArrayList<>();

		try
		{
			in = Files.newInputStream(paperInfo);
			reader = new BufferedReader(new InputStreamReader(in));
			ArrayList<String> lines = new ArrayList<>();
			
			while ((line=reader.readLine())!=null)		
				lines.add(line);
			
			arrayOfLines = lines.toArray();
			Arrays.sort(arrayOfLines);
			
			for (int i=0; i<arrayOfLines.length;i++)
			{
				stk = new StringTokenizer(arrayOfLines[i].toString()," ");
				paperKeywords.add(stk.nextToken());
				paperTFIDF.add(Float.valueOf(stk.nextToken()));
			}
		}
		catch(IOException ex)
		{
			System.out.println(ex);
		}		
	}

	/**
	 * Parsing every paper information and extract its length, term frequency for all of its keywords
	 * and put this information in a Paper object to make future usage more simple. 
	 */		
	public void parsingPapersToMap()
	{
		int paperIndex = 0;
		int paperNumber = 0;
		int keywordNumber = 0;
		int termFreq = 0;
		int docFreq = 0;
		int documentLength = 0;		
		int dataSetSize = 0;
		int[]  keywordList;
		float[] tfValueList ;
		float[] tfidfValueList;
		int publicationYear;
		Paper paperObj;
		float tfidf;
		float idf;
		String keyword;	

		dataSetSize = papersList.size();
		wordsDocumentFreq = new int [wordsList.size()];
		for (int i=0;i<wordsDocumentFreq.length;i++)
		{
			wordsDocumentFreq[i] = 0;
		}				
		try
		{	
			papersMap = new HashMap<Integer, Paper>();			
			//for (paperIndex=0;paperIndex<20;paperIndex++)
			for (paperIndex=0;paperIndex<papersList.size();paperIndex++)
			{
				paperNumber = paperIndex + 1;
				makePaperKeywordsAndTFIDF(paperIndex);
				publicationYear = papersYear.get(paperIndex);
	
				keywordList = new int[paperKeywords.size()];
				tfValueList = new float [paperKeywords.size()];			
				
				for (int index = 0;index<paperKeywords.size();index++ )
				{
					keyword = paperKeywords.get(index);
					keywordNumber = wordsList.indexOf(keyword)+1;
					if (wordsDocumentFreq[keywordNumber-1] == 0)
					{
						//read keyword information file
						readInfo = FileSystems.getDefault().getPath("LETOR-data/orginaltermscore/"+keyword);
						in = Files.newInputStream(readInfo);
						reader = new BufferedReader(new InputStreamReader(in));
						//compute this keyword comes in how many papers
						while (reader.readLine()!=null)
							docFreq++;
						wordsDocumentFreq[keywordNumber-1] = docFreq;
						reader.close();
						in.close();
					}
					else
					{
						docFreq = wordsDocumentFreq[keywordNumber-1];
					}
					tfidf = paperTFIDF.get(index);
					idf = (float)(Math.log10((float)dataSetSize/docFreq))/(float)Math.log10(2f);
					termFreq = (int) Math.round(tfidf/idf);
					documentLength = documentLength + termFreq;
					keywordList[index] = keywordNumber;
					tfValueList[index] = termFreq;								
					docFreq = 0;
				}								
				
				tfidfValueList = new float[paperTFIDF.size()];
				for (int j=0; j<paperTFIDF.size();j++)
				{
					tfidfValueList[j] = paperTFIDF.get(j);
				}
				paperObj = new Paper(keywordList, tfValueList, tfidfValueList, documentLength,publicationYear);			
				papersMap.put(paperNumber, paperObj);
				documentLength = 0; 					
			}
			System.out.println("FileReading Class: papers mapped to objects! ^^");
		}		
		catch (IOException ex)
		{
			System.out.print(ex);
		}				
	}		
	
	/**
	 * Reading a graph file and extracting the list of in-links plus their appropriate weight for each node 
	 */	
	public void makeWeightedInlinksMaps(String weightedGraph)
	{
	
		StringTokenizer stk;
		inlinksWeightMap = new HashMap<Integer, ArrayList<Float>>();
		inlinksMap = new HashMap<Integer, ArrayList<Integer>>();	
		ArrayList<Integer> inLinks;
		ArrayList<Float> inLinksWeight;
		readInfo = FileSystems.getDefault().getPath(weightedGraph);
		try
		{
			in = Files.newInputStream(readInfo);
			reader = new BufferedReader(new InputStreamReader(in));
			String line = null;
			while ((line=reader.readLine())!=null)
			{
				stk = new StringTokenizer(line,"\t");
				String firstNode = stk.nextToken().trim();
				String secondNode = stk.nextToken().trim();
				String weightValue = stk.nextToken().trim();				
				/*
				 * for a line (A,B,W) adds A to the inlinks list of B and W to weight list of B
				 * as we see the order of inlinks and weights are same in both maps, so we can refer them by an identical index 
				 */
				if (!inlinksMap.containsKey(papersList.indexOf(secondNode)))
				{
					inLinks = new ArrayList<Integer>();
					inLinks.add(papersList.indexOf(firstNode));
					inlinksMap.put(papersList.indexOf(secondNode), inLinks);
				
					inLinksWeight = new ArrayList<Float>();
					inLinksWeight.add(Float.parseFloat(weightValue));
					inlinksWeightMap.put(papersList.indexOf(secondNode), inLinksWeight);
									
				}
				else
				{
					inlinksMap.get(papersList.indexOf(secondNode)).add(papersList.indexOf(firstNode));
					inlinksWeightMap.get(papersList.indexOf(secondNode)).add(Float.parseFloat(weightValue));
				}
			}
			reader.close();
			in.close();
			System.out.println("FileReading.java: InlinkeMap and WeightMap created for each node! ^^");
			System.out.println();
		}
		catch (IOException ex)
		{
			System.out.print(ex);		
		}
		
		/*		
		//Reading the map for testing 
		Object[] queries = inlinksMap.keySet().toArray();				
		for (int i=0; i<queries.length; i++)
		{
			System.out.println("Node: "+ papersList.get(Integer.parseInt(queries[i].toString())));
			
			for (int j=0;j<inlinksMap.get(queries[i]).size();j++)
			{
				System.out.print("inlinks: "+papersList.get(inlinksMap.get(queries[i]).get(j)));
				System.out.println(", weight: "+inlinksWeightMap.get(queries[i]).get(j));
			}
			System.out.println("=================================");
		}
		*/
	}	
	
	/**
	 * Reading a reserved graph file and extracting the list of out-links plus their appropriate weight for each node 
	 */	
	public void makeWeightedOutlinksMaps(String rvsWeightedGraph)
	{
	
		StringTokenizer stk;
		outlinksWeightMap = new HashMap<Integer, ArrayList<Float>>();
		outlinksMap = new HashMap<Integer, ArrayList<Integer>>();	
		ArrayList<Integer> outLinks;
		ArrayList<Float> outLinksWeight;
		readInfo = FileSystems.getDefault().getPath(rvsWeightedGraph);
		try
		{
			in = Files.newInputStream(readInfo);
			reader = new BufferedReader(new InputStreamReader(in));
			String line = null;
			while ((line=reader.readLine())!=null)
			{
				stk = new StringTokenizer(line,"\t");
				String firstNode = stk.nextToken().trim();
				String secondNode = stk.nextToken().trim();
				String weightValue = stk.nextToken().trim();				
				/*
				 * for a line (A,B,W) adds A to the outlinks list of B and W to weight list of B
				 * as we see the order of outlinks and weights are same in both maps, so we can refer them by an identical index 
				 */
				if (!outlinksMap.containsKey(papersList.indexOf(secondNode)))
				{
					outLinks = new ArrayList<Integer>();
					outLinks.add(papersList.indexOf(firstNode));
					outlinksMap.put(papersList.indexOf(secondNode), outLinks);
				
					outLinksWeight = new ArrayList<Float>();
					outLinksWeight.add(Float.parseFloat(weightValue));
					outlinksWeightMap.put(papersList.indexOf(secondNode), outLinksWeight);
									
				}
				else
				{
					outlinksMap.get(papersList.indexOf(secondNode)).add(papersList.indexOf(firstNode));
					outlinksWeightMap.get(papersList.indexOf(secondNode)).add(Float.parseFloat(weightValue));
				}
			}
			reader.close();
			in.close();
			System.out.println("FileReading.java: OutlinkeMap and WeightMap created for each node! ^^");
			System.out.println();
		}
		catch (IOException ex)
		{
			System.out.print(ex);		
		}
		
		/*		
		//Reading the map for testing
		
		Object[] queries = outlinksMap.keySet().toArray();				
		for (int i=0; i<queries.length; i++)
		{
			System.out.println("Node: "+ papersList.get(Integer.parseInt(queries[i].toString())));			
			
			for (int j=0;j<outlinksMap.get(queries[i]).size();j++)
			{
				System.out.print("outlinks: "+papersList.get(outlinksMap.get(queries[i]).get(j)));
				System.out.println(", weight: "+outlinksWeightMap.get(queries[i]).get(j));
			}
			System.out.println("=================================");
		}
		 */
	}	

	
		
	/**
	 * Reading a graph file and extracting ONLY the list of in-links for each nodes
	 */	
	public void makeInlinksMaps(String graph)
	{
		StringTokenizer stk;
		inlinksMap = new HashMap<Integer, ArrayList<Integer>>();	
		ArrayList<Integer> inLinks;
		readInfo = FileSystems.getDefault().getPath(graph);
		try
		{
			in = Files.newInputStream(readInfo);
			reader = new BufferedReader(new InputStreamReader(in));
			String line = null;
			while ((line=reader.readLine())!=null)
			{
				stk = new StringTokenizer(line,"\t");
				String firstNode = stk.nextToken().trim();
				String secondNode = stk.nextToken().trim();
				
				/*
				 * for a line (A,B) adds A to the inlinks list of B 
				 */
				if (!inlinksMap.containsKey(papersList.indexOf(secondNode)))
				{
					inLinks = new ArrayList<Integer>();
					inLinks.add(papersList.indexOf(firstNode));
					inlinksMap.put(papersList.indexOf(secondNode), inLinks);
				}
				else
					inlinksMap.get(papersList.indexOf(secondNode)).add(papersList.indexOf(firstNode));
			}
			reader.close();
			in.close();
			System.out.println("FileReading.java: InlinkeMap created for each node! ^^");
			System.out.println();
		}
		catch (IOException ex)
		{
			System.out.print(ex);		
		}		
	}	

	/**
	 * Reading a reversed graph file and extracting ONLY the list of out-links for each nodes
	 */	
	public void makeOutlinksMaps(String rvsGraph)
	{
		StringTokenizer stk;
		outlinksMap = new HashMap<Integer, ArrayList<Integer>>();	
		ArrayList<Integer> outLinks;
		readInfo = FileSystems.getDefault().getPath(rvsGraph);
		try
		{
			in = Files.newInputStream(readInfo);
			reader = new BufferedReader(new InputStreamReader(in));
			String line = null;
			while ((line=reader.readLine())!=null)
			{
				stk = new StringTokenizer(line,"\t");
				String firstNode = stk.nextToken().trim();
				String secondNode = stk.nextToken().trim();
				
				/*
				 * for a line (A,B) adds A to the outlinks list of B 
				 */
				if (!outlinksMap.containsKey(papersList.indexOf(secondNode)))
				{
					outLinks = new ArrayList<Integer>();
					outLinks.add(papersList.indexOf(firstNode));
					outlinksMap.put(papersList.indexOf(secondNode), outLinks);
				}
				else
					outlinksMap.get(papersList.indexOf(secondNode)).add(papersList.indexOf(firstNode));
			}
			reader.close();
			in.close();
			System.out.println("FileReading.java: OutlinkeMap created for each node! ^^");
			System.out.println();
		}
		catch (IOException ex)
		{
			System.out.print(ex);		
		}		
	}	

	
	public static void main(String args[])
	{
		FileReading fr = new FileReading();
		fr.makeWeightedOutlinksMaps("rvs_weighted_cosine_graph.txt");
		//fr.makeAllWords();
		//fr.parsingPapersToMap();
		//fr.getPapersMap();
		
		
		
	}
}
