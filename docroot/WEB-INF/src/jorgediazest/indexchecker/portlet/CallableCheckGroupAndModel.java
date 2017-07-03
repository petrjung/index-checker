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

package jorgediazest.indexchecker.portlet;

import com.liferay.portal.kernel.dao.shard.ShardUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.security.auth.CompanyThreadLocal;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import jorgediazest.indexchecker.ExecutionMode;
import jorgediazest.indexchecker.index.IndexSearchHelper;
import jorgediazest.indexchecker.model.IndexCheckerPermissionsHelper;
import jorgediazest.indexchecker.model.IndexCheckerQueryHelper;
import jorgediazest.indexchecker.util.ConfigurationUtil;

import jorgediazest.util.data.Comparison;
import jorgediazest.util.data.ComparisonUtil;
import jorgediazest.util.data.Data;
import jorgediazest.util.data.DataComparator;
import jorgediazest.util.data.DataUtil;
import jorgediazest.util.model.Model;
import jorgediazest.util.modelquery.ModelQueryFactory;

/**
 * @author Jorge Díaz
 */
public class CallableCheckGroupAndModel implements Callable<Comparison> {

	public CallableCheckGroupAndModel(
		long companyId, List<Long> groupIds, ModelQueryFactory mqFactory,
		Model model, Set<ExecutionMode> executionMode) {

		this.companyId = companyId;
		this.groupIds = groupIds;
		this.mqFactory = mqFactory;
		this.model = model;
		this.executionMode = executionMode;
	}

	@Override
	public Comparison call() throws Exception {

		boolean showBothExact = executionMode.contains(
			ExecutionMode.SHOW_BOTH_EXACT);
		boolean showBothNotExact = executionMode.contains(
			ExecutionMode.SHOW_BOTH_NOTEXACT);
		boolean showOnlyLiferay = executionMode.contains(
			ExecutionMode.SHOW_LIFERAY);
		boolean showOnlyIndex = executionMode.contains(
			ExecutionMode.SHOW_INDEX);

		boolean oldIgnoreCase = DataUtil.getIgnoreCase();

		try {
			DataUtil.setIgnoreCase(true);

			CompanyThreadLocal.setCompanyId(companyId);

			ShardUtil.pushCompanyService(companyId);

			if (_log.isInfoEnabled()) {
				String strGroupIds = null;

				if (groupIds != null) {
					strGroupIds = Arrays.toString(groupIds.toArray());

					if (strGroupIds.length() > 100) {
						strGroupIds = strGroupIds.substring(0, 97) + "...";
					}
				}

				_log.info(
					"Model: " + model.getName() + " - CompanyId: " +
						companyId + " - GroupId: " + strGroupIds);
			}

			if ((groupIds != null) && (groupIds.size() == 0)) {
				return null;
			}

			if ((groupIds != null) && (!groupIds.contains(0L) &&
				 !model.hasAttribute("groupId"))) {

				return null;
			}

			IndexCheckerQueryHelper queryHelper =
				ConfigurationUtil.getQueryHelper(model);

			Map<Long, Data> liferayDataMap = queryHelper.getLiferayData(
				model, companyId, groupIds);

			IndexCheckerPermissionsHelper permissionsHelper =
				ConfigurationUtil.getPermissionsHelper(model);

			for (Data data : liferayDataMap.values()) {
				permissionsHelper.addPermissionsClassNameGroupIdFields(data);
			}

			queryHelper.addRelatedModelData(
				liferayDataMap, mqFactory, model, companyId, groupIds);

			for (Data data : liferayDataMap.values()) {
				permissionsHelper.addRolesFields(data);
			}

			Set<Data> liferayData = new HashSet<Data>(liferayDataMap.values());

			Set<Data> indexData;

			IndexSearchHelper indexSearchHelper =
				ConfigurationUtil.getIndexSearchHelper(model);

			if ((!showOnlyIndex && liferayData.isEmpty())||
				(indexSearchHelper == null)) {

				indexData = new HashSet<Data>();
			}
			else {
				Set<Model> relatedModels = queryHelper.calculateRelatedModels(
					model);

				DataComparator dataComparator =
					ConfigurationUtil.getDataComparator(model);

				Set<String> indexAttributesToQuery = new HashSet<String>(
					ConfigurationUtil.getModelAttributesToQuery(model));

				indexAttributesToQuery.addAll(
					ConfigurationUtil.getExactAttributesToCheck(model));

				indexData = indexSearchHelper.getIndexData(
					model, relatedModels, indexAttributesToQuery,
					dataComparator, companyId, groupIds);
			}

			return ComparisonUtil.getComparison(
				model, liferayData, indexData, showBothExact, showBothNotExact,
				showOnlyLiferay, showOnlyIndex);
		}
		catch (Throwable t) {
			return ComparisonUtil.getError(model, t);
		}
		finally {
			DataUtil.setIgnoreCase(oldIgnoreCase);

			ShardUtil.popCompanyService();
		}
	}

	private static Log _log = LogFactoryUtil.getLog(
		CallableCheckGroupAndModel.class);

	private long companyId = -1;
	private Set<ExecutionMode> executionMode = null;
	private List<Long> groupIds = null;
	private Model model = null;
	private ModelQueryFactory mqFactory = null;

}