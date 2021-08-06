<%--
/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
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