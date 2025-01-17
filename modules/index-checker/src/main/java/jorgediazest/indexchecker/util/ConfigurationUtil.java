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

package jorgediazest.indexchecker.util;

import com.liferay.petra.string.CharPool;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.util.ReleaseInfo;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.io.IOException;
import java.io.InputStream;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jorgediazest.indexchecker.index.IndexSearchHelper;
import jorgediazest.indexchecker.model.IndexCheckerPermissionsHelper;
import jorgediazest.indexchecker.model.IndexCheckerQueryHelper;

import jorgediazest.util.model.Model;
import jorgediazest.util.service.Service;
import jorgediazest.util.service.ServiceUtil;

import org.yaml.snakeyaml.Yaml;

/**
 * @author Jorge Díaz
 */
public class ConfigurationUtil {

	public static Object getConfigurationEntry(String configurationEntry) {
		Map<String, Object> configuration = getConfiguration();

		if (configuration == null) {
			return null;
		}

		return configuration.get(configurationEntry);
	}

	public static int getDefaultNumberThreads() {
		return PortletPropsValues.NUMBER_THREADS;
	}

	@SuppressWarnings("unchecked")
	public static Collection<String> getExactAttributesToCheck(Model model) {
		return (Collection<String>)getModelInfo(
			model, "exactAttributesToCheck");
	}

	public static String getIndexAttributeName(Model model, String attribute) {
		@SuppressWarnings("unchecked")
		Map<String, Object> indexAttributeNameMapping =
			(Map<String, Object>)getModelInfo(
				model, "indexAttributeNameMapping");

		String indexAttribute = (String)indexAttributeNameMapping.get(
			attribute);

		if ((indexAttribute == null) &&
			Objects.equals(attribute, model.getPrimaryKeyAttribute())) {

			indexAttribute = (String)indexAttributeNameMapping.get("pk");
		}

		if (indexAttribute == null) {
			indexAttribute = attribute;
		}

		return indexAttribute;
	}

	public static IndexSearchHelper getIndexSearchHelper(Model model) {
		return (IndexSearchHelper)getModelInfo(model, "indexSearchHelperClass");
	}

	public static boolean getJournalArticleIndexAllVersions() {
		try {
			return _getJournalArticleIndexAllVersions();
		}
		catch (Exception e) {
			if (_log.isDebugEnabled()) {
				_log.warn(
					"Error getting JournalServiceConfiguration." +
						"indexAllArticleVersionsEnabled: " + e.getMessage(),
					e);
			}
			else if (_log.isWarnEnabled()) {
				_log.warn(
					"Error getting JournalServiceConfiguration." +
						"indexAllArticleVersionsEnabled: " + e.getMessage());
			}

			return true;
		}
	}

	@SuppressWarnings("unchecked")
	public static List<String> getKeyAttributes(Model model) {
		return (List<String>)getModelInfo(model, "keyAttributes");
	}

	@SuppressWarnings("unchecked")
	public static Collection<String> getModelAttributesToQuery(Model model) {
		Collection<String> attributesToCheck = (Collection<String>)getModelInfo(
			model, "attributesToQuery");

		if (attributesToCheck == null) {
			return Collections.emptySet();
		}

		return attributesToCheck;
	}

	public static Map<String, Map<String, Object>> getModelInfo() {
		if (_modelInfo != null) {
			return _modelInfo;
		}

		synchronized (ConfigurationUtil.class) {
			if (_modelInfo == null) {
				Map<String, Map<String, Object>> modelInfoAux = new HashMap<>();

				@SuppressWarnings("unchecked")
				Collection<Map<String, Object>> modelInfoList =
					(Collection<Map<String, Object>>)getConfigurationEntry(
						"modelInfo");

				for (Map<String, Object> modelMap : modelInfoList) {
					String model = (String)modelMap.get("model");

					modelInfoAux.put(model, modelMap);
				}

				_modelInfo = modelInfoAux;
			}

			return _modelInfo;
		}
	}

	public static Object getModelInfo(Model model, String entry) {
		Object value = _getModelInfo(model.getClassName(), entry);

		if (value != null) {
			return value;
		}

		if (model.isWorkflowEnabled()) {
			value = _getModelInfo("workflowedModel", entry);

			if (value != null) {
				return value;
			}
		}

		if (model.isResourcedModel()) {
			value = _getModelInfo("resourcedModel", entry);

			if (value != null) {
				return value;
			}
		}

		return _getModelInfo("default", entry);
	}

	public static IndexCheckerPermissionsHelper getPermissionsHelper(
		Model model) {

		return (IndexCheckerPermissionsHelper)getModelInfo(
			model, "permissionsHelperClass");
	}

	public static IndexCheckerQueryHelper getQueryHelper(Model model) {
		return (IndexCheckerQueryHelper)getModelInfo(model, "queryHelperClass");
	}

	public static List<Map<String, Object>> getRelatedDataToQuery(Model model) {
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> relatedDataToQueryList =
			(List<Map<String, Object>>)getModelInfo(
				model, "relatedDataToQuery");

		if (relatedDataToQueryList == null) {
			return Collections.emptyList();
		}

		return relatedDataToQueryList;
	}

	public static String getStringFilter(Model model) {
		return (String)getModelInfo(model, "filter");
	}

	public static boolean ignoreClassName(String className) {
		if (Validator.isNull(className)) {
			return true;
		}

		return configurationListEntryContainsValue(
			"ignoreClassNames", className);
	}

	public static boolean modelNotIndexed(String className) {
		return configurationListEntryContainsValue(
			"modelNotIndexed", className);
	}

	protected static boolean configurationListEntryContainsValue(
		String configurationEntry, String value) {

		@SuppressWarnings("unchecked")
		Collection<String> list = (Collection<String>)getConfigurationEntry(
			configurationEntry);

		return list.contains(value);
	}

	protected static Map<String, Object> getConfiguration() {
		if (_configuration != null) {
			return _configuration;
		}

		synchronized (ConfigurationUtil.class) {
			if (_configuration == null) {
				_configuration = readConfiguration(
					_CONFIGURATION_FILE_NAME, _CONFIGURATION_FILE_EXT);

				if (_configuration == null) {
					throw new RuntimeException(
						"Error reading _configuration file");
				}
			}

			return _configuration;
		}
	}

	protected static String readConfiguration(
		ClassLoader classLoader, String configurationFile) {

		try {
			InputStream inputStream = classLoader.getResourceAsStream(
				configurationFile);

			if (inputStream == null) {
				return null;
			}

			return StringUtil.read(inputStream);
		}
		catch (IOException ioException) {
			if (_log.isDebugEnabled()) {
				_log.debug(ioException, ioException);
			}
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	protected static Map<String, Object> readConfiguration(
		String configurationFileName, String configurationFileExt) {

		int liferayBuildNumber = ReleaseInfo.getBuildNumber();

		ClassLoader classLoader = ConfigurationUtil.class.getClassLoader();

		String configuration = readConfiguration(
			classLoader,
			configurationFileName + CharPool.UNDERLINE + liferayBuildNumber +
				CharPool.PERIOD + configurationFileExt);

		if (configuration == null) {
			configuration = readConfiguration(
				classLoader,
				configurationFileName + CharPool.UNDERLINE +
					((int)liferayBuildNumber / 100) + CharPool.PERIOD +
						configurationFileExt);
		}

		if (configuration == null) {
			configuration = readConfiguration(
				classLoader,
				configurationFileName + CharPool.PERIOD + configurationFileExt);
		}

		String journalArticleIndexPrimaryKeyAttribute;

		if (getJournalArticleIndexAllVersions()) {
			journalArticleIndexPrimaryKeyAttribute = "pk";
		}
		else {
			journalArticleIndexPrimaryKeyAttribute = "resourcePrimKey";
		}

		configuration = configuration.replace(
			"$$JOURNAL_ARTICLE_INDEX_PRIMARY_KEY_ATTRIBUTE$$",
			journalArticleIndexPrimaryKeyAttribute);

		Yaml yaml = new Yaml();

		return (Map<String, Object>)yaml.load(configuration);
	}

	private static boolean _getJournalArticleIndexAllVersions()
		throws Exception {

		Service journalArticleService = ServiceUtil.getService(
			"com.liferay.journal.model.JournalArticle");

		return (boolean)IndexCheckerUtil.getCompanyConfigurationKey(
			CompanyThreadLocal.getCompanyId(),
			journalArticleService.getClassLoader(),
			"com.liferay.journal.configuration.JournalServiceConfiguration",
			"indexAllArticleVersionsEnabled");
	}

	private static Object _getModelInfo(String modelName, String key) {
		Map<String, Object> modelMap = getModelInfo().get(
			modelName.split("#")[0]);

		if (modelMap == null) {
			return null;
		}

		return modelMap.get(key);
	}

	private static final String _CONFIGURATION_FILE_EXT = "yml";

	private static final String _CONFIGURATION_FILE_NAME = "configuration";

	private static Log _log = LogFactoryUtil.getLog(ConfigurationUtil.class);

	private static Map<String, Object> _configuration = null;
	private static Map<String, Map<String, Object>> _modelInfo = null;

}