steal.plugins('jqueryui/sortable').css('composer').then(function($) {

    $.Controller("Editor.Composer",

    /* @prototype */
    {
     /**
     * Initialize a new MenuBar controller.
     */
        init: function(el) {
          $(el).sortable({connectWith: ".composer", distance: 15});

		$(el).bind('sortupdate', function(event, ui){
			
		});

		$(el).find('div.pagelet').hover(function() {
		//log('pagelet hover in');
		$(this).append('<div class="icon_editing"></div>');
		}, function() {
		// log('pagelet hover out');
		$(this).find('div.icon_editing').remove();
		});
        }
    });

});