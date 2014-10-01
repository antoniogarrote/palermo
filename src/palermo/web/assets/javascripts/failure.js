(function() {
  jQuery(function() {
    $(".backtrace").click(function(e) {
      e.preventDefault();
      return $(this).next().toggle();
    });
    return $("ul.failed li").hover(function() {
      return $(this).toggleClass("hover");
    });
  });

}).call(this);
