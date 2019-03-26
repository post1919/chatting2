<%--@elvariable id="sessions" type="java.util.List<com.wrox.chat.ChatSession>"--%>

<template:basic htmlTitle="Support Chat" bodyTitle="Support Chat Requests">
    <c:choose>
        <c:when test="${fn:length(sessions) == 0}">
            <i>There are no pending support chat requests.</i>
        </c:when>
        <c:otherwise>
            Click on a chat request to accept it:<br /><br />
            <c:forEach var="session" items="${sessions}">
                <a href="javascript:void 0;"
                   onclick="join(${session.sessionId});">${session.userName}</a><br />
            </c:forEach>
        </c:otherwise>
    </c:choose>
    
    <script>
        var join = function(id) {
            postInvisibleForm(
            	  '<c:url value="/chat" />'
            	, {
					  action: 'join'
					, chatSessionId: id
            	}
            );
        };
    </script>
    
</template:basic>
