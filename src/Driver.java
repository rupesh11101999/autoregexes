import is2.lemmatizer.Options;
import is2.parser.Parser;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.didion.jwnl.JWNL;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

class Driver{
	int MAX = 35000;
	public static void main(String[] args) throws IOException, ClassNotFoundException{
		
		//getting the text
		MaxentTagger posTagger = new MaxentTagger("/Users/lakshmiramachandran/Documents/Add-ons/stanford-postagger-2011-09-14/models/bidirectional-distsim-wsj-0-18.tagger");
		//for the graph-genator's SRL code
		String[] opts ={"-model","/Users/lakshmiramachandran/Documents/Add-ons/srl-20101031/featuresets/prs-eng.model"};
		Options options = new Options(opts);
		// create a parser
		Parser parser = new Parser(options);

		//initializing the dictionary for JWNL
		//initializing the Java WordNet Library
		String propsFile = "/Users/lakshmiramachandran/Documents/workspace/ShortAnswerScoringGraphRelevance/file_properties.xml";
		try {
			JWNL.initialize(new FileInputStream(propsFile));
		} catch (Exception ex) {
		    ex.printStackTrace();
		    System.exit(-1);
		}
		
		//"50479", "50670-07", "50674-07", "50942", "51034", "80090", "80138-07", "80530-06", 
//		String[] prompts = {"85056-08", "85076-08"};
		//String[] prompts = {"Set1", "Set2", "Set3", "Set4", "Set5", "Set6", "Set7", "Set8", "Set9", "Set10" };
		String[] prompts = {
				"COSC120160", "COSC120170", "COSC120274", "COSC120275", "COSC130037", "COSC130066", 
				"COSC130086", "COSC130088", "COSC130108", "COSC130196", "COSC130202", "COSC130242", "COSC130247", 
				"COSC130267", "COSS120325", "COSS120329", "COSS120363", "COSS120382","COSS130095", "COSS130110"};
//		String[] prompts = {"1.1", "1.2", "1.3", "1.4", "1.5", "1.6", "1.7", "2.1", "2.2", "2.3", "2.4", "2.5", "2.6", "2.7", 
//				"3.1", "3.2", "3.3", "3.4", "3.5", "3.6", "3.7", "4.1", "4.2", "4.3", "4.4", "4.5", "5.1", "5.2", "5.3", "5.4", 
//				"6.1", "6.2", "6.3", "6.4", "6.5", "6.6", "6.7", "7.1", "7.2", "7.3", "7.4", "7.5", "7.6", "7.7", "8.1", "8.2", 
//				"8.3", "8.4", "8.6", "8.7", "9.1", "9.2", "9.3", "9.4", "9.6", "10.1", "10.2", "10.3", "10.4", "10.5", "10.6", 
//				"10.7", "11.1", "11.2", "11.3", "11.4", "11.5", "11.6", "11.7", "11.8", "11.9", "11.10", "12.1", "12.2", "12.4", 
//				"12.5", "12.6", "12.7", "12.8", "12.9", "12.10"};
//		String[] prompts = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11","12"}; //assignments
//		String[] prompts = {"COSC120274"};
		for(int k=0; k < prompts.length; k++){
			Driver dr = new Driver();
			//Step 1:
			//get the rubric text as a set of sentence segments, and top-scoring responses
			//rubricSegments have the top scoring essays for PARCC since there are no sample respones
			String[] rubricSegments = dr.readFromFile("/Users/lakshmiramachandran/Documents/pearson-datasets/Colorado/Operational Data/autoregexes/"+prompts[k]+"-prompt-stimulus.csv", 1);
//			String[] rubricSegments = dr.readFromFile("/Users/lakshmiramachandran/Documents/Kaggle/ASAP-SAS/regexes/"+prompts[k]+"-rubric.csv", 1);
			//topScoringResponses has top two scored essays
			String[] topScoringResponses = dr.readFromFile("/Users/lakshmiramachandran/Documents/pearson-datasets/Colorado/Operational Data/Attempt01/traintestsets/"+prompts[k]+"-spell-topscorers.csv", 1);
//			String[] topScoringResponses = dr.readFromFile("/Users/lakshmiramachandran/Documents/Kaggle/ASAP-SAS/regexes/"+prompts[k]+"-topscorers.csv", 1);
			//if prompt-stimulus text is available
			//String[] promptStimulusText = dr.readFromFile("/Users/lakshmiramachandran/Documents/pearson-datasets/Colorado/Operational Data/autoregexes/"+prompts[k]+"-prompt-stimulus.csv", 1);
//			String[] promptStimulusText = dr.readFromFile("/Users/lakshmiramachandran/Documents/pearson-datasets/Maryland/"+prompts[k]+"/Set"+prompts[k]+"-prompt-stimulus.csv", 1);
			
			//Step 2:
			//extract long phrases from the text (edges from the word-order graph generated)
			ExtractPhrases extractphr = new ExtractPhrases();
			ArrayList<String> rubricPhrases = extractphr.extractPhrasesFromText(rubricSegments, posTagger, parser);
			//System.out.println(rubricPhrases);
			
			//eliminate or replace stop-words
			//Step 3A:
			//clean the rubric responses and top-scoring answers -- eliminate stopwords
			ElimOrReplaceStopWords elim = new ElimOrReplaceStopWords();
			rubricSegments = elim.eliminateStopWords(rubricSegments);
			topScoringResponses = elim.eliminateStopWords(topScoringResponses);
			//promptStimulusText = elim.eliminateStopWords(promptStimulusText);
			
			//Step 3B:
			//replace stop-words with \\w{0,4} in the extracted rubric phrases
			rubricPhrases = elim.replaceStopWords(rubricPhrases);
			
			//Step 4: Tokenize rubric and top-scoreres' text
			Tokenize tok = new Tokenize();
			ArrayList<ArrayList<String>> rubricTokens = tok.tokenizeRubric(rubricSegments);
			ArrayList<String> topScoringTokens = tok.tokenizeTopScorers(topScoringResponses);
			//ArrayList<String> promptStimulusTokens = tok.tokenizeTopScorers(promptStimulusText);
			//adding prompt-stimulus text tokens to set of top scoring tokens
			//topScoringTokens.addAll(promptStimulusTokens);
			
			//Step 5: Select most frequent words among the topscorers' and prompt/stimulus texts' tokens
			//so comparison of the rubric text is with a subset of tokens only
			FrequentTokens ft = new FrequentTokens();
			topScoringTokens = ft.getFrequentTokens(topScoringTokens);
			
			//step 6A:
			//Identify equivalence classes for the tokens in the rubric text (using semantic relatedness metrics include spelling mistakes)
			String writeToFile = "/Users/lakshmiramachandran/Documents/pearson-datasets/Colorado/Operational Data/Attempt01/traintestsets/regex-phrases-spell-"+prompts[k]+".csv";
			PrintWriter csvWriter = new PrintWriter(new FileWriter(writeToFile));
			GenerateEquivalenceClasses genEqClass = new GenerateEquivalenceClasses();
			ArrayList finalListOfTokenClasses = new ArrayList();
			System.out.println("# of rubric segments: "+rubricTokens.size());
			for(int i = 0; i < rubricTokens.size(); i++){//every element contains tokens from each rubric segment
			  System.out.println("rubricSegments["+i+"]: "+rubricTokens.get(i));
			  finalListOfTokenClasses = genEqClass.identifyClassesOfWords(rubricTokens.get(i), topScoringTokens, finalListOfTokenClasses, posTagger);
			  System.out.println("finalListOfTokenClasses: "+finalListOfTokenClasses);
			}
			
			//step 6B: 
			//iterate over the extracted phrases in the rubric texts
			ArrayList<String> outputRubricPhrases = new ArrayList<String>();
			System.out.println("rubric phrases: "+rubricPhrases.size());
			for(int i = 0; i < rubricPhrases.size(); i++){
			  System.out.println("rubricPhrases.get("+i+"): "+rubricPhrases.get(i));
			  finalListOfTokenClasses.add(genEqClass.identifyClassesOfPhrases(rubricPhrases.get(i), topScoringTokens, posTagger));
			}
			
			//step 7:
			//writing out the results, converting to Perl regex format
			for(int i = 0; i < finalListOfTokenClasses.size(); i++){
				if(finalListOfTokenClasses.get(i) == null)
						continue;
				String temp = finalListOfTokenClasses.get(i).toString();
				System.out.println("temp before: "+temp);
				if(temp.contains("] @@ ["))
					temp = temp.replace("] @@ [", ").*)(?=.*(");
				if(temp.contains("]["))
					temp = temp.replace("][", ").*)(?=.*(");
				if(temp.contains("@@ ["))
					temp = temp.replace("@@ [", "(?=.*(");
				if(temp.contains("} "))
					temp = temp.replace("} ", "}");
				if(temp.contains(") "))
					temp = temp.replace(") ", ")");
				if(temp.contains("] "))
					temp = temp.replace("] ", "]");
				temp = temp.replace("]", ").*)");
				temp = temp.replace("[", "(?=.*(");
				temp = temp.replace(", ", "|");
				System.out.println("temp after: "+temp);
				csvWriter.append(temp); 
				csvWriter.append("\n");
			}
			csvWriter.close();
		}
	}
	
	public String[] readFromFile(String filename, int flag) throws IOException{
		String[] segments = new String[MAX];
		BufferedReader reader = new BufferedReader(new FileReader(filename));
		String temp = "";
		int i = 0;
		StringTokenizer st;
		while((temp = reader.readLine()) != null){
			//if(i > 0){ //skipping the header in the .csv file
				if(flag == 0){
					st = new StringTokenizer(temp, ",");
					st.nextToken(); st.nextToken(); st.nextToken();
					segments[i] = st.nextToken();
				} else{
					segments[i] = temp;
				}
				//System.out.println("i-1: "+(i-1)+" -- "+segments[i-1]);
			//}
			i=i+1;
		}
		segments = Arrays.copyOf(segments, i);
		return segments;
	}
}