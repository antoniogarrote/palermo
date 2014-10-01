(function() {
  jQuery(function() {
    var format_toggler, relatizer;
    relatizer = function() {
      var dt, relatized;
      dt = $(this).text();
      $(this).relativeDate();
      relatized = $(this).text();
      if ($(this).parents("a").size() > 0 || $(this).is("a")) {
        $(this).relativeDate();
        if (!$(this).attr("title")) {
          return $(this).attr("title", dt);
        }
      } else {
        return $(this).html("<a href='#'' class='toggle_format' title='" + dt + "'>\n  <span class='date_time'>" + dt + "</span>\n  <span class='relatized_time'>" + relatized + "</span>\n</a>");
      }
    };
    format_toggler = function(e) {
      e.preventDefault();
      $(".time a.toggle_format span").toggle();
      return $(this).attr("title", $("span:hidden", this).text());
    };
    $(".time").each(relatizer);
    $(".time a.toggle_format .date_time").hide();
    return $(".time").on("click", "a.toggle_format", format_toggler);
  });

}).call(this);
