/**
 * Copyright (c) 2015-present Jorge Díaz All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package jorgediazest.indexchecker.model;

import com.liferay.portal.kernel.dao.orm.Conjunction;
import com.liferay.portal.kernel.dao.orm.Criterion;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.ProjectionFactoryUtil;
import com.liferay.portal.kernel.dao.orm.ProjectionList;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.search.SearchException;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.workflow.WorkflowConstants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jorgediazest.indexchecker.data.Data;

import jorgediazest.util.model.Model;
import jorgediazest.util.model.ModelImpl;
import jorgediazest.util.service.Service;

/**
 * @author Jorge Díaz
 */
public class IndexCheckerModel extends ModelImpl {

	public static String[] auditedModelAttributes =
		new String[] { "companyId", "createDate", "modifiedDate"};
	public static String[] groupedModelAttributes = new String[] { "groupId" };
	public static String[] resourcedModelAttributes =
		new String[] { "resourcePrimKey" };
	public static String[] stagedModelAttributes =
		new String[] { "companyId", "createDate", "modifiedDate" };
	public static String[] workflowModelAttributes = new String[] { "status" };

	@Override
	public Model clone() {
		IndexCheckerModel model;
		try {
			model = (IndexCheckerModel)super.clone();
			model.indexedAttributes = ListUtil.copy(this.indexedAttributes);
		}
		catch (Exception e) {
			_log.error("Error executing clone");
			throw new RuntimeException(e);
		}

		return model;
	}

	public void delete(Data value) throws SearchException {
		getIndexer().delete(value.getCompanyId(), value.getUid());
	}

	public Map<Data, String> deleteAndCheck(Collection<Data> dataCollection) {

		Map<Data, String> errors = new HashMap<Data, String>();

		if (_log.isDebugEnabled()) {
			_log.debug(
				"Deleting " + dataCollection.size() + " objects of type " +
					this.getClassName());
		}

		int i = 0;

		for (Data data : dataCollection) {
			/* Delete object from index */
			try {
				this.delete(data);

				if (_log.isDebugEnabled()) {
					_log.debug("Deleting " + (i++) + " uid: " + data.getUid());
				}
			}
			catch (SearchException e) {
				errors.put(data, e.getClass() + " - " + e.getMessage());

				if (_log.isDebugEnabled()) {
					_log.debug(e.getClass() + " - " + e.getMessage(), e);
				}
			}

			/* Reindex object, perhaps we deleted it by error */
			try {
				this.reindex(data);
			}
			catch (Exception e) {
			}
		}

		return errors;
	}

	public Criterion generateQueryFilter() {
		if (!this.isWorkflowEnabled()) {
			return null;
		}

		return this.generateCriterionFilter(
			"status=" + WorkflowConstants.STATUS_APPROVED +"+" +
			"status=" + WorkflowConstants.STATUS_IN_TRASH);
	}

	public Criterion getCompanyGroupFilter(long companyId) {
		return getCompanyGroupFilter(companyId, null);
	}

	public Criterion getCompanyGroupFilter(
		long companyId, List<Long> listGroupId) {

		Conjunction conjunction = RestrictionsFactoryUtil.conjunction();

		if (this.hasAttribute("companyId")) {
			conjunction.add(getProperty("companyId").eq(companyId));
		}

		if (this.hasAttribute("groupId")) {
			conjunction.add(getProperty("groupId").in(listGroupId));
		}

		return conjunction;
	}

	public List<String> getIndexAttributes() {
		return indexedAttributes;
	}

	public Map<Long, Data> getLiferayData(Criterion filter) throws Exception {

		Map<Long, Data> dataMap = new HashMap<Long, Data>();

		DynamicQuery query = service.newDynamicQuery();

		ProjectionList projectionList =
			this.getPropertyProjection(
				indexedAttributes.toArray(new String[0]));

		query.setProjection(ProjectionFactoryUtil.distinct(projectionList));

		query.add(filter);

		@SuppressWarnings("unchecked")
		List<Object[]> results = (List<Object[]>)service.executeDynamicQuery(
			query);

		for (Object[] result : results) {
			Data data = new Data(this);
			data.init(result);
			dataMap.put(data.getPrimaryKey(), data);
		}

		return dataMap;
	}

	@Override
	public void init(
			String classPackageName, String classSimpleName, Service service)
		throws Exception {

		super.init(classPackageName, classSimpleName, service);

		this.indexedAttributes = new ArrayList<String>();

		String primaryKey = this.getPrimaryKeyAttribute();

		this.setIndexPrimaryKey(primaryKey);

		if (Validator.isNull(primaryKey)) {
			throw new RuntimeException("Missing primary key!!");
		}

		if (this.hasAttribute("companyId")) {
			this.addIndexedAttribute("companyId");
		}

		if (this.isAuditedModel()) {
			addIndexedAttributes(auditedModelAttributes);
		}

		if (this.isGroupedModel()) {
			addIndexedAttributes(groupedModelAttributes);
		}

		if (this.isResourcedModel()) {
			addIndexedAttributes(resourcedModelAttributes);
		}

		if (this.isStagedModel()) {
			addIndexedAttributes(stagedModelAttributes);
		}

		if (this.isWorkflowEnabled()) {
			addIndexedAttributes(workflowModelAttributes);
		}

		this.setFilter(this.generateQueryFilter());
	}

	public Map<Data, String> reindex(Collection<Data> dataCollection) {

		Map<Data, String> errors = new HashMap<Data, String>();

		if (_log.isDebugEnabled()) {
			_log.debug(
				"Reindexing " + dataCollection.size() + " objects of type " +
					this.getClassName());
		}

		int i = 0;

		for (Data data : dataCollection) {
			try {
				this.reindex(data);

				if (_log.isDebugEnabled()) {
					_log.debug(
						"Reindexing " + (i++) + " pk: " + data.getPrimaryKey());
				}
			}
			catch (SearchException e) {
				errors.put(data, e.getClass() + " - " + e.getMessage());

				if (_log.isDebugEnabled()) {
					_log.debug(e.getClass() + " - " + e.getMessage(), e);
				}
			}
		}

		return errors;
	}

	public void reindex(Data value) throws SearchException {
		getIndexer().reindex(getClassName(), value.getPrimaryKey());
	}

	public String toString() {
		String toString = this.getClassSimpleName()+":";

		for (String attr : this.indexedAttributes) {
			toString += " " + attr;
		}

		return toString;
	}

	protected void addIndexedAttribute(String col) {
		if (!indexedAttributes.contains(col)) {
			indexedAttributes.add(col);
		}
	}

	protected void addIndexedAttributes(String[] modelAttributes) {

		for (int i = 0; i<modelAttributes.length; i++)
		{
			this.addIndexedAttribute((modelAttributes[i]));
		}
	}

	protected void removeIndexedAttribute(String col) {
		while (indexedAttributes.contains(col)) {
			indexedAttributes.remove(col);
		}
	}

	protected void setIndexPrimaryKey(String col) {
		if (indexedAttributes.contains(col)) {
			indexedAttributes.remove(col);
		}

		indexedAttributes.add(0, col);
	}

	private static Log _log = LogFactoryUtil.getLog(IndexCheckerModel.class);

	private List<String> indexedAttributes = null;

}