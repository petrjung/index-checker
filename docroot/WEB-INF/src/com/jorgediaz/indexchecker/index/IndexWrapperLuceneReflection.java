/**
 * Space for Copyright
 */

package com.jorgediaz.indexchecker.index;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.MethodKey;
import com.liferay.portal.kernel.util.PortalClassInvoker;
import com.liferay.portal.kernel.util.PortalClassLoaderUtil;

import java.lang.reflect.Method;

import java.util.HashSet;
import java.util.Set;
public class IndexWrapperLuceneReflection extends IndexWrapperLucene {

	public IndexWrapperLuceneReflection(long companyId) {
		Object indexSearcher;
		try {
			indexSearcher = IndexWrapperLuceneReflection.getIndexSearcher(
				companyId);
		}
		catch (Exception e) {
			_log.error("Error: " + e.getClass() + " - " + e.getMessage(), e);
			throw new RuntimeException(e);
		}

		try {
			getIndexReader =
				indexSearcher.getClass().getMethod("getIndexReader");
			index = getIndexReader.invoke(indexSearcher);
			/* ReadOnlyDirectoryReader => DirectoryReader => IndexReader*/
			indexReaderClass = index.getClass().getSuperclass().getSuperclass();
			termClass =
				indexReaderClass.getClassLoader().loadClass(
					"org.apache.lucene.index.Term");
			termEnumClass =
				indexReaderClass.getClassLoader().loadClass(
					"org.apache.lucene.index.TermEnum");
			terms = indexReaderClass.getMethod("terms", termClass);
			numDocs = indexReaderClass.getMethod("numDocs");
			maxDoc = indexReaderClass.getMethod("maxDoc");
			isDeleted = indexReaderClass.getMethod("isDeleted", int.class);
			document = indexReaderClass.getMethod("document", int.class);
		}
		catch (Exception e) {
			_log.error("Error: " + e.getClass() + " - " + e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public Set<String> getTermValues(String field) {

		Set<String> values = new HashSet<String>();
		try {
			Object termObj = termClass.getConstructor(
				String.class).newInstance(field);
			Object termEnum = terms.invoke(index, termObj);
			Method termMethod = termEnumClass.getMethod("term");
			Method nextMethod = termEnumClass.getMethod("next");
			Method termTextMethod = termClass.getMethod("text");
			Method termFieldMethod = termClass.getMethod("field");
			Object currTermObj = termMethod.invoke(termEnum);
			while ((currTermObj != null) &&
				   ((String)termFieldMethod.invoke(currTermObj)).equals(
					   field)) {

				values.add((String)termTextMethod.invoke(currTermObj));
				nextMethod.invoke(termEnum);
				currTermObj = termMethod.invoke(termEnum);
			}
		}
		catch (Exception e) {
			_log.error("Error: " + e.getClass() + " - " + e.getMessage(), e);
			throw new RuntimeException(e);
		}

		return values;
	}

	@Override
	public int numDocs() {
		if (index == null) {
			return -1;
		}

		try {
			return (Integer)numDocs.invoke(index);
		}
		catch (Exception e) {
			_log.error("Error: " + e.getClass() + " - " + e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}

	protected static Object getIndexSearcher(long companyId)
		throws ClassNotFoundException, Exception {

	/* We execute from Portlet:
	*		IndexSearcher indexSearcher =
	*			LuceneHelperUtil.getIndexSearcher(company.getCompanyId());
	*/

		Class<?> luceneHelperUtil =
			PortalClassLoaderUtil.getClassLoader().loadClass(
				"com.liferay.portal.search.lucene.LuceneHelperUtil");
		MethodKey getIndexSearcher = new MethodKey(
			luceneHelperUtil,"getIndexSearcher", long.class);
		return PortalClassInvoker.invoke(false, getIndexSearcher, companyId);
	}

	@Override
	protected DocumentWrapper document(int i) {
		if (index == null) {
			return null;
		}

		try {
			return new DocumentWrapperLuceneReflection(
				document.invoke(index, i));
		}
		catch (Exception e) {
			_log.error("Error: " + e.getClass() + " - " + e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}

	@Override
	protected boolean isDeleted(int i) {
		if (index == null) {
			return true;
		}

		try {
			return (Boolean)isDeleted.invoke(index, i);
		}
		catch (Exception e) {
			_log.error("Error: " + e.getClass() + " - " + e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}

	@Override
	protected int maxDoc() {
		if (index == null) {
			return 0;
		}

		try {
			return (Integer)maxDoc.invoke(index);
		}
		catch (Exception e) {
			_log.error("Error: " + e.getClass() + " - " + e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}

	protected Method document = null;
	protected Method getIndexReader = null;
	protected Class<?> indexReaderClass = null;
	protected Method isDeleted = null;
	protected Method maxDoc = null;
	protected Method numDocs = null;
	protected Class<?> termClass = null;
	protected Class<?> termEnumClass = null;
	protected Method terms = null;

	private static Log _log = LogFactoryUtil.getLog(
		IndexWrapperLuceneReflection.class);

}