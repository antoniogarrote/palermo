<footer id="footer">
  <div class="container">
    <p>Powered by <a href="http://github.com/antoniogarrote/palermo">Palermo</a> v0.0.1 (a job processing system built with &#10084;)</p>
    <p>        
    Connected to <img src="assets/images/rabbit.png" style="width: 15px; margin-top: -8px"></img>
    <a href="http://<%= host %>:15672/#/exchanges/%2F/<%= exchange %>">
        <%= host %>:<%= port %>/<%= exchange %>
    </a></p>
  </div>
</footer>

</body>
</html>