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

import com.liferay.portal.kernel.dao.orm.Criterion;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.ProjectionFactoryUtil;
import com.liferay.portal.kernel.dao.orm.ProjectionList;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portlet.documentlibrary.model.DLFileVersion;

import java.util.List;
import java.util.Map;

import jorgediazest.indexchecker.data.Data;

import jorgediazest.util.model.Model;

/**
 * @author Jorge Díaz
 */
public class DLFileEntry extends IndexCheckerModel {

	public Map<Long, Data> getLiferayData(Criterion filter) throws Exception {
		Map<Long, Data> dataMap = super.getLiferayData(filter);

		Model modelDLFileVersion = this.getModelFactory().getModelObject(
			null, DLFileVersion.class.getName());

		DynamicQuery queryDLFileVersion =
			modelDLFileVersion.getService().newDynamicQuery();

		ProjectionList projectionList = ProjectionFactoryUtil.projectionList();
		projectionList.add(
			modelDLFileVersion.getPropertyProjection("fileEntryId"));
		projectionList.add(modelDLFileVersion.getPropertyProjection("status"));

		queryDLFileVersion.setProjection(projectionList);

		queryDLFileVersion.add(filter);

		@SuppressWarnings("unchecked")
		List<Object[]> results =
			(List<Object[]>)modelDLFileVersion.getService().executeDynamicQuery(
				queryDLFileVersion);

		for (Object[] result : results) {
			long fileEntryId = (Long)result[0];
			int status = (Integer)result[1];

			if ((status == WorkflowConstants.STATUS_APPROVED) ||
				(status == WorkflowConstants.STATUS_IN_TRASH)) {

				dataMap.get(fileEntryId).setStatus(status);
			}
			else {
				dataMap.remove(fileEntryId);
			}
		}

		return dataMap;
	}

}