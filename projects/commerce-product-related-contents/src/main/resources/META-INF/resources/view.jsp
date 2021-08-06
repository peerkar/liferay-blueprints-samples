<%--
/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of the Liferay Enterprise
 * Subscription License ("License"). You may not use this file except in
 * compliance with the License. You can obtain a copy of the License by
 * contacting Liferay, Inc. See the License for the specific language governing
 * permissions and limitations under the License, including but not limited to
 * distribution rights of the Software.
 *
 *
 *
 */
--%>

<%@ include file="/init.jsp" %>

<%
JSONArray hitsJSONArray = (JSONArray)request.getAttribute("hits");

if (hitsJSONArray == null) {
	return;
}
%>

<h1><liferay-ui:message key="related-contents" /></h1>

<ul class="list">

	<%
	for (int i = 0; i < hitsJSONArray.length(); i++) {
		JSONObject hitJsonObject = hitsJSONArray.getJSONObject(i);
	%>

		<li>
			<a href="<%= hitJsonObject.getString("b_viewURL") %>"><%= hitJsonObject.getString("b_title") %></a>
		</li>

	<%
	}
	%>

</ul>