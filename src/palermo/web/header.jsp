<!DOCTYPE html>
<html lang="en">
<%@ page import="palermo.Server" %>
<head>
  <meta charset="utf-8">
  <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
  <meta name="description" content="">
  <meta name="viewport" content="width=device-width">
  <title>Palermo.</title>
  <link href="assets/stylesheets/application.css" media="all" rel="stylesheet" />
  <link href="assets/stylesheets/bootstrap.css" media="all" rel="stylesheet" />
  <link href="assets/stylesheets/fontawesome.css" media="all" rel="stylesheet" />
  <link href="assets/stylesheets/bootstrap_and_overrides.css" media="all" rel="stylesheet" />
  <script src="assets/javascripts/jquery.js"></script>
  <script src="assets/javascripts/jquery_ujs.js"></script>
  <script src="assets/javascripts/bootstrap-transition.js"></script>
  <script src="assets/javascripts/bootstrap-alert.js"></script>
  <script src="assets/javascripts/bootstrap-modal.js"></script>
  <script src="assets/javascripts/bootstrap-dropdown.js"></script>
  <script src="assets/javascripts/bootstrap-scrollspy.js"></script>
  <script src="assets/javascripts/bootstrap-tab.js"></script>
  <script src="assets/javascripts/bootstrap-tooltip.js"></script>
  <script src="assets/javascripts/bootstrap-popover.js"></script>
  <script src="assets/javascripts/bootstrap-button.js"></script>
  <script src="assets/javascripts/bootstrap-collapse.js"></script>
  <script src="assets/javascripts/bootstrap-carousel.js"></script>
  <script src="assets/javascripts/bootstrap-typeahead.js"></script>
  <script src="assets/javascripts/bootstrap-affix.js"></script>
  <script src="assets/javascripts/bootstrap.js"></script>
  <script src="assets/javascripts/bootstrap-resque.js"></script>
  <script src="assets/javascripts/failure.js"></script>
  <script src="assets/javascripts/jquery.relative-date.js"></script>
  <script src="assets/javascripts/polling.js"></script>
  <script src="assets/javascripts/relative_date.js"></script>
  <script src="assets/javascripts/application.js"></script>
</head>
<body>
<%
  String host     = application.getInitParameter("palermo.host");
  int port        = Integer.parseInt(application.getInitParameter("palermo.port"));
  String username = application.getInitParameter("palermo.username");
  String password = application.getInitParameter("palermo.password");
  String vhost    = application.getInitParameter("palermo.vhost");
  String exchange = application.getInitParameter("palermo.exchange");

  Server palermo = new Server(host, port, username, password, exchange, vhost);
%>


<div class="navbar navbar-inverse navbar-fixed-top">
  <div class="navbar-inner">
    <div class="container">
      <a class="btn btn-navbar navbar-toggle" data-toggle="collapse" data-target=".nav-collapse">
        <span class="icon-bar"></span>
        <span class="icon-bar"></span>
        <span class="icon-bar"></span>
      </a>
      <a href='.' class="brand navbar-brand">Palermo</a>
      <div class="nav-collapse navbar-collapse collapse">
        <ul class="nav navbar-nav">
            <li><a href="index.jsp">Overview</a></li>
            <li><a href="#">Working</a></li>
            <li><a href="#">Failures</a></li>
            <li><a href="#">Queues</a></li>
            <li><a href="#">Workers</a></li>
            <li><a href="#">Stats</a></li>
        </ul>
      </div>
    </div>
  </div>
</div>