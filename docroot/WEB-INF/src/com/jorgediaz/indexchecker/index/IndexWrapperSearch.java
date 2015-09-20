package com.jorgediaz.indexchecker.index;

import com.jorgediaz.indexchecker.data.Data;
import com.jorgediaz.indexchecker.model.BaseModelIndexChecker;
import com.liferay.portal.kernel.search.BooleanQuery;
import com.liferay.portal.kernel.search.BooleanQueryFactoryUtil;
import com.liferay.portal.kernel.search.Document;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.search.Hits;
import com.liferay.portal.kernel.search.SearchContext;
import com.liferay.portal.kernel.search.SearchEngineUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class IndexWrapperSearch extends IndexWrapper {

	private long companyId;

	public IndexWrapperSearch(long companyId) {
		this.companyId = companyId;
	}

	@Override
	public Set<String> getTermValues(String term) {
		// TODO Pendiente
		Set<String> values=new HashSet<String>();
		values.add("Only implemented for 'Lucene'...");
		return values;
	}

	@Override
	public int numDocs() {
		return -1;
	}

	@Override
	public Set<Data> getClassNameData(BaseModelIndexChecker modelClass) {

		Set<Data> indexData = new HashSet<Data>();

		SearchContext searchContext = new SearchContext();
		searchContext.setCompanyId(companyId);
		BooleanQuery contextQuery = BooleanQueryFactoryUtil.create(searchContext);
		contextQuery.addRequiredTerm(Field.COMPANY_ID, companyId);
		contextQuery.addRequiredTerm(Field.ENTRY_CLASS_NAME, modelClass.getFullClassName());

		try {
			Hits hits = SearchEngineUtil.search(searchContext, contextQuery);
	
			Document[] docs = hits.getDocs();
			
			if (docs != null) {
				
				for (int i = 0; i < docs.length; i++) {
					
					DocumentWrapper doc = new DocumentWrapperSearch(docs[i]);
	
					String entryClassName = doc.getEntryClassName();
	
					if(entryClassName != null && entryClassName.equals(modelClass.getFullClassName()))
					{
						Data data = new Data(modelClass);
						data.init(doc);

						indexData.add(data);
					}
				}
			}
		}
		catch(Exception e) {
			System.err.println("\t" + "EXCEPTION: " + e.getClass() + " - " + e.getMessage());
		}
		return indexData;
	}


	@Override
	public Map<Long,Set<Data>> getClassNameDataByGroupId(BaseModelIndexChecker modelClass) {
		Map<Long,Set<Data>> indexData = new HashMap<Long,Set<Data>>();

		SearchContext searchContext = new SearchContext();
		searchContext.setCompanyId(companyId);
		BooleanQuery contextQuery = BooleanQueryFactoryUtil.create(searchContext);
		contextQuery.addRequiredTerm(Field.COMPANY_ID, companyId);
		contextQuery.addRequiredTerm(Field.ENTRY_CLASS_NAME, modelClass.getFullClassName());


		try {
			Hits hits = SearchEngineUtil.search(searchContext, contextQuery);
	
			Document[] docs = hits.getDocs();
			
			if (docs != null) {
				
				for (int i = 0; i < docs.length; i++) {
					
					DocumentWrapper doc =  new DocumentWrapperSearch(docs[i]);

					String entryClassName = doc.getEntryClassName();

					if(entryClassName != null && entryClassName.equals(modelClass.getFullClassName()))
					{
						Data data = new Data(modelClass);
						data.init(doc);

						Long groupId = data.getGroupId();

						Set<Data> indexDataSet = indexData.get(groupId);
						if(indexDataSet == null) {
							indexDataSet = new HashSet<Data>();
							indexData.put(groupId, indexDataSet);
						}

						indexDataSet.add(data);
					}
	
				}
			}
		}
		catch(Exception e) {
			System.err.println("\t" + "EXCEPTION: " + e.getClass() + " - " + e.getMessage());
		}

		return indexData;
	}

}