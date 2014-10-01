<%@ include file="header.jsp" %>

<!--
< unless subtabs.empty? >
<ul class="nav subnav">
  <div class="container">
    < subtabs.each do |tab_name| >
      < subtab tab_name >
    < end >
  </div>
</ul>
< end >
-->

<div class="container" id="main">
<%@ include file="queues.jsp" %>
<hr>
<%@ include file="working.jsp" %>
</div>

<%@ include file="footer.jsp" %>