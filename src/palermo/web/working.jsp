  <%
    java.util.HashMap workersQueues = palermo.getWorkersInfo();

    int numWorkers = 0;
    for(Object queue : workersQueues.keySet()) {
      java.util.ArrayList workers = (java.util.ArrayList) workersQueues.get(queue);
      numWorkers = numWorkers + workers.size();
    }
  %>
  <h1>Workers</h1>
  <p class="intro">The list below contains all workers which are currently running a job.</p>
  <table class="table table-bordered workers">
    <tr>
      <th>Where</th>
      <th>Queue</th>
      <th>Tag</th>
    </tr>
    <% if(numWorkers == 0) { %>
    <tr>
      <td colspan="4" class="no-data">Nothing is happening right now...</td>
    </tr>
    <% } %>

    <% for(Object queue : workersQueues.keySet()) { %>
    <% 
       String queueName = (String) queue;
       java.util.ArrayList<java.util.HashMap> workers = (java.util.ArrayList<java.util.HashMap>) workersQueues.get(queue);
     %>
     <% for(Object workerInfo : workers) { %>
      <tr>
        <%    
           java.util.HashMap workerInfoHash = (java.util.HashMap) workerInfo;
           String hostName = (String) ((java.util.HashMap) workerInfoHash.get("channel_details")).get("peer_host");
           String tag = (String) workerInfoHash.get("consumer_tag");
        %>
        <td class="where"><%= hostName %></td>
        <td class="queues queue">
          <%= queueName %>
        </td>
        <td class="process">
            <code><%= tag %></code>
        </td>
      </tr>
    <% } %>
    <% } %>
  </table>
