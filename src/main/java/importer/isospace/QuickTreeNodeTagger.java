package importer.isospace;


import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.fit.util.JCasUtil.selectCovered;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.texttechnologylab.annotation.semaf.isobase.Entity;
import org.texttechnologylab.annotation.semaf.isobase.Link;
import org.texttechnologylab.annotation.semaf.isospace.Location;
import org.texttechnologylab.annotation.semaf.isospace.Place;
import org.texttechnologylab.annotation.semaf.isospace.Path;
import org.texttechnologylab.annotation.semaf.isospace.SpatialEntity;
import org.texttechnologylab.annotation.semaf.isospace.SRelation;
import org.texttechnologylab.annotation.semaf.isospace.Measure;
import org.texttechnologylab.annotation.semaf.isospace.MRelation;
import org.texttechnologylab.annotation.semaf.isospace.Motion;
import org.texttechnologylab.annotation.semaf.isospace.NonMotionEvent;
import org.texttechnologylab.annotation.type.QuickTreeNode;


import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import eu.openminted.share.annotations.api.Component;
import eu.openminted.share.annotations.api.DocumentationResource;
import eu.openminted.share.annotations.api.constants.OperationType;

public class QuickTreeNodeTagger extends JCasAnnotator_ImplBase
{
 
	private int errorcount;
    @Override
    public void initialize(UimaContext aContext)
        throws ResourceInitializationException
    {
        super.initialize(aContext);
        errorcount = 0;
    }

	private HashMap<Integer, QuickTreeNode> startMap;
	private HashMap<Integer, QuickTreeNode> endMap;
	
    @Override
    public void process(JCas aJCas)
    {
    	startMap = new HashMap<>();
    	endMap = new HashMap<>();

    	createQUickTreeNodes4Token(aJCas);
        
    	for (Entity se : select(aJCas, Entity.class))
    	{
    		int start = se.getBegin();
    		int end = se.getEnd();
    		merge(start, end, aJCas);
    	}

    	System.out.println(errorcount);
    	removeToken(aJCas);
    	regenerateToken(aJCas);
    }
    
    private void merge(int start, int end, JCas aJCas) {
    	
    	//Skip empty token
		if(end == 0) {
			return;
		}
		start = testaround(start,startMap);
		end = testaround(end,endMap);
		
		if(!startMap.containsKey(start)) {
			System.out.println("Split Start");
			splittoken(start,aJCas);
		}
		if(!endMap.containsKey(end)) {
			System.out.println("Split End");
			splittoken(end+1,aJCas);
		}
		
		
		
		if(startMap.containsKey(start) && endMap.containsKey(end)) {
			if(startMap.get(start) != endMap.get(end)) {
				System.out.println("Multitoken detected!");
				
				ArrayList<QuickTreeNode> childList = new ArrayList<>();
				for(int i = start; i <= end; i++) {
					if(startMap.containsKey(i)) {
						childList.add(startMap.get(i));
					}
					assert childList.size() > 1;
				}
				
				QuickTreeNode multitoken = new QuickTreeNode(aJCas);
				multitoken.setBegin(start);
				multitoken.setEnd(end);
				System.out.println(start);
				FSArray childArray = new FSArray(aJCas, childList.size());
				int ccount = 0;
				for (QuickTreeNode child: childList) {
					child.setParent(multitoken);
					childArray.set(ccount, child);
					ccount++;
				}
				multitoken.setChildren(childArray);
				multitoken.addToIndexes();
			}
		}else {
			System.out.println("How??!?");
			System.exit(0);
		}
		
    }
    
    private int testaround(int value, HashMap<Integer, QuickTreeNode> testmap) {
    	if(!testmap.containsKey(value)) {
    		if(testmap.containsKey(value+1)) {
    			System.out.println("Could resolve +1");
    			return value + 1;
    		}else if(testmap.containsKey(value-1)) {
    			System.out.println("Could resolve -1");
    			return value - 1;
    		}else if(testmap.containsKey(value+2)) {
    			System.out.println("Could resolve +2");
    			return value + 2;
    		}else if(testmap.containsKey(value-2)) {
    			System.out.println("Could resolve -2");
    			return value - 2;
    		}
    	}
    	return value;
    }
    
    private void splittoken(int midvalue, JCas aJCas) {
    	int start = midvalue;
    	System.out.println(start);
    	while(!startMap.containsKey(start)) {
    		start--;
    		assert start < 0;
    	}
    	QuickTreeNode nodetosplit = startMap.get(start);
    	System.out.println(nodetosplit.toString());
    	int end = nodetosplit.getEnd();
    	
    	nodetosplit.setEnd(midvalue-1);
    	
    	QuickTreeNode splitnode = new QuickTreeNode(aJCas);
    	splitnode.setBegin(midvalue);
    	splitnode.setEnd(end);
    	splitnode.addToIndexes();
    	
    	System.out.println(nodetosplit.toString());
    	System.out.println(splitnode.toString());
    	//Cleenup
    	endMap.remove(end);
    	endMap.put(midvalue-1, nodetosplit);
    	
    	endMap.put(end, splitnode);
    	startMap.put(midvalue, splitnode);
    }
    
    private void createQUickTreeNodes4Token(JCas aJCas) {
    	for (Sentence uimaSentence : select(aJCas, Sentence.class)) {
	    	for (Token token : selectCovered(Token.class, uimaSentence)) 
	    	{
	    		QuickTreeNode qtn = new QuickTreeNode(aJCas);
	    		startMap.put(token.getBegin(), qtn);
	    		qtn.setBegin(token.getBegin());

	    		endMap.put(token.getEnd(), qtn);
	    		qtn.setEnd(token.getEnd());
	    		qtn.addToIndexes();
	    	}
    	}
    }
    
    private void removeToken(JCas aJCas){
    	for (Token token : select(aJCas, Token.class)) 
    	{
    		token.removeFromIndexes();
    	}
    }
    
    public void regenerateToken(JCas aJCas) {
    	for (QuickTreeNode node : select(aJCas, QuickTreeNode.class)) {
    		if(node.getChildren() == null) {
    			Token token = new Token(aJCas, node.getBegin(), node.getEnd());
    			token.addToIndexes();
    		}
    	}
    }
}