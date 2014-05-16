// Javascript set up code to get pulldown menus for the
// Navigation bar.  Uses the Yahoo web GUI library.

YAHOO.widget.MenuItem.prototype.SUBMENU_INDICATOR_IMAGE_PATH = "/icons/sprite.png";

// Initialize and render the menu bar when it is available in the DOM
YAHOO.util.Event.onContentReady("productsandservices", function () {

       // Instantiate and render the menu bar
       var oMenuBar = new YAHOO.widget.MenuBar("productsandservices", { autosubmenudisplay: true, hidedelay: 750, lazyload: true });

       /*
          Call the "render" method with no arguments since the markup for 
          this menu already exists in the DOM.
        */
       oMenuBar.render();
});
