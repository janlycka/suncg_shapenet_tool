package importer.isospace;


import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import org.apache.uima.UimaContext;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.texttechnologylab.annotation.semaf.isobase.Entity;
import org.texttechnologylab.annotation.type.QuickTreeNode;

import java.util.ArrayList;
import java.util.HashMap;

import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.fit.util.JCasUtil.selectCovered;

public class MoveTokenAndNodesToView extends JCasAnnotator_ImplBase
{

    @Override
    public void initialize(UimaContext aContext)
        throws ResourceInitializationException
    {
        super.initialize(aContext);
    }

    @Override
    public void process(JCas aJCas)
    {
		JCas goldcas = null;
		try {
			goldcas = aJCas.getView("SpaceEvalGold");
		} catch (CASException e) {
			e.printStackTrace();
		}

		for (Token se : select(aJCas, Token.class))
    	{
			Token qtn = new Token(goldcas);
			qtn.setBegin(se.getBegin());
			qtn.setEnd(se.getEnd());
			qtn.addToIndexes();
    	}

		for (Sentence se : select(aJCas, Sentence.class))
		{
			Sentence qtn = new Sentence(goldcas);
			qtn.setBegin(se.getBegin());
			qtn.setEnd(se.getEnd());
			qtn.addToIndexes();
		}

		for (QuickTreeNode se : select(aJCas, QuickTreeNode.class)) {
			QuickTreeNode qtn = new QuickTreeNode(goldcas);
			qtn.setBegin(se.getBegin());
			qtn.setEnd(se.getEnd());
			qtn.addToIndexes();
		}
    }
}