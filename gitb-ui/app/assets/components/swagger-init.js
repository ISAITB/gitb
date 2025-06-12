window.onload = function() {
  const swaggerDivId = 'swagger-ui';
  const urlToLoad = document.getElementById(swaggerDivId).getAttribute('data-api-url');
  const ui = SwaggerUIBundle({
    url: urlToLoad,
    dom_id: "#"+swaggerDivId
  });
};