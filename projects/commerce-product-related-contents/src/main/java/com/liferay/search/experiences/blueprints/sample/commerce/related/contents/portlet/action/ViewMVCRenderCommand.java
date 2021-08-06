package com.liferay.search.experiences.blueprints.sample.commerce.related.contents.portlet.action;

import com.liferay.commerce.product.catalog.CPCatalogEntry;
import com.liferay.commerce.product.content.util.CPContentHelper;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCRenderCommand;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.ResourceBundleUtil;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.search.document.Document;
import com.liferay.portal.search.engine.adapter.SearchEngineAdapter;
import com.liferay.portal.search.engine.adapter.search.SearchSearchRequest;
import com.liferay.portal.search.engine.adapter.search.SearchSearchResponse;
import com.liferay.portal.search.hits.SearchHit;
import com.liferay.portal.search.hits.SearchHits;
import com.liferay.portal.search.index.IndexNameBuilder;
import com.liferay.portal.search.query.Queries;
import com.liferay.portal.search.searcher.SearchResponse;
import com.liferay.search.experiences.blueprints.engine.attributes.BlueprintsAttributes;
import com.liferay.search.experiences.blueprints.engine.attributes.BlueprintsAttributesBuilder;
import com.liferay.search.experiences.blueprints.engine.util.BlueprintsEngineHelper;
import com.liferay.search.experiences.blueprints.message.Messages;
import com.liferay.search.experiences.blueprints.model.Blueprint;
import com.liferay.search.experiences.blueprints.sample.commerce.related.contents.constants.CProductRelatedContentsPortletKeys;
import com.liferay.search.experiences.blueprints.service.BlueprintService;
import com.liferay.search.experiences.blueprints.util.attributes.BlueprintsAttributesHelper;
import com.liferay.search.experiences.searchresponse.json.translator.SearchResponseJSONTranslator;
import com.liferay.search.experiences.searchresponse.json.translator.constants.ResponseAttributeKeys;

import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import javax.portlet.PortletPreferences;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
	immediate = true,
	property = {
		"javax.portlet.name=" + CProductRelatedContentsPortletKeys.COMMERCE_PRODUCT_RELATED_CONTENTS,
		"mvc.command.name=/"
	},
	service = MVCRenderCommand.class
)
public class ViewMVCRenderCommand implements MVCRenderCommand {

	@Override
	public String render(
		RenderRequest renderRequest, RenderResponse renderResponse) {

		Blueprint blueprint = _getBlueprint(renderRequest);

		if (blueprint == null) {
			return "/error.jsp";
		}

		try {
			CPCatalogEntry cpCatalogEntry = _cpContentHelper.getCPCatalogEntry(
				_portal.getHttpServletRequest(renderRequest));

			if (cpCatalogEntry != null) {
				JSONObject hitsJSONObject = _getResults(
					renderRequest, renderResponse, blueprint,
					_getAssetCategoryIds(
						renderRequest, cpCatalogEntry.getCProductId()));

				if (hitsJSONObject != null) {
					renderRequest.setAttribute(
						"hits", hitsJSONObject.get("hits"));
				}
			}
		}
		catch (PortalException portalException) {
			_log.error(portalException.getMessage(), portalException);
		}

		return "/view.jsp";
	}

	private List<Long> _getAssetCategoryIds(
		RenderRequest renderRequest, long productId) {

		SearchSearchRequest searchSearchRequest = _getSearchRequest(
			renderRequest, productId);

		SearchSearchResponse searchSearchResponse =
			_searchEngineAdapter.execute(searchSearchRequest);

		SearchHits searchHits = searchSearchResponse.getSearchHits();

		if (searchHits.getTotalHits() > 0) {
			List<SearchHit> hits = searchHits.getSearchHits();

			SearchHit searchHit = hits.get(0);

			Document document = searchHit.getDocument();

			return document.getLongs("assetCategoryIds");
		}

		return null;
	}

	private Blueprint _getBlueprint(RenderRequest renderRequest) {
		PortletPreferences preferences = renderRequest.getPreferences();

		long blueprintId = GetterUtil.getLong(
			preferences.getValue("blueprintId", "0"));

		if (blueprintId == 0) {
			return null;
		}

		try {
			return _blueprintService.getBlueprint(blueprintId);
		}
		catch (PortalException portalException) {
			_log.error(portalException.getMessage(), portalException);
		}

		return null;
	}

	private BlueprintsAttributes _getBlueprintsRequestAttributes(
		RenderRequest renderRequest, Blueprint blueprint,
		long[] assetCategoryIds) {

		BlueprintsAttributesBuilder blueprintsAttributesBuilder =
			_blueprintsAttributesHelper.getBlueprintsRequestAttributesBuilder(
				renderRequest, blueprint);

		blueprintsAttributesBuilder.addAttribute(
			"assetCategoryIds", assetCategoryIds);

		return blueprintsAttributesBuilder.build();
	}

	private BlueprintsAttributes _getBlueprintsResponseAttributes(
		RenderRequest renderRequest, RenderResponse renderResponse,
		BlueprintsAttributes blueprintsRequestAttributes, Blueprint blueprint) {

		BlueprintsAttributesBuilder blueprintsAttributesBuilder =
			_blueprintsAttributesHelper.getBlueprintsResponseAttributesBuilder(
				renderRequest, renderResponse, blueprint,
				blueprintsRequestAttributes);

		blueprintsAttributesBuilder.addAttribute(
			ResponseAttributeKeys.INCLUDE_RESULT, true);

		return blueprintsAttributesBuilder.build();
	}

	private CPCatalogEntry _getCPCatalogEntry(RenderRequest renderRequest) {
		try {
			return _cpContentHelper.getCPCatalogEntry(
				_portal.getHttpServletRequest(renderRequest));
		}
		catch (PortalException portalException) {
			_log.error(portalException.getMessage(), portalException);
		}

		return null;
	}

	private ResourceBundle _getResourceBundle(RenderRequest renderRequest) {
		ThemeDisplay themeDisplay = (ThemeDisplay)renderRequest.getAttribute(
			WebKeys.THEME_DISPLAY);

		return ResourceBundleUtil.getBundle(
			"content.Language", themeDisplay.getLocale(), getClass());
	}

	private JSONObject _getResults(
		RenderRequest renderRequest, RenderResponse renderResponse,
		Blueprint blueprint, List<Long> assetCategoryIds) {

		if (assetCategoryIds == null) {
			return null;
		}

		try {
			Messages messages = new Messages();

			Stream<Long> stream = assetCategoryIds.stream();

			BlueprintsAttributes blueprintsRequestAttributes =
				_getBlueprintsRequestAttributes(
					renderRequest, blueprint,
					stream.mapToLong(
						l -> l
					).toArray());

			SearchResponse searchResponse = _blueprintsEngineHelper.search(
				blueprint, blueprintsRequestAttributes, messages);

			return _translateResponse(
				renderRequest, renderResponse, blueprint,
				blueprintsRequestAttributes, searchResponse, messages);
		}
		catch (Exception exception) {
			_log.error(exception.getMessage(), exception);
		}

		return null;
	}

	private SearchSearchRequest _getSearchRequest(
		RenderRequest renderRequest, long productId) {

		ThemeDisplay themeDisplay = (ThemeDisplay)renderRequest.getAttribute(
			WebKeys.THEME_DISPLAY);

		SearchSearchRequest searchSearchRequest = new SearchSearchRequest();

		searchSearchRequest.setFetchSourceIncludes(
			new String[] {"assetCategoryIds"});
		searchSearchRequest.setIndexNames(
			_indexNameBuilder.getIndexName(themeDisplay.getCompanyId()));
		searchSearchRequest.setQuery(_queries.term("cpProductId", productId));
		searchSearchRequest.setSize(1);
		searchSearchRequest.setStart(0);

		return searchSearchRequest;
	}

	private JSONObject _translateResponse(
		RenderRequest renderRequest, RenderResponse renderResponse,
		Blueprint blueprint, BlueprintsAttributes blueprintsRequestAttributes,
		SearchResponse searchResponse, Messages messages) {

		BlueprintsAttributes blueprintsResponseAttributes =
			_getBlueprintsResponseAttributes(
				renderRequest, renderResponse, blueprintsRequestAttributes,
				blueprint);

		try {
			return _searchResponseJSONTranslator.translate(
				searchResponse, blueprint, blueprintsResponseAttributes,
				_getResourceBundle(renderRequest), messages);
		}
		catch (PortalException portalException) {
			_log.error(portalException.getMessage(), portalException);
		}

		return null;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		ViewMVCRenderCommand.class);

	@Reference
	private BlueprintsAttributesHelper _blueprintsAttributesHelper;

	@Reference
	private BlueprintsEngineHelper _blueprintsEngineHelper;

	@Reference
	private BlueprintService _blueprintService;

	@Reference
	private CPContentHelper _cpContentHelper;

	@Reference
	private IndexNameBuilder _indexNameBuilder;

	@Reference
	private JSONFactory _jsonFactory;

	@Reference
	private Portal _portal;

	@Reference
	private Queries _queries;

	@Reference
	private SearchEngineAdapter _searchEngineAdapter;

	@Reference
	private SearchResponseJSONTranslator _searchResponseJSONTranslator;

}