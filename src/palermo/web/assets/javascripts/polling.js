(function() {
  jQuery(function() {
    var poll_interval, poll_start;
    poll_interval = 2;
    poll_start = function(el) {
      var href;
      href = el.attr("href");
      el.parent().text("Starting...");
      $("#main").addClass("polling");
      setInterval((function() {
        return $.ajax({
          dataType: "text",
          type: "get",
          url: href,
          success: function(data) {
            $("#main").html(data);
            return $("#main .time").relativeDate();
          }
        });
      }), poll_interval * 1000);
      return location.hash = "#poll";
    };
    if (location.hash === "#poll") {
      poll_start($("a[rel=poll]"));
    }
    return $("a[rel=poll]").click(function(e) {
      e.preventDefault();
      return poll_start($(this));
    });
  });

}).call(this);
