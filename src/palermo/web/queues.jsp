<h1>Queues</h1>
<p class="intro">The list below contains all the registered queues with the number of jobs currently in the queue. Select a queue from above to view all jobs currently pending on the queue.</p>

<table class="table table-bordered queues">
  <tr>
    <th>Name</th>
    <th>Jobs</th>
  </tr>
  <% java.util.HashMap queuesInfo = palermo.getQueuesInfo(); %>
  <% for(Object queueName : queuesInfo.keySet())  { %>
     <% if(!queueName.equals("failed")) { %>
      <tr>
        <% java.util.HashMap info = (java.util.HashMap) queuesInfo.get(queueName); %>
        <td class="queue"><%= queueName %></td>
        <td class="size"><%=  info.get("jobs") %></td>
      </tr>
    <% } %>
  <% } %>

  <tr class="failed first_failure">
        <% java.util.HashMap info = (java.util.HashMap) queuesInfo.get("failed"); %>
    <td class="queue failed">failed</td>
    <td class="size"><%= info != null ? info.get("jobs") : 0 %></td>
  </tr>
</table>
